/**
 * Build the Optic installer and publish it as a launcher update.
 *
 *   npm run release:launcher -- --version 1.2.5 --notes "New play button"
 *   npm run release:launcher -- --version 1.2.5 --no-publish   # build Optic-Setup.exe only
 *
 * Bumps package.json, builds, then electron-builder uploads Optic-Setup.exe, its
 * blockmap and latest.yml to a v<version> release on WiktorIsInsane/Optic-Client.
 * Installed launchers see it on their next start and update themselves.
 * The version bump is committed and tagged locally; add --push to push them.
 *
 * Needs GH_TOKEN (Contents read/write on the repo) unless --no-publish.
 */
import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const args = process.argv.slice(2)
const flag = (name) => {
  const at = args.indexOf(name)
  return at >= 0 ? args[at + 1] ?? '' : ''
}
const version = flag('--version')
const notes = flag('--notes')
const publish = !args.includes('--no-publish')
const push = args.includes('--push')

if (!/^\d+\.\d+\.\d+$/.test(version)) {
  console.error('Usage: npm run release:launcher -- --version X.Y.Z [--notes "..."] [--no-publish] [--push]')
  process.exit(1)
}
if (publish && !process.env.GH_TOKEN && !process.env.GITHUB_TOKEN) {
  console.error('Set GH_TOKEN to a GitHub token with Contents read/write on WiktorIsInsane/Optic-Client.')
  process.exit(1)
}

function run(cmd, cmdArgs, { shell = false } = {}) {
  const r = spawnSync(cmd, cmdArgs, { cwd: root, stdio: 'inherit', shell, windowsHide: true })
  if (r.status !== 0) process.exit(r.status ?? 1)
}
const win = process.platform === 'win32'
const npm = (cmdArgs) => run(win ? 'npm.cmd' : 'npm', cmdArgs, { shell: win })
const npx = (cmdArgs) => run(win ? 'npx.cmd' : 'npx', cmdArgs, { shell: win })

const pkgPath = path.join(root, 'package.json')
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'))
const previous = pkg.version
pkg.version = version
fs.writeFileSync(pkgPath, `${JSON.stringify(pkg, null, 2)}\n`)
npm(['install', '--package-lock-only', '--ignore-scripts'])
console.log(`Version ${previous} → ${version}`)

npm(['run', 'sync:optic'])
npm(['run', 'build'])

// electron-builder reads build/release-notes.md as the GitHub release body (shown as launcher news).
const notesFile = path.join(root, 'build', 'release-notes.md')
if (notes) fs.writeFileSync(notesFile, `${notes}\n`)
try {
  npx(['electron-builder', '--win', '--publish', publish ? 'always' : 'never'])
} finally {
  if (notes) fs.rmSync(notesFile, { force: true })
}

const setup = path.join(root, 'release', 'Optic-Setup.exe')
console.log(`\n✔ Installer: ${setup}`)
if (publish) console.log(`✔ Published v${version}. Launchers update on their next start.`)

run('git', ['add', 'package.json', 'package-lock.json'])
const commit = spawnSync('git', ['commit', '-m', `Release launcher ${version}`], { cwd: root, stdio: 'inherit' })
if (commit.status === 0) run('git', ['tag', '-f', `v${version}`])
// The GitHub release already created the remote v<version> tag, so only the branch is pushed.
if (push) run('git', ['push', 'origin', 'HEAD'])
