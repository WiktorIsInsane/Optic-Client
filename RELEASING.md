# Releasing Optic

Everything ships from **github.com/WiktorIsInsane/Optic-Client** releases.

| What | Where it lives | Who picks it up |
| --- | --- | --- |
| Launcher installer | `vX.Y.Z` release: `Optic-Setup.exe`, `latest.yml` | Installed launchers update themselves on start |
| In-game clients (1.8.9 + 1.7.10) | `client` pre-release: per-file assets + `optic-files.json` | Launcher, on start and before every Play |

Download link for players (always the newest installer):
`https://github.com/WiktorIsInsane/Optic-Client/releases/latest/download/Optic-Setup.exe`

## One-time setup

1. Push this repo to `WiktorIsInsane/Optic-Client` (GitHub can't create releases on an empty repo).
2. Create a fine-grained token at github.com/settings/personal-access-tokens.
   Repository access: only `Optic-Client`. Permissions: **Contents: Read and write**.
3. In each PowerShell window you release from: `$env:GH_TOKEN = "<token>"`

## Ship a client update (most updates)

```powershell
npm run release:client -- --build --notes "Fixed potion icons, faster hitreg"
```

`--build` rebuilds both clients (`install-optic-notch.mjs --mc 1.8.9` and `--mc 1.7.10`)
into `%APPDATA%\.optic\versions\1.0.0\libs`. Then the script uploads only the changed
files and swaps `optic-files.json`. Leave out `--build` to publish what's already installed.
`--dry-run` writes the manifest without uploading.

What ships is listed in [`client-files.json`](client-files.json). Minecraft's own files
are never shipped; the launcher downloads them from Mojang. Java 8 for 1.7.10 comes
from Eclipse Adoptium on first launch.

## Ship a launcher update

```powershell
npm run release:launcher -- --version 1.2.5 --notes "New settings page"
```

This bumps `package.json`, builds `release\Optic-Setup.exe`, and publishes the `v1.2.5`
release. The version bump is committed and tagged locally (`--push` pushes the branch).
Use `--no-publish`, or `npm run dist:installer`, to build the installer only.

## Your own PC (dev builds)

`install-optic-notch.mjs` writes `%APPDATA%\.optic\versions\1.0.0\optic-dev`. While that
file exists, your launcher won't replace your local jars with the published ones.
Delete it to get normal auto-updates again.
