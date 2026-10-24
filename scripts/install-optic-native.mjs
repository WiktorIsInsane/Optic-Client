/**
 * Build and install the original Optic 1.0.0 Legacy Fabric client runtime.
 *
 * This does not include or redistribute Mojang's game jar. The launcher keeps
 * using ensureVanilla() to download authenticated/public Mojang dependencies.
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import AdmZip from 'adm-zip'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const project = path.join(root, 'optic-client')
const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming')
const appRoot = fs.existsSync(path.join(appData, '.optic'))
  ? path.join(appData, '.optic')
  : path.join(appData, '.lunar-remake')
const versionRoot = path.join(appRoot, 'versions', '1.0.0')
const dest = path.join(versionRoot, 'libs', 'optic-client')
const profileUrl = 'https://meta.legacyfabric.net/v2/versions/loader/1.8.9/0.18.3/profile/json'

function runBuild() {
  const gradle = process.platform === 'win32' ? 'gradlew.bat' : './gradlew'
  const result = spawnSync(gradle, ['build'], { cwd: project, stdio: 'inherit', shell: process.platform === 'win32' })
  if (result.status !== 0) process.exit(result.status ?? 1)
}

function coordinateParts(coordinate) {
  const [group, name, version] = coordinate.split(':')
  if (!group || !name || !version) throw new Error(`Invalid Maven coordinate: ${coordinate}`)
  const base = `${name}-${version}`
  return {
    rel: `${group.replace(/\./g, '/')}/${name}/${version}/${base}.jar`,
    nativeRel: `${group.replace(/\./g, '/')}/${name}/${version}/${base}-natives-windows.jar`
  }
}

async function download(url, file) {
  if (fs.existsSync(file) && fs.statSync(file).size > 0) return
  const response = await fetch(url)
  if (!response.ok) throw new Error(`${response.status} downloading ${url}`)
  const bytes = Buffer.from(await response.arrayBuffer())
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, bytes)
}

async function main() {
  runBuild()
  fs.mkdirSync(path.join(dest, 'mods'), { recursive: true })
  fs.mkdirSync(path.join(dest, 'libraries'), { recursive: true })
  fs.mkdirSync(path.join(dest, 'natives'), { recursive: true })

  const built = path.join(project, 'build', 'libs', 'optic-client-1.0.0.jar')
  fs.copyFileSync(built, path.join(dest, 'mods', 'optic-client-1.0.0.jar'))
  // Drop any previously installed Radium jars — they need Legacy Fabric API and
  // crash Optic's lean loader with NoClassDefFoundError: TriState.
  for (const name of fs.readdirSync(path.join(dest, 'mods'))) {
    if (/^radium-.*\.jar$/i.test(name) || /^sodium-fabric-.*\.jar$/i.test(name)) {
      fs.rmSync(path.join(dest, 'mods', name), { force: true })
    }
  }

  const profileResponse = await fetch(profileUrl)
  if (!profileResponse.ok) throw new Error(`Could not fetch Legacy Fabric profile (${profileResponse.status})`)
  const profile = await profileResponse.json()

  for (const library of profile.libraries) {
    const parts = coordinateParts(library.name)
    const baseUrl = library.url || 'https://maven.fabricmc.net/'
    if (!library.natives) {
      const file = path.join(dest, 'libraries', ...parts.rel.split('/'))
      await download(new URL(parts.rel, baseUrl).toString(), file)
    }

    if (library.natives?.windows) {
      const nativeJar = path.join(dest, 'native-jars', path.basename(parts.nativeRel))
      await download(new URL(parts.nativeRel, baseUrl).toString(), nativeJar)
      new AdmZip(nativeJar).extractAllTo(path.join(dest, 'natives'), true)
    }
  }

  fs.writeFileSync(
    path.join(dest, 'optic-client.json'),
    JSON.stringify(
      {
        id: 'optic-1.0.0',
        name: 'Optic',
        version: '1.0.0',
        minecraft: '1.8.9',
        loader: 'Legacy Fabric 0.18.3',
        mainClass: profile.mainClass
      },
      null,
      2
    ) + '\n',
    'utf8'
  )
  fs.mkdirSync(path.join(versionRoot, 'libs'), { recursive: true })
  console.log(`Installed Optic native client -> ${dest}`)
}

await main()
