package dev.musat.musatplay;

import java.net.URI;
import java.util.Set;

public final class BrowserPolicy {
    public static final String HOME = "https://music.youtube.com";

    private BrowserPolicy() {}

    public static boolean isMusic(String url) {
        URI uri = https(url);
        return uri != null && "music.youtube.com".equalsIgnoreCase(uri.getHost());
    }

    public static boolean canNavigate(String url) {
        return https(url) != null;
    }

    public static URI https(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443) ? uri : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static boolean isArtwork(URI uri) {
        if (uri == null || https(uri.toString()) == null) return false;
        String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        return Set.of("googleusercontent.com", "ggpht.com", "ytimg.com", "youtube.com").stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }
}
