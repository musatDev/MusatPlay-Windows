package dev.musat.musatplay;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class ArtworkLoader implements AutoCloseable {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().daemon().name("musatplay-artwork").factory());
    private final Map<URI, BufferedImage> cache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<URI, BufferedImage> e) { return size() > 32; }
    };
    private URI current;
    private Future<?> pending;
    private boolean closed;

    public void load(URI url, Consumer<BufferedImage> callback) {
        if (closed || java.util.Objects.equals(current, url)) return;
        current = url;
        if (pending != null) pending.cancel(true);
        callback.accept(null);
        if (!BrowserPolicy.isArtwork(url)) return;
        BufferedImage hit = cache.get(url);
        if (hit != null) { callback.accept(hit); return; }
        pending = executor.submit(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(5)).GET().build();
                var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                BufferedImage image;
                try (var body = response.body()) {
                    if (response.statusCode() != 200) return;
                    byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                    if (bytes.length > MAX_BYTES) return;
                    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                        var readers = ImageIO.getImageReaders(input);
                        if (!readers.hasNext()) return;
                        var reader = readers.next();
                        try {
                            reader.setInput(input);
                            if (reader.getWidth(0) > 4_096 || reader.getHeight(0) > 4_096) return;
                            image = AppImages.scaled(reader.read(0), 112, 112);
                        } finally { reader.dispose(); }
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (!closed && url.equals(current)) { cache.put(url, image); callback.accept(image); }
                });
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (IOException | RuntimeException ignored) {}
        });
    }

    @Override public void close() {
        closed = true;
        if (pending != null) pending.cancel(true);
        executor.shutdownNow();
        http.shutdownNow();
        cache.clear();
    }
}
