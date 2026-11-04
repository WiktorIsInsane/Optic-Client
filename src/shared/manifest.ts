export interface ManifestFile {
  path: string
  sha256: string
  size: number
}

export interface ClientManifest {
  version: string
  minecraft: '1.8.9'
  files: ManifestFile[]
}

/**
 * Everything publishes to github.com/WiktorIsInsane/Optic-Client:
 * - launcher releases (tag vX.Y.Z): Optic-Setup.exe + latest.yml for electron-updater
 * - the `client` pre-release: in-game client files + optic-files.json (see clientSync.ts)
 */
export const CLIENT_OWNER = 'WiktorIsInsane'
export const CLIENT_REPO = 'Optic-Client'
/** Pre-release tag that holds the client files. Pre-release so it never becomes "latest". */
export const CLIENT_FILES_TAG = 'client'

/** @deprecated use CLIENT_OWNER — kept for older imports */
export const GITHUB_OWNER = CLIENT_OWNER

/** Launcher installer + electron-updater feed (Optic-Setup.exe). */
export const LAUNCHER_OWNER = 'WiktorIsInsane'
export const LAUNCHER_REPO = 'Optic-Client'

export const MC_VERSION = '1.8.9'
/** Official Minecraft launcher client. Sign-in is Minecraft's, and the account is stored only in this app. */
export const MS_CLIENT_ID = '00000000402b5328'
