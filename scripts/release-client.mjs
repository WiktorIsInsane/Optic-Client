/**
 * Publish an in-game client update (1.8.9 + 1.7.10) that every Optic launcher picks up
 * on its next start or Play press.
 *
 *   npm run release:client -- --notes "Fixed potion icons"            # publish what is installed
 *   npm run release:client -- --build --notes "..."                   # rebuild both clients first
 *   npm run release:client -- --dry-run                               # only write the manifest
 *
 * Options:
 *   --build             run scripts/install-optic-notch.mjs for 1.8.9 and 1.7.10 first
 *   --jdk <path>        JDK for --build (default JAVA_HOME, then ~/.optic-buildjdk)
 *   --version <v>       client version label (default: date stamp, e.g. 2026.11.04-1530)
 *   --notes <text>      shown in the launcher as the client changelog
 *   --runtime <dir>     runtime folder to publish from (default %APPDATA%\.optic\versions\1.0.0)
 *   --dry-run           stage + write release-client/optic-files.json, upload nothing
 *
 * Needs a GitHub token with "Contents: read and write" on WiktorIsInsane/Optic-Client
 * in GH_TOKEN (or GITHUB_TOKEN).
 *
 * How it works: every file is uploaded once as a content-addressed asset
 * (<sha256-prefix>-<name>) on the `client` pre-release, then optic-files.json is replaced.
 * Unchanged files are never re-uploaded, and assets no longer referenced are deleted.
 */
import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const OWNER = 'WiktorIsInsane'
const REPO = 'Optic-Client'
const TAG = 'client'
const MANIFEST = 'optic-files.json'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const args = process.argv.slice(2)
const flag = (name, fallback = '') => {
  const at = args.indexOf(name)
  return at >= 0 && args[at + 1] && !args[at + 1].startsWith('--') ? args[at + 1] : fallback
}
const has = (name) => args.includes(name)

const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming')
const runtime = path.resolve(flag('--runtime', path.join(appData, '.optic', 'versions', '1.0.0')))
const now = new Date()
const pad = (n) => String(n).padStart(2, '0')
const version =
  flag('--version') ||
  `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}-${pad(now.getHours())}${pad(now.getMinutes())}`
const notes = flag('--notes')
const dryRun = has('--dry-run')

function fail(message) {
  console.error(`\n✖ ${message}`)
  process.exit(1)
}

// 1. Optionally rebuild both clients into the runtime.
if (has('--build')) {
  for (const mc of ['1.8.9', '1.7.10']) {
    console.log(`\n▶ Building Optic ${mc}`)
    const jdk = flag('--jdk')
    const result = spawnSync(
      process.execPath,
      [path.join(root, 'scripts', 'install-optic-notch.mjs'), '--mc', mc, ...(jdk ? ['--jdk', jdk] : [])],
      { cwd: root, stdio: 'inherit' }
    )
    if (result.status !== 0) fail(`Build for ${mc} failed.`)
  }
}

// 2. Collect files.
const config = JSON.parse(fs.readFileSync(path.join(root, 'client-files.json'), 'utf8'))
const excluded = (name) =>
  config.exclude.some((pattern) =>
    pattern.startsWith('*') ? name.toLowerCase().endsWith(pattern.slice(1).toLowerCase()) : name === pattern
  )

const files = []
function add(full) {
  const data = fs.readFileSync(full)
  const sha256 = createHash('sha256').update(data).digest('hex')
  const rel = path.relative(runtime, full).split(path.sep).join('/')
  const base = path.basename(full).replace(/[^A-Za-z0-9._-]/g, '_')
  files.push({ path: rel, sha256, size: data.length, asset: `${sha256.slice(0, 12)}-${base}`, full })
}
function walk(full) {
  if (excluded(path.basename(full))) return
  const stat = fs.statSync(full)
  if (stat.isDirectory()) for (const name of fs.readdirSync(full).sort()) walk(path.join(full, name))
  else add(full)
}
for (const rel of config.include) {
  const full = path.join(runtime, ...rel.split('/'))
  if (!fs.existsSync(full)) fail(`Missing ${rel} in ${runtime}. Build/install it first (or pass --build).`)
  walk(full)
}

const manifest = {
  schema: 1,
  version,
  publishedAt: now.toISOString(),
  notes,
  files: files.map(({ full: _full, ...entry }) => entry)
}
const outDir = path.join(root, 'release-client')
fs.mkdirSync(outDir, { recursive: true })
const manifestPath = path.join(outDir, MANIFEST)
fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`)

const totalMb = (files.reduce((sum, f) => sum + f.size, 0) / 1048576).toFixed(1)
console.log(`\nClient ${version}: ${files.length} files, ${totalMb} MB from ${runtime}`)
for (const f of files) console.log(`  ${f.sha256.slice(0, 12)}  ${f.path}`)
console.log(`Manifest: ${manifestPath}`)
if (dryRun) process.exit(0)

// 3. Publish.
const token = process.env.GH_TOKEN || process.env.GITHUB_TOKEN
if (!token) fail('Set GH_TOKEN to a GitHub token with Contents read/write on WiktorIsInsane/Optic-Client.')

const api = `https://api.github.com/repos/${OWNER}/${REPO}`
const headers = {
  Authorization: `Bearer ${token}`,
  Accept: 'application/vnd.github+json',
  'X-GitHub-Api-Version': '2022-11-28',
  'User-Agent': 'optic-release'
}

async function gh(method, url, body) {
  const response = await fetch(url, {
    method,
    headers: body && !(body instanceof Buffer) ? { ...headers, 'Content-Type': 'application/json' } : headers,
    body: body && !(body instanceof Buffer) ? JSON.stringify(body) : body
  })
  if (response.status === 204) return null
  const text = await response.text()
  const json = text ? JSON.parse(text) : null
  if (!response.ok) {
    const error = new Error(`${method} ${url} → ${response.status} ${json?.message ?? text}`)
    error.status = response.status
    throw error
  }
  return json
}

async function upload(release, name, data) {
  const url = `https://uploads.github.com/repos/${OWNER}/${REPO}/releases/${release.id}/assets?name=${encodeURIComponent(name)}`
  const response = await fetch(url, {
    method: 'POST',
    headers: { ...headers, 'Content-Type': 'application/octet-stream', 'Content-Length': String(data.length) },
    body: data
  })
  if (!response.ok) throw new Error(`Upload ${name} failed: ${response.status} ${await response.text()}`)
  return response.json()
}

async function listAssets(release) {
  const all = []
  for (let page = 1; ; page++) {
    const batch = await gh('GET', `${api}/releases/${release.id}/assets?per_page=100&page=${page}`)
    all.push(...batch)
    if (batch.length < 100) return all
  }
}

const body = [`Optic client **${version}**`, '', notes || '_No notes._', '', 'Installed automatically by the Optic launcher. Do not delete this release.'].join('\n')

let release
try {
  release = await gh('GET', `${api}/releases/tags/${TAG}`)
} catch (error) {
  if (error.status !== 404) fail(error.message)
  try {
    release = await gh('POST', `${api}/releases`, {
      tag_name: TAG,
      name: 'Optic client files',
      body,
      prerelease: true,
      make_latest: 'false'
    })
    console.log('Created the `client` pre-release.')
  } catch (createError) {
    if (createError.status === 422) {
      fail('GitHub could not create the `client` release. The repo needs at least one commit on its default branch: push the code first.')
    }
    fail(createError.message)
  }
}

const existing = await listAssets(release)
const byName = new Map(existing.map((asset) => [asset.name, asset]))
let uploaded = 0
for (const file of files) {
  if (byName.has(file.asset)) continue
  process.stdout.write(`  ↑ ${file.path} … `)
  await upload(release, file.asset, fs.readFileSync(file.full))
  uploaded += 1
  console.log('done')
}

// Swap the manifest last so launchers never see a manifest that points at missing files.
const oldManifest = byName.get(MANIFEST)
if (oldManifest) await gh('DELETE', `${api}/releases/assets/${oldManifest.id}`)
await upload(release, MANIFEST, fs.readFileSync(manifestPath))

const keep = new Set([MANIFEST, ...files.map((f) => f.asset)])
for (const asset of existing) {
  if (!keep.has(asset.name)) {
    await gh('DELETE', `${api}/releases/assets/${asset.id}`)
    console.log(`  ✕ removed old ${asset.name}`)
  }
}
await gh('PATCH', `${api}/releases/${release.id}`, { body, prerelease: true, make_latest: 'false' })

console.log(`\n✔ Published client ${version} (${uploaded} new file${uploaded === 1 ? '' : 's'}).`)
console.log('  Launchers install it on their next start or Play press.')
