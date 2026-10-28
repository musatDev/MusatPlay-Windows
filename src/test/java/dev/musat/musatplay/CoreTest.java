package dev.musat.musatplay;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreTest {
    @TempDir Path temp;

    @Test void webViewNativePathIsStableUnderTurkishLocale() {
        var original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            WebViewPlatform.initialize();
            assertEquals("windows_64/", ca.weblite.webview.nativelib.NativeLibraryUtil.getPlatformLibraryPath(""));
            assertEquals("tr-TR", java.util.Locale.getDefault().toLanguageTag());
        } finally { java.util.Locale.setDefault(original); }
    }

    @Test void applicationUsesOnlyUsEnglishEvenOnTurkishWindows() {
        assertEquals("en-US", Language.normalize("tr-TR"));
        assertEquals("en-US", Language.normalize("en-GB"));
        assertEquals("en-US", Language.normalize("de-DE"));
    }

    @Test void topBarContainsOnlyApplicationAndHelpMenus() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            AppTheme.install();
            var menu = AppMenu.create(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
            assertEquals(2, menu.getMenuCount());
            assertEquals("MusatPlay", menu.getMenu(0).getText());
            assertEquals("Help", menu.getMenu(1).getText());
            assertEquals("Check for updates…", menu.getMenu(1).getItem(1).getText());
        });
    }

    @Test void musicOriginRejectsLookalikesAndCredentials() {
        assertTrue(BrowserPolicy.isMusic("https://music.youtube.com/watch?v=1"));
        for (String url : new String[]{"http://music.youtube.com", "https://music.youtube.com.evil.com",
                "https://music.youtube.com@evil.com", "https://evil@music.youtube.com", "https://music.youtube.com:8443", "javascript:alert(1)"})
            assertFalse(BrowserPolicy.isMusic(url), url);
    }

    @Test void httpsNavigationStaysInAppIncludingRegionalAuthentication() {
        for (String url : new String[]{"https://accounts.google.com/ServiceLogin",
                "https://accounts.youtube.com/accounts/SetSID", "https://accounts.google.com.tr/",
                "https://accounts.google.de/", "https://example.com/"}) {
            assertTrue(BrowserPolicy.canNavigate(url), url);
            assertFalse(BrowserPolicy.isMusic(url), url);
        }
        for (String url : new String[]{"http://accounts.youtube.com/", "https://evil@accounts.youtube.com/",
                "javascript:alert(1)", "file:///C:/test", "microsoft-edge:https://music.youtube.com/"})
            assertFalse(BrowserPolicy.canNavigate(url), url);
    }

    @Test void mediaValidationRejectsMalformedOrOversizedMessages() {
        for (String json : new String[]{"[]", "null", "{", "{\"title\":true}",
                "{\"title\":\"Track\",\"playing\":\"true\"}", "x".repeat(16_385)})
            assertThrows(IllegalArgumentException.class, () -> MediaState.parse(json));
    }

    @Test void missingMetadataClearsStateAndUntrustedArtworkIsDropped() {
        assertEquals(MediaState.EMPTY, MediaState.parse("{}"));
        var state = MediaState.parse("{\"title\":\"Track\",\"artwork\":\"https://localhost/private\",\"repeat\":\"ONE\"}");
        assertNull(state.artwork());
        assertEquals(MediaState.Repeat.ONE, state.repeat());
        assertTrue(state.differentTrack(MediaState.EMPTY));
    }

    @Test void playbackAndLateArtworkDoNotRetriggerTrackNotification() {
        var first = MediaState.parse("{\"title\":\"Track\",\"trackId\":\"id\"}");
        var next = MediaState.parse("{\"title\":\"Track\",\"trackId\":\"id\",\"playing\":true,\"artwork\":\"https://lh3.googleusercontent.com/a\"}");
        assertNotNull(next.artwork());
        assertFalse(next.differentTrack(first));
        assertTrue(MediaState.parse("{\"title\":\"Track\",\"trackId\":\"different\"}").differentTrack(first));
    }

    @Test void notificationFadesSlidesAndDismisses() {
        var timeline = new NotificationTimeline();
        timeline.show(0);
        assertEquals(0, timeline.tick(0).alpha());
        assertEquals(20, timeline.tick(0).offset());
        assertEquals(0.5f, timeline.tick(375).alpha(), 0.001f);
        assertEquals(1, timeline.tick(750).alpha());
        assertEquals(1, timeline.tick(4250).alpha());
        assertEquals(0.5f, timeline.tick(4625).alpha(), 0.001f);
        assertFalse(timeline.tick(5000).visible());
    }

    @Test void hoverKeepsOpenThenWaitsFiveSeconds() {
        var timeline = new NotificationTimeline();
        timeline.show(0);
        timeline.hover(true, 1000);
        assertEquals(1, timeline.tick(100_000).alpha());
        timeline.hover(false, 100_000);
        assertEquals(1, timeline.tick(104_999).alpha());
        timeline.tick(105_000);
        assertFalse(timeline.tick(105_750).visible());
    }

    @Test void newTrackAndHoverCancelFadeOut() {
        var timeline = new NotificationTimeline();
        timeline.show(0);
        timeline.tick(4250);
        assertTrue(timeline.tick(4500).alpha() < 1);
        timeline.show(4500);
        assertEquals(1, timeline.tick(4800).alpha());
        timeline.tick(8000);
        timeline.hover(true, 8200);
        assertEquals(1, timeline.tick(9000).alpha());
        assertTrue(timeline.tick(100_000).visible());
    }

    @Test void fullscreenRequiresMatchingPositionNotJustSize() {
        assertTrue(WindowsIntegration.fills(-1920, 0, 0, 1080, -1920, 0, 0, 1080));
        assertFalse(WindowsIntegration.fills(0, 0, 1920, 1080, -1920, 0, 0, 1080));
        assertFalse(WindowsIntegration.fills(0, 20, 1920, 1080, 0, 0, 1920, 1080));
    }

    @Test void secondLaunchActivatesExistingInstanceAndLockCanBeReused() throws Exception {
        CountDownLatch activated = new CountDownLatch(1);
        try (var first = SingleInstance.acquire(temp, activated::countDown).orElseThrow()) {
            assertTrue(SingleInstance.acquire(temp, () -> fail("Unexpected second listener")).isEmpty());
            assertTrue(activated.await(3, TimeUnit.SECONDS));
        }
        try (var restarted = SingleInstance.acquire(temp, () -> {}).orElseThrow()) {
            assertTrue(Files.exists(temp.resolve("instance.endpoint")));
        }
        assertFalse(Files.exists(temp.resolve("instance.endpoint")));
    }

    @Test void windowsIconContainsValidPngEntry() throws Exception {
        Path icon = temp.resolve("app.ico");
        AppImages.writeWindowsIcon(icon);
        byte[] data = Files.readAllBytes(icon);
        var header = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, header.getShort());
        assertEquals(1, header.getShort());
        assertEquals(1, header.getShort());
        assertEquals(0, data[6]);
        assertEquals(22, header.getInt(18));
        assertEquals((byte) 0x89, data[22]);
    }
}
