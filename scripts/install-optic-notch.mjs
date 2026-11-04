/**
 * Build the Fabric client, remap it to official (notch) names and install it for the
 * launcher's launchwrapper + OptiFine path.
 *
 *   1.8.9  (default)  optic-client/      -> libs/optic-notch.jar
 *   1.7.10 (--mc 1.7.10) optic-client-1.7/ -> libs/optic-notch-1.7.10.jar
 *
 * Usage: node scripts/install-optic-notch.mjs [--mc 1.7.10] [--jdk <path>]
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
const forge = path.join(root, 'optic-client', 'tools', 'forge')
const appData = process.env.APPDATA || path.join(os.homedir(), 'AppData', 'Roaming')
const libs = path.join(appData, '.optic', 'versions', '1.0.0', 'libs')

function flag(name) {
  const at = process.argv.indexOf(name)
  return at > 0 ? process.argv[at + 1] : ''
}

const mc = flag('--mc') || '1.8.9'
const jdk = flag('--jdk') || process.env.JAVA_HOME || path.join(os.homedir(), '.optic-buildjdk')
const exe = (name) => path.join(jdk, 'bin', process.platform === 'win32' ? `${name}.exe` : name)

/** Loom's intermediary-named Minecraft jar for a version, used as the remap classpath. */
function loomIntermediaryJar(version) {
  const base = path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', 'minecraftMaven', 'net', 'minecraft', 'minecraft-merged-legacy-intermediary-1-v2-intermediary')
  const dir = fs.existsSync(base) ? fs.readdirSync(base).find((name) => name.startsWith(`${version}-`)) : ''
  if (!dir) throw new Error(`No Loom intermediary jar for ${version}. Build the ${version} project once first.`)
  const jar = fs.readdirSync(path.join(base, dir)).find((name) => name.endsWith('.jar'))
  return path.join(base, dir, jar)
}

const TARGETS = {
  '1.8.9': {
    project: path.join(root, 'optic-client'),
    built: 'optic-client-1.0.0.jar',
    mappings: (work) => {
      new AdmZip(path.join(forge, 'intermediary.jar')).extractEntryTo('mappings/mappings.tiny', work, false, true)
      return 'mappings.tiny'
    },
    classpath: () => path.join(forge, 'mc-intermediary.jar'),
    // 1.8.9 boots through a notch-name mixin compiled outside this project.
    boot: ['gg/optic/client/mixin/OpticBootMixin.class', 'gg/optic/forge/OpticTweaker.class', 'net/fabricmc/api/ClientModInitializer.class'],
    bootMixin: 'OpticBootMixin',
    output: 'optic-notch.jar'
  },
  '1.7.10': {
    project: path.join(root, 'optic-client-1.7'),
    built: 'optic-client-1.7-1.0.0.jar',
    mappings: () => path.join(os.homedir(), '.gradle', 'caches', 'fabric-loom', '1.7.10', 'legacy-intermediary-1-v2.tiny'),
    classpath: () => loomIntermediaryJar('1.7.10'),
    // 1.7.10 starts Optic from its own MinecraftClientMixin, so only the tweaker is added.
    boot: ['gg/optic/forge/OpticTweaker.class', 'net/fabricmc/api/ClientModInitializer.class'],
    bootMixin: '',
    output: 'optic-notch-1.7.10.jar'
  }
}

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
  const target = TARGETS[mc]
  if (!target) throw new Error(`Unknown --mc ${mc}. Use 1.8.9 or 1.7.10.`)
  if (!fs.existsSync(exe('javac'))) throw new Error(`No JDK at ${jdk}. Pass --jdk <path>.`)

  const project = target.project
  const work = path.join(project, 'build', 'notch')
  run(process.platform === 'win32' ? path.join(project, 'gradlew.bat') : './gradlew', ['build', '-x', 'test'], project)
  const built = path.join(project, 'build', 'libs', target.built)

  fs.rmSync(work, { recursive: true, force: true })
  fs.mkdirSync(path.join(work, 'tools'), { recursive: true })
  const mappings = target.mappings(work)

  const remapper = path.join(forge, 'tiny-remapper.jar')
  run(exe('javac'), ['-nowarn', '-cp', remapper, '-d', 'tools', path.join(forge, 'src', 'gg', 'optic', 'tools', 'RemapToOfficial.java')], work)
  const remapped = path.join(work, 'remapped.jar')
  run(exe('java'), ['-cp', ['tools', remapper].join(path.delimiter), 'gg.optic.tools.RemapToOfficial', mappings, built, remapped, target.classpath()], work)

  // The launchwrapper path boots through OpticTweaker, which needs these beside the client.
  const zip = new AdmZip(remapped)
  const boot = path.join(forge, 'boot-classes')
  for (const rel of target.boot) {
    zip.addFile(rel, fs.readFileSync(path.join(boot, ...rel.split('/'))))
  }
  if (target.bootMixin) {
    const mixins = JSON.parse(zip.readAsText('optic.mixins.json'))
    if (!mixins.client.includes(target.bootMixin)) mixins.client.push(target.bootMixin)
    zip.updateFile('optic.mixins.json', Buffer.from(JSON.stringify(mixins, null, '\t') + '\n', 'utf8'))
  }
  const out = path.join(work, target.output)
  zip.writeZip(out)

  const dest = path.join(libs, target.output)
  if (fs.existsSync(dest)) fs.copyFileSync(dest, `${dest}.bak`)
  fs.copyFileSync(out, dest)
  console.log(`Installed ${dest} (previous copy kept as ${target.output}.bak)`)
}

main()
