/**
 * Brand the in-game main menu as Optic: logos, footer text, empty locker
 * cosmetics, and strip Satellite.
 */
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'
import AdmZip from 'adm-zip'
import { brandLocalPatchesJar } from './brand-optic-client.mjs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')
const opticLogo = path.join(root, 'src', 'renderer', 'assets', 'logo.png')

function versionsRoots() {
  const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming')
  const out = []
  for (const folder of ['.optic', '.lunar-remake']) {
    const versions = path.join(appData, folder, 'versions')
    if (!fs.existsSync(versions)) continue
    for (const ver of fs.readdirSync(versions)) {
      const libs = path.join(versions, ver, 'libs')
      if (fs.existsSync(path.join(libs, 'lunar-localpatches.jar'))) out.push(libs)
    }
  }
  return out
}

function replaceAllEqual(buf, from, to) {
  const a = Buffer.from(from, 'utf8')
  const b = Buffer.from(to, 'utf8')
  if (a.length !== b.length) throw new Error(`len ${from.length} != ${to.length}`)
  let hits = 0
  let idx = 0
  const out = Buffer.from(buf)
  while ((idx = out.indexOf(a, idx)) >= 0) {
    b.copy(out, idx)
    idx += a.length
    hits++
  }
  return { buf: out, hits }
}

const STRING_REPLACEMENTS = [
  // Footer / brand (keep byte length for class constants)
  ['Lunar Client ', 'Optic 1.8.9   '], // 13 → "Optic 1.8.9   " + "(v1.0.0)" ≈ Optic 1.8.9 (v1.0.0)
  ['Lunar Client', 'Optic       '],
  [' (v', ' ('], // may fail length — handled below only if equal
  ['gui.components.btnSatellite', 'gui.components.btnOpticGone\0'] // 27 vs 26 — skip, use other approach
]

function brandBuffer(buf) {
  let next = Buffer.from(buf)
  let total = 0
  const pairs = [
    // "Lunar Client 1.8.9 (v1.0.0)" → "Optic        1.8.9 (v1.0.0)"
    ['Lunar Client ', 'Optic        '], // 13
    ['Lunar Client', 'Optic       '] // 12
  ]
  for (const [from, to] of pairs) {
    try {
      const r = replaceAllEqual(next, from, to)
      next = r.buf
      total += r.hits
    } catch {
      /* skip unequal */
    }
  }
  return { buf: next, hits: total }
}

function brandJarFile(jarPath, { classesOnly = false } = {}) {
  if (!fs.existsSync(jarPath)) return 0
  const zip = new AdmZip(jarPath)
  let hits = 0
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory) continue
    const name = entry.entryName.replace(/\\/g, '/')
    if (classesOnly && !name.endsWith('.class')) continue
    if (!classesOnly && !/\.(class|json|txt|properties|js)$/i.test(name)) continue
    let raw
    try {
      raw = entry.getData()
    } catch {
      continue
    }
    const { buf, hits: n } = brandBuffer(raw)
    if (n > 0) {
      zip.updateFile(entry.entryName, buf)
      hits += n
    }
  }
  if (hits > 0) zip.writeZip(jarPath)
  return hits
}

function emptyCosmetics(libs) {
  const cosmetics = path.join(libs, 'lunar-assets', 'textures', 'assets', 'lunar', 'cosmetics.json')
  fs.mkdirSync(path.dirname(cosmetics), { recursive: true })
  fs.writeFileSync(cosmetics, '[]\n', 'utf8')
  const indexes = path.join(libs, 'lunar-assets', 'textures', 'assets', 'lunar', 'cosmetics', 'indexes')
  if (fs.existsSync(indexes)) {
    for (const f of fs.readdirSync(indexes)) {
      if (f.endsWith('.json')) fs.writeFileSync(path.join(indexes, f), '[]\n', 'utf8')
    }
  }
  console.log('emptied cosmetics', cosmetics)
}

function resizeLogoPngs(outDir) {
  fs.mkdirSync(outDir, { recursive: true })
  const ps = `
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile(${JSON.stringify(opticLogo)})
function Save-Square([int]\\$size, [string]\\$dest) {
  \\$bmp = New-Object System.Drawing.Bitmap \\$size, \\$size
  \\$g = [System.Drawing.Graphics]::FromImage(\\$bmp)
  \\$g.Clear([System.Drawing.Color]::Transparent)
  \\$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  \\$g.DrawImage(\\$src, 0, 0, \\$size, \\$size)
  \\$g.Dispose()
  \\$bmp.Save(\\$dest, [System.Drawing.Imaging.ImageFormat]::Png)
  \\$bmp.Dispose()
}
Save-Square 128 ${JSON.stringify(path.join(outDir, 'logo_128.png'))}
Save-Square 64 ${JSON.stringify(path.join(outDir, 'logo_64.png'))}
Save-Square 22 ${JSON.stringify(path.join(outDir, 'logo_22.png'))}
\\$src.Dispose()
`
  // Fix accidental escapes - write clean script file instead
  const script = path.join(outDir, '_resize.ps1')
  fs.writeFileSync(
    script,
    `
Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile('${opticLogo.replace(/'/g, "''")}')
function Save-Square([int]$size, [string]$dest) {
  $bmp = New-Object System.Drawing.Bitmap $size, $size
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::Transparent)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($src, 0, 0, $size, $size)
  $g.Dispose()
  $bmp.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}
Save-Square 128 '${path.join(outDir, 'logo_128.png').replace(/'/g, "''")}'
Save-Square 64 '${path.join(outDir, 'logo_64.png').replace(/'/g, "''")}'
Save-Square 22 '${path.join(outDir, 'logo_22.png').replace(/'/g, "''")}'
$src.Dispose()
`,
    'utf8'
  )
  const r = spawnSync('powershell', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', script], {
    stdio: 'inherit'
  })
  if (r.status !== 0) throw new Error('logo resize failed')
}

function writeLogoSvg(dest, pngPath) {
  const b64 = fs.readFileSync(pngPath).toString('base64')
  const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 128 128" width="128" height="128">
  <image width="128" height="128" xlink:href="data:image/png;base64,${b64}" href="data:image/png;base64,${b64}"/>
</svg>
`
  fs.mkdirSync(path.dirname(dest), { recursive: true })
  fs.writeFileSync(dest, svg, 'utf8')
}

function installLogos(libs, generated) {
  const tex = path.join(libs, 'lunar-assets', 'textures', 'assets', 'lunar')
  const map = [
    ['animatedlogo/128/logo_128_no_stars.png', 'logo_128.png'],
    ['animatedlogo/128/logo-anniversary-hat.png', 'logo_128.png'],
    ['animatedlogo/128/logo-santa-hat.png', 'logo_128.png'],
    ['animatedlogo/64/logo_64_no_stars.png', 'logo_64.png'],
    ['icons/assets/logo-22x22.png', 'logo_22.png'],
    ['icons/mainmenu/easter_eggs/lunar-client.png', 'logo_128.png'],
    ['logo/logo-100x100.png', 'logo_128.png'],
    ['logo/logo-128x117.png', 'logo_128.png'],
    ['logo/logo-256x256.png', 'logo_128.png'],
    ['logo/logo-64x64.png', 'logo_64.png'],
    ['logo/logo-32x32.png', 'logo_22.png'],
    ['logo/logo-24x24.png', 'logo_22.png'],
    ['logo/logo-16x16.png', 'logo_22.png']
  ]
  // stamp star frames with the same Optic mark (static)
  for (let i = 1; i <= 8; i++) {
    map.push([`animatedlogo/128/logo_128_star_${i}.png`, 'logo_128.png'])
    map.push([`animatedlogo/64/logo_64_star_${i}.png`, 'logo_64.png'])
  }
  for (const [rel, srcName] of map) {
    const dest = path.join(tex, rel)
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.copyFileSync(path.join(generated, srcName), dest)
  }
  writeLogoSvg(path.join(tex, 'icons/mainmenu/logo.svg'), path.join(generated, 'logo_128.png'))
  console.log('installed Optic logos under', tex)
}

function patchLunarJarLogos(libs, generated) {
  const jarPath = path.join(libs, 'multiver-full', 'lunar.jar')
  if (!fs.existsSync(jarPath)) return
  const zip = new AdmZip(jarPath)
  const logo128 = fs.readFileSync(path.join(generated, 'logo_128.png'))
  const logo64 = fs.readFileSync(path.join(generated, 'logo_64.png'))
  const logo22 = fs.readFileSync(path.join(generated, 'logo_22.png'))
  const logoSvg = Buffer.from(
    `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 128 128" width="128" height="128">
  <image width="128" height="128" xlink:href="data:image/png;base64,${logo128.toString('base64')}" href="data:image/png;base64,${logo128.toString('base64')}"/>
</svg>
`,
    'utf8'
  )
  let n = 0
  for (const entry of zip.getEntries()) {
    const name = entry.entryName.replace(/\\/g, '/')
    if (
      /animatedlogo\/128\/logo_128/.test(name) ||
      /animatedlogo\/128\/logo-(anniversary|santa)-hat\.png$/.test(name) ||
      /mainmenu\/easter_eggs\/lunar-client\.png$/.test(name) ||
      /\/logo\/logo-(100x100|128x117|256x256)\.png$/.test(name)
    ) {
      zip.updateFile(entry.entryName, logo128)
      n++
    } else if (/animatedlogo\/64\/logo_64/.test(name) || /\/logo\/logo-64x64\.png$/.test(name)) {
      zip.updateFile(entry.entryName, logo64)
      n++
    } else if (
      /icons\/assets\/logo-22x22\.png$/.test(name) ||
      /\/logo\/logo-(16x16|24x24|32x32)\.png$/.test(name)
    ) {
      zip.updateFile(entry.entryName, logo22)
      n++
    } else if (/icons\/mainmenu\/logo\.svg$/.test(name)) {
      zip.updateFile(entry.entryName, logoSvg)
      n++
    }
  }
  if (n > 0) {
    zip.writeZip(jarPath)
    console.log('patched lunar.jar logos', n)
  }
}

function patchUiPlanet(libs, generated) {
  const uiRoot = path.join(libs, 'lunar-assets', 'ui')
  if (!fs.existsSync(uiRoot)) return
  const logo = path.join(generated, 'logo_128.png')
  for (const hash of fs.readdirSync(uiRoot)) {
    const planet = path.join(uiRoot, hash, 'assets', 'planet.png')
    if (fs.existsSync(planet)) {
      fs.copyFileSync(logo, planet)
      console.log('replaced UI planet.png', planet)
    }
  }
}

/** Stage branded textures into resources/optic so applyOpticPack deploys them on launch. */
function stageOpticPackAssets(generated) {
  const packTex = path.join(root, 'resources', 'optic', 'textures', 'assets', 'lunar')
  const map = [
    ['animatedlogo/128/logo_128_no_stars.png', 'logo_128.png'],
    ['animatedlogo/128/logo-anniversary-hat.png', 'logo_128.png'],
    ['animatedlogo/128/logo-santa-hat.png', 'logo_128.png'],
    ['animatedlogo/64/logo_64_no_stars.png', 'logo_64.png'],
    ['icons/assets/logo-22x22.png', 'logo_22.png'],
    ['icons/mainmenu/easter_eggs/lunar-client.png', 'logo_128.png']
  ]
  for (let i = 1; i <= 8; i++) {
    map.push([`animatedlogo/128/logo_128_star_${i}.png`, 'logo_128.png'])
    map.push([`animatedlogo/64/logo_64_star_${i}.png`, 'logo_64.png'])
  }
  for (const [rel, srcName] of map) {
    const dest = path.join(packTex, rel)
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.copyFileSync(path.join(generated, srcName), dest)
  }
  writeLogoSvg(path.join(packTex, 'icons/mainmenu/logo.svg'), path.join(generated, 'logo_128.png'))
  fs.writeFileSync(path.join(packTex, 'cosmetics.json'), '[]\n', 'utf8')
  const packJson = path.join(root, 'resources', 'optic', 'pack.json')
  const meta = fs.existsSync(packJson) ? JSON.parse(fs.readFileSync(packJson, 'utf8')) : {}
  meta.version = meta.version || '1.0.0'
  meta.builtAt = new Date().toISOString()
  meta.menuBrand = 'optic'
  fs.writeFileSync(packJson, JSON.stringify(meta, null, 2) + '\n', 'utf8')
  console.log('staged Optic menu brand into resources/optic')
}

function patchLangSatellite(libs) {
  const jarPath = path.join(libs, 'multiver-full', 'lunar-lang.jar')
  if (!fs.existsSync(jarPath)) return
  const zip = new AdmZip(jarPath)
  let files = 0
  for (const entry of zip.getEntries()) {
    if (!entry.entryName.endsWith('.json')) continue
    let text = entry.getData().toString('utf8')
    const before = text
    text = text.replace(/"btnSatellite"\s*:\s*"[^"]*"/g, '"btnSatellite": ""')
    text = text.replace(/Lunar Client/g, 'Optic')
    if (text !== before) {
      zip.updateFile(entry.entryName, Buffer.from(text, 'utf8'))
      files++
    }
  }
  if (files) {
    zip.writeZip(jarPath)
    console.log('patched lang files', files)
  }
}

function stripSatelliteUi(libs) {
  const uiRoot = path.join(libs, 'lunar-assets', 'ui')
  if (!fs.existsSync(uiRoot)) return
  for (const hash of fs.readdirSync(uiRoot)) {
    const indexPath = path.join(uiRoot, hash, 'static', 'index.js')
    if (!fs.existsSync(indexPath)) continue
    let js = fs.readFileSync(indexPath, 'utf8')
    const before = js
    js = js.replace(/showSatelliteWindow/g, 'showOpticNoop')
    js = js.replace(/btnSatellite/g, 'btnOpticGone')
    js = js.replace(/"Satellite"/g, '""')
    js = js.replace(/faSatelliteDish/g, 'faOpticGone')
    if (js !== before) {
      fs.writeFileSync(indexPath, js, 'utf8')
      console.log('stripped Satellite from UI', indexPath)
    }
  }
}

function patchSatelliteButtonClass(libs) {
  // Neutralize main-menu Satellite button id so it won't resolve a visible control.
  const jarPath = path.join(libs, 'multiver-full', 'lunar.jar')
  if (!fs.existsSync(jarPath)) return
  const zip = new AdmZip(jarPath)
  let hits = 0
  for (const entry of zip.getEntries()) {
    if (!entry.entryName.endsWith('.class')) continue
    let data
    try {
      data = entry.getData()
    } catch {
      continue
    }
    const from = Buffer.from('gui.components.btnSatellite')
    const to = Buffer.from('gui.components.btnOpticHide') // same length 27
    if (from.length !== to.length) continue
    if (!data.includes(from)) continue
    const { buf, hits: n } = replaceAllEqual(data, from.toString('utf8'), to.toString('utf8'))
    if (n > 0) {
      zip.updateFile(entry.entryName, buf)
      hits += n
    }
  }
  if (hits) {
    zip.writeZip(jarPath)
    console.log('hid btnSatellite refs', hits)
  }
}

function main() {
  if (!fs.existsSync(opticLogo)) throw new Error('missing ' + opticLogo)
  const generated = path.join(root, 'release-client', '_optic-logos')
  resizeLogoPngs(generated)

  const libsList = versionsRoots()
  if (!libsList.length) throw new Error('no installed client libs found')

  for (const libs of libsList) {
    console.log('\n==', libs)
    emptyCosmetics(libs)
    installLogos(libs, generated)
    patchLunarJarLogos(libs, generated)
    patchUiPlanet(libs, generated)
    patchLangSatellite(libs)
    stripSatelliteUi(libs)
    patchSatelliteButtonClass(libs)

    const patches = path.join(libs, 'lunar-localpatches.jar')
    brandLocalPatchesJar(patches)
    const extra = brandJarFile(patches)
    console.log('extra brand hits patches', extra)
    // brand footer string inside lunar.jar override candidates
    const lunarJar = path.join(libs, 'multiver-full', 'lunar.jar')
    console.log('branded lunar.jar hits', brandJarFile(lunarJar, { classesOnly: true }))
  }

  stageOpticPackAssets(generated)

  // Keep launcher optic pack jar branded too
  const packJar = path.join(root, 'resources', 'optic', 'lunar-localpatches.jar')
  if (fs.existsSync(packJar)) {
    brandLocalPatchesJar(packJar)
    brandJarFile(packJar)
  }
  console.log('done')
}

main()
