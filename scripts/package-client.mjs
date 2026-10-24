/**
 * Package the Optic runtime + hitreg pack as Optic-{version}.zip
 * for download by the Optic launcher (WiktorDvD/Atlas-Client).
 *
 * Usage:
 *   node scripts/package-client.mjs
 *   node scripts/package-client.mjs --runtime <path> --version 1.0.0 --publish
 *   node scripts/package-client.mjs --publish --release-tag v1.2.0
 */
import { createHash } from 'node:crypto'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const args = process.argv.slice(2)

function flag(name, fallback) {
  const index = args.indexOf(name)
  return index >= 0 ? args[index + 1] : fallback
}

function findInstalledRuntime() {
  const appData = path.join(os.homedir(), 'AppData', 'Roaming')
  for (const folder of ['.optic', '.lunar-remake']) {
    const versionsRoot = path.join(appData, folder, 'versions')
    if (!fs.existsSync(versionsRoot)) continue
    const candidates = fs
      .readdirSync(versionsRoot, { withFileTypes: true })
      .filter((e) => e.isDirectory() && !e.name.endsWith('.incoming') && !e.name.endsWith('.retiring'))
      .map((e) => e.name)
      .filter((name) => fs.existsSync(path.join(versionsRoot, name, 'libs')))
      .sort((a, b) => {
        const aNative = fs.existsSync(path.join(versionsRoot, a, 'libs', 'optic-client', 'optic-client.json'))
        const bNative = fs.existsSync(path.join(versionsRoot, b, 'libs', 'optic-client', 'optic-client.json'))
        if (aNative !== bNative) return aNative ? -1 : 1
        return b.localeCompare(a, undefined, { numeric: true })
      })
    if (candidates[0]) return path.join(versionsRoot, candidates[0])
  }
  return ''
}

const defaultRuntime =
  findInstalledRuntime() ||
  (fs.existsSync('F:/Projects/LunarClientremake-main/libs')
    ? 'F:/Projects/LunarClientremake-main'
    : '')

const runtime = path.resolve(flag('--runtime', defaultRuntime || '.'))
const version = flag('--version', '1.0.0')
const outDir = path.resolve(flag('--out', path.join(root, 'release-client')))
const opticPack = path.resolve(flag('--optic', path.join(root, 'resources', 'optic')))
const releaseTag = flag('--release-tag', 'v1.2.0')
const repo = flag('--repo', 'WiktorDvD/Atlas-Client')
const publish = args.includes('--publish')
const zipName = `Optic-${version}.zip`

if (!fs.existsSync(path.join(runtime, 'libs'))) {
  console.error('No client runtime found at', runtime)
  console.error('Pass --runtime <path-with-libs> or install via Optic Play first.')
  process.exit(1)
}

const include = [
  'libs/optic-client',
  'libs/vanilla',
]

const MULTIVER_SKIP = new Set(['cache', 'hypixel'])
const SKIP_FILE_RE = /\.bak$/i

const stage = path.join(outDir, `Optic-${version}`)
fs.rmSync(stage, { recursive: true, force: true })
fs.mkdirSync(stage, { recursive: true })

function shouldSkip(relParts) {
  if (relParts.some((p) => MULTIVER_SKIP.has(p))) return true
  const base = relParts[relParts.length - 1] || ''
  return SKIP_FILE_RE.test(base)
}

function copyFiltered(from, dest) {
  const st = fs.statSync(from)
  if (st.isFile()) {
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.copyFileSync(from, dest)
    return
  }
  fs.mkdirSync(dest, { recursive: true })
  for (const entry of fs.readdirSync(from, { withFileTypes: true })) {
    if (MULTIVER_SKIP.has(entry.name) || SKIP_FILE_RE.test(entry.name)) continue
    copyFiltered(path.join(from, entry.name), path.join(dest, entry.name))
  }
}

console.log('runtime', runtime)
console.log('version', version)
console.log('optic pack', opticPack)

for (const rel of include) {
  const from = path.join(runtime, rel)
  if (!fs.existsSync(from)) {
    console.warn('missing', rel)
    continue
  }
  const dest = path.join(stage, rel)
  if (rel === 'libs/multiver-full') {
    copyFiltered(from, dest)
  } else {
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.cpSync(from, dest, { recursive: true })
  }
  console.log('staged', rel)
}

/** Bake Optic hitreg / icons into the zip so Play installs a ready pack. */
function bakeOptic() {
  const jarSrc = path.join(opticPack, 'lunar-localpatches.jar')
  if (!fs.existsSync(jarSrc)) {
    console.warn('optic pack jar missing; shipping runtime patches as-is')
    return
  }
  const libs = path.join(stage, 'libs')
  const destA = path.join(libs, 'lunar-localpatches.jar')
  const destB = path.join(libs, 'multiver-full', 'lunar-localpatches.jar')
  fs.copyFileSync(jarSrc, destA)
  if (fs.existsSync(path.dirname(destB))) fs.copyFileSync(jarSrc, destB)

  let packVer = version
  try {
    const meta = JSON.parse(fs.readFileSync(path.join(opticPack, 'pack.json'), 'utf8'))
    packVer = meta.builtAt || meta.version || version
  } catch {
    /* keep version */
  }
  fs.writeFileSync(path.join(libs, 'optic.version'), packVer, 'utf8')
  fs.writeFileSync(path.join(libs, 'atlas-client.version'), version, 'utf8')

  const badgeJson = path.join(opticPack, 'textures', 'badges.json')
  if (fs.existsSync(badgeJson)) {
    const assetDir = path.join(libs, 'lunar-assets', 'textures', 'assets', 'lunar')
    fs.mkdirSync(path.join(assetDir, 'badges'), { recursive: true })
    fs.copyFileSync(badgeJson, path.join(assetDir, 'badges.json'))
    for (const n of ['owner.webp', 'partner.webp', 'user.webp']) {
      const src = path.join(opticPack, 'textures', 'badges', n)
      if (fs.existsSync(src)) fs.copyFileSync(src, path.join(assetDir, 'badges', n))
    }
  }
  console.log('baked Optic pack', packVer)
}

const files = []
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    const rel = path.relative(stage, full).replace(/\\/g, '/')
    if (shouldSkip(rel.split('/'))) continue
    if (entry.isDirectory()) walk(full)
    else {
      const data = fs.readFileSync(full)
      files.push({
        path: rel,
        sha256: createHash('sha256').update(data).digest('hex'),
        size: data.size ?? data.length
      })
    }
  }
}
walk(stage)
const manifest = {
  version,
  minecraft: '1.8.9',
  runtimeKind: 'optic-native',
  mainClass: 'net.fabricmc.loader.impl.launch.knot.KnotClient',
  javaMajor: 25,
  files
}
fs.writeFileSync(path.join(stage, 'manifest.json'), JSON.stringify(manifest, null, 2))

fs.mkdirSync(outDir, { recursive: true })
const zip = path.join(outDir, zipName)
if (fs.existsSync(zip)) fs.rmSync(zip)

console.log('compressing', zip)
// Avoid shell:true so paths with spaces (e.g. "lunar remake") stay intact.
const packed = spawnSync('tar', ['-a', '-c', '-f', zip, '-C', stage, '.'], {
  stdio: 'inherit'
})
if (packed.status !== 0) {
  const stageStar = path.join(stage, '*')
  const fallback = spawnSync(
    'powershell',
    [
      '-NoProfile',
      '-Command',
      `Compress-Archive -Path ${JSON.stringify(stageStar)} -DestinationPath ${JSON.stringify(zip)} -Force`
    ],
    { stdio: 'inherit' }
  )
  if (fallback.status !== 0) process.exit(fallback.status ?? 1)
}

const zipMb = (fs.statSync(zip).size / (1024 * 1024)).toFixed(1)
console.log('wrote', zip, `(${zipMb} MB)`)

if (!publish) process.exit(0)

console.log('publishing to', repo, releaseTag)
const upload = spawnSync(
  'gh',
  ['release', 'upload', releaseTag, zip, '--repo', repo, '--clobber'],
  { stdio: 'inherit' }
)
if (upload.status !== 0) process.exit(upload.status ?? 1)

const view = spawnSync(
  'gh',
  ['release', 'view', releaseTag, '--repo', repo, '--json', 'assets'],
  { encoding: 'utf8' }
)
if (view.status === 0 && view.stdout) {
  try {
    const assets = JSON.parse(view.stdout).assets || []
    for (const asset of assets) {
      const name = asset.name || ''
      if (!name.endsWith('.zip')) continue
      if (name === zipName) continue
      if (!/^(Optic(?:-[\w.-]+)?|Atlas-Client-[\w.-]+|lunar-revamped-client-[\w.-]+)\.zip$/i.test(name)) continue
      // keep going — only delete older client zips, never Optic.exe etc.
      console.log('removing old client zip', name)
      spawnSync('gh', ['release', 'delete-asset', releaseTag, name, '--repo', repo, '--yes'], {
        stdio: 'inherit'
      })
    }
  } catch (error) {
    console.warn('could not prune old zips', error)
  }
}

console.log('published', zipName, 'on', releaseTag)
