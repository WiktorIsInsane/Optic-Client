import fs from 'node:fs'
import path from 'node:path'
import type { CosmeticCategory, CosmeticItem, CosmeticsState } from '../../shared/types'
import { folders } from '../paths'

/**
 * Cosmetics shared with the game at `<optic>/shared/cosmetics.json`.
 * The launcher writes the catalog; the launcher and the 1.7.10 client both write `equipped`,
 * so equipping in either one shows up in the other.
 */
export const COSMETIC_CATEGORIES: CosmeticCategory[] = ['icons', 'capes', 'wings', 'bandannas']

/** Items offered in each tab. Add entries here; the game picks them up from the shared file. */
const CATALOG: Record<CosmeticCategory, CosmeticItem[]> = {
  icons: [],
  // Ids must match OpticCosmetics in the 1.7.10 client, which draws them.
  capes: [{ id: 'optic', name: 'Optic Cape' }],
  wings: [{ id: 'ender_dragon', name: 'Ender Dragon Wings' }],
  bandannas: []
}

interface CosmeticsFile {
  version: 1
  catalog: Record<CosmeticCategory, CosmeticItem[]>
  equipped: Record<CosmeticCategory, string>
  updatedAt: number
}

export function sharedCosmeticsPath(): string {
  return path.join(folders.root(), 'shared', 'cosmetics.json')
}

function emptyEquipped(): Record<CosmeticCategory, string> {
  return { icons: '', capes: '', wings: '', bandannas: '' }
}

function readFile(): CosmeticsFile | null {
  try {
    return JSON.parse(fs.readFileSync(sharedCosmeticsPath(), 'utf8')) as CosmeticsFile
  } catch {
    return null
  }
}

function writeFile(equipped: Record<CosmeticCategory, string>): void {
  const payload: CosmeticsFile = { version: 1, catalog: CATALOG, equipped, updatedAt: Date.now() }
  const file = sharedCosmeticsPath()
  fs.mkdirSync(path.dirname(file), { recursive: true })
  const tmp = `${file}.tmp`
  fs.writeFileSync(tmp, JSON.stringify(payload, null, 2), 'utf8')
  fs.renameSync(tmp, file)
}

/** Current state; also rewrites the file when the catalog changed so the game sees new items. */
export function getCosmetics(): CosmeticsState {
  const current = readFile()
  const equipped = emptyEquipped()
  for (const category of COSMETIC_CATEGORIES) {
    const id = current?.equipped?.[category] ?? ''
    // Drop selections that are no longer in the catalog.
    equipped[category] = id && CATALOG[category].some((item) => item.id === id) ? id : ''
  }
  if (!current || JSON.stringify(current.catalog) !== JSON.stringify(CATALOG) || JSON.stringify(current.equipped) !== JSON.stringify(equipped)) {
    writeFile(equipped)
  }
  return { catalog: CATALOG, equipped }
}

export function equipCosmetic(category: CosmeticCategory, id: string): CosmeticsState {
  if (!COSMETIC_CATEGORIES.includes(category)) throw new Error('Unknown cosmetic category')
  if (id && !CATALOG[category].some((item) => item.id === id)) throw new Error('Unknown cosmetic')
  const state = getCosmetics()
  const equipped = { ...state.equipped, [category]: id }
  writeFile(equipped)
  return { catalog: CATALOG, equipped }
}
