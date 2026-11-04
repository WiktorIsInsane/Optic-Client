import { app } from 'electron'
import { registerIpc } from './ipc'
import { loadStore } from './store'
import { bootBundledPvpJava } from './services/java'
import { installLatestClient } from './services/download'
import { bootAutoUpdate, checkLauncherUpdate } from './services/updater'
import { createWindow, ensureTray, getWindow, markQuitting } from './window'

const gotLock = app.requestSingleInstanceLock()
if (!gotLock) app.quit()

app.whenReady().then(() => {
  try {
    loadStore()
  } catch (error) {
    console.error('[Optic] loadStore failed, using empty defaults', error)
  }
  registerIpc()
  createWindow()
  try {
    if (loadStore().settings.onLaunch === 'tray') ensureTray()
  } catch {
    /* ignore */
  }
  void bootBundledPvpJava()
  void bootAutoUpdate()
  // Pull client file updates in the background so Play starts instantly; Play re-checks anyway.
  void installLatestClient().catch(() => undefined)
  // Launchers left open in the tray still see new versions; they install on the next start.
  setInterval(() => void checkLauncherUpdate(false).catch(() => undefined), 60 * 60 * 1000)
})

app.on('before-quit', () => markQuitting())
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin' && loadStore().settings.onLaunch !== 'tray') app.quit()
})
app.on('second-instance', () => {
  getWindow()?.show()
})
