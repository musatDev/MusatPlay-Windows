package dev.musat.musatplay;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.time.Duration;
import java.util.*;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

final class UpdateService {
    static final String REPOSITORY = "https://github.com/musatDev/MusatPlay-Windows";
    static final URI API = URI.create("https://api.github.com/repos/musatDev/MusatPlay-Windows/releases/latest");
    private static final String DOWNLOAD_PATH = "/musatDev/MusatPlay-Windows/releases/download/";
    private static final long MAX_INSTALLER = 512L * 1024 * 1024;
    private final Fetcher fetcher;
    private final String current;

    interface Fetcher { Response get(URI uri) throws IOException, InterruptedException; }
    record Response(int status, InputStream body) implements AutoCloseable {
        public void close() throws IOException { body.close(); }
    }
    record Release(String version, String fileName, URI download, long size, String digest, URI checksum) {}

    UpdateService() { this(network(), currentVersion()); }
    UpdateService(Fetcher fetcher, String current) { this.fetcher = fetcher; this.current = current; }

    static String currentVersion() {
        String version = System.getProperty("jpackage.app-version");
        if (version == null) version = UpdateService.class.getPackage().getImplementationVersion();
        return version == null ? "0.0.0" : version;
    }

    Optional<Release> check() throws IOException, InterruptedException {
        try (Response response = fetcher.get(API)) {
            if (response.status == 404) return Optional.empty();
            if (response.status == 403 || response.status == 429)
                throw new IOException("GitHub is temporarily limiting update checks. Please try again later.");
            if (response.status != 200) throw new IOException("Could not reach GitHub. Please try again later.");
            try { return parseLatest(readText(response.body, 2 * 1024 * 1024), current); }
            catch (RuntimeException error) { throw new IOException("The release information could not be read.", error); }
        }
    }

    static Optional<Release> parseLatest(String json, String current) throws IOException {
        JsonObject release = JsonParser.parseString(json).getAsJsonObject();
        if ((release.has("draft") && release.get("draft").getAsBoolean())
                || (release.has("prerelease") && release.get("prerelease").getAsBoolean())) return Optional.empty();
        String tag = release.get("tag_name").getAsString();
        String version = version(tag);
        if (compare(version, current) <= 0) return Optional.empty();
        String name = "MusatPlay-Setup-" + version + ".exe";
        JsonObject installer = null;
        URI checksum = null;
        for (JsonElement element : release.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            String assetName = asset.get("name").getAsString();
            if (assetName.equals(name)) installer = asset;
            if (assetName.equals(name + ".sha256"))
                checksum = assetUrl(asset.get("browser_download_url").getAsString(), tag, assetName);
        }
        if (installer == null) throw new IOException("A new release exists, but its Windows installer has not been uploaded yet.");
        long size = installer.get("size").getAsLong();
        if (size <= 0 || size > MAX_INSTALLER) throw new IOException("The release installer has an unexpected size.");
        String digest = installer.has("digest") && !installer.get("digest").isJsonNull()
                ? installer.get("digest").getAsString() : "";
        if (digest.matches("sha256:[a-fA-F0-9]{64}")) digest = digest.substring(7).toLowerCase(Locale.ROOT);
        else digest = "";
        return Optional.of(new Release(version, name,
                assetUrl(installer.get("browser_download_url").getAsString(), tag, name), size, digest, checksum));
    }

    static URI assetUrl(String value, String tag, String name) throws IOException {
        URI uri;
        try { uri = URI.create(value); } catch (IllegalArgumentException error) { throw new IOException("Invalid update address."); }
        if (!"https".equals(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                || !uri.getRawPath().equals(DOWNLOAD_PATH + tag + "/" + name) || uri.getQuery() != null)
            throw new IOException("The installer is not hosted in the MusatPlay Windows release repository.");
        return uri;
    }

    Path download(Release release, Path updates, LongConsumer progress) throws IOException, InterruptedException {
        String expected = release.digest;
        if (expected.isEmpty() && release.checksum != null) {
            try (Response response = fetcher.get(release.checksum)) {
                if (response.status != 200) throw new IOException("The update checksum could not be downloaded.");
                String text = readText(response.body, 4096).trim();
                var match = Pattern.compile("^([a-fA-F0-9]{64})(?:\\s+\\*?" + Pattern.quote(release.fileName) + ")?$").matcher(text);
                if (!match.matches()) throw new IOException("The release checksum is invalid.");
                expected = match.group(1).toLowerCase(Locale.ROOT);
            }
        }
        if (!expected.matches("[a-f0-9]{64}"))
            throw new IOException("This release has no SHA-256 checksum. The publisher needs to upload a verified installer.");
        Files.createDirectories(updates);
        Path folder = Files.createTempDirectory(updates, "update-" + release.version + "-");
        Path temporary = folder.resolve("download.partial");
        Path result = folder.resolve(release.fileName);
        boolean complete = false;
        try (Response response = fetcher.get(release.download)) {
            if (response.status != 200) throw new IOException("The update could not be downloaded.");
            MessageDigest hash;
            try { hash = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            long count = 0;
            try (OutputStream output = Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = response.body.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    count += read;
                    if (count > release.size || count > MAX_INSTALLER) throw new IOException("The download is larger than expected.");
                    hash.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                    progress.accept(count);
                }
            }
            if (count != release.size || !HexFormat.of().formatHex(hash.digest()).equals(expected))
                throw new IOException("The download did not pass verification. Please try again.");
            try (InputStream executable = Files.newInputStream(temporary)) {
                if (executable.read() != 'M' || executable.read() != 'Z') throw new IOException("The update is not a Windows installer.");
            }
            Files.move(temporary, result, StandardCopyOption.ATOMIC_MOVE);
            complete = true;
            return result;
        } finally {
            if (!complete) { Files.deleteIfExists(temporary); Files.deleteIfExists(folder); }
        }
    }

    static String version(String value) {
        if (!value.matches("v?\\d{1,5}\\.\\d{1,5}\\.\\d{1,5}")) throw new IllegalArgumentException("Use a stable x.y.z release tag.");
        return value.startsWith("v") ? value.substring(1) : value;
    }
    static int compare(String a, String b) {
        String[] left = version(a).split("\\."), right = version(b).split("\\.");
        for (int i = 0; i < 3; i++) {
            int result = Integer.compare(Integer.parseInt(left[i]), Integer.parseInt(right[i]));
            if (result != 0) return result;
        }
        return 0;
    }
    private static String readText(InputStream body, int limit) throws IOException {
        byte[] bytes = body.readNBytes(limit + 1);
        if (bytes.length > limit) throw new IOException("The response is too large.");
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
    private static Fetcher network() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(uri.equals(API) ? Duration.ofSeconds(20) : Duration.ofMinutes(5))
                .header("Accept", uri.equals(API) ? "application/vnd.github+json" : "application/octet-stream")
                .header("User-Agent", "MusatPlay/" + currentVersion()).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String host = response.uri().getHost();
            if (!"https".equals(response.uri().getScheme()) || !Set.of("api.github.com", "github.com",
                    "release-assets.githubusercontent.com", "objects.githubusercontent.com").contains(host)) {
                response.body().close();
                throw new IOException("The update download was redirected to an unexpected site.");
            }
            return new Response(response.statusCode(), response.body());
        };
    }
}
