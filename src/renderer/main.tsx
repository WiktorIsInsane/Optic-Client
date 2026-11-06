import { StrictMode, useEffect, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { SplashScreen } from './components/SplashScreen'
import './index.css'
import { IpcChannel } from '@shared/ipc'
import { useApp } from './stores/appStore'
import type { ConsoleLine, DownloadProgress, LaunchState, MicrosoftLoginState, UpdaterState } from '@shared/types'

function isUpdaterBusy(phase: string | undefined): boolean {
  return (
    phase === 'checking' ||
    phase === 'available' ||
    phase === 'downloading' ||
    phase === 'downloaded' ||
    phase === 'installing'
  )
}

function Boot() {
  const refresh = useApp((s) => s.refresh)
  const pushConsole = useApp((s) => s.pushConsole)
  const loading = useApp((s) => s.loading)
  const snap = useApp((s) => s.snap)
  const toast = useApp((s) => s.toast)
  const [splashDone, setSplashDone] = useState(false)
  const autoLaunchTried = useRef(false)
  const onSplashDone = useRef(() => setSplashDone(true))
  const showSplash = !splashDone

  const updater = snap?.updater
  const updating = isUpdaterBusy(updater?.phase)
  // Updates download in the background and install silently after Play, so the splash only
  // covers the few seconds of an actual install.
  const holdSplash = updater?.phase === 'installing'

  const splashStatus = holdSplash
    ? updater?.phase === 'installing'
      ? 'Updating…'
      : updater?.phase === 'downloading' && updater.percent > 0
        ? `Updating… ${updater.percent}%`
        : 'Updating…'
    : updating && updater?.phase === 'checking'
      ? 'Checking for updates…'
      : undefined

  useEffect(() => {
    if (!window.lunar) return
    void refresh()
    // Never leave a permanent black screen if snapshot hangs.
    const failSafe = window.setTimeout(() => {
      if (useApp.getState().loading) useApp.setState({ loading: false })
    }, 8000)
    const offs = [
      window.lunar.on(IpcChannel.toast, (payload) => {
        if (typeof payload === 'string' && payload) useApp.getState().toast(payload)
      }),
      window.lunar.on(IpcChannel.console, (payload) => pushConsole(payload as ConsoleLine)),
      window.lunar.on(IpcChannel.progress, (payload) => useApp.setState({ progress: payload as DownloadProgress })),
      window.lunar.on(IpcChannel.authProgress, (payload) => {
        const next = payload as MicrosoftLoginState
        useApp.setState({ login: next })
        if (next.state === 'success') {
          useApp.getState().toast(next.message || 'Account added')
          void refresh()
        }
        if (next.state === 'error') useApp.getState().toast(next.message)
      }),
      window.lunar.on(IpcChannel.launchState, (payload) => {
        const launch = payload as LaunchState
        const current = useApp.getState().snap
        if (current) useApp.setState({ snap: { ...current, launch } })
      }),
      window.lunar.on(IpcChannel.clientStatus, () => void refresh()),
      window.lunar.on(IpcChannel.updater, (payload) => {
        useApp.getState().applyUpdater(payload as UpdaterState)
      })
    ]
    return () => {
      window.clearTimeout(failSafe)
      offs.forEach((off) => off())
    }
  }, [refresh, pushConsole])

  // Reveal as soon as splash ends; keep waiting on loading only briefly via fail-safe above.
  const revealApp = splashDone && !loading

  // Open launcher UI, then auto-start the game once per boot when enabled.
  useEffect(() => {
    if (!revealApp || autoLaunchTried.current) return
    const current = useApp.getState().snap
    if (!current) return
    if (!current.settings.autoLaunchGame) return
    // Only an install in progress blocks auto-launch; downloads continue in the background.
    if (current.updater.phase === 'installing') return
    const account = current.accounts.find((item) => item.id === current.activeAccountId)
    if (!account || current.launch.running) return
    autoLaunchTried.current = true
    void window.lunar
      .startGame()
      .then(() => toast('Launching'))
      .catch((error) => {
        autoLaunchTried.current = false
        toast(error instanceof Error ? error.message : 'Launch failed')
      })
  }, [revealApp, toast, snap?.activeAccountId, snap?.accounts.length, snap?.updater.phase])

  return (
    <>
      {showSplash ? (
        <SplashScreen hold={holdSplash} statusText={splashStatus} onDone={onSplashDone.current} />
      ) : null}
      <div className={revealApp ? 'boot-app-in' : 'boot-app-hidden'} aria-hidden={!revealApp}>
        <App />
      </div>
    </>
  )
}

const root = document.getElementById('root')
if (root) {
  createRoot(root).render(
    <StrictMode>
      <Boot />
    </StrictMode>
  )
}
