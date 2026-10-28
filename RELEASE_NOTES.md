# MusatPlay for Windows v1.1.3

Fixes the HTTPS handshake failure shown by **Help → Check for updates…** in v1.1.2.

## Fixed

- Include Java's `jdk.crypto.ec` security provider in the bundled runtime so secure connections to GitHub can complete.
- Build a new runtime image instead of reusing the earlier image without elliptic-curve support.
- Check the real GitHub update endpoint through the packaged Windows EXE during packaging. Packaging stops if that check fails.

## Install

Download **MusatPlay-Setup-1.1.3.exe**, quit MusatPlay, and run Setup. Users of v1.1.2 must install this release manually because its update connection is affected by this bug. Your saved application profile is retained.

Windows 10/11 x64 and Microsoft Edge WebView2 Evergreen Runtime are required. Java is bundled. Optional desktop and Start menu shortcuts and Uninstall.exe are included. The app and installer are unsigned.

## Verification

The previous module set reproduces the reported `handshake_failure`; adding `jdk.crypto.ec` makes the same GitHub request succeed. The release passes 21 Java tests and 4 JavaScript tests, plus a live HTTPS update check from the packaged EXE.

The installer and its SHA-256 sidecar are attached below. Account and playback availability remain controlled by YouTube Music.

## License and independence

MusatPlay is an independent, unofficial YouTube Music wrapper, licensed under MIT. It is not affiliated with, endorsed by, or sponsored by Google LLC, YouTube, YouTube Music, or Microsoft. Third-party components retain their licenses and notices.
