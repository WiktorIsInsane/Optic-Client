/**
 * Build the Fabric client, remap it to official (notch) names and install it as
 * optic-notch.jar — the jar the launcher's Java 25 + OptiFine path loads first.
 *
 * Usage: node scripts/install-optic-notch.mjs [--jdk <path>]
 * The JDK must be a plain one: the bundled zulu-25 java.exe is a launch wrapper
 * that breaks Gradle. Defaults to JAVA_HOME, then ~/.optic-buildjdk.
 */
import { spawnSync } from 'node:child_process'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import AdmZip from 'adm-zip'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const project = path.join(root, 'optic-client')
const forge = path.join(project, 'tools', 'forge')
const work = path.join(project, 'build', 'notch')
const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming')
const libs = path.join(appData, '.optic', 'versions', '1.0.0', 'libs')

const jdkFlag = process.argv.indexOf('--jdk')
const jdk = jdkFlag > 0 ? process.argv[jdkFlag + 1] : process.env.JAVA_HOME || path.join(os.homedir(), '.optic-buildjdk')
const exe = (name) => path.join(jdk, 'bin', process.platform === 'win32' ? `${name}.exe` : name)

function run(cmd, args, cwd, extraEnv = {}) {
  const result = spawnSync(cmd, args, {
    cwd,
    stdio: 'inherit',
    shell: process.platform === 'win32' && cmd.endsWith('.bat'),
    env: { ...process.env, JAVA_HOME: jdk, ...extraEnv }
  })
  if (result.status !== 0) throw new Error(`${path.basename(cmd)} failed (${result.status})`)
}

function main() {
  if (!fs.existsSync(exe('javac'))) throw new Error(`No JDK at ${jdk}. Pass --jdk <path>.`)

  run(process.platform === 'win32' ? path.join(project, 'gradlew.bat') : './gradlew', ['build', '-x', 'test'], project)
  const built = path.join(project, 'build', 'libs', 'optic-client-1.0.0.jar')

  fs.rmSync(work, { recursive: true, force: true })
  fs.mkdirSync(path.join(work, 'tools'), { recursive: true })
  new AdmZip(path.join(forge, 'intermediary.jar')).extractEntryTo('mappings/mappings.tiny', work, false, true)

  const remapper = path.join(forge, 'tiny-remapper.jar')
  run(exe('javac'), ['-nowarn', '-cp', remapper, '-d', 'tools', path.join(forge, 'src', 'gg', 'optic', 'tools', 'RemapToOfficial.java')], work)
  const remapped = path.join(work, 'remapped.jar')
  run(
    exe('java'),
    ['-cp', ['tools', remapper].join(path.delimiter), 'gg.optic.tools.RemapToOfficial', 'mappings.tiny', built, remapped, path.join(forge, 'mc-intermediary.jar')],
    work
  )

  // The launch-java25 path boots through OpticTweaker, which needs these beside the client.
  const zip = new AdmZip(remapped)
  const boot = path.join(forge, 'boot-classes')
  for (const rel of ['gg/optic/client/mixin/OpticBootMixin.class', 'gg/optic/forge/OpticTweaker.class', 'net/fabricmc/api/ClientModInitializer.class']) {
    zip.addFile(rel, fs.readFileSync(path.join(boot, ...rel.split('/'))))
  }
  const mixins = JSON.parse(zip.readAsText('optic.mixins.json'))
  if (!mixins.client.includes('OpticBootMixin')) mixins.client.push('OpticBootMixin')
  zip.updateFile('optic.mixins.json', Buffer.from(JSON.stringify(mixins, null, '\t') + '\n', 'utf8'))
  const out = path.join(work, 'optic-notch.jar')
  zip.writeZip(out)

  const dest = path.join(libs, 'optic-notch.jar')
  if (fs.existsSync(dest)) fs.copyFileSync(dest, `${dest}.bak`)
  fs.copyFileSync(out, dest)
  console.log(`Installed ${dest} (previous copy kept as optic-notch.jar.bak)`)
}

main()
