import { safeStorage } from 'electron'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import Store from 'electron-store'
import { DEFAULT_ACCENT, isStockLauncherRam, optimalGameRamMb } from '../shared/jvm'
import type { Account, Instance, LaunchSettings } from '../shared/types'
import { bundledPvpJavaPath, folders } from './paths'
import { mergeSharedIntoStore, writeSharedAccounts } from './services/sharedAccounts'

export interface Persisted {
  accounts: Account[]
  activeAccountId: string
  instances: Instance[]
  activeInstanceId: string
  settings: LaunchSettings
  installedClientVersion: string
  previousClientVersion: string
}

const REMOTE_RUNTIME = 'F:\\Projects\\LunarClientremake-main'

function resolveDefaultJava(): string {
  const bundled = bundledPvpJavaPath()
  if (fs.existsSync(bundled)) return bundled
  // Dev fallback before first bundled install completes.
  return ''
}

export function defaultSettings(): LaunchSettings {
  const dev = process.env.LUNAR_REVAMPED_RUNTIME || (fs.existsSync(REMOTE_RUNTIME) ? REMOTE_RUNTIME : '')
  const javaPath = resolveDefaultJava()
  return {
    minRamMb: optimalGameRamMb(Math.round(os.totalmem() / (1024 * 1024))),
    maxRamMb: optimalGameRamMb(Math.round(os.totalmem() / (1024 * 1024))),
    jvmArgs: '',
    // Light GraalVM 17 kit; keep preset default (no over-tuned G1 stack).
    jvmPreset: 'default',
    gameArgs: '',
    width: 1280,
    height: 720,
    fullscreen: false,
    borderless: false,
    rememberSize: true,
    gameDir: '',
    env: [],
    preLaunch: '',
    postExit: '',
    wrapper: '',
    closeOnLaunch: false,
    showConsole: false,
    debugMode: false,
    autoLaunchGame: false,
    backupLauncherSettings: false,
    backupGameSettings: false,
    keepOpen: true,
    onLaunch: 'stay',
    javaPath,
    pvpKitPath: '',
    theme: 'dark',
    accent: DEFAULT_ACCENT,
    devRuntime: dev
  }
}

function canCrypt(): boolean {
  try {
    return safeStorage.isEncryptionAvailable()
  } catch {
    return false
  }
}

function seal(value: string): string {
  if (!value) return ''
  if (!canCrypt()) return `plain:${Buffer.from(value, 'utf8').toString('base64')}`
  return `enc:${safeStorage.encryptString(value).toString('base64')}`
}

function open(value: string | undefined): string {
  if (!value) return ''
  try {
    if (value.startsWith('enc:')) {
      return safeStorage.decryptString(Buffer.from(value.slice(4), 'base64'))
    }
    if (value.startsWith('plain:')) {
      return Buffer.from(value.slice(6), 'base64').toString('utf8')
    }
    return value
  } catch {
    // DPAPI key changed / corrupted ciphertext — drop token so launcher still boots.
    return ''
  }
}

interface DiskAccount extends Omit<Account, 'accessToken' | 'refreshToken'> {
  accessToken: string
  refreshToken?: string
}

function defaultData(): Persisted {
  const id = 'default'
  const settings = defaultSettings()
  return {
    accounts: [],
    activeAccountId: '',
    instances: [
      {
        id,
        name: 'Optic',
        minecraftVersion: '1.8.9',
        createdAt: new Date().toISOString(),
        settings: {}
      }
    ],
    activeInstanceId: id,
    settings,
    installedClientVersion: '',
    previousClientVersion: ''
  }
}

let cache: Persisted | null = null
let disk: Store<Persisted> | null = null

function db(): Store<Persisted> {
  if (!disk) {
    disk = new Store<Persisted>({
      cwd: folders.root(),
      name: 'settings',
      defaults: defaultData()
    })
  }
  return disk
}

function shouldAdoptDefaultJava(current: string, bundled: string): boolean {
  if (!current) return true
  if (!bundled) return false
  if (path.normalize(current) === path.normalize(bundled)) return false
  // Migrate older launcher-managed Java 17/21 kits onto bundled Azul Zulu 25.
  if (/mc-pvp-java17|graalvm-ce-17|mc-pvp-java21|graalvm-ce-21|temurin-21|zulu-21|zulu-25/i.test(current)) {
    return true
  }
  return false
}

export function loadStore(): Persisted {
  if (cache) return cache
  const raw = structuredClone(db().store)
  let migrated = false
  raw.accounts = (raw.accounts as DiskAccount[]).map((account) => {
    const accessToken = open(account.accessToken)
    const refreshToken = open(account.refreshToken)
    if ((account.accessToken && !accessToken) || (account.refreshToken && !refreshToken)) {
      migrated = true
    }
    return {
      ...account,
      accessToken,
      refreshToken
    }
  })
  const defaults = defaultSettings()
  const diskSettings = { ...(raw.settings as LaunchSettings) }
  const hadDebugMode = Object.prototype.hasOwnProperty.call(diskSettings, 'debugMode')
  raw.settings = { ...defaults, ...diskSettings }
  // First boot after debugMode landed: stop opening console by default.
  if (!hadDebugMode) {
    raw.settings.debugMode = false
    raw.settings.showConsole = false
    migrated = true
  }
  // Force Optic lime if still on older launcher accents.
  if (
    !raw.settings.accent ||
    raw.settings.accent === '#3ec6ff' ||
    raw.settings.accent === '#1eb8ef'
  ) {
    raw.settings.accent = DEFAULT_ACCENT
    migrated = true
  }
  if (isStockLauncherRam(raw.settings.minRamMb, raw.settings.maxRamMb)) {
    const ram = optimalGameRamMb(Math.round(os.totalmem() / (1024 * 1024)))
    raw.settings.minRamMb = ram
    raw.settings.maxRamMb = ram
    migrated = true
  }
  const currentJava = (raw.settings.javaPath || '').trim()
  const bundled = resolveDefaultJava()
  const targetJava = bundledPvpJavaPath()
  if (shouldAdoptDefaultJava(currentJava, bundled || targetJava)) {
    raw.settings.javaPath = bundled || targetJava
    raw.settings.pvpKitPath = ''
    raw.settings.jvmPreset = 'default'
    migrated = true
  }
  for (const instance of raw.instances || []) {
    if (/^(Lunar Revamped|Lunar Remake|LethalGens( PvP)?|Atlas Client)$/i.test(instance.name || '')) {
      instance.name = 'Optic'
      migrated = true
    }
  }
  cache = raw
  // Pull in-game account switches / newly added accounts into the launcher store.
  try {
    const merged = mergeSharedIntoStore(raw.accounts, raw.activeAccountId)
    if (merged.changed) {
      raw.accounts = merged.accounts
      raw.activeAccountId = merged.activeAccountId
      migrated = true
    }
  } catch {
    /* shared file optional */
  }
  if (migrated) saveStore()
  else {
    try {
      writeSharedAccounts(raw.accounts, raw.activeAccountId)
    } catch {
      /* ignore */
    }
  }
  return raw
}

export function saveStore(): void {
  if (!cache) return
  const sealed: Persisted = {
    ...cache,
    accounts: cache.accounts.map((account) => ({
      ...account,
      accessToken: seal(account.accessToken),
      refreshToken: account.refreshToken ? seal(account.refreshToken) : ''
    }))
  }
  db().store = sealed
  try {
    writeSharedAccounts(cache.accounts, cache.activeAccountId)
  } catch {
    /* shared roster is best-effort */
  }
}

export function updateStore(mutator: (data: Persisted) => void): Persisted {
  const data = loadStore()
  mutator(data)
  saveStore()
  return data
}
