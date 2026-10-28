<div align="center">
  <img src="src/main/resources/icon.png" width="88" height="88" alt="MusatPlay icon">
  <h1>MusatPlay for Windows</h1>
  <p>A dedicated desktop window for YouTube Music.</p>
  <p>Java + Swing + Microsoft Edge WebView2</p>
  <p><a href="https://github.com/musatDev/MusatPlay-Windows/releases/latest">Download for Windows</a> · <a href="#getting-started">Getting started</a> · <a href="#build-from-source">Build from source</a> · <a href="LICENSE">MIT license</a></p>
</div>

**MusatPlay is an independent, unofficial wrapper. It is not affiliated with, endorsed by, or sponsored by Google LLC, YouTube, YouTube Music, or Microsoft.** YouTube Music runs inside an application window; the service, music catalog, account system, subscriptions, and playback availability are provided by YouTube Music.

## A desktop home for your music

- A single YouTube Music window with an application profile separate from your everyday browser.
- Background playback when you minimize or close the window; restore it from the system tray.
- Track notifications with artwork, title, artist, and playback controls. Hover to keep a notification visible.
- Previous / next, play / pause, shuffle, and repeat controls in the notification and tray.
- Global keyboard shortcuts, with notification suppression when another application fills the screen.
- A clean dark interface with only **MusatPlay** and **Help** menus.
- English (US) application and installer.
- **Help → Check for updates…** checks this repository's published Windows releases, downloads the installer, and verifies its SHA-256 before offering installation.

Java is included in the download. The application uses the shared WebView2 Evergreen Runtime already present on many Windows systems instead of shipping a separate JCEF browser distribution. WebView2 itself is Chromium-based; this architecture does not guarantee a particular RAM footprint.

## System requirements

| Component | Requirement |
| --- | --- |
| Operating system | Windows 10 or Windows 11, 64-bit |
| Processor architecture | x64; no native ARM64 or 32-bit package is provided |
| Web engine | Microsoft Edge WebView2 Evergreen Runtime |
| Java | Included; a separate Java installation is not required |
| Installer helpers | .NET Framework 4.x, normally included with Windows 10/11 |
| Connection | Internet access to YouTube Music, Google sign-in, and GitHub for update checks |
| Account | A Google account for personalized YouTube Music features; service restrictions and subscription requirements are determined by YouTube Music |

The installer is approximately 39 MB. Installed size and profile/cache storage vary. Native engine startup and automated tests have been run on Windows 11 x64. Windows 10 and real account/playback scenarios still require the acceptance checks in [TESTING.md](TESTING.md); no unmeasured minimum RAM or performance guarantee is claimed.

## Getting started

1. Open the [latest release](https://github.com/musatDev/MusatPlay-Windows/releases/latest) and download **MusatPlay-Setup-1.1.3.exe**.
2. Exit any running copy of MusatPlay using **MusatPlay → Quit** or the tray menu.
3. Run Setup. Choose the installation folder and whether to create desktop and Start menu shortcuts.
4. If WebView2 is missing, Setup offers Microsoft's download page before installation starts. Install the Evergreen Runtime and select **Check again**.
5. Open MusatPlay. YouTube Music loads inside the application. Use **MusatPlay → Sign in to Google** or the site's own sign-in button to access your account.

The app and installer are unsigned, so Windows may display a publisher or SmartScreen warning. Download only from this repository's Releases section. A SHA-256 sidecar is provided with each installer.

Google may restrict sign-in in embedded browsers. MusatPlay does not bypass Google's browser, account, or security restrictions. External-browser cookies are not imported into the application.

## Everyday use

Closing the main window keeps music playing when the tray icon is available. Click the tray icon, choose **Open MusatPlay**, or launch MusatPlay again to restore the existing window. Choose **Quit** to stop the application completely.

The main menu offers Google sign-in, page reload, and Quit. Help contains keyboard shortcuts and update checks. Playback controls remain in YouTube Music, the track notification, and the tray menu.

| Shortcut | Action |
| --- | --- |
| Ctrl + Alt + Space | Play / pause |
| Ctrl + Alt + Left / Right | Previous / next track |
| Ctrl + Alt + S | Shuffle |
| Ctrl + Alt + R | Repeat mode |
| Ctrl + Q | Quit |
| Ctrl + R | Reload page |

Another application may already own a global shortcut. In that case, MusatPlay reports the conflict and the player/tray controls remain available.

## Updates

Choose **Help → Check for updates…**. When a newer stable release is available, select **Download update**. The application checks the downloaded file's size and SHA-256; it never launches an unverified download. Select **Install update** to close MusatPlay and open the new installer. Your application profile stays saved.

Update checks use GitHub Releases, not source-code commits. If no newer release is published, the application reports that normally. Network failures and GitHub rate limits are shown without interrupting playback.

## Profiles and uninstalling

- Account data and browser storage: `%LOCALAPPDATA%\MusatPlay\webview2-profile`
- Application logs and instance state: `%LOCALAPPDATA%\MusatPlay`
- Downloaded update installers: `%LOCALAPPDATA%\MusatPlay\updates`

The WebView2 profile is separate from Microsoft Edge and other applications. Application sign-in and sign-out links stay inside MusatPlay. Do not upload or share the profile directory: it contains account/session data.

Run **Uninstall.exe** from the installation folder, use the Start menu uninstall shortcut if selected, or remove MusatPlay from Windows Settings. Uninstalling keeps the account profile. Delete that profile yourself only if you want to clear the application's saved session and site storage.

Older development versions used JCEF. Their profiles are not imported or deleted; moving to this edition requires signing in again.

## Build from source

### Developer tools

- Windows x64
- JDK 21 x64, with `JAVA_HOME` and `PATH` pointing to it
- Node.js 22 or newer for the JavaScript tests
- WiX Toolset 3 (`heat.exe`, `candle.exe`, `light.exe`) to build Setup.exe
- The Windows .NET Framework C# compiler for the small installer/uninstaller helpers

Maven is downloaded by the included wrapper. The application remains Java; the C# helpers only launch the Windows installer and uninstaller.

```powershell
git clone https://github.com/musatDev/MusatPlay-Windows.git
cd MusatPlay-Windows
.\mvnw.cmd -B verify
node --test tests/player-bridge.test.cjs
.\scripts\package-webview2.ps1 -WixHome C:\tools\wix314
```

The installer and checksum are written to `release/`. Build output stays under `target/` and is excluded from source control. To create a portable ZIP instead:

```powershell
.\scripts\package-windows.ps1
```

Extract the entire portable folder and run MusatPlay.exe. Keep its neighboring files; do not copy the executable alone.

To run an unpackaged development build, Java 21 must be installed:

```powershell
java -jar target/webview2-build/MusatPlay.jar
```

### Verification

Java tests cover navigation and origin rules, media validation, single-instance handling, notification timing, English-only menus, release selection, and update verification. JavaScript tests exercise the actual player-state reader against a simulated page.

The native smoke check evaluates JavaScript through the real installed WebView2 engine, uses an isolated profile, writes its result, and exits without accessing an account:

```powershell
java -jar target/webview2-build/MusatPlay.jar --webview2-smoke target/webview2-build/smoke.json
```

See [TESTING.md](TESTING.md) for manual playback, account, notification, and multi-monitor checks.

### Publishing a new version

1. Update the project version in `pom.xml` and the default version in `scripts/package-webview2.ps1`.
2. Build and complete the relevant verification steps.
3. Create a stable GitHub release with a tag such as `v1.1.3`.
4. Upload **MusatPlay-Setup-1.1.3.exe** and **MusatPlay-Setup-1.1.3.exe.sha256** from `release/`.

The tag, application version, and installer filename must match. The updater prefers GitHub's asset SHA-256 digest and uses the sidecar as a fallback. Source archives alone are not Windows application updates.

## License and independence

Original application code is licensed under the [MIT License](LICENSE). Dependencies, the Java runtime, and installer resources retain their own licenses; see [THIRD_PARTY.md](THIRD_PARTY.md). Copyright (c) 2026 musatDev.

YouTube and YouTube Music are trademarks of Google LLC. Microsoft Edge and WebView2 are Microsoft trademarks. MusatPlay is an independent wrapper and has no official connection to those companies or services. It does not provide its own music service, downloads, ad blocking, or a way around subscriptions or security restrictions. Your use of YouTube Music remains subject to its terms and availability.