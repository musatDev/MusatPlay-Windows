package dev.musat.musatplay;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class UpdateServiceTest {
    @TempDir Path temp;
    private static final byte[] INSTALLER = "MZverified Windows installer fixture".getBytes(StandardCharsets.UTF_8);
    private static final String ASSET = UpdateService.REPOSITORY + "/releases/download/v1.2.0/MusatPlay-Setup-1.2.0.exe";

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    private static JsonObject release() throws Exception {
        var release = new JsonObject();
        release.addProperty("tag_name", "v1.2.0");
        release.addProperty("draft", false); release.addProperty("prerelease", false);
        var asset = new JsonObject();
        asset.addProperty("name", "MusatPlay-Setup-1.2.0.exe");
        asset.addProperty("browser_download_url", ASSET);
        asset.addProperty("digest", "sha256:" + hash(INSTALLER));
        asset.addProperty("size", INSTALLER.length);
        var assets = new JsonArray(); assets.add(asset); release.add("assets", assets);
        return release;
    }
    private static UpdateService.Response response(int status, byte[] content) {
        return new UpdateService.Response(status, new ByteArrayInputStream(content));
    }
    @Test void comparesNumericVersionsAndRejectsUnstableTags() {
        assertTrue(UpdateService.compare("v1.10.0", "1.9.9") > 0);
        assertEquals(0, UpdateService.compare("v1.1.1", "1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> UpdateService.version("1.2.0-beta.1"));
    }
    @Test void selectsNewInstallerButNotOlderOrPrerelease() throws Exception {
        var release = release();
        assertEquals("1.2.0", UpdateService.parseLatest(release.toString(), "1.1.1").orElseThrow().version());
        assertTrue(UpdateService.parseLatest(release.toString(), "1.2.0").isEmpty());
        assertTrue(UpdateService.parseLatest(release.toString(), "2.0.0").isEmpty());
        release.addProperty("prerelease", true);
        assertTrue(UpdateService.parseLatest(release.toString(), "1.1.1").isEmpty());
    }
    @Test void refusesInstallersOutsideTheConfiguredRepository() throws Exception {
        for (String url : List.of(ASSET.replace("github.com", "github.com.evil.test"),
                ASSET.replace("musatDev", "someoneElse"), ASSET.replace("https:", "http:"))) {
            var release = release();
            release.getAsJsonArray("assets").get(0).getAsJsonObject().addProperty("browser_download_url", url);
            assertThrows(IOException.class, () -> UpdateService.parseLatest(release.toString(), "1.1.1"));
        }
    }
    @Test void emptyRepositoryAndRateLimitsHaveDistinctOutcomes() throws Exception {
        assertTrue(new UpdateService(uri -> response(404, new byte[0]), "1.1.1").check().isEmpty());
        assertThrows(IOException.class, () -> new UpdateService(uri -> response(403, new byte[0]), "1.1.1").check());
        var release = release(); release.getAsJsonArray("assets").remove(0);
        assertThrows(IOException.class, () -> UpdateService.parseLatest(release.toString(), "1.1.1"));
    }
    @Test void downloadsAndVerifiesInstallerBeforeMakingItAvailable() throws Exception {
        var release = UpdateService.parseLatest(release().toString(), "1.1.1").orElseThrow();
        var service = new UpdateService(uri -> response(200, INSTALLER), "1.1.1");
        Path result = service.download(release, temp, ignored -> {});
        assertArrayEquals(INSTALLER, Files.readAllBytes(result));
        assertEquals("MusatPlay-Setup-1.2.0.exe", result.getFileName().toString());
    }
    @Test void corruptAndTruncatedDownloadsNeverBecomeExecutable() throws Exception {
        var release = UpdateService.parseLatest(release().toString(), "1.1.1").orElseThrow();
        for (byte[] payload : List.of("MZcorrupt".getBytes(StandardCharsets.UTF_8), Arrays.copyOf(INSTALLER, INSTALLER.length - 1))) {
            var service = new UpdateService(uri -> response(200, payload), "1.1.1");
            assertThrows(IOException.class, () -> service.download(release, temp, ignored -> {}));
            try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
        }
    }
    @Test void missingDigestNeedsSidecarBeforeDownloadingInstaller() throws Exception {
        var json = release();
        json.getAsJsonArray("assets").get(0).getAsJsonObject().remove("digest");
        var release = UpdateService.parseLatest(json.toString(), "1.1.1").orElseThrow();
        var service = new UpdateService(uri -> { fail("Installer must not be downloaded without a checksum"); return null; }, "1.1.1");
        assertThrows(IOException.class, () -> service.download(release, temp, ignored -> {}));
        var sidecar = new JsonObject();
        sidecar.addProperty("name", "MusatPlay-Setup-1.2.0.exe.sha256");
        sidecar.addProperty("browser_download_url", ASSET + ".sha256");
        json.getAsJsonArray("assets").add(sidecar);
        var verified = UpdateService.parseLatest(json.toString(), "1.1.1").orElseThrow();
        byte[] checksum = (hash(INSTALLER) + "  " + verified.fileName()).getBytes(StandardCharsets.UTF_8);
        var fallback = new UpdateService(uri -> response(200, uri.toString().endsWith(".sha256") ? checksum : INSTALLER), "1.1.1");
        assertTrue(Files.exists(fallback.download(verified, temp, ignored -> {})));
    }
}
