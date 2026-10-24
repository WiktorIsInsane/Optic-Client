import { app } from 'electron'
import fs from 'node:fs'
import path from 'node:path'

function hasNativeOpticClient(root: string): boolean {
  const versions = path.join(root, 'versions')
  if (!fs.existsSync(versions)) return false
  try {
    for (const name of fs.readdirSync(versions)) {
      if (
        fs.existsSync(path.join(versions, name, 'libs', 'optic-client', 'optic-client.json'))
      ) {
        return true
      }
    }
  } catch {
    return false
  }
  return false
}

export function appDataRoot(): string {
  const optic = path.join(app.getPath('appData'), '.optic')
  const legacy = path.join(app.getPath('appData'), '.lunar-remake')
  // Prefer a root that already has the native Optic client. An empty/new .optic folder
  // must not steal launches from a working .lunar-remake install (that fell back to Genesis).
  let root = optic
  if (hasNativeOpticClient(optic)) root = optic
  else if (hasNativeOpticClient(legacy)) root = legacy
  else if (fs.existsSync(optic)) root = optic
  else if (fs.existsSync(legacy)) root = legacy
  fs.mkdirSync(root, { recursive: true })
  return root
}

export function ensureDir(dir: string): string {
  fs.mkdirSync(dir, { recursive: true })
  return dir
}

export const folders = {
  root: () => appDataRoot(),
  instances: () => ensureDir(path.join(appDataRoot(), 'instances')),
  runtimes: () => ensureDir(path.join(appDataRoot(), 'runtimes')),
  libraries: () => ensureDir(path.join(appDataRoot(), 'libraries')),
  assets: () => ensureDir(path.join(appDataRoot(), 'assets')),
  versions: () => ensureDir(path.join(appDataRoot(), 'versions')),
  logs: () => ensureDir(path.join(appDataRoot(), 'logs')),
  cache: () => ensureDir(path.join(appDataRoot(), 'cache')),
  accounts: () => ensureDir(path.join(appDataRoot(), 'accounts'))
}

/** Installed under `%APPDATA%\.optic\runtimes\` (or legacy `.lunar-remake`). */
export const PVP_JAVA_DIR = 'mc-pvp-java25'
/** Azul Zulu 25 runtime used by Optic 1.0.0. The java.exe there is a flag-injecting wrapper. */
export const GRAAL_RUNTIME_DIR = 'zulu-25'
export const ZULU_RUNTIME_DIR = GRAAL_RUNTIME_DIR
/** Stock Temurin 25. This is the Java Optic bundles. */
export const STOCK_JAVA_DIR = 'temurin-25'

export function bundledPvpJavaPath(): string {
  const file = process.platform === 'win32' ? 'java.exe' : 'java'
  const stock = path.join(folders.runtimes(), STOCK_JAVA_DIR, 'bin', file)
  if (fs.existsSync(stock)) return stock
  return path.join(folders.runtimes(), ZULU_RUNTIME_DIR, 'bin', file)
}

export function instanceRoot(id: string): string {
  return ensureDir(path.join(folders.instances(), id))
}

/** Active instance Minecraft game directory (creates it if missing). */
export function resolveGameDir(activeInstanceId: string, customGameDir?: string): string {
  const custom = customGameDir?.trim()
  if (custom) return ensureDir(custom)
  return ensureDir(path.join(instanceRoot(activeInstanceId), 'game'))
}
