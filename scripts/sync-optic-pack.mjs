/**
 * Copy Wiktor's live PvP overlay into resources/optic for electron-builder,
 * then brand Lunar Revamped → Optic inside the jar.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { brandLocalPatchesJar } from './brand-optic-client.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.join(__dirname, '..')
const dest = path.join(root, 'resources', 'optic')
const destJar = path.join(dest, 'lunar-localpatches.jar')

const appData = process.env.APPDATA || ''

function findLibs() {
  for (const folder of ['.optic', '.lunar-remake']) {
    const versionsRoot = path.join(appData, folder, 'versions')
    if (!fs.existsSync(versionsRoot)) continue
    const versions = fs
      .readdirSync(versionsRoot, { withFileTypes: true })
      .filter((e) => e.isDirectory() && !e.name.endsWith('.incoming') && !e.name.endsWith('.retiring'))
      .map((e) => e.name)
      .filter((name) => fs.existsSync(path.join(versionsRoot, name, 'libs', 'lunar-localpatches.jar')))
      .sort((a, b) => b.localeCompare(a, undefined, { numeric: true }))
    if (versions[0]) return path.join(versionsRoot, versions[0], 'libs')
  }
  return ''
}

const libs = findLibs()
const patchJar = libs ? path.join(libs, 'lunar-localpatches.jar') : ''
const badgeSrc = libs ? path.join(libs, 'lunar-assets', 'textures', 'assets', 'lunar') : ''
const fallbackBadges = path.join(root, '..', 'client-patches', 'assets', 'lunar-icons')

function copyFile(src, out) {
  fs.mkdirSync(path.dirname(out), { recursive: true })
  fs.copyFileSync(src, out)
  console.log('+', path.relative(root, out))
}

if (!patchJar || !fs.existsSync(patchJar)) {
  if (fs.existsSync(destJar)) {
    const hits = brandLocalPatchesJar(destJar)
    console.log('Live patch jar missing; branded committed Optic pack ->', dest, `(${hits})`)
    process.exit(0)
  }
  console.error('Missing patched jar under %APPDATA%\\.optic or .lunar-remake\\versions')
  console.error('Run client-patches\\build.ps1 first, then launch once.')
  process.exit(1)
}

fs.rmSync(dest, { recursive: true, force: true })
fs.mkdirSync(dest, { recursive: true })

copyFile(patchJar, path.join(dest, 'lunar-localpatches.jar'))

const badgeDir = fs.existsSync(path.join(badgeSrc, 'badges.json')) ? badgeSrc : null
const iconDir = fs.existsSync(path.join(fallbackBadges, 'badges-slim.json')) ? fallbackBadges : null

if (badgeDir) {
  copyFile(path.join(badgeDir, 'badges.json'), path.join(dest, 'textures', 'badges.json'))
  for (const n of ['owner.webp', 'partner.webp', 'user.webp']) {
    const p = path.join(badgeDir, 'badges', n)
    if (fs.existsSync(p)) copyFile(p, path.join(dest, 'textures', 'badges', n))
  }
} else if (iconDir) {
  copyFile(path.join(iconDir, 'badges-slim.json'), path.join(dest, 'textures', 'badges.json'))
  for (const n of ['owner.webp', 'partner.webp', 'user.webp']) {
    const p = path.join(iconDir, n)
    if (fs.existsSync(p)) copyFile(p, path.join(dest, 'textures', 'badges', n))
  }
}

fs.writeFileSync(
  path.join(dest, 'pack.json'),
  JSON.stringify(
    {
      id: 'optic',
      name: 'Optic',
      version: '1.0.0',
      builtAt: new Date().toISOString(),
      includes: [
        'Butter AttackScheduler (absolute 50ms grid)',
        'Sticky WASD + mods menu last-mod memory',
        'Soft 2-tick visual KB / hurt-cam',
        'Lunar Icons (Owner/Partner/User)',
        'PvP Optimizer defaults (freshRaycast)'
      ]
    },
    null,
    2
  ),
  'utf8'
)

const hits = brandLocalPatchesJar(destJar)
console.log('Optic pack synced + branded ->', dest, `(${hits} replacements)`)

// Also brand every installed runtime so the next game launch shows Optic.
for (const folder of ['.optic', '.lunar-remake']) {
  const versionsRoot = path.join(appData, folder, 'versions')
  if (!fs.existsSync(versionsRoot)) continue
  for (const ver of fs.readdirSync(versionsRoot)) {
    for (const rel of ['lunar-localpatches.jar', path.join('multiver-full', 'lunar-localpatches.jar')]) {
      const live = path.join(versionsRoot, ver, 'libs', rel)
      if (!fs.existsSync(live)) continue
      try {
        const n = brandLocalPatchesJar(live)
        if (n > 0) console.log('branded live', live, n)
      } catch (error) {
        console.warn('could not brand', live, error instanceof Error ? error.message : error)
      }
    }
  }
}
