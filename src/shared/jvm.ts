import type { JvmPreset } from './types'

export const DEFAULT_ACCENT = '#5cff8a'

/** Lunar 1.8.9 + Ichor/Genesis sweet spot: enough heap, not so large that G1 pauses stretch. */
export const OPTIMAL_GAME_RAM_MB = 4096

export function optimalGameRamMb(totalMb: number): number {
  const cap = Math.max(2048, Math.floor((totalMb * 0.3) / 256) * 256)
  return Math.min(OPTIMAL_GAME_RAM_MB, cap)
}

/** Previous launcher defaults — migrate these on update, leave custom RAM alone. */
export function isStockLauncherRam(minMb: number, maxMb: number): boolean {
  return (
    (minMb === 512 && (maxMb === 512 || maxMb === 2048)) ||
    (minMb === 2048 && maxMb === 2048)
  )
}

/**
 * Light GraalVM 17 profile — intentional under-tuning after heavy G1 stacks hurt hitreg.
 * Keep in sync with resources/mc-pvp-java17/pvp-client.args.
 */
export const PVP_SMOOTH_FLAGS: string[] = [
  '-XX:+UseG1GC',
  '-XX:MaxGCPauseMillis=10',
  '-XX:+DisableExplicitGC',
  '-XX:+PerfDisableSharedMem',
  '--add-modules=jdk.incubator.foreign',
  '--enable-native-access=ALL-UNNAMED',
  '-Djava.net.preferIPv4Stack=true',
  '-Dio.netty.leakDetection.level=DISABLED',
  '-Dorg.lwjgl.util.NoChecks=true'
]

/**
 * Java 21+ client stack for a plain Zulu runtime (no pvp-latency wrapper).
 * Matches pvp-latency.args: generational ZGC, compact headers off, 9800X3D
 * thread counts. The wrapper path does not use this list.
 */
export function zgcSmoothFlags(major: number): string[] {
  const flags = ['-XX:+UseZGC', '-XX:-UseCompactObjectHeaders']
  // Generational mode is experimental before 23 and the only mode from 24 on.
  if (major >= 21 && major < 23) {
    flags.push('-XX:+UnlockExperimentalVMOptions', '-XX:+ZGenerational')
  }
  flags.push(
    '-XX:-ZProactive',
    '-XX:-ZUncommit',
    '-XX:-UseDynamicNumberOfGCThreads',
    '-XX:-UseNUMA',
    '-XX:ConcGCThreads=2',
    '-XX:ParallelGCThreads=6',
    '-XX:CICompilerCount=3',
    '-XX:ZAllocationSpikeTolerance=5',
    '-XX:+AlwaysPreTouch',
    '-XX:+DisableExplicitGC',
    '-XX:+PerfDisableSharedMem',
    '-XX:ReservedCodeCacheSize=256M',
    '-XX:InitialCodeCacheSize=48M',
    '-XX:SoftRefLRUPolicyMSPerMB=50',
    '--enable-native-access=ALL-UNNAMED',
    '-Djava.net.preferIPv4Stack=true',
    '-Dio.netty.leakDetectionLevel=disabled',
    '-Dio.netty.tryReflectionSetAccessible=true',
    '-Dorg.lwjgl.util.NoChecks=true',
    '-Dsun.net.inetaddr.ttl=60'
  )
  return flags
}

export function presetArgs(preset: JvmPreset, major: number): string {
  switch (preset) {
    case 'default':
      return major >= 21 ? zgcSmoothFlags(major).join(' ') : '-XX:+UseG1GC -Djava.net.preferIPv4Stack=true'
    case 'aikar':
      return [
        '-XX:+UseG1GC',
        '-XX:+ParallelRefProcEnabled',
        '-XX:MaxGCPauseMillis=200',
        '-XX:+UnlockExperimentalVMOptions',
        '-XX:+DisableExplicitGC',
        '-XX:+AlwaysPreTouch',
        '-XX:G1NewSizePercent=30',
        '-XX:G1MaxNewSizePercent=40',
        '-XX:G1HeapRegionSize=8M',
        '-XX:G1ReservePercent=20',
        '-XX:G1HeapWastePercent=5',
        '-XX:G1MixedGCCountTarget=4',
        '-XX:InitiatingHeapOccupancyPercent=15',
        '-XX:G1MixedGCLiveThresholdPercent=90',
        '-XX:G1RSetUpdatingPauseTimePercent=5',
        '-XX:SurvivorRatio=32',
        '-XX:+PerfDisableSharedMem',
        '-XX:MaxTenuringThreshold=1',
        '-Dusing.aikars.flags=https://mcflags.emc.gs',
        '-Daikars.new.flags=true'
      ].join(' ')
    case 'low':
      return '-XX:+UseSerialGC -XX:+UseStringDeduplication -Djava.net.preferIPv4Stack=true'
    case 'zgc':
      return major >= 21 ? zgcSmoothFlags(major).join(' ') : '-XX:+UseG1GC -Djava.net.preferIPv4Stack=true'
    case 'pvp':
      return major >= 21 ? zgcSmoothFlags(major).join(' ') : PVP_SMOOTH_FLAGS.join(' ')
    case 'custom':
      return ''
    default: {
      const _never: never = preset
      return _never
    }
  }
}

export interface ArgCheck {
  args: string[]
  warnings: string[]
  error?: string
}

/** Split a JVM/game arg string into tokens. Rejects shell chaining. */
export function parseArgs(raw: string): ArgCheck {
  const warnings: string[] = []
  const args: string[] = []
  if (/[\r\n|&;`$]/.test(raw)) {
    return { args: [], warnings, error: 'Arguments cannot contain newlines or shell operators.' }
  }
  const re = /"([^"]*)"|'([^']*)'|(\S+)/g
  let match: RegExpExecArray | null
  while ((match = re.exec(raw)) !== null) {
    const token = match[1] ?? match[2] ?? match[3] ?? ''
    if (token) args.push(token)
  }
  for (const token of args) {
    if (token.startsWith('-XX:') && token.includes(' ')) {
      warnings.push(`Suspicious flag: ${token}`)
    }
  }
  return { args, warnings }
}
