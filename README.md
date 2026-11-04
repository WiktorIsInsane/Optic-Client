# Optic

Optic launcher and PvP client for Minecraft 1.8.9 and 1.7.10.

**Install:** download [`Optic-Setup.exe`](https://github.com/WiktorIsInsane/Optic-Client/releases/latest/download/Optic-Setup.exe).

**Updates:** the launcher updates itself from this repo's releases, and on start and every Play it downloads changed 1.8.9 / 1.7.10 client files from the `client` release.

## Dev

```bash
npm install
npm run sync:optic
npm run sync:pvp-java
npm run dev
```

## Release

See [RELEASING.md](RELEASING.md).

```bash
npm run release:client -- --build --notes "..."
npm run release:launcher -- --version X.Y.Z --notes "..."
```
