const { app } = require('electron')

app.whenReady().then(async () => {
  try {
    const { startGame } = require('../dist/main/services/launch')
    const state = await startGame()
    console.log(JSON.stringify(state))
    setTimeout(() => app.quit(), 1500)
  } catch (error) {
    console.error(error)
    app.exit(1)
  }
})
