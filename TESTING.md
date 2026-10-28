# Windows acceptance checklist

Automated tests do not replace real account, playback, and desktop testing. Mark checks only after completing them on the target system.

## Installation and updates

Packaging runs the produced EXE with `--check-updates <report-path>` against the real GitHub endpoint. The EXE must exit successfully and write the report using only its bundled Java runtime. This catches missing TLS providers that unit tests running on a full JDK cannot detect. Internet access to GitHub is required for this packaging check.

- Install on Windows 11 x64 with WebView2 present and confirm no Java installation is needed.
- On a system without WebView2, confirm the prerequisite screen appears before installation begins and the Microsoft download button works.
- Test both shortcut choices independently, including no shortcuts.
- Change the install location, install, and start from the selected folder.
- Upgrade from an earlier installer and confirm the WebView2 profile is retained.
- Check for updates with no newer release, a newer valid release, missing assets, a network failure, and a GitHub rate limit.
- Reject a corrupted/truncated installer and a checksum mismatch. Only offer installation after verification succeeds.
- Confirm Uninstall.exe removes the installed application while leaving the account profile intact.

## Account and playback

- Confirm sign-in/sign-out stays inside the application and does not change the everyday browser's session.
- Sign in to Google, restart, and confirm the session persists. Record any embedded-browser rejection without bypassing security controls.
- Play several tracks; exercise seek, skip, shuffle, repeat, pause, and resume against the site's controls.
- Confirm metadata, artwork, notification controls, and tray controls match the current track.
- Minimize and close the main window while playing; restore from the tray and a second launch.
- Quit from the menu, tray, and Ctrl+Q. Check for orphaned application/browser processes.
- Test network loss/recovery, sleep/wake, and an expired account session.

## Desktop integration

- Verify each Ctrl+Alt shortcut with another application focused, and report conflicts correctly.
- Check notification fade/slide, hover hold, dismissal after leaving, and rapid track changes.
- Repeated play/pause should update controls without reopening a dismissed notification.
- Check that notifications do not take focus and are suppressed over full-screen applications.
- Test multiple monitors, negative coordinates, different taskbar positions, and 100–200% display scaling.
- Verify the app has only MusatPlay and Help top-level menus and all application/installer text is English.
- Test missing artwork, long titles, emoji, and media metadata being cleared.

## Compatibility and resource use

- Validate Windows 10 x64 and Windows 11 x64 separately.
- Measure all application and WebView2 processes during cold startup, idle use, playback, and background playback.
- Distinguish private memory from summed working sets, which may double-count shared pages.
- Do not claim a RAM improvement or a minimum memory requirement without measurements from the same playback scenario.
- Review bundled dependency notices and signing status before publishing each release.
