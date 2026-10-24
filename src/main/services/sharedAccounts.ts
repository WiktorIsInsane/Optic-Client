import fs from 'node:fs'
import path from 'node:path'
import type { Account } from '../../shared/types'
import { folders } from '../paths'

export interface SharedAccount {
  id: string
  username: string
  uuid: string
  kind: 'microsoft' | 'offline'
  accessToken?: string
  refreshToken?: string
}

export interface SharedAccountsFile {
  version: 1
  activeAccountId: string
  accounts: SharedAccount[]
  updatedAt: number
}

export function sharedAccountsPath(): string {
  return path.join(folders.root(), 'shared', 'accounts.json')
}

/** Roster + tokens so the game can hot-swap sessions without relaunch. */
export function writeSharedAccounts(accounts: Account[], activeAccountId: string): void {
  const payload: SharedAccountsFile = {
    version: 1,
    activeAccountId,
    accounts: accounts.map((account) => ({
      id: account.id,
      username: account.username,
      uuid: account.uuid,
      kind: account.kind,
      accessToken: account.accessToken || undefined,
      refreshToken: account.refreshToken || undefined
    })),
    updatedAt: Date.now()
  }
  const file = sharedAccountsPath()
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, JSON.stringify(payload, null, 2), 'utf8')
}

export function readSharedAccounts(): SharedAccountsFile | null {
  const file = sharedAccountsPath()
  if (!fs.existsSync(file)) return null
  try {
    return JSON.parse(fs.readFileSync(file, 'utf8')) as SharedAccountsFile
  } catch {
    return null
  }
}

/**
 * Merge accounts the game added (offline / Microsoft) into the launcher store.
 * Tokens from the shared file are imported when the launcher does not have them yet.
 */
export function mergeSharedIntoStore(
  accounts: Account[],
  activeAccountId: string
): { accounts: Account[]; activeAccountId: string; changed: boolean } {
  const shared = readSharedAccounts()
  if (!shared) return { accounts, activeAccountId, changed: false }

  let changed = false
  const byId = new Map(accounts.map((account) => [account.id, account]))

  for (const item of shared.accounts) {
    const existing = byId.get(item.id)
    if (!existing) {
      byId.set(item.id, {
        id: item.id,
        username: item.username,
        uuid: item.uuid,
        kind: item.kind,
        accessToken: item.accessToken || '',
        refreshToken: item.refreshToken || '',
        expiresAt: undefined
      })
      changed = true
      continue
    }
    if (item.accessToken && item.accessToken !== '0' && item.accessToken !== existing.accessToken) {
      existing.accessToken = item.accessToken
      existing.expiresAt = undefined
      changed = true
    }
    if (item.refreshToken && item.refreshToken !== existing.refreshToken) {
      existing.refreshToken = item.refreshToken
      changed = true
    }
  }

  let active = activeAccountId
  if (shared.activeAccountId && byId.has(shared.activeAccountId) && shared.activeAccountId !== active) {
    active = shared.activeAccountId
    changed = true
  }

  return { accounts: [...byId.values()], activeAccountId: active, changed }
}
