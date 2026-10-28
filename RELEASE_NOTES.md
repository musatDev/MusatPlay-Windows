# MusatPlay for Windows v1.1.2

The first public Windows release of MusatPlay: an independent desktop wrapper for YouTube Music, built with Java, Swing, and the system WebView2 Runtime.

## Included

- A clean English-only dark interface with MusatPlay and Help menus.
- YouTube Music in its own application profile, separate from your everyday browser.
- Background playback, a system tray menu, and global playback shortcuts.
- Track notifications with artwork and playback controls.
- In-app update checks with installer size and SHA-256 verification.
- Bundled Java: no separate Java setup required.
- Optional desktop and Start menu shortcuts, an installation folder chooser, and Uninstall.exe.

## Download and install

Download **MusatPlay-Setup-1.1.2.exe** below, exit any running copy of MusatPlay, and run Setup. The **.sha256** file is provided for integrity verification and the application's update mechanism.

**Requirements:** Windows 10/11 x64, internet access, and Microsoft Edge WebView2 Evergreen Runtime. If WebView2 is missing, Setup links to Microsoft's download page before installation starts. A Google account is needed for personalized YouTube Music features.

## Verification and limitations

The release passes 21 Java tests and 4 JavaScript tests. Packaged native startup and JavaScript execution have been checked through WebView2 on Windows 11 x64. English-only runtime data, installer options, uninstaller presence, and release checksums have been inspected.

These checks do not establish full account/playback compatibility on every machine. Google may restrict embedded-browser sign-in; those restrictions are not bypassed. Windows 10 and full playback scenarios need the manual checks documented in TESTING.md. The app and installer are unsigned, so Windows may show a publisher or SmartScreen warning. RAM use depends on YouTube Music content and WebView2; no fixed performance improvement is claimed.

## License and notice

Original code is licensed under MIT. Dependency licenses and the bundled Java runtime's legal files remain included.

MusatPlay is an independent, unofficial wrapper, not a music service. It is not affiliated with, endorsed by, or sponsored by Google LLC, YouTube, YouTube Music, or Microsoft. YouTube and YouTube Music are Google LLC trademarks. Playback, account access, subscriptions, and service availability are supplied by YouTube Music. MusatPlay provides no music downloads, ad blocking, or bypass of service/security restrictions.