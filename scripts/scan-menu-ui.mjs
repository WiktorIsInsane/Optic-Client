import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'

const libs = path.join(os.homedir(), 'AppData', 'Roaming', '.lunar-remake', 'versions', '1.0.0', 'libs')
const uiRoot = path.join(libs, 'lunar-assets', 'ui')
const hash = fs.readdirSync(uiRoot)[0]
const base = path.join(uiRoot, hash)
const indexPath = path.join(base, 'static', 'index.js')
const index = fs.readFileSync(indexPath, 'utf8')
console.log('index', indexPath, (index.length / 1e6).toFixed(1), 'MB')

for (const n of ['Lunar Client', 'btnSatellite', 'Satellite', 'cosmetics', 'mainmenu', 'logo', 'locker']) {
  const re = new RegExp(n.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g')
  console.log(n, (index.match(re) || []).length)
}

const logos = new Set()
for (const m of index.matchAll(/["']([^"']*logo[^"']*\.(?:png|webp|svg|jpg))["']/gi)) {
  logos.add(m[1])
}
console.log('logo urls', [...logos].slice(0, 40))

const mainmenu = new Set()
for (const m of index.matchAll(/icons\/mainmenu\/[A-Za-z0-9._@-]+/g)) mainmenu.add(m[0])
console.log('mainmenu icons', [...mainmenu])

const sat = new Set()
for (const m of index.matchAll(/[^"'\\\s]{0,40}[Ss]atellite[^"'\\\s]{0,40}/g)) sat.add(m[0])
console.log('satellite ctx', [...sat].slice(0, 30))

// assets folder names with moon/logo
const assets = path.join(base, 'assets')
for (const name of fs.readdirSync(assets)) {
  if (/logo|moon|lunar|satellit|brand/i.test(name)) console.log('asset', name, fs.statSync(path.join(assets, name)).size)
}
