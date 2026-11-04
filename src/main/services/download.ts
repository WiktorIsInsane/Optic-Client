import { createHash } from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import AdmZip from 'adm-zip'
import { IpcChannel } from '../../shared/ipc'
import { CLIENT_OWNER, CLIENT_REPO, MC_VERSION } from '../../shared/manifest'
import type { ClientStatus, DownloadProgress, NewsItem } from '../../shared/types'
import { emit } from '../bus'
import { folders } from '../paths'
import { loadStore, updateStore } from '../store'
import { cachedClientManifest, clientTargetRoot, fetchClientManifest, syncClientFiles } from './clientSync'

export interface DownloadOptions {
  label: string
  sha1?: string
  sha256?: string
  overallReceived?: number
  overallTotal?: number
}

let lastProgressAt = 0
let installingClient = false
const GITHUB_UA = 'OpticLauncher'
const GITHUB_API_VERSION = '2022-11-28'
const RELEASE_CACHE_MS = 60_000

function report(progress: DownloadProgress): void {
  const now = Date.now()
  const finished = progress.total > 0 && progress.received === progress.total
  if (now - lastProgressAt < 150 && !finished && progress.received > 0) return
  lastProgressAt = now
  emit(IpcChannel.progress, progress)
}

export async function downloadFile(url: string, dest: string, options: DownloadOptions): Promise<void> {
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  const part = `${dest}.part`
  let received = fs.existsSync(part) ? fs.statSync(part).size : 0
  const headers: Record<string, string> = { 'User-Agent': GITHUB_UA }
  if (received > 0) headers.Range = `bytes=${received}-`
  const response = await fetch(url, { headers, redirect: 'follow' })
  if (response.status === 416) {
    fs.renameSync(part, dest)
    return
  }
  if (!response.ok && response.status !== 206) {
    throw downloadHttpError(response.status, path.basename(dest))
  }
  const totalHeader = response.headers.get('content-length')
  const total = (totalHeader ? Number(totalHeader) : 0) + (response.status === 206 ? received : 0)
  if (response.status === 200) received = 0
  const file = fs.createWriteStream(part, { flags: response.status === 206 ? 'a' : 'w' })
  const body = response.body
  if (!body) throw new Error(`Empty download: ${url}`)
  const started = Date.now()
  const reader = body.getReader()
  try {
    for (;;) {
      const chunk = await reader.read()
      if (chunk.done) break
      file.write(Buffer.from(chunk.value))
      received += chunk.value.byteLength
      const elapsed = Math.max(0.001, (Date.now() - started) / 1000)
      const speed = received / elapsed
      report({
        file: path.basename(dest),
        received,
        total,
        speedBps: speed,
        etaSeconds: speed > 0 && total > 0 ? (total - received) / speed : 0,
        overallReceived: (options.overallReceived ?? 0) + received,
        overallTotal: options.overallTotal ?? total,
        label: options.label
      })
    }
  } finally {
    await new Promise<void>((resolve) => file.end(() => resolve()))
  }
  if (options.sha1) await verify(part, 'sha1', options.sha1)
  if (options.sha256) await verify(part, 'sha256', options.sha256)
  fs.renameSync(part, dest)
}

function verify(file: string, algo: 'sha1' | 'sha256', expected: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const hash = createHash(algo)
    fs.createReadStream(file)
      .on('data', (chunk) => hash.update(chunk))
      .on('error', reject)
      .on('end', () => {
        const actual = hash.digest('hex')
        if (actual.toLowerCase() !== expected.toLowerCase()) {
          fs.rmSync(file, { force: true })
          reject(new Error(`${path.basename(file)} failed ${algo} check.`))
        } else resolve()
      })
  })
}

interface GithubRelease {
  tag_name?: string
  body?: string
  html_url?: string
  published_at?: string
  assets?: { name: string; browser_download_url: string; size: number }[]
}

interface GithubReleaseCache {
  repo: string
  at: number
  value: GithubRelease | null
}

const releaseInFlight = new Map<string, Promise<GithubRelease | null>>()
let releaseCache: GithubReleaseCache | undefined

function downloadHttpError(status: number, file: string): Error {
  if (status === 403) return new Error(`GitHub denied the download for ${file} (forbidden).`)
  if (status === 429) return new Error(`GitHub rate limit reached while downloading ${file}.`)
  return new Error(`Download failed (${status}) for ${file}`)
}

function githubApiHeaders(): Record<string, string> {
  return {
    Accept: 'application/vnd.github+json',
    'User-Agent': GITHUB_UA,
    'X-GitHub-Api-Version': GITHUB_API_VERSION
  }
}

async function githubHttpError(response: Response, repo: string): Promise<Error> {
  const remaining = response.headers.get('x-ratelimit-remaining')
  const resetHeader = response.headers.get('x-ratelimit-reset')
  let message = ''
  try {
    const raw = await response.text()
    if (raw) {
      const body = JSON.parse(raw) as { message?: unknown }
      if (typeof body.message === 'string') message = body.message
    }
  } catch {
    /* ignore a non-JSON GitHub error page */
  }
  const rateLimited = remaining === '0' || /rate limit/i.test(message) || /secondary rate/i.test(message)
  if (response.status === 403 || response.status === 429) {
    if (rateLimited) {
      const until =
        resetHeader && /^\d+$/.test(resetHeader)
          ? ` Try again after ${new Date(Number(resetHeader) * 1000).toLocaleTimeString()}.`
          : ' Try again in a few minutes.'
      return new Error(`GitHub rate limit reached.${until}`)
    }
    if (response.status === 403) return new Error('GitHub denied the client update check (forbidden).')
  }
  return new Error(message || `GitHub returned ${response.status} for ${repo}.`)
}

async function fetchGithubLatest(repo: string): Promise<{ release: GithubRelease | null; error?: Error }> {
  const response = await fetch(`https://api.github.com/repos/${CLIENT_OWNER}/${repo}/releases/latest`, {
    headers: githubApiHeaders()
  })
  if (response.status === 404) return { release: null }
  if (response.ok) {
    const release = (await response.json()) as GithubRelease
    return { release }
  }
  return { release: null, error: await githubHttpError(response, repo) }
}

async function loadLatestRelease(repo: string): Promise<GithubRelease | null> {
  const result = await fetchGithubLatest(repo)
  if (result.error) throw result.error
  return result.release
}

async function latestRelease(repo: string, force = false): Promise<GithubRelease | null> {
  const now = Date.now()
  if (!force && releaseCache && releaseCache.repo === repo && now - releaseCache.at < RELEASE_CACHE_MS) {
    return releaseCache.value
  }
  const existing = releaseInFlight.get(repo)
  if (existing) return existing
  const task = loadLatestRelease(repo)
    .then((value) => {
      releaseCache = { repo, at: Date.now(), value }
      rememberRelease(value)
      return value
    })
    .finally(() => {
      releaseInFlight.delete(repo)
    })
  releaseInFlight.set(repo, task)
  return task
}

function configuredDevRuntime(): string {
  return loadStore().settings.devRuntime || process.env.LUNAR_REVAMPED_RUNTIME || ''
}

function clientLibsRoot(dir: string): string {
  if (!dir || !fs.existsSync(dir)) return ''
  if (fs.existsSync(path.join(dir, 'libs'))) return dir
  let names: string[]
  try {
    names = fs.readdirSync(dir)
  } catch {
    return ''
  }
  if (names.length !== 1) return ''
  const nested = path.join(dir, names[0])
  try {
    if (fs.statSync(nested).isDirectory() && fs.existsSync(path.join(nested, 'libs'))) return nested
  } catch {
    return ''
  }
  return ''
}

export function runtimeRoot(): string {
  const data = loadStore()
  const installed = path.join(folders.versions(), data.installedClientVersion || 'none')
  if (data.installedClientVersion) {
    const tree = clientLibsRoot(installed)
    if (tree) return tree
  }
  const dev = clientLibsRoot(configuredDevRuntime())
  if (dev) return dev
  return installed
}

let cachedNewsItems: NewsItem[] = []
let remoteRefreshScheduled = false

function newsItemFromRelease(release: GithubRelease): NewsItem {
  return {
    title: release.tag_name ?? 'Update',
    body: (release.body ?? '').trim() || 'No notes for this release.',
    url: release.html_url ?? '',
    publishedAt: release.published_at ?? ''
  }
}

function newsFromRelease(release: GithubRelease | null): NewsItem[] {
  if (!release) return []
  return [newsItemFromRelease(release)]
}

function rememberRelease(release: GithubRelease | null): void {
  cachedNewsItems = newsFromRelease(release)
  if (release && releaseCache?.repo) {
    void refreshNewsFeed(releaseCache.repo).catch(() => undefined)
  }
}

async function refreshNewsFeed(repo: string): Promise<void> {
  try {
    const response = await fetch(
      `https://api.github.com/repos/${CLIENT_OWNER}/${repo}/releases?per_page=8`,
      { headers: githubApiHeaders() }
    )
    if (!response.ok) return
    const releases = (await response.json()) as GithubRelease[]
    if (!Array.isArray(releases) || releases.length === 0) return
    cachedNewsItems = releases.map(newsItemFromRelease)
  } catch {
    /* keep single-item cache */
  }
}

function statusFromLocal(remote = '', changelog = '', errorMessage = ''): ClientStatus {
  const data = loadStore()
  const root = runtimeRoot()
  const hasLibs = fs.existsSync(path.join(root, 'libs'))
  const installed = data.clientFilesVersion || ''
  let kind: ClientStatus['kind'] = hasLibs ? 'ready' : 'missing'
  let message = 'Ready'
  if (installingClient) {
    kind = 'downloading'
    message = remote ? `Downloading ${remote}` : 'Downloading client'
  } else if (clientSyncDisabled()) {
    message = 'Dev runtime (client auto-update off)'
  } else if (!data.installedClientVersion && configuredDevRuntime() && hasLibs) {
    kind = 'ready'
    message = 'Using local development runtime'
  } else if (remote && hasLibs && remote !== installed) {
    kind = 'update'
    message = `Update available: ${remote} (installs when you press Play)`
  } else if (kind === 'missing') {
    message = errorMessage || (remote ? `Install ${remote}` : 'No client release published yet')
    if (errorMessage && !hasLibs) kind = 'error'
  } else {
    message = installed ? `Client ${installed}` : 'Local runtime'
  }
  return {
    kind,
    installedVersion: installed || data.installedClientVersion,
    remoteVersion: remote,
    message,
    runtimeRoot: root,
    changelog
  }
}

/**
 * Dev opt-out: a file named `optic-dev` in the runtime folder (written by
 * scripts/install-optic-notch.mjs) stops the launcher from replacing locally built jars.
 */
export function clientSyncDisabled(): boolean {
  return fs.existsSync(path.join(clientTargetRoot(), 'optic-dev'))
}

export function localClientStatus(): ClientStatus {
  const manifest = cachedClientManifest()
  return statusFromLocal(manifest?.version ?? '', manifest?.notes ?? '')
}

export function cachedNews(): NewsItem[] {
  return cachedNewsItems
}

export function scheduleClientStatusRefresh(): void {
  if (cachedClientManifest() && releaseCache && Date.now() - releaseCache.at < RELEASE_CACHE_MS) return
  if (remoteRefreshScheduled) return
  remoteRefreshScheduled = true
  void clientStatus()
    .then((status) => emit(IpcChannel.clientStatus, status))
    .finally(() => {
      remoteRefreshScheduled = false
    })
}

export async function clientStatus(): Promise<ClientStatus> {
  // News comes from the launcher releases; it must not block the client status.
  void latestRelease(CLIENT_REPO).catch(() => null)
  try {
    const manifest = await fetchClientManifest()
    return statusFromLocal(manifest?.version ?? '', manifest?.notes ?? '')
  } catch (error) {
    const detail = error instanceof Error ? error.message : 'Could not check for updates'
    if (fs.existsSync(path.join(runtimeRoot(), 'libs'))) {
      return statusFromLocal('', '', detail)
    }
    return statusFromLocal('', '', installingClient ? 'Downloading client' : detail)
  }
}

export async function news(): Promise<NewsItem[]> {
  if (cachedNewsItems.length && releaseCache && Date.now() - releaseCache.at < RELEASE_CACHE_MS) {
    return cachedNewsItems
  }
  try {
    const release = await latestRelease(CLIENT_REPO)
    rememberRelease(release)
    return cachedNewsItems
  } catch {
    return cachedNewsItems
  }
}

export interface ClientPlayPrepare {
  warning: string
}

function libsReady(dir: string): boolean {
  return Boolean(clientLibsRoot(dir))
}

/** Runs on every Play: pull changed 1.7.10 / 1.8.9 client files before launch. */
export async function ensureClientBeforePlay(): Promise<ClientPlayPrepare> {
  if (clientSyncDisabled()) return { warning: '' }
  const hadLibs = libsReady(runtimeRoot())
  installingClient = true
  try {
    const result = await syncClientFiles(true)
    if (result.updated.length) {
      emit(IpcChannel.toast, `Optic client updated to ${result.version}`)
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'Could not check for client updates'
    if (hadLibs || libsReady(configuredDevRuntime())) return { warning: message }
    throw new Error(message)
  } finally {
    installingClient = false
    emit(IpcChannel.clientStatus, localClientStatus())
  }
  if (libsReady(runtimeRoot()) || libsReady(configuredDevRuntime())) return { warning: '' }
  throw new Error('No Optic client release published yet.')
}

/** "Install / Update client" button: same file sync that Play runs. */
export async function installLatestClient(): Promise<ClientStatus> {
  if (clientSyncDisabled()) return clientStatus()
  installingClient = true
  try {
    await syncClientFiles(true)
  } finally {
    installingClient = false
  }
  const status = await clientStatus()
  emit(IpcChannel.clientStatus, status)
  return status
}

export function rollbackClient(): ClientStatus {
  const data = loadStore()
  if (!data.previousClientVersion) throw new Error('No previous build to roll back to.')
  const previous = path.join(folders.versions(), data.previousClientVersion)
  if (!fs.existsSync(previous)) throw new Error('Previous build files are missing.')
  updateStore((store) => {
    const current = store.installedClientVersion
    store.installedClientVersion = store.previousClientVersion
    store.previousClientVersion = current
  })
  return {
    kind: 'ready',
    installedVersion: loadStore().installedClientVersion,
    remoteVersion: '',
    message: `Rolled back to ${loadStore().installedClientVersion}`,
    runtimeRoot: runtimeRoot(),
    changelog: ''
  }
}

interface MojangArtifact {
  url: string
  path: string
  sha1: string
  size: number
}

interface MojangLibrary {
  name: string
  downloads?: { artifact?: MojangArtifact; classifiers?: Record<string, MojangArtifact> }
  natives?: Record<string, string>
  rules?: { action: string; os?: { name?: string } }[]
}

function libAllowed(lib: MojangLibrary): boolean {
  if (!lib.rules) return true
  let allow = false
  for (const rule of lib.rules) {
    const osName = rule.os?.name
    const matches = !osName || osName === (process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'osx' : 'linux')
    if (matches) allow = rule.action === 'allow'
  }
  return allow
}

async function mapPool<T>(items: T[], concurrency: number, worker: (item: T) => Promise<void>): Promise<void> {
  if (!items.length) return
  let next = 0
  const run = async () => {
    for (;;) {
      const index = next
      next += 1
      if (index >= items.length) return
      await worker(items[index])
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, items.length) }, () => run()))
}

/** What the launcher needs to start a plain Mojang version (1.7.10). */
export interface LegacyVersion {
  id: string
  jar: string
  libraries: string[]
  nativesDir: string
  assetsDir: string
  assetIndex: string
  mainClass: string
  minecraftArguments: string
}

/** OptiFine runs as a launchwrapper tweaker; vanilla 1.7.10 does not ship these. */
const LAUNCHWRAPPER_LIBS: MojangArtifact[] = [
  {
    url: 'https://libraries.minecraft.net/net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar',
    path: 'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar',
    sha1: '',
    size: 0
  },
  {
    url: 'https://libraries.minecraft.net/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar',
    path: 'org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar',
    sha1: '',
    size: 0
  }
]

export function launchwrapperJars(runtime: string): string[] {
  return LAUNCHWRAPPER_LIBS.map((lib) => path.join(runtime, 'libs', 'vanilla', 'libraries', lib.path))
}

function nativeClassifier(lib: MojangLibrary): string {
  const os = process.platform === 'win32' ? 'windows' : process.platform === 'darwin' ? 'osx' : 'linux'
  const key = lib.natives?.[os]
  return key ? key.replace('${arch}', process.arch === 'ia32' ? '32' : '64') : ''
}

/**
 * Download a plain Mojang version (game jar, libraries, natives, assets) into the shared
 * vanilla folder. Files already on disk are kept, so later launches only read the version json.
 */
export async function ensureLegacyVersion(runtime: string, id: string, prepare: boolean): Promise<LegacyVersion> {
  const libRoot = path.join(runtime, 'libs', 'vanilla')
  const metaPath = path.join(libRoot, 'versions', id, `${id}.json`)
  const jar = path.join(libRoot, 'versions', id, `${id}.jar`)
  const nativesDir = path.join(libRoot, 'natives', id)
  if (!fs.existsSync(metaPath)) {
    if (!prepare) throw new Error(`Minecraft ${id} is not downloaded yet. Press Play to download it.`)
    const manifestRes = await fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
    const manifest = (await manifestRes.json()) as { versions: { id: string; url: string }[] }
    const version = manifest.versions.find((item) => item.id === id)
    if (!version) throw new Error(`Mojang manifest has no ${id}.`)
    const text = await (await fetch(version.url)).text()
    fs.mkdirSync(path.dirname(metaPath), { recursive: true })
    fs.writeFileSync(metaPath, text)
  }
  const meta = JSON.parse(fs.readFileSync(metaPath, 'utf8')) as {
    mainClass: string
    minecraftArguments: string
    downloads: { client: { url: string; sha1: string } }
    libraries: MojangLibrary[]
    assetIndex: { id: string; url: string; sha1: string }
  }
  const libraries = meta.libraries.filter(libAllowed)
  const classpath: string[] = []
  for (const lib of libraries) {
    const artifact = lib.downloads?.artifact
    if (artifact?.path && !nativeClassifier(lib)) classpath.push(path.join(libRoot, 'libraries', artifact.path))
  }
  const result: LegacyVersion = {
    id,
    jar,
    libraries: classpath,
    nativesDir,
    assetsDir: path.join(libRoot, 'assets'),
    assetIndex: meta.assetIndex.id,
    mainClass: meta.mainClass,
    minecraftArguments: meta.minecraftArguments
  }
  if (!prepare) return result

  if (!fs.existsSync(jar)) {
    await downloadFile(meta.downloads.client.url, jar, { label: `Minecraft ${id}`, sha1: meta.downloads.client.sha1 })
  }
  const wanted: MojangArtifact[] = [...LAUNCHWRAPPER_LIBS]
  const natives: MojangArtifact[] = []
  for (const lib of libraries) {
    const classifier = nativeClassifier(lib)
    if (classifier) {
      const native = lib.downloads?.classifiers?.[classifier]
      if (native) natives.push(native)
    } else if (lib.downloads?.artifact?.url) {
      wanted.push(lib.downloads.artifact)
    }
  }
  let overall = 0
  const overallTotal = [...wanted, ...natives].reduce((sum, item) => sum + (item.size ?? 0), 0)
  await mapPool([...wanted, ...natives], 8, async (item) => {
    const dest = path.join(libRoot, 'libraries', item.path)
    if (!fs.existsSync(dest)) {
      await downloadFile(item.url, dest, {
        label: item.path,
        sha1: item.sha1 || undefined,
        overallReceived: overall,
        overallTotal
      })
    }
    overall += item.size ?? 0
  })
  const lwjglMarker = process.platform === 'win32' ? 'lwjgl64.dll' : process.platform === 'darwin' ? 'liblwjgl.dylib' : 'liblwjgl64.so'
  if (!fs.existsSync(path.join(nativesDir, lwjglMarker))) {
    fs.mkdirSync(nativesDir, { recursive: true })
    for (const item of natives) {
      const zip = new AdmZip(path.join(libRoot, 'libraries', item.path))
      for (const entry of zip.getEntries()) {
        if (entry.isDirectory || entry.entryName.startsWith('META-INF/')) continue
        zip.extractEntryTo(entry, nativesDir, false, true)
      }
    }
  }

  const indexPath = path.join(libRoot, 'assets', 'indexes', `${meta.assetIndex.id}.json`)
  if (!fs.existsSync(indexPath)) {
    await downloadFile(meta.assetIndex.url, indexPath, { label: 'Asset index', sha1: meta.assetIndex.sha1 })
  }
  const index = JSON.parse(fs.readFileSync(indexPath, 'utf8')) as { objects: Record<string, { hash: string }> }
  await mapPool(Object.values(index.objects), 8, async (object) => {
    const hash = object.hash
    const dest = path.join(libRoot, 'assets', 'objects', hash.slice(0, 2), hash)
    if (fs.existsSync(dest)) return
    await downloadFile(`https://resources.download.minecraft.net/${hash.slice(0, 2)}/${hash}`, dest, {
      label: hash,
      sha1: hash
    })
  })
  return result
}

export async function ensureVanilla(runtime: string): Promise<void> {
  const versionJar = path.join(runtime, 'libs', 'vanilla', 'versions', MC_VERSION, `${MC_VERSION}.jar`)
  if (fs.existsSync(versionJar)) return
  const manifestRes = await fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')
  const manifest = (await manifestRes.json()) as { versions: { id: string; url: string }[] }
  const version = manifest.versions.find((item) => item.id === MC_VERSION)
  if (!version) throw new Error('Mojang manifest has no 1.8.9.')
  const meta = (await (await fetch(version.url)).json()) as {
    downloads: { client: { url: string; sha1: string } }
    libraries: MojangLibrary[]
    assetIndex: { id: string; url: string; sha1: string }
  }
  const libRoot = path.join(runtime, 'libs', 'vanilla')
  await downloadFile(meta.downloads.client.url, versionJar, {
    label: 'Minecraft 1.8.9',
    sha1: meta.downloads.client.sha1
  })
  const libraries = meta.libraries.filter(libAllowed)
  let overall = 0
  const overallTotal = libraries.reduce((sum, lib) => sum + (lib.downloads?.artifact?.size ?? 0), 0)
  await mapPool(libraries, 8, async (lib) => {
    const artifact = lib.downloads?.artifact
    if (!artifact?.url || !artifact.path) return
    const dest = path.join(libRoot, 'libraries', artifact.path)
    if (!fs.existsSync(dest)) {
      await downloadFile(artifact.url, dest, {
        label: artifact.path,
        sha1: artifact.sha1,
        overallReceived: overall,
        overallTotal
      })
    }
    overall += artifact.size ?? 0
  })
  const indexPath = path.join(libRoot, 'assets', 'indexes', `${meta.assetIndex.id}.json`)
  await downloadFile(meta.assetIndex.url, indexPath, { label: 'Asset index', sha1: meta.assetIndex.sha1 })
  const index = JSON.parse(fs.readFileSync(indexPath, 'utf8')) as { objects: Record<string, { hash: string }> }
  const objects = Object.values(index.objects)
  await mapPool(objects, 8, async (object) => {
    const hash = object.hash
    const dest = path.join(libRoot, 'assets', 'objects', hash.slice(0, 2), hash)
    if (fs.existsSync(dest)) return
    const url = `https://resources.download.minecraft.net/${hash.slice(0, 2)}/${hash}`
    await downloadFile(url, dest, { label: hash, sha1: hash })
  })
}
