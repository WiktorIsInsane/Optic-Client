import fs from 'node:fs'
import path from 'node:path'

const FEEL_VERSION = '8'
const MARKER = 'pvp-feel.version'

/** Cheapest OptiFine path: no sky, weather, clouds, or animated sprites. */
const OPTIFINE_PATCH: Record<string, string> = {
  ofSmoothWorld: 'false',
  ofSmoothFps: 'false',
  ofFastRender: 'true',
  ofFastMath: 'true',
  ofRenderRegions: 'true',
  ofLazyChunkLoading: 'true',
  ofSmoothBiomes: 'false',
  ofChunkUpdates: '1',
  ofChunkUpdatesDynamic: 'false',
  ofDynamicFov: 'false',
  ofSmartAnimations: 'false',
  ofAaLevel: '0',
  ofAfLevel: '1',
  ofSky: 'false',
  ofStars: 'false',
  ofSunMoon: 'false',
  ofWeather: 'false',
  ofClouds: '3',
  ofCustomSky: 'false',
  ofCustomEntityModels: 'false',
  ofCustomGuis: 'false',
  ofEmissiveTextures: 'false',
  ofBetterGrass: '3',
  ofConnectedTextures: '3',
  ofDynamicLights: '3',
  ofTrees: '1',
  ofRain: '3',
  ofTranslucentBlocks: '1',
  ofAnimatedWater: '0',
  ofAnimatedLava: '0',
  ofAnimatedFire: 'false',
  ofAnimatedFlame: 'false',
  ofAnimatedExplosion: 'false',
  ofAnimatedPortal: 'false',
  ofAnimatedRedstone: 'false',
  ofAnimatedSmoke: 'false',
  ofAnimatedTerrain: 'false',
  ofAnimatedTextures: 'false',
  ofVoidParticles: 'false',
  ofWaterParticles: 'false',
  ofPortalParticles: 'false',
  ofPotionParticles: 'false',
  ofFireworkParticles: 'false',
  ofDrippingWaterLava: 'false',
  ofRainSplash: 'false',
  ofShowCapes: 'false',
  ofDroppedItems: '0',
  ofVignette: '0',
  ofLagometer: 'false'
}

const VANILLA_PATCH: Record<string, string> = {
  particles: '2',
  bobView: 'false',
  fancyGraphics: 'false',
  ao: '0',
  renderClouds: 'false',
  enableVsync: 'false',
  useVbo: 'true',
  useNativeTransport: 'true',
  entityShadows: 'false',
  mipmapLevels: '0',
  maxFps: '260',
  pauseOnLostFocus: 'false',
  renderDistance: '6'
}

const DISABLE_MODS = [
  'PVP_OPTIMIZER',
  'DELAY_FIX',
  'PARTICLE_CHANGER',
  'ITEM_PHYSICS',
  'MOTION_BLUR',
  '3D_SKINS',
  'COLOR_SATURATION',
  'MENU_BLUR',
  'MINIMAP'
] as const

function removeOptimizer(dir: string): void {
  try {
    fs.rmSync(path.join(dir, 'pvpoptimizer.properties'), { force: true })
  } catch {
    /* ignore */
  }
}

function patchKeyFile(file: string, patch: Record<string, string>, jsonStyle: boolean): void {
  if (!fs.existsSync(file)) return
  if (jsonStyle) {
    try {
      const data = JSON.parse(fs.readFileSync(file, 'utf8')) as Record<string, unknown>
      for (const [key, value] of Object.entries(patch)) data[key] = value
      fs.writeFileSync(file, JSON.stringify(data), 'utf8')
    } catch {
      /* leave user file alone if corrupt */
    }
    return
  }
  const lines = fs.readFileSync(file, 'utf8').split(/\r?\n/)
  const seen = new Set<string>()
  const out = lines.map((line) => {
    const i = line.indexOf(':')
    if (i < 0) return line
    const key = line.slice(0, i)
    if (!(key in patch)) return line
    seen.add(key)
    return `${key}:${patch[key]}`
  })
  for (const [key, value] of Object.entries(patch)) {
    if (!seen.has(key)) out.push(`${key}:${value}`)
  }
  fs.writeFileSync(file, `${out.filter((l, idx) => l.length > 0 || idx < out.length - 1).join('\n').replace(/\n+$/, '')}\n`, 'utf8')
}

function patchModsJson(file: string): void {
  if (!fs.existsSync(file)) return
  try {
    const mods = JSON.parse(fs.readFileSync(file, 'utf8')) as Record<string, Record<string, unknown>>
    for (const id of DISABLE_MODS) {
      if (!mods[id]) continue
      mods[id] = { ...mods[id], enabled: false }
    }
    fs.writeFileSync(file, JSON.stringify(mods), 'utf8')
  } catch {
    /* ignore */
  }
}

/**
 * One-shot video profile: cheapest draw path, and the added optimizer mods turned off.
 * No click delay lives in the game client, not in a mod toggle.
 */
export function applyPvpFeelProfile(gameDir: string, dataDir: string): void {
  removeOptimizer(gameDir)
  removeOptimizer(process.cwd())
  try {
    removeOptimizer(path.dirname(process.execPath))
  } catch {
    /* ignore */
  }

  const marker = path.join(gameDir, MARKER)
  const already = fs.existsSync(marker) && fs.readFileSync(marker, 'utf8').trim() === FEEL_VERSION
  if (already) return

  patchKeyFile(path.join(gameDir, 'optionsof.txt'), OPTIFINE_PATCH, false)
  patchKeyFile(path.join(gameDir, 'options.txt'), VANILLA_PATCH, false)
  patchKeyFile(path.join(gameDir, 'optionsLC.txt'), VANILLA_PATCH, true)

  const settingsGame = path.join(dataDir, 'settings', 'game')
  for (const profile of ['Default', 'Arena PvP']) {
    patchModsJson(path.join(settingsGame, profile, 'mods.json'))
  }

  fs.writeFileSync(marker, FEEL_VERSION, 'utf8')
}
