package dev.musat.musatplay;


public final class Language {
    private Language() {}
    public static String normalize(String ignored) { return "en-US"; }
    public static String currentTag() { return "en-US"; }
    public static String text(String english) { return english; }
    public static String shortcutsHelp() {
        return "Ctrl + Alt + Space: Play / pause\nCtrl + Alt + Left / Right: Previous / next\n"
            + "Ctrl + Alt + S: Shuffle\nCtrl + Alt + R: Repeat\nCtrl + Q: Quit\n\n"
            + "Closing the window keeps music playing. Restore it from the system tray.";
    }
}