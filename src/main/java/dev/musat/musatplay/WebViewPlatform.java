package dev.musat.musatplay;

import java.util.Locale;

final class WebViewPlatform {
    private static boolean ready;
    static synchronized void initialize() {
        if (ready) return;
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ROOT);
            ca.weblite.webview.nativelib.NativeLibraryUtil.getPlatformLibraryPath("");
            ready = true;
        } finally { Locale.setDefault(original); }
    }
}
