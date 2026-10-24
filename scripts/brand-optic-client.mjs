/**
 * Rewrite Lunar Revamped branding inside lunar-localpatches.jar to Optic.
 * Uses equal-length UTF-8 replacements so class constant pools stay valid.
 */
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import AdmZip from 'adm-zip'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const root = path.resolve(__dirname, '..')

/** Longer keys first so they win over shorter substrings. */
const REPLACEMENTS = [
  ['Starting Lunar Revamped', 'Starting Optic Client  '],
  ['--app-name=Lunar Revamped', '--app-name=Optic Client  '],
  ['Lunar Revamped', 'Optic Client  '],
  ['Lunar Remake', 'Optic Client'],
  ['Lunar Client ', 'Optic        '],
  ['Lunar Client', 'Optic       ']
]

function replaceAllEqual(buf, from, to) {
  const a = Buffer.from(from, 'utf8')
  const b = Buffer.from(to, 'utf8')
  if (a.length !== b.length) {
    throw new Error(`length mismatch: ${JSON.stringify(from)} (${a.length}) vs ${JSON.stringify(to)} (${b.length})`)
  }
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

function brandBuffer(buf) {
  let next = Buffer.from(buf)
  let total = 0
  for (const [from, to] of REPLACEMENTS) {
    const r = replaceAllEqual(next, from, to)
    next = r.buf
    total += r.hits
  }
  return { buf: next, hits: total }
}

function brandBuildData(text) {
  return text
    .replace(/Lunar Revamped/g, 'Optic')
    .replace(/Lunar Remake/g, 'Optic')
    .replace(/^lunarVersion=.*/m, 'lunarVersion=1.0.0')
}

export function brandLocalPatchesJar(jarPath) {
  if (!fs.existsSync(jarPath)) throw new Error(`missing jar: ${jarPath}`)
  const zip = new AdmZip(jarPath)
  let touched = 0
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory) continue
    const name = entry.entryName.replace(/\\/g, '/')
    const raw = entry.getData()
    if (name === 'lunarBuildData.txt') {
      const next = brandBuildData(raw.toString('utf8'))
      if (next !== raw.toString('utf8')) {
        zip.updateFile(entry.entryName, Buffer.from(next, 'utf8'))
        touched++
      }
      continue
    }
    if (!/\.(class|txt|properties|json|lang)$/i.test(name)) continue
    const { buf, hits } = brandBuffer(raw)
    if (hits > 0) {
      zip.updateFile(entry.entryName, buf)
      touched += hits
    }
  }
  zip.writeZip(jarPath)
  return touched
}

function main() {
  const args = process.argv.slice(2)
  const flag = (name, fallback) => {
    const i = args.indexOf(name)
    return i >= 0 ? args[i + 1] : fallback
  }
  const jar = path.resolve(
    flag(
      '--jar',
      path.join(root, 'resources', 'optic', 'lunar-localpatches.jar')
    )
  )
  const hits = brandLocalPatchesJar(jar)
  console.log(`branded ${jar} (${hits} replacements)`)
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main()
}
