import { app } from 'electron'
import { execFile } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { promisify } from 'node:util'
import AdmZip from 'adm-zip'
import type { JavaRuntime } from '../../shared/types'
import { bundledPvpJavaPath, folders, GRAAL_RUNTIME_DIR, PVP_JAVA_DIR } from '../paths'
import { updateStore } from '../store'
import { downloadFile } from './download'

const execFileAsync = promisify(execFile)

/** Optic's modern Legacy Fabric/LWJGL compatibility layer requires Java 17+. */
const MIN_JAVA_MAJOR = 17
/** Optic 1.0.0 ships on Azul Zulu Java 25 as requested. */
const TARGET_JAVA_MAJOR = 25

const KIT_MARKER = 'kit.version'
const KIT_VERSION = '25.0.4-zulu-optic'

export { bundledPvpJavaPath, PVP_JAVA_DIR, GRAAL_RUNTIME_DIR }
/** @deprecated */
export const ZULU_RUNTIME_DIR = GRAAL_RUNTIME_DIR

const GRAAL17_WIN_URL =
  'https://cdn.azul.com/zulu/bin/zulu25.36.15-ca-jre25.0.4-win_x64.zip'
const GRAAL17_WIN_ZIP = 'zulu25.36.15-ca-jre25.0.4-win_x64.zip'

let installPromise: Promise<JavaRuntime> | null = null
const describeCache = new Map<string, JavaRuntime>()

function javaCacheKey(javaPath: string): string {
  return path.normalize(javaPath)
}

export function memoryMb(): { totalMb: number; freeMb: number } {
  return {
    totalMb: Math.round(os.totalmem() / (1024 * 1024)),
    freeMb: Math.round(os.freemem() / (1024 * 1024))
  }
}

function kitRoot(): string {
  return path.join(folders.runtimes(), PVP_JAVA_DIR)
}

function graalHome(): string {
  return path.join(folders.runtimes(), GRAAL_RUNTIME_DIR)
}

function packagedKitRoot(): string {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, PVP_JAVA_DIR)
  }
  return path.join(app.getAppPath(), 'resources', PVP_JAVA_DIR)
}

function packagedZuluRoot(): string {
  if (app.isPackaged) return path.join(process.resourcesPath, GRAAL_RUNTIME_DIR)
  return path.join(app.getAppPath(), 'resources', GRAAL_RUNTIME_DIR)
}

function isOverTunedRemakePath(p: string): boolean {
  return /zulu-25|mc-pvp-java25|mc-pvp-java21|java25-pvp|zulu-21|[\\/]java25[\\/]/i.test(p)
}

async function describe(javaPath: string, source: JavaRuntime['source']): Promise<JavaRuntime | null> {
  if (!javaPath || !fs.existsSync(javaPath)) return null
  const cached = describeCache.get(javaCacheKey(javaPath))
  if (cached) return { ...cached, source }
  try {
    const { stderr, stdout } = await execFileAsync(javaPath, ['-version'], { windowsHide: true })
    const text = `${stderr}\n${stdout}`
    const numeric = /version "([^"]+)"/.exec(text)?.[1] ?? 'unknown'
    const vendor = /^(?:OpenJDK|Java) Runtime Environment (.+)$/m.exec(text)?.[1]?.trim()
    const version = vendor ? `${numeric} ${vendor}` : numeric
    const majorMatch = /version "1\.(\d+)/.exec(text) ?? /version "(\d+)/.exec(text)
    const major = majorMatch ? Number(majorMatch[1]) : 0
    const arch = /64-Bit/.test(text) ? 'x64' : os.arch()
    const info: JavaRuntime = {
      path: javaPath,
      version,
      major: numeric.startsWith('1.') ? major : Number(numeric.split('.')[0] ?? major),
      arch,
      source
    }
    describeCache.set(javaCacheKey(javaPath), info)
    return info
  } catch {
    return null
  }
}

function pushJavaBins(found: string[], base: string): void {
  const push = (file: string) => {
    if (file && fs.existsSync(file)) found.push(file)
  }
  push(path.join(base, 'bin', 'java.exe'))
  push(path.join(base, 'bin', 'java'))
  push(path.join(base, 'Contents', 'Home', 'bin', 'java'))
}

function candidates(): string[] {
  const found: string[] = []
  pushJavaBins(found, kitRoot())
  pushJavaBins(found, graalHome())
  if (process.env.JAVA_HOME) pushJavaBins(found, process.env.JAVA_HOME)
  // Prefer an existing Graal 17 from the older lunar-revamped tree if present.
  pushJavaBins(found, path.join(os.homedir(), 'AppData', 'Roaming', '.lunar-revamped', 'runtimes', 'graalvm-ce-17'))
  const roots = [
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files\\Java',
    'C:\\Program Files\\GraalVM',
    'C:\\Program Files\\Microsoft',
    '/Library/Java/JavaVirtualMachines',
    '/usr/lib/jvm',
    path.join(os.homedir(), '.sdkman', 'candidates', 'java')
  ]
  for (const root of roots) {
    if (!fs.existsSync(root)) continue
    for (const entry of fs.readdirSync(root)) {
      const base = path.join(root, entry)
      pushJavaBins(found, base)
      try {
        for (const nested of fs.readdirSync(base)) {
          pushJavaBins(found, path.join(base, nested))
        }
      } catch {
        /* not a directory */
      }
    }
  }
  return [...new Set(found)]
}

export async function listJava(): Promise<JavaRuntime[]> {
  const runtimes: JavaRuntime[] = []
  for (const file of candidates()) {
    const source: JavaRuntime['source'] = file.includes(`${path.sep}runtimes${path.sep}`)
      ? 'bundled'
      : 'detected'
    const info = await describe(file, source)
    if (info && info.major >= MIN_JAVA_MAJOR) runtimes.push(info)
  }
  return runtimes
}

export async function testJava(javaPath: string): Promise<JavaRuntime> {
  describeCache.delete(javaCacheKey(javaPath))
  const info = await describe(javaPath, 'custom')
  if (!info) throw new Error('Could not run that Java executable.')
  if (info.major < MIN_JAVA_MAJOR) {
    throw new Error(
      `Java ${info.version} is too old. The client needs Java ${MIN_JAVA_MAJOR}+ (Genesis is class file 61).`
    )
  }
  return info
}

function preferGraal17(a: JavaRuntime, b: JavaRuntime): number {
  const score = (j: JavaRuntime) => {
    let s = 0
    if (j.path.includes(`${path.sep}${PVP_JAVA_DIR}${path.sep}`)) s += 80
    if (j.path.includes(`${path.sep}${GRAAL_RUNTIME_DIR}${path.sep}`)) s += 50
    if (j.major === TARGET_JAVA_MAJOR) s += 40
    if (/zulu|azul/i.test(j.version) || /zulu/i.test(j.path)) s += 25
    if (j.major !== TARGET_JAVA_MAJOR) s -= 40
    return s
  }
  return score(b) - score(a)
}

function shouldAdoptBundledJava(current: string, bundled: string): boolean {
  if (!current) return true
  if (path.normalize(current) === path.normalize(bundled)) return false
  return true
}

function adoptBundledJava(javaPath: string): void {
  updateStore((data) => {
    if (shouldAdoptBundledJava(data.settings.javaPath || '', javaPath)) {
      data.settings.javaPath = javaPath
      data.settings.pvpKitPath = ''
      data.settings.jvmPreset = 'default'
    }
  })
}

function copyKitPayload(dest: string): void {
  const src = packagedKitRoot()
  if (!fs.existsSync(src)) {
    throw new Error(`PVP Java kit is missing from the launcher package (${src}).`)
  }
  fs.mkdirSync(path.join(dest, 'bin'), { recursive: true })
  for (const name of ['timer-agent.jar', 'timer-agent.dll', 'pvp-client.args']) {
    const from = path.join(src, name)
    if (!fs.existsSync(from)) throw new Error(`PVP kit missing ${name}.`)
    fs.copyFileSync(from, path.join(dest, name))
  }
  for (const name of process.platform === 'win32' ? ['java.exe', 'javaw.exe'] : ['java']) {
    const from = path.join(src, 'bin', name)
    if (!fs.existsSync(from)) throw new Error(`PVP kit missing bin/${name}.`)
    fs.copyFileSync(from, path.join(dest, 'bin', name))
  }
  fs.writeFileSync(path.join(dest, KIT_MARKER), KIT_VERSION, 'utf8')
}

function copyTree(src: string, dest: string): void {
  fs.mkdirSync(dest, { recursive: true })
  for (const entry of fs.readdirSync(src, { withFileTypes: true })) {
    const from = path.join(src, entry.name)
    const to = path.join(dest, entry.name)
    if (entry.isDirectory()) copyTree(from, to)
    else fs.copyFileSync(from, to)
  }
}

async function ensureGraal17Runtime(): Promise<string> {
  const home = graalHome()
  const probe = path.join(home, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  if (fs.existsSync(probe)) {
    const ok = await describe(probe, 'bundled')
    if (ok && ok.major === TARGET_JAVA_MAJOR && /graal/i.test(ok.version + ok.path)) return home
    if (ok && ok.major === TARGET_JAVA_MAJOR) return home
  }

  const packaged = packagedZuluRoot()
  const packagedJava = path.join(packaged, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  if (fs.existsSync(packagedJava)) {
    fs.rmSync(home, { recursive: true, force: true })
    copyTree(packaged, home)
    const copied = await describe(
      path.join(home, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'),
      'bundled'
    )
    if (copied?.major === TARGET_JAVA_MAJOR) return home
    fs.rmSync(home, { recursive: true, force: true })
  }

  // Reuse older lunar-revamped Graal 17 install if present.
  const legacy = path.join(os.homedir(), 'AppData', 'Roaming', '.lunar-revamped', 'runtimes', 'graalvm-ce-17')
  const legacyJava = path.join(legacy, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
  if (fs.existsSync(legacyJava)) {
    const ok = await describe(legacyJava, 'detected')
    if (ok && ok.major === TARGET_JAVA_MAJOR) {
      fs.rmSync(home, { recursive: true, force: true })
      copyTree(legacy, home)
      return home
    }
  }

  if (process.platform !== 'win32') {
    throw new Error('Bundled GraalVM 17 is currently Windows-only. Install GraalVM CE 17 manually.')
  }

  const archive = path.join(folders.cache(), GRAAL17_WIN_ZIP)
  await downloadFile(GRAAL17_WIN_URL, archive, {
    label: 'GraalVM CE 17'
  })
  const staging = path.join(folders.runtimes(), `${GRAAL_RUNTIME_DIR}.extracting`)
  fs.rmSync(staging, { recursive: true, force: true })
  fs.rmSync(home, { recursive: true, force: true })
  fs.mkdirSync(staging, { recursive: true })
  const zip = new AdmZip(archive)
  zip.extractAllTo(staging, true)
  const extracted = fs
    .readdirSync(staging)
    .map((entry) => path.join(staging, entry))
    .find((entry) => fs.existsSync(path.join(entry, 'bin', 'java.exe')))
  if (!extracted) {
    fs.rmSync(staging, { recursive: true, force: true })
    throw new Error('GraalVM 17 zip did not contain a JDK home.')
  }
  fs.renameSync(extracted, home)
  fs.rmSync(staging, { recursive: true, force: true })
  const info = await describe(path.join(home, 'bin', 'java.exe'), 'bundled')
  if (!info || info.major !== TARGET_JAVA_MAJOR) {
    throw new Error('GraalVM 17 downloaded but could not be verified.')
  }
  return home
}

/**
 * Installs the launcher-bundled kit (timer agent + forwarder) and ensures
 * GraalVM CE 17 under `%APPDATA%\.lunar-remake\runtimes\graalvm-ce-17`.
 */
export async function ensureBundledPvpJava(): Promise<JavaRuntime> {
  if (installPromise) return installPromise
  installPromise = (async () => {
    const home = await ensureGraal17Runtime()
    const javaPath = path.join(home, 'bin', process.platform === 'win32' ? 'java.exe' : 'java')
    describeCache.delete(javaCacheKey(javaPath))
    const info = await describe(javaPath, 'bundled')
    if (!info || info.major !== TARGET_JAVA_MAJOR) {
      throw new Error('Azul Zulu Java 25 downloaded but could not be started.')
    }
    updateStore((data) => {
      data.settings.javaPath = javaPath
      data.settings.pvpKitPath = ''
      data.settings.jvmPreset = 'default'
    })
    return info
  })().finally(() => {
    installPromise = null
  })
  return installPromise
}

export async function bootBundledPvpJava(): Promise<void> {
  try {
    await ensureBundledPvpJava()
  } catch (error) {
    console.warn(
      '[java] Bundled GraalVM 17 install deferred:',
      error instanceof Error ? error.message : error
    )
  }
}

export async function ensureJava(preferred: string): Promise<JavaRuntime> {
  if (preferred) {
    const custom = await describe(preferred, 'custom')
    if (custom && custom.major === TARGET_JAVA_MAJOR) return custom
    if (custom) {
      console.warn(
        `[java] Ignoring Java ${custom.version} at ${custom.path}; Optic uses Azul Java 25.`
      )
    }
  }

  const bundledPath = bundledPvpJavaPath()
  const bundled = await describe(bundledPath, 'bundled')
  if (bundled && bundled.major === TARGET_JAVA_MAJOR) {
    adoptBundledJava(bundledPath)
    return bundled
  }

  try {
    return await ensureBundledPvpJava()
  } catch (error) {
    console.warn('[java] Bundled Azul Java 25 failed, falling back:', error instanceof Error ? error.message : error)
  }

  const found = await listJava()
  const best = found.sort(preferGraal17)[0]
  if (best) return best
  throw new Error('No Java 25 found. Launch once online so Azul Zulu Java 25 can download.')
}
