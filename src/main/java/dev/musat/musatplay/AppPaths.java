package dev.musat.musatplay;

import com.sun.jna.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AppPaths(Path root, Path profile) {
    public static AppPaths create() throws IOException {
        String local = System.getenv("LOCALAPPDATA");
        Path root = Platform.isWindows() && local != null && !local.isBlank()
                ? Path.of(local, "MusatPlay")
                : Path.of(System.getProperty("user.home"), ".musatplay-windows-preview");
        Path profile = root.resolve("webview2-profile");
        Files.createDirectories(profile);
        return new AppPaths(root, profile);
    }
}
