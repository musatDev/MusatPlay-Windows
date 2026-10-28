package dev.musat.musatplay;

public enum PlayerAction {
    SHUFFLE(".shuffle", "Shuffle", 'S'),
    PREVIOUS(".previous-button", "Previous track", 0x25),
    PLAY_PAUSE(".play-pause-button", "Play / pause", 0x20),
    NEXT(".next-button", "Next track", 0x27),
    REPEAT(".repeat", "Repeat mode", 'R');

    public final String selector;
    public final String label;
    public final int virtualKey;

    PlayerAction(String selector, String label, int virtualKey) {
        this.selector = selector;
        this.label = Language.text(label);
        this.virtualKey = virtualKey;
    }

    public String script() {
        return "(() => { const b = document.querySelector('ytmusic-player-bar " + selector
                + "'); if (b && !b.disabled && b.getAttribute('aria-disabled') !== 'true') b.click(); })();";
    }
}
