<div align="center">
  <img src="src/main/resources/icon.png" width="88" height="88" alt="MusatPlay icon">
  <h1>MusatPlay for Windows</h1>
  <p>YouTube Music in a dedicated desktop window.</p>
  <p><a href="https://github.com/musatDev/MusatPlay-Windows/releases/latest">Download for Windows</a> · <a href="#installation">Installation</a> · <a href="#build-from-source">Build from source</a></p>
</div>

MusatPlay is an independent, unofficial YouTube Music wrapper built with Java and Microsoft Edge WebView2. It is not affiliated with, endorsed by, or sponsored by Google, YouTube, YouTube Music, or Microsoft.

## Features

- Dark interface in English.
- Background playback with system tray controls.
- Track notifications with artwork and playback controls.
- Global keyboard shortcuts.
- A dedicated application sign-in profile.
- Update checks from the Help menu.

## Requirements

- Windows 10 or Windows 11, x64.
- Microsoft Edge WebView2 Evergreen Runtime.
- Internet connection.
- A Google account for personalized YouTube Music features.

Java is included. Music availability and subscription requirements are determined by YouTube Music.

## Installation

1. Download the installer from the [latest release](https://github.com/musatDev/MusatPlay-Windows/releases/latest).
2. Run Setup and choose the installation folder and optional shortcuts.
3. If WebView2 is missing, use **Download WebView2**, install it, then select **Check again**.
4. Open MusatPlay and sign in through **MusatPlay → Sign in to Google**.

The application and installer are unsigned, so Windows may display a publisher or SmartScreen warning. Google may restrict sign-in in embedded browsers.

## Usage

Closing the window keeps MusatPlay in the system tray. Use the tray icon to reopen it, or choose **Quit** to exit.

| Shortcut | Action |
| --- | --- |
| Ctrl + Alt + Space | Play / pause |
| Ctrl + Alt + Left / Right | Previous / next track |
| Ctrl + Alt + S | Shuffle |
| Ctrl + Alt + R | Repeat mode |
| Ctrl + Q | Quit |
| Ctrl + R | Reload |

For updates, choose **Help → Check for updates…**, then **Download update** and **Install update**. Downloads are verified with SHA-256.

To uninstall, run **Uninstall.exe** from the installation folder or use Windows Settings. Uninstalling retains your saved sign-in profile in `%LOCALAPPDATA%\MusatPlay\webview2-profile`.

## Build from source

Requires Windows x64, JDK 21, Node.js 22+, WiX Toolset 3, and the Windows .NET Framework C# compiler. Set `JAVA_HOME` and `PATH` to your JDK. Maven is provided through the wrapper.

```powershell
git clone https://github.com/musatDev/MusatPlay-Windows.git
cd MusatPlay-Windows
.\mvnw.cmd -B verify
node --test tests/player-bridge.test.cjs
.\scripts\package-webview2.ps1 -WixHome C:\tools\wix314
```

The installer and checksum are written to `release/`. Packaging requires access to GitHub to verify update connectivity. See [TESTING.md](TESTING.md) for additional checks.

## License

Licensed under [MIT](LICENSE). Third-party components retain their own licenses; see [THIRD_PARTY.md](THIRD_PARTY.md).

YouTube and YouTube Music are trademarks of Google LLC. Microsoft Edge and WebView2 are Microsoft trademarks. MusatPlay provides a desktop wrapper for the existing YouTube Music service.
