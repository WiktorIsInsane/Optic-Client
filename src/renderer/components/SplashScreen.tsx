import { useEffect, useState } from 'react'
import logo from '../assets/logo.png'
import wordmark from '../assets/optic-wordmark.png'
import { playBootHit, playCompressWhoosh, playNeedleSpin, playResolveChime } from '../lib/splashAudio'

type Phase = 'boot' | 'wordmark' | 'compress' | 'spin' | 'hold' | 'out'

export function SplashScreen(props: {
  onDone: () => void
  /** Keep splash visible (e.g. while a launcher update downloads/installs). */
  hold?: boolean
  /** Overrides the phase status line — used for "Updating…". */
  statusText?: string
}) {
  const [phase, setPhase] = useState<Phase>('boot')
  const [visible, setVisible] = useState(true)
  const [animDone, setAnimDone] = useState(false)

  useEffect(() => {
    let cancelled = false
    playBootHit()

    const timers = [
      window.setTimeout(() => {
        if (!cancelled) setPhase('wordmark')
      }, 180),
      window.setTimeout(() => {
        if (cancelled) return
        setPhase('compress')
        playCompressWhoosh()
      }, 1100),
      window.setTimeout(() => {
        if (cancelled) return
        setPhase('spin')
        playNeedleSpin(1.55)
      }, 1750),
      window.setTimeout(() => {
        if (cancelled) return
        setPhase('hold')
        playResolveChime()
      }, 3400),
      window.setTimeout(() => {
        if (!cancelled) setAnimDone(true)
      }, 4000)
    ]
    return () => {
      cancelled = true
      timers.forEach((id) => window.clearTimeout(id))
    }
  }, [])

  // Stay on splash while hold (updating); finish once animation + hold clear.
  useEffect(() => {
    if (!animDone) return
    if (props.hold) {
      setPhase((p) => (p === 'out' ? 'hold' : p === 'boot' || p === 'wordmark' || p === 'compress' ? 'spin' : p))
      return
    }
    setPhase('out')
    const t = window.setTimeout(() => {
      setVisible(false)
      props.onDone()
    }, 550)
    return () => window.clearTimeout(t)
  }, [animDone, props.hold, props.onDone])

  if (!visible) return null

  const showWord = phase === 'boot' || phase === 'wordmark' || phase === 'compress'
  const showO = phase === 'compress' || phase === 'spin' || phase === 'hold' || phase === 'out'
  const spinning = phase === 'spin' || (Boolean(props.hold) && animDone)
  const exiting = phase === 'out' && !props.hold

  const defaultStatus =
    phase === 'boot' || phase === 'wordmark'
      ? 'Starting Optic…'
      : phase === 'compress'
        ? 'Loading…'
        : phase === 'spin'
          ? 'Calibrating…'
          : phase === 'hold' || phase === 'out'
            ? props.hold
              ? 'Updating…'
              : 'Ready'
            : null

  const status = props.statusText || defaultStatus

  return (
    <div className={`splash-root ${exiting ? 'splash-out' : ''}`} aria-hidden>
      <div className="splash-glow" />
      <div className="splash-stage">
        {showWord ? (
          <img
            src={wordmark}
            alt=""
            className={`splash-wordmark splash-wordmark-${phase}`}
            draggable={false}
          />
        ) : null}

        {showO ? (
          <div className={`splash-o-wrap splash-o-${phase}`}>
            <img
              src={logo}
              alt=""
              className={`splash-o-mark ${spinning ? 'splash-o-mark-spin' : 'splash-o-mark-rest'}`}
              draggable={false}
            />
          </div>
        ) : null}
      </div>

      <div
        className={`splash-status ${props.statusText || props.hold ? 'splash-status-busy' : `splash-status-${phase}`}`}
      >
        {status}
      </div>
    </div>
  )
}
