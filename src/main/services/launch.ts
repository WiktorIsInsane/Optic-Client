import { execFile, execFileSync, spawn, type ChildProcess } from 'node:child_process'
import fs from 'node:fs'
import net from 'node:net'
import path from 'node:path'
import { parseArgs, presetArgs } from '../../shared/jvm'
import { IpcChannel } from '../../shared/ipc'
import type {
  CommandPreview,
  ConsoleLine,
  JavaRuntime,
  LaunchSettings,
  LaunchState
} from '../../shared/types'
import { emit } from '../bus'
import { instanceRoot } from '../paths'
import { loadStore } from '../store'
import { activeAccount, writeGameAccounts } from './auth'
import { ensureLegacyVersion, ensureVanilla, launchwrapperJars, runtimeRoot } from './download'
import { ensureJava } from './java'
import { extractNatives } from './natives'
import { startLocalServices } from './fakeServices'
import { applyPvpFeelProfile } from './pvpFeel'
import { applyOpticPack } from './opticPack'

const VANILLA_LIBS = [
  'oshi-project/oshi-core/1.1/oshi-core-1.1.jar',
  'com/ibm/icu/icu4j-core-mojang/51.2/icu4j-core-mojang-51.2.jar',
  'net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar',
  'com/paulscode/codecjorbis/20101023/codecjorbis-20101023.jar',
  'com/paulscode/codecwav/20101023/codecwav-20101023.jar',
  'com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar',
  'com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar',
  'com/paulscode/soundsystem/20120107/soundsystem-20120107.jar',
  'io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar',
  'com/google/guava/guava/17.0/guava-17.0.jar',
  'org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar',
  'commons-io/commons-io/2.4/commons-io-2.4.jar',
  'commons-codec/commons-codec/1.9/commons-codec-1.9.jar',
  'net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar',
  'net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar',
  'com/google/code/gson/gson/2.2.4/gson-2.2.4.jar',
  'com/mojang/authlib/1.5.21/authlib-1.5.21.jar',
  'com/mojang/realms/1.7.59/realms-1.7.59.jar',
  'org/apache/commons/commons-compress/1.8.1/commons-compress-1.8.1.jar',
  'org/apache/httpcomponents/httpclient/4.3.3/httpclient-4.3.3.jar',
  'commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar',
  'org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar',
  'org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar',
  'org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar',
  'org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209.jar',
  'tv/twitch/twitch/6.5/twitch-6.5.jar',
  'org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar',
  'org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar'
]

const FORGE_LIBS = [
  'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar',
  'org/ow2/asm/asm-all/5.2/asm-all-5.2.jar',
  'com/typesafe/akka/akka-actor_2.11/2.3.3/akka-actor_2.11-2.3.3.jar',
  'com/typesafe/config/1.2.1/config-1.2.1.jar',
  'org/scala-lang/scala-actors-migration_2.11/1.1.0/scala-actors-migration_2.11-1.1.0.jar',
  'org/scala-lang/scala-compiler/2.11.1/scala-compiler-2.11.1.jar',
  'org/scala-lang/plugins/scala-continuations-library_2.11/1.0.2/scala-continuations-library_2.11-1.0.2.jar',
  'org/scala-lang/plugins/scala-continuations-plugin_2.11.1/1.0.2/scala-continuations-plugin_2.11.1-1.0.2.jar',
  'org/scala-lang/scala-library/2.11.1/scala-library-2.11.1.jar',
  'org/scala-lang/scala-parser-combinators_2.11/1.0.1/scala-parser-combinators_2.11-1.0.1.jar',
  'org/scala-lang/scala-reflect/2.11.1/scala-reflect-2.11.1.jar',
  'org/scala-lang/scala-swing_2.11/1.0.1/scala-swing_2.11-1.0.1.jar',
  'org/scala-lang/scala-xml_2.11/1.0.2/scala-xml_2.11-1.0.2.jar',
  'lzma/lzma/0.0.1/lzma-0.0.1.jar',
  'java3d/vecmath/1.5.2/vecmath-1.5.2.jar',
  'net/sf/trove4j/trove4j/3.0.3/trove4j-3.0.3.jar',
  'net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9.jar'
]

const MODULES = [
  'lunar.jar',
  'common-0.1.0-SNAPSHOT-all-nomappings.jar',
  'legacy-0.1.0-SNAPSHOT-all-nomappings.jar',
  'optifine-0.1.0-SNAPSHOT-all.jar',
  'lunar-platform-mappings-v1_8.jar',
  'OptiFine_v1_8.jar',
  'lunar-lang.jar'
]

let game: ChildProcess | null = null
let helpers: ChildProcess[] = []
let state: LaunchState = { running: false, pid: null, startedAt: null, exitCode: null, crashReport: '' }
const lines: ConsoleLine[] = []

export function launchState(): LaunchState {
  return state
}

export function consoleLines(): ConsoleLine[] {
  return lines.slice(-400)
}

function pushLine(stream: ConsoleLine['stream'], text: string): void {
  const line: ConsoleLine = { stream, text, at: Date.now() }
  lines.push(line)
  if (lines.length > 2000) lines.splice(0, lines.length - 1500)
  emit(IpcChannel.console, line)
}

function setState(next: Partial<LaunchState>): void {
  state = { ...state, ...next }
  emit(IpcChannel.launchState, state)
}

function mergedSettings(): LaunchSettings {
  const data = loadStore()
  const instance = data.instances.find((item) => item.id === data.activeInstanceId)
  return { ...data.settings, ...instance?.settings }
}

function portListening(port: number): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = net.connect({ host: '127.0.0.1', port })
    const finish = (ok: boolean) => {
      socket.removeAllListeners()
      socket.destroy()
      resolve(ok)
    }
    socket.setTimeout(100)
    socket.once('connect', () => finish(true))
    socket.once('error', () => finish(false))
    socket.once('timeout', () => finish(false))
  })
}

async function waitForLocalPorts(ports: number[], timeoutMs = 800): Promise<void> {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const ready = await Promise.all(ports.map(portListening))
    if (ready.every(Boolean)) return
    await new Promise((resolve) => setTimeout(resolve, 50))
  }
}

function tryBindPort(port: number): Promise<number | null> {
  return new Promise((resolve) => {
    const server = net.createServer()
    server.unref()
    server.once('error', () => resolve(null))
    server.listen(port, '127.0.0.1', () => {
      const address = server.address()
      const bound = typeof address === 'object' && address ? address.port : port
      server.close(() => resolve(bound))
    })
  })
}

/** Prefer ports near `start`; fall back to an OS-assigned ephemeral port. */
async function freePort(start: number, attempts = 300): Promise<number> {
  for (let i = 0; i < attempts; i++) {
    const port = await tryBindPort(start + i)
    if (port != null) return port
  }
  const ephemeral = await tryBindPort(0)
  if (ephemeral != null) return ephemeral
  throw new Error('No free port')
}

/**
 * Previous launches leave FakeLauncher/FakeBackend detached; they fill 28190+
 * and surface as "No free port". Reclaim listeners in the launcher IPC band.
 */
function reclaimStaleIpcPorts(from = 28190, to = 28500): void {
  if (process.platform !== 'win32') return
  try {
    const out = execFileSync('netstat', ['-ano'], { encoding: 'utf8', windowsHide: true })
    const pids = new Set<number>()
    for (const line of out.split(/\r?\n/)) {
      const m = line.match(/^\s*TCP\s+127\.0\.0\.1:(\d+)\s+\S+\s+LISTENING\s+(\d+)\s*$/i)
      if (!m) continue
      const port = Number(m[1])
      const pid = Number(m[2])
      if (port >= from && port <= to && pid > 0) pids.add(pid)
    }
    for (const pid of pids) {
      try {
        process.kill(pid)
      } catch {
        /* already gone / access denied */
      }
    }
  } catch {
    /* netstat unavailable */
  }
}

function findUi(dataDir: string, runtime: string): string {
  const roots = [path.join(dataDir, 'ui-local'), path.join(dataDir, 'ui'), path.join(runtime, 'libs', 'lunar-assets', 'ui')]
  for (const root of roots) {
    if (!fs.existsSync(root)) continue
    for (const entry of fs.readdirSync(root)) {
      const dir = path.join(root, entry)
      if (fs.existsSync(path.join(dir, 'index.html'))) return dir
    }
    if (fs.existsSync(path.join(root, 'index.html'))) return root
  }
  return path.join(dataDir, 'ui')
}

function linkDir(target: string, linkPath: string): void {
  if (!fs.existsSync(target) || fs.existsSync(linkPath)) return
  fs.mkdirSync(path.dirname(linkPath), { recursive: true })
  const type = process.platform === 'win32' ? 'junction' : 'dir'
  fs.symlinkSync(target, linkPath, type)
}

function opticClientDir(runtime: string): string {
  const dir = path.join(runtime, 'libs', 'optic-client')
  return fs.existsSync(path.join(dir, 'optic-client.json')) ? dir : ''
}

function listJars(dir: string): string[] {
  if (!fs.existsSync(dir)) return []
  const jars: string[] = []
  const walk = (current: string): void => {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const full = path.join(current, entry.name)
      if (entry.isDirectory()) walk(full)
      else if (entry.name.toLowerCase().endsWith('.jar')) jars.push(full)
    }
  }
  walk(dir)
  return jars
}

/** A user-supplied OptiFine jar for this Minecraft version in libs/optifine, if any. */
function optifineJar(runtime: string, mcVersion: string): string {
  const dir = path.join(runtime, 'libs', 'optifine')
  if (!fs.existsSync(dir)) return ''
  const name = fs
    .readdirSync(dir)
    .find((file) => /^optifine.*\.jar$/i.test(file) && file.includes(mcVersion))
  return name ? path.join(dir, name) : ''
}

function forgeJava(runtime: string): string {
  const home = path.join(runtime, '..', '..', 'runtimes', 'temurin-8', 'bin')
  const javaw = path.join(home, 'javaw.exe')
  const java = path.join(home, 'java.exe')
  if (process.platform === 'win32' && fs.existsSync(javaw)) return javaw
  if (fs.existsSync(java)) return java
  throw new Error('Java 8 for Forge is missing. Expected it under the Optic runtimes folder.')
}

/** Forge loads every jar in the instance mods folder. Keep the Fabric build out of that folder. */
function stageForgeMod(opticDir: string, gameDir: string): void {
  const src = path.join(opticDir, 'mods', 'optic-forge.jar')
  const mods = path.join(gameDir, 'mods')
  fs.mkdirSync(mods, { recursive: true })
  fs.copyFileSync(src, path.join(mods, 'optic-forge.jar'))
  for (const name of fs.readdirSync(mods)) {
    if (/^optic-client-.*\.jar$/i.test(name)) {
      const from = path.join(mods, name)
      const to = `${from}.fabric-disabled`
      if (fs.existsSync(to)) fs.rmSync(from)
      else fs.renameSync(from, to)
    }
  }
}

function modJars(opticDir: string, gameDir: string): string[] {
  const jars: string[] = []
  for (const dir of [path.join(opticDir, 'mods'), path.join(gameDir, 'mods')]) {
    if (!fs.existsSync(dir)) continue
    for (const name of fs.readdirSync(dir)) {
      if (name.toLowerCase().endsWith('.jar')) jars.push(path.join(dir, name))
    }
  }
  return jars
}

export async function previewCommand(): Promise<CommandPreview> {
  const built = await buildPlan(false)
  return { command: built.display, warnings: built.warnings }
}

interface Plan {
  java: string
  args: string[]
  display: string
  warnings: string[]
  gameDir: string
  logFile: string
  env: NodeJS.ProcessEnv
  pre: string
  post: string
  wrapper: string
  ipc: number
  auth: number
  asset: number
  runtime: string
  dataDir: string
  cwd: string
  nativeClient: boolean
}

async function buildPlan(prepare: boolean): Promise<Plan> {
  const settings = mergedSettings()
  const runtime = runtimeRoot()
  if (!fs.existsSync(path.join(runtime, 'libs'))) {
    throw new Error('Client runtime is not installed. Download it from Play, or set a dev runtime in Settings.')
  }
  if (prepare) await ensureVanilla(runtime)
  const java = await ensureJava(settings.javaPath)
  const data = loadStore()
  const home = instanceRoot(data.activeInstanceId)
  const gameDir = settings.gameDir || path.join(home, 'game')
  const dataDir = path.join(home, 'lunarclient')
  const logs = path.join(home, 'logs')
  fs.mkdirSync(gameDir, { recursive: true })
  fs.mkdirSync(dataDir, { recursive: true })
  fs.mkdirSync(logs, { recursive: true })
  fs.mkdirSync(path.join(dataDir, 'profiles', '1.8', 'mods'), { recursive: true })
  applyPvpFeelProfile(gameDir, dataDir)
  applyOpticPack()
  const opticDir = opticClientDir(runtime)
  const mc = path.join(runtime, 'libs', 'vanilla')
  const assets = path.join(mc, 'assets')
  if (fs.existsSync(assets)) linkDir(assets, path.join(gameDir, 'assets'))
  const account = activeAccount()
  const username = account?.username || 'Player'
  const uuid = account?.uuid || '00000000000000000000000000000000'
  const token = account?.accessToken || '0'
  // The Zulu wrapper already loads pvp-latency.args. A second flag set
  // (compact headers, a different compiler count) was landing after it.
  const preset = isZuluPvpWrapper(java.path) ? '' : presetArgs(settings.jvmPreset, java.major)
  const jvmRaw = settings.jvmPreset === 'custom' ? settings.jvmArgs : `${preset} ${settings.jvmArgs}`.trim()
  const jvm = parseArgs(jvmRaw)
  if (jvm.error) throw new Error(jvm.error)
  const gameArgs = parseArgs(settings.gameArgs)
  if (gameArgs.error) throw new Error(gameArgs.error)
  const warnings = [...jvm.warnings, ...gameArgs.warnings]
  const pvpExtras = pvpLaunchArgs(settings.pvpKitPath, java)
  warnings.push(...pvpExtras.warnings)
  jvm.args = [...pvpExtras.args, ...jvm.args]
  if (settings.maxRamMb > 8192 && java.arch !== 'x64' && !java.arch.includes('64')) {
    warnings.push('Max RAM is high for a 32-bit Java.')
  }
  const heapMb = Math.max(settings.minRamMb, settings.maxRamMb)
  const commitHeap =
    isPvpKitForwarder(java.path) ||
    /graal|zulu|azul/i.test(java.version) ||
    /graal|zulu|java25/i.test(java.path)
  const mem = commitHeap
    ? [`-Xms${heapMb}M`, `-Xmx${heapMb}M`]
    : [`-Xms${settings.minRamMb}M`, `-Xmx${settings.maxRamMb}M`]
  const env: NodeJS.ProcessEnv = { ...process.env }
  for (const pair of settings.env) {
    if (pair.key) env[pair.key] = pair.value
  }
  const quote = (arg: string) => (/\s/.test(arg) ? `"${arg}"` : arg)

  if (settings.gameVersion === '1.7.10') {
    const version = await ensureLegacyVersion(runtime, '1.7.10', prepare)
    // Own folder: 1.7 and 1.8 options.txt / optionsof.txt differ.
    const gameDir17 = path.join(home, 'game-1.7.10')
    fs.mkdirSync(gameDir17, { recursive: true })
    const optifine = optifineJar(runtime, '1.7.10')
    if (!optifine) {
      warnings.push(
        `OptiFine 1.7.10 not found. Put OptiFine_1.7.10_HD_U_E7.jar in ${path.join(runtime, 'libs', 'optifine')} to launch with it.`
      )
    }
    const cp = optifine
      ? [...launchwrapperJars(runtime), optifine, ...version.libraries, version.jar]
      : [...version.libraries, version.jar]
    const values: Record<string, string> = {
      auth_player_name: username,
      version_name: version.id,
      game_directory: gameDir17,
      assets_root: version.assetsDir,
      assets_index_name: version.assetIndex,
      auth_uuid: uuid,
      auth_access_token: token,
      user_properties: '{}',
      user_type: 'legacy'
    }
    const mcArgs = version.minecraftArguments
      .split(' ')
      .filter(Boolean)
      .map((arg) => arg.replace(/\$\{(\w+)\}/g, (_m, key: string) => values[key] ?? ''))
    // 1.7.10 and its launchwrapper are Java 8 era; newer Java breaks the class loader.
    const javaBin = forgeJava(runtime)
    const args = [
      ...mem,
      '-Dlog4j2.formatMsgNoLookups=true',
      `-Djava.library.path=${version.nativesDir}`,
      `-Dorg.lwjgl.librarypath=${version.nativesDir}`,
      '-cp',
      cp.join(path.delimiter),
      optifine ? 'net.minecraft.launchwrapper.Launch' : version.mainClass,
      ...mcArgs,
      ...(optifine ? ['--tweakClass', 'optifine.OptiFineTweaker'] : []),
      '--width',
      String(settings.width),
      '--height',
      String(settings.height),
      ...gameArgs.args
    ]
    if (settings.fullscreen) args.push('--fullscreen')
    return {
      java: javaBin,
      args,
      display: [`"${javaBin}"`, ...args.map(quote)].join(' '),
      warnings,
      gameDir: gameDir17,
      logFile: path.join(logs, 'game.log'),
      env,
      pre: settings.preLaunch,
      post: settings.postExit,
      wrapper: '',
      ipc: 0,
      auth: 0,
      asset: 0,
      runtime,
      dataDir,
      cwd: gameDir17,
      nativeClient: true
    }
  }

  const notch = path.join(runtime, 'libs', 'optic-notch.jar')
  const launch25 = path.join(runtime, 'libs', 'launch-java25.jar')
  if (fs.existsSync(notch) && fs.existsSync(launch25)) {
    const gameJar = path.join(mc, 'versions', '1.8.9', '1.8.9.jar')
    if (!fs.existsSync(gameJar)) {
      throw new Error('Minecraft 1.8.9 is not installed yet. Download the client from Play first.')
    }
    const natives = path.join(runtime, 'libs', 'forge-natives')
    if (!fs.existsSync(path.join(natives, process.platform === 'win32' ? 'lwjgl64.dll' : 'liblwjgl64.so'))) {
      throw new Error('LWJGL natives are missing from the Optic runtime.')
    }
    const asm9 = path.join(runtime, 'libs', 'asm9')
    const extra = [
      path.join(asm9, 'asm-9.9.jar'),
      path.join(asm9, 'asm-commons-9.9.jar'),
      path.join(asm9, 'asm-tree-9.9.jar'),
      path.join(asm9, 'asm-analysis-9.9.jar'),
      path.join(asm9, 'asm-util-9.9.jar'),
      path.join(mc, 'libraries', 'net', 'minecraft', 'launchwrapper', '1.12', 'launchwrapper-1.12.jar'),
      path.join(runtime, 'libs', 'javax.annotation-api-1.3.2.jar'),
      path.join(runtime, 'libs', 'mixin-0.8.2.jar'),
      path.join(runtime, 'libs', 'guava-mixin-shim.jar'),
      path.join(runtime, 'libs', 'jna-5.14.0.jar'),
      path.join(runtime, 'libs', 'jna-platform-5.14.0.jar'),
      notch,
      path.join(runtime, 'libs', 'optifine', 'OptiFine-1.8.9_HD_U_M5.jar')
    ]
    for (const file of extra) {
      if (!fs.existsSync(file)) throw new Error(`Missing ${path.basename(file)} in the Optic runtime.`)
    }
    const libRoot = path.join(mc, 'libraries')
    const cp: string[] = [launch25, ...extra.slice(0, 5), gameJar]
    for (const rel of VANILLA_LIBS) {
      const file = path.join(libRoot, rel)
      if (!fs.existsSync(file)) {
        if (rel.includes('log4j')) continue
        throw new Error(`Minecraft library is missing: ${rel}`)
      }
      cp.push(file)
    }
    cp.push(...extra.slice(5))
    const javaw = path.join(path.dirname(java.path), 'javaw.exe')
    const javaBin = process.platform === 'win32' && fs.existsSync(javaw) ? javaw : java.path
    const args = [
      ...mem,
      '-Dlog4j2.formatMsgNoLookups=true',
      '--add-opens=java.base/java.lang=ALL-UNNAMED',
      '--add-opens=java.base/java.lang.reflect=ALL-UNNAMED',
      '--add-opens=java.base/java.util=ALL-UNNAMED',
      '--add-opens=java.base/java.io=ALL-UNNAMED',
      '--add-opens=java.base/java.nio=ALL-UNNAMED',
      '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED',
      '--add-opens=java.base/java.net=ALL-UNNAMED',
      '--enable-native-access=ALL-UNNAMED',
      `-Djava.library.path=${natives}`,
      `-Dorg.lwjgl.librarypath=${natives}`,
      '-cp',
      cp.join(path.delimiter),
      'net.minecraft.launchwrapper.Launch',
      '--version',
      '1.8.9',
      '--gameDir',
      gameDir,
      '--assetsDir',
      fs.existsSync(assets) ? assets : path.join(gameDir, 'assets'),
      '--assetIndex',
      '1.8',
      '--username',
      username,
      '--uuid',
      uuid,
      '--accessToken',
      token,
      '--userProperties',
      '{}',
      '--userType',
      'legacy',
      '--versionType',
      'release',
      '--tweakClass',
      'optifine.OptiFineTweaker',
      '--tweakClass',
      'gg.optic.forge.OpticTweaker',
      '--width',
      String(settings.width),
      '--height',
      String(settings.height),
      ...gameArgs.args
    ]
    if (settings.fullscreen) args.push('--fullscreen')
    const display = [`"${javaBin}"`, ...args.map(quote)].join(' ')
    return {
      java: javaBin,
      args,
      display,
      warnings,
      gameDir,
      logFile: path.join(logs, 'game.log'),
      env,
      pre: settings.preLaunch,
      post: settings.postExit,
      wrapper: '',
      ipc: 0,
      auth: 0,
      asset: 0,
      runtime,
      dataDir,
      cwd: gameDir,
      nativeClient: true
    }
  }

  if (opticDir && fs.existsSync(path.join(opticDir, 'mods', 'optic-forge.jar'))) {
    const gameJar = path.join(mc, 'versions', '1.8.9', '1.8.9.jar')
    if (!fs.existsSync(gameJar)) {
      throw new Error('Minecraft 1.8.9 is not installed yet. Download the client from Play first.')
    }
    const natives = path.join(runtime, 'libs', 'forge-natives')
    if (!fs.existsSync(path.join(natives, process.platform === 'win32' ? 'lwjgl64.dll' : 'liblwjgl64.so'))) {
      throw new Error('Forge natives are missing from the Optic runtime.')
    }
    const optifine = path.join(runtime, 'libs', 'optifine', 'OptiFine-1.8.9_HD_U_M5.jar')
    const mixin = path.join(runtime, 'libs', 'mixin-0.8.2.jar')
    for (const file of [optifine, mixin]) {
      if (!fs.existsSync(file)) throw new Error(`Missing ${path.basename(file)} in the Optic runtime.`)
    }
    const libRoot = path.join(mc, 'libraries')
    const cp: string[] = [gameJar]
    for (const rel of [...VANILLA_LIBS, ...FORGE_LIBS]) {
      const file = path.join(libRoot, rel)
      if (!fs.existsSync(file)) {
        if (rel.includes('log4j')) continue
        throw new Error(`Forge library is missing: ${rel}`)
      }
      cp.push(file)
    }
    const guavaShim = path.join(runtime, 'libs', 'guava-mixin-shim.jar')
    if (!fs.existsSync(guavaShim)) throw new Error('Missing guava-mixin-shim.jar in the Optic runtime.')
    const jna = path.join(runtime, 'libs', 'jna-5.14.0.jar')
    const jnaPlatform = path.join(runtime, 'libs', 'jna-platform-5.14.0.jar')
    for (const file of [jna, jnaPlatform]) {
      if (!fs.existsSync(file)) throw new Error(`Missing ${path.basename(file)} in the Optic runtime.`)
    }
    cp.push(mixin, guavaShim, jna, jnaPlatform, optifine)
    stageForgeMod(opticDir, gameDir)
    const java8 = forgeJava(runtime)
    const args = [
      ...mem,
      '-Dlog4j2.formatMsgNoLookups=true',
      `-Djava.library.path=${natives}`,
      `-Dorg.lwjgl.librarypath=${natives}`,
      '-Dfml.ignoreInvalidMinecraftCertificates=true',
      '-Dfml.ignorePatchDiscrepancies=true',
      '-cp',
      cp.join(path.delimiter),
      'net.minecraft.launchwrapper.Launch',
      '--version',
      '1.8.9-forge1.8.9-11.15.1.2318-1.8.9',
      '--gameDir',
      gameDir,
      '--assetsDir',
      fs.existsSync(assets) ? assets : path.join(gameDir, 'assets'),
      '--assetIndex',
      '1.8',
      '--username',
      username,
      '--uuid',
      uuid,
      '--accessToken',
      token,
      '--userProperties',
      '{}',
      '--userType',
      'legacy',
      '--versionType',
      'release',
      '--tweakClass',
      'net.minecraftforge.fml.common.launcher.FMLTweaker',
      '--tweakClass',
      'optifine.OptiFineForgeTweaker',
      '--width',
      String(settings.width),
      '--height',
      String(settings.height),
      ...gameArgs.args
    ]
    if (settings.fullscreen) args.push('--fullscreen')
    const display = [`"${java8}"`, ...args.map(quote)].join(' ')
    return {
      java: java8,
      args,
      display,
      warnings,
      gameDir,
      logFile: path.join(logs, 'game.log'),
      env,
      pre: settings.preLaunch,
      post: settings.postExit,
      wrapper: '',
      ipc: 0,
      auth: 0,
      asset: 0,
      runtime,
      dataDir,
      cwd: gameDir,
      nativeClient: true
    }
  }

  if (opticDir) {
    const gameJar = path.join(mc, 'versions', '1.8.9', '1.8.9.jar')
    if (!fs.existsSync(gameJar)) {
      throw new Error('Minecraft 1.8.9 is not installed yet. Download the client from Play first.')
    }
    const natives = path.join(opticDir, 'natives')
    if (!fs.existsSync(path.join(natives, 'lwjgl-windows-amd64.dll')) && process.platform === 'win32') {
      throw new Error('Optic natives are missing. Run npm run install:client in the launcher folder.')
    }
    const mods = modJars(opticDir, gameDir)
    if (!mods.some((file) => path.basename(file).toLowerCase().startsWith('optic-client'))) {
      throw new Error('Optic client jar is missing. Run npm run install:client in the launcher folder.')
    }
    const vanillaJars = listJars(path.join(mc, 'libraries')).filter(
      (file) => !file.replace(/\\/g, '/').toLowerCase().includes('/org/lwjgl/')
    )
    const cp = [...listJars(path.join(opticDir, 'libraries')), ...vanillaJars]
    const sys = [
      '-Dlog4j2.formatMsgNoLookups=true',
      `-Djava.library.path=${natives}`,
      `-Dorg.lwjgl.librarypath=${natives}`,
      `-Dfabric.gameJarPath=${gameJar}`,
      `-Dfabric.addMods=${mods.join(path.delimiter)}`,
      '--add-opens=java.base/java.lang=ALL-UNNAMED',
      '--add-opens=java.base/java.lang.reflect=ALL-UNNAMED',
      '--add-opens=java.base/java.util=ALL-UNNAMED',
      '--add-opens=java.base/java.io=ALL-UNNAMED',
      '--add-opens=java.base/java.nio=ALL-UNNAMED',
      '--add-opens=java.base/sun.nio.ch=ALL-UNNAMED'
    ]
    const args = [
      ...mem,
      ...jvm.args,
      ...sys,
      '-cp',
      cp.join(path.delimiter),
      'net.fabricmc.loader.impl.launch.knot.KnotClient',
      '--version',
      '1.8.9',
      '--gameDir',
      gameDir,
      '--assetsDir',
      fs.existsSync(assets) ? assets : path.join(gameDir, 'assets'),
      '--assetIndex',
      '1.8',
      '--username',
      username,
      '--uuid',
      uuid,
      '--accessToken',
      token,
      '--userProperties',
      '{}',
      '--userType',
      'legacy',
      '--versionType',
      'release',
      '--width',
      String(settings.width),
      '--height',
      String(settings.height),
      ...gameArgs.args
    ]
    if (settings.fullscreen) args.push('--fullscreen')
    const display = [settings.wrapper, `"${java.path}"`, ...args.map(quote)].filter(Boolean).join(' ')
    return {
      java: java.path,
      args,
      display,
      warnings,
      gameDir,
      logFile: path.join(logs, 'game.log'),
      env,
      pre: settings.preLaunch,
      post: settings.postExit,
      wrapper: settings.wrapper,
      ipc: 0,
      auth: 0,
      asset: 0,
      runtime,
      dataDir,
      cwd: gameDir,
      nativeClient: true
    }
  }

  throw new Error(
    'Optic client is not installed in this runtime. Lunar is not used. Run npm run install:client in the launcher folder.'
  )

  const launcherCache = path.join(dataDir, 'launcher-cache')
  fs.mkdirSync(launcherCache, { recursive: true })
  fs.writeFileSync(path.join(launcherCache, 'installation-id'), 'lunar-revamped')
  const textures = path.join(runtime, 'libs', 'lunar-assets', 'textures')
  if (fs.existsSync(textures)) linkDir(textures, path.join(dataDir, 'textures'))
  const natives = path.join(gameDir, 'natives')
  if (prepare) extractNatives(runtime, natives)
  const sep = path.delimiter
  const mv = path.join(runtime, 'libs', 'multiver-full')
  const cp: string[] = [path.join(mc, 'versions', '1.8.9', '1.8.9.jar')]
  const localPatch = path.join(runtime, 'libs', 'lunar-localpatches.jar')
  if (fs.existsSync(localPatch)) cp.push(localPatch)
  const genesis = path.join(mv, 'genesis-0.1.0-SNAPSHOT-all.jar')
  if (fs.existsSync(genesis)) cp.push(genesis)
  for (const jar of MODULES) {
    const file = path.join(mv, jar)
    if (fs.existsSync(file)) cp.push(file)
  }
  for (const rel of VANILLA_LIBS) {
    const file = path.join(mc, 'libraries', rel)
    if (fs.existsSync(file)) cp.push(file)
  }
  const sentry = path.join(runtime, 'libs', 'sentry-off.jar')
  if (fs.existsSync(sentry)) cp.push(sentry)

  if (prepare) reclaimStaleIpcPorts()
  const ipc = prepare ? await freePort(28190) : 28190
  const asset = prepare ? await freePort(ipc + 1) : ipc + 1
  const auth = prepare ? await freePort(asset + 1) : asset + 1
  const ui = findUi(dataDir, runtime)
  const logConfig = path.join(logs, 'config.xml')
  if (!fs.existsSync(logConfig)) {
    fs.writeFileSync(
      logConfig,
      `<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN"><Appenders><Console name="SysOut" target="SYSTEM_OUT">
<PatternLayout pattern="[%d{HH:mm:ss}] [%t/%level]: %msg%n"/>
</Console></Appenders><Loggers><Root level="info"><AppenderRef ref="SysOut"/></Root></Loggers></Configuration>`
    )
  }
  const logUri = `file:///${logConfig.replace(/\\/g, '/')}`
  const sys = [
    '-Dlog4j2.formatMsgNoLookups=true',
    '-Dichor.filteredGenesisSentries=.*lcqt.*',
    `-Dlunar.dataDir=${dataDir}`,
    `-Dichor.fabric.localModPath=${path.join(dataDir, 'profiles', '1.8', 'mods')}`,
    '-Dichor.usingIsolatedProfiles=true',
    `-Dichor.logsFile=${path.join(logs, 'ichor-boot.log')}`,
    `-Djava.library.path=${natives}`,
    `-Dlog4j.configurationFile=${logUri}`,
    `-DserviceOverrideAuthenticator=ws://127.0.0.1:${auth}`,
    `-DserviceOverrideAssetServer=ws://127.0.0.1:${asset}`,
    '-DserviceOverrideApi=http://127.0.0.1:9',
    '-DserviceOverrideThirdPartyCache=http://127.0.0.1:9',
    '-DserviceOverrideSkins=http://127.0.0.1:9',
    '-DserviceOverrideStyngr=http://127.0.0.1:9'
  ]
  if (process.platform !== 'linux') sys.push('-DLWJGL_DISABLE_XRANDR=true')
  const ichorCp =
    'lunar-localpatches.jar,sentry-off.jar,lunar.jar,common-0.1.0-SNAPSHOT-all-nomappings.jar,legacy-0.1.0-SNAPSHOT-all-nomappings.jar,optifine-0.1.0-SNAPSHOT-all.jar,genesis-0.1.0-SNAPSHOT-all.jar,lunar-lang.jar'
  const ichorExternal =
    'OptiFine_v1_8.jar,kill-sound-chat-patterns.json,vanilla_capes.json,waypoint-patterns.json,user-message-patterns.json,tier-tagger.json'
  const args = [
    ...mem,
    ...jvm.args,
    ...sys,
    '-cp',
    cp.join(sep),
    'com.moonsworth.lunar.genesis.Genesis',
    '--version',
    '1.8.9',
    '--classpathDir',
    mv,
    '--workingDirectory',
    gameDir,
    '--texturesDir',
    path.join(dataDir, 'textures'),
    '--webosrDir',
    natives,
    '--jitDir',
    path.join(dataDir, 'jit'),
    '--uiDir',
    ui,
    '--ipcPort',
    String(ipc),
    '--ichorClassPath',
    ichorCp,
    '--ichorExternalFiles',
    ichorExternal,
    '--installationId',
    'lunar-revamped',
    '--gameDir',
    gameDir,
    '--assetsDir',
    path.join(gameDir, 'assets'),
    '--assetIndex',
    '1.8',
    '--username',
    username,
    '--uuid',
    uuid,
    '--accessToken',
    token,
    '--userType',
    'legacy',
    '--width',
    String(settings.width),
    '--height',
    String(settings.height),
    ...gameArgs.args
  ]
  if (settings.fullscreen) args.push('--fullscreen')
  const display = [settings.wrapper, `"${java.path}"`, ...args.map(quote)].filter(Boolean).join(' ')
  return {
    java: java.path,
    args,
    display,
    warnings,
    gameDir,
    logFile: path.join(logs, 'game.log'),
    env,
    pre: settings.preLaunch,
    post: settings.postExit,
    wrapper: settings.wrapper,
    ipc,
    auth,
    asset,
    runtime,
    dataDir,
    cwd: path.dirname(process.execPath),
    nativeClient: false
  }
}

function spawnJava(
  java: string,
  args: string[],
  env: NodeJS.ProcessEnv,
  logFile: string,
  cwd: string,
  genesis: boolean
): ChildProcess {
  // Genesis writes .ichor/genesis.log in the process directory and exits if that folder is missing.
  fs.mkdirSync(cwd, { recursive: true })
  if (genesis) fs.mkdirSync(path.join(cwd, '.ichor'), { recursive: true })
  const log = fs.openSync(logFile, 'a')
  const child = spawn(java, args, {
    cwd,
    detached: true,
    env,
    stdio: ['ignore', log, log],
    windowsHide: false
  })
  child.unref()
  if (child.pid) raiseWindowsPriority(child.pid)
  return child
}

/** High priority so 1ms timer + attack/place packets aren't delayed by desktop scheduling. */
function raiseWindowsPriority(pid: number): void {
  if (process.platform !== 'win32' || !Number.isInteger(pid) || pid <= 0) return
  execFile(
    'powershell.exe',
    [
      '-NoProfile',
      '-WindowStyle',
      'Hidden',
      '-Command',
      `try { (Get-Process -Id ${pid}).PriorityClass = 'High' } catch { }`
    ],
    { windowsHide: true },
    () => undefined
  )
}

function watchLog(file: string): void {
  if (!fs.existsSync(file)) fs.writeFileSync(file, '')
  let position = fs.statSync(file).size
  const timer = setInterval(() => {
    if (!state.running && !game) {
      clearInterval(timer)
      return
    }
    const size = fs.statSync(file).size
    if (size < position) position = 0
    if (size === position) return
    const fd = fs.openSync(file, 'r')
    const length = size - position
    const buffer = Buffer.alloc(length)
    fs.readSync(fd, buffer, 0, length, position)
    fs.closeSync(fd)
    position = size
    for (const text of buffer.toString('utf8').split(/\r?\n/)) {
      if (text) pushLine('out', text)
    }
  }, 400)
}

async function runShell(command: string, cwd: string): Promise<void> {
  if (!command.trim()) return
  await new Promise<void>((resolve, reject) => {
    const shell = process.platform === 'win32' ? 'cmd.exe' : 'bash'
    const args = process.platform === 'win32' ? ['/d', '/s', '/c', command] : ['-lc', command]
    const child = spawn(shell, args, { cwd, windowsHide: true })
    child.on('exit', (code) => {
      if (code && code !== 0) reject(new Error(`Command failed (${code}): ${command}`))
      else resolve()
    })
    child.on('error', reject)
  })
}

function joinServerArgs(address: string | undefined): string[] {
  const raw = (address ?? '').trim()
  if (!raw) return []
  const cleaned = raw.replace(/^minecraft:\/\//i, '')
  const hostPart = cleaned.includes('/') ? cleaned.slice(0, cleaned.indexOf('/')) : cleaned
  const [host, portRaw] = hostPart.split(':')
  if (!host) return []
  const port = /^\d+$/.test(portRaw ?? '') ? portRaw! : '25565'
  return ['--server', host, '--port', port]
}

export async function startGame(joinServer?: string): Promise<LaunchState> {
  if (state.running) throw new Error('The game is already running.')
  let plan: Plan | null = null
  try {
    plan = await buildPlan(true)
    const join = joinServerArgs(joinServer)
    if (join.length) {
      plan.args.push(...join)
      plan.display = `${plan.display} ${join.map((arg) => (/\s/.test(arg) ? `"${arg}"` : arg)).join(' ')}`
      pushLine('launcher', `Auto-joining ${join[1]}:${join[3]}`)
    }
    await runShell(plan.pre, plan.gameDir)
    const accounts = path.join(plan.dataDir, 'settings', 'game', 'accounts.json')
    writeGameAccounts(accounts)
    if (!plan.nativeClient) {
      const logs = path.dirname(plan.logFile)
      const discovery =
        [path.join(plan.runtime, 'tools', 'fake-launcher', 'discovery.json'), path.join(plan.runtime, 'discovery.json')].find(
          (file) => fs.existsSync(file)
        ) ?? ''
      const fake = startLocalServices({
        java: plan.java,
        runtime: plan.runtime,
        ipc: plan.ipc,
        asset: plan.asset,
        auth: plan.auth,
        accountsFile: accounts,
        logsDir: logs,
        discovery,
        env: plan.env
      })
      helpers = fake
      await waitForLocalPorts([plan.ipc, plan.asset, plan.auth], 3000)
    }
    const javaBin = plan.wrapper ? plan.wrapper : plan.java
    const javaArgs = plan.wrapper ? [plan.java, ...plan.args] : plan.args
    fs.writeFileSync(plan.logFile, '')
    game = spawnJava(javaBin, javaArgs, plan.env, plan.logFile, plan.cwd, !plan.nativeClient)
    setState({ running: true, pid: game.pid ?? null, startedAt: Date.now(), exitCode: null, crashReport: '' })
    pushLine('launcher', `${plan.nativeClient ? 'Optic' : 'Genesis'} started (pid ${game.pid ?? '?'})`)
    watchLog(plan.logFile)
    const pid = game.pid
    const poll = setInterval(() => {
      if (!pid) return
      try {
        process.kill(pid, 0)
      } catch {
        clearInterval(poll)
        for (const helper of helpers) {
          if (helper.pid) {
            try {
              process.kill(helper.pid)
            } catch {
              /* already gone */
            }
          }
        }
        helpers = []
        const crash = findCrash(plan!.gameDir)
        setState({ running: false, exitCode: 0, crashReport: crash })
        pushLine('launcher', crash ? `Game exited. Crash report: ${crash}` : 'Game exited.')
        void runShell(plan!.post, plan!.gameDir).catch((error: unknown) => {
          pushLine('err', error instanceof Error ? error.message : 'Post-exit command failed')
        })
        game = null
      }
    }, 1500)
    return state
  } catch (error) {
    for (const helper of helpers) {
      if (helper.pid) {
        try {
          process.kill(helper.pid)
        } catch {
          /* already gone */
        }
      }
    }
    helpers = []
    game = null
    setState({ running: false })
    throw error
  }
}

function findCrash(gameDir: string): string {
  const dir = path.join(gameDir, 'crash-reports')
  if (!fs.existsSync(dir)) return ''
  const files = fs.readdirSync(dir).filter((name) => name.endsWith('.txt')).sort()
  const latest = files[files.length - 1]
  return latest ? path.join(dir, latest) : ''
}

/** True when java.exe is the pvp-latency wrapper (sibling java_zulu.exe). */
function isZuluPvpWrapper(javaPath: string): boolean {
  const dir = path.dirname(javaPath)
  return fs.existsSync(path.join(dir, 'java_zulu.exe')) || fs.existsSync(path.join(dir, 'javaw_zulu.exe'))
}

/** True when javaPath is the mc-pvp-java* forwarder (sibling graalvm.path). */
function isPvpKitForwarder(javaPath: string): boolean {
  const dir = path.dirname(javaPath)
  const root = path.basename(dir).toLowerCase() === 'bin' ? path.dirname(dir) : dir
  return fs.existsSync(path.join(root, 'graalvm.path'))
}

/**
 * Light kit extras for GraalVM 17:
 * - timer-agent when using a manual kit folder
 * - forwarder already injects timer-agent + light pvp-client.args (do not double-stack heavy G1)
 */
function pvpLaunchArgs(
  kitPath: string,
  java: JavaRuntime
): { args: string[]; warnings: string[] } {
  const args: string[] = []
  const warnings: string[] = []
  const kitForwarder = isPvpKitForwarder(java.path)
  if (kitForwarder) {
    // Forwarder already injects timer-agent + light pvp-client.args — do not stack more.
    return { args, warnings }
  }
  const kit = kitPath.trim()
  if (kit) {
    const agentJar = path.join(kit, 'timer-agent.jar')
    const agentDll = path.join(kit, 'timer-agent.dll')
    if (fs.existsSync(agentJar) && fs.existsSync(agentDll)) {
      args.push(`-javaagent:${agentJar}`)
    } else if (fs.existsSync(agentJar) || fs.existsSync(agentDll)) {
      warnings.push('PVP kit is incomplete (need both timer-agent.jar and timer-agent.dll).')
    } else if (fs.existsSync(kit)) {
      warnings.push('PVP kit folder has no timer-agent; timings agent not loaded.')
    }
  }
  return { args, warnings }
}

export function stopGame(): void {
  if (game?.pid) {
    try {
      process.kill(game.pid)
    } catch {
      /* already gone */
    }
  }
  for (const helper of helpers) {
    if (helper.pid) {
      try {
        process.kill(helper.pid)
      } catch {
        /* already gone */
      }
    }
  }
  helpers = []
  game = null
  setState({ running: false })
}
