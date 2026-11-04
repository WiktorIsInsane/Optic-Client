import { createHash } from 'node:crypto'
import fs from 'node:fs'
import path from 'node:path'
import AdmZip from 'adm-zip'
import { IpcChannel } from '../../shared/ipc'
import { CLIENT_FILES_TAG, CLIENT_OWNER, CLIENT_REPO } from '../../shared/manifest'
import type { DownloadProgress } from '../../shared/types'
import { emit } from '../bus'
import { folders } from '../paths'
import { loadStore, updateStore } from '../store'

/**
 * Client file updates for the in-game clients (1.8.9 optic-notch.jar, 1.7.10
 * optic-notch-1.7.10.jar) and the small runtime jars they boot with.
 *
 * `npm run release:client` uploads every file as a content-addressed asset on the
 * `client` release of the Optic repo, plus `optic-files.json` listing path + sha256.
 * The launcher compares hashes and downloads only files that changed, so an update
 * is a few MB, not the whole runtime.
 */

export interface ClientFileEntry {
  /** Path relative to the client runtime folder, always forward slashes (e.g. libs/optic-notch.jar). */
  path: string
  sha256: string
  size: number
  /** Asset name on the `client` release. */
  asset: string
}

export interface ClientFilesManifest {
  schema: 1
  version: string
  publishedAt: string
  notes: string
  files: ClientFileEntry[]
}

export interface ClientSyncResult {
  version: string
  updated: string[]
  root: string
}

/** Folder name under versions/ that a fresh install uses. Matches the existing 1.0.0 runtime. */
const DEFAULT_RUNTIME_ID = '1.0.0'
const UA = 'OpticLauncher'
const MANIFEST_NAME = 'optic-files.json'

function releaseBase(): string {
  return `https://github.com/${CLIENT_OWNER}/${CLIENT_REPO}/releases/download/${CLIENT_FILES_TAG}`
}

let manifestCache: { at: number; value: ClientFilesManifest } | undefined
let syncTask: Promise<ClientSyncResult> | null = null
const hashCache = new Map<string, { size: number; mtimeMs: number; sha256: string }>()

export async function fetchClientManifest(force = false): Promise<ClientFilesManifest | null> {
  if (!force && manifestCache && Date.now() - manifestCache.at < 60_000) return manifestCache.value
  // The query string defeats any CDN cache in front of the redirect.
  const response = await fetch(`${releaseBase()}/${MANIFEST_NAME}?t=${Date.now()}`, {
    headers: { 'User-Agent': UA, Accept: 'application/json' },
    redirect: 'follow',
    signal: AbortSignal.timeout(20_000)
  })
  if (response.status === 404) return null
  if (!response.ok) throw new Error(`Client update check failed (${response.status}).`)
  const value = (await response.json()) as ClientFilesManifest
  if (value?.schema !== 1 || !Array.isArray(value.files)) throw new Error('Client update manifest is invalid.')
  manifestCache = { at: Date.now(), value }
  return value
}

export function cachedClientManifest(): ClientFilesManifest | null {
  return manifestCache?.value ?? null
}

export function installedClientFilesVersion(): string {
  return loadStore().clientFilesVersion ?? ''
}

/** The runtime folder the launcher launches from, created on a fresh install. */
export function clientTargetRoot(): string {
  const data = loadStore()
  const id = data.installedClientVersion || DEFAULT_RUNTIME_ID
  return path.join(folders.versions(), id)
}

function safeJoin(root: string, rel: string): string {
  if (!rel || rel.includes('\\') || rel.startsWith('/') || /^[a-z]:/i.test(rel)) {
    throw new Error(`Refusing client file path ${rel}.`)
  }
  const full = path.resolve(root, ...rel.split('/'))
  const base = path.resolve(root) + path.sep
  if (!full.startsWith(base)) throw new Error(`Refusing client file path ${rel}.`)
  return full
}

function sha256Of(file: string): string {
  const stat = fs.statSync(file)
  const cached = hashCache.get(file)
  if (cached && cached.size === stat.size && cached.mtimeMs === stat.mtimeMs) return cached.sha256
  const sha256 = createHash('sha256').update(fs.readFileSync(file)).digest('hex')
  hashCache.set(file, { size: stat.size, mtimeMs: stat.mtimeMs, sha256 })
  return sha256
}

function upToDate(file: string, entry: ClientFileEntry): boolean {
  try {
    return fs.statSync(file).size === entry.size && sha256Of(file) === entry.sha256.toLowerCase()
  } catch {
    return false
  }
}

function progress(p: DownloadProgress): void {
  emit(IpcChannel.progress, p)
}

async function fetchVerified(url: string, entry: ClientFileEntry): Promise<Buffer> {
  const response = await fetch(url, { headers: { 'User-Agent': UA }, redirect: 'follow' })
  if (!response.ok) throw new Error(`Download failed (${response.status}) for ${entry.path}`)
  const data = Buffer.from(await response.arrayBuffer())
  const actual = createHash('sha256').update(data).digest('hex')
  if (actual !== entry.sha256.toLowerCase()) throw new Error(`${entry.path} failed its sha256 check.`)
  return data
}

function replaceFile(dest: string, data: Buffer): void {
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  const part = `${dest}.part`
  fs.writeFileSync(part, data)
  try {
    fs.renameSync(part, dest)
  } catch (error) {
    fs.rmSync(part, { force: true })
    const code = (error as { code?: string }).code
    if (code === 'EPERM' || code === 'EBUSY' || code === 'EACCES') {
      throw new Error(`${path.basename(dest)} is in use. Close Minecraft, then press Play again.`)
    }
    throw error
  }
}

async function runSync(force: boolean): Promise<ClientSyncResult> {
  const root = clientTargetRoot()
  const manifest = await fetchClientManifest(force)
  if (!manifest) return { version: installedClientFilesVersion(), updated: [], root }

  const stale = manifest.files.filter((entry) => !upToDate(safeJoin(root, entry.path), entry))
  const total = stale.reduce((sum, entry) => sum + entry.size, 0)
  let done = 0
  const label = `Optic client ${manifest.version}`
  for (const entry of stale) {
    const dest = safeJoin(root, entry.path)
    progress({ file: entry.path, received: done, total, speedBps: 0, etaSeconds: 0, overallReceived: done, overallTotal: total, label })
    const data = await fetchVerified(`${releaseBase()}/${encodeURIComponent(entry.asset)}`, entry)
    replaceFile(dest, data)
    done += entry.size
  }
  if (stale.length) {
    progress({ file: '', received: total, total, speedBps: 0, etaSeconds: 0, overallReceived: total, overallTotal: total, label })
  }

  updateStore((data) => {
    if (!data.installedClientVersion) data.installedClientVersion = path.basename(root)
    data.clientFilesVersion = manifest.version
  })
  return { version: manifest.version, updated: stale.map((entry) => entry.path), root }
}

/** Bring the in-game client files up to date. Safe to call often; concurrent calls share one run. */
export function syncClientFiles(force = false): Promise<ClientSyncResult> {
  if (!syncTask) {
    syncTask = runSync(force).finally(() => {
      syncTask = null
    })
  }
  return syncTask
}

/**
 * Minecraft 1.7.10 needs Java 8. Download Temurin 8 (Eclipse Adoptium, GPLv2+CE) into
 * runtimes/temurin-8 the first time someone plays 1.7.10 on a fresh install.
 */
export async function ensureTemurin8(): Promise<void> {
  const runtimes = folders.runtimes()
  const dest = path.join(runtimes, 'temurin-8')
  const exe = process.platform === 'win32' ? 'java.exe' : 'java'
  if (fs.existsSync(path.join(dest, 'bin', exe))) return
  if (process.platform !== 'win32') throw new Error('Java 8 for Minecraft 1.7.10 is missing.')

  const arch = process.arch === 'arm64' ? 'aarch64' : 'x64'
  const url = `https://api.adoptium.net/v3/binary/latest/8/ga/windows/${arch}/jre/hotspot/normal/eclipse`
  progress({ file: 'temurin-8.zip', received: 0, total: 0, speedBps: 0, etaSeconds: 0, overallReceived: 0, overallTotal: 0, label: 'Java 8 for 1.7.10' })
  const response = await fetch(url, { headers: { 'User-Agent': UA }, redirect: 'follow' })
  if (!response.ok) throw new Error(`Could not download Java 8 (${response.status}).`)
  const zip = new AdmZip(Buffer.from(await response.arrayBuffer()))

  const incoming = `${dest}.incoming`
  fs.rmSync(incoming, { recursive: true, force: true })
  zip.extractAllTo(incoming, true)
  // Adoptium zips hold one top folder (jdk8uXXX-jre); lift it to temurin-8.
  const entries = fs.readdirSync(incoming)
  const inner = entries.length === 1 ? path.join(incoming, entries[0]) : incoming
  if (!fs.existsSync(path.join(inner, 'bin', exe))) {
    fs.rmSync(incoming, { recursive: true, force: true })
    throw new Error('Java 8 download did not contain bin/java.exe.')
  }
  fs.rmSync(dest, { recursive: true, force: true })
  fs.renameSync(inner, dest)
  fs.rmSync(incoming, { recursive: true, force: true })
}
