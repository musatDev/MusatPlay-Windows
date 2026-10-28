package dev.musat.musatplay;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;

public record MediaState(String trackId, String title, String artist, URI artwork,
                         boolean playing, boolean shuffle, Repeat repeat) {
    public enum Repeat { NONE, ALL, ONE }
    public static final MediaState EMPTY = new MediaState("", "", "", null, false, false, Repeat.NONE);

    public static MediaState parse(String json) {
        if (json == null || json.length() > 16_384) throw new IllegalArgumentException("Invalid media message size");
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String title = text(o, "title", 1_000);
            if (title.isBlank()) return EMPTY;
            String repeat = text(o, "repeat", 8);
            Repeat mode = switch (repeat) { case "ALL" -> Repeat.ALL; case "ONE" -> Repeat.ONE; default -> Repeat.NONE; };
            URI artwork = BrowserPolicy.https(text(o, "artwork", 2_048));
            if (!BrowserPolicy.isArtwork(artwork)) artwork = null;
            return new MediaState(text(o, "trackId", 2_048), title, text(o, "artist", 1_000),
                    artwork, bool(o, "playing"), bool(o, "shuffle"), mode);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid media message", e);
        }
    }

    private static String text(JsonObject o, String key, int limit) {
        if (!o.has(key) || o.get(key).isJsonNull()) return "";
        if (!o.get(key).isJsonPrimitive() || !o.get(key).getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Expected string: " + key);
        String value = o.get(key).getAsString();
        if (value.length() > limit) throw new IllegalArgumentException("Text is too long: " + key);
        return value;
    }

    private static boolean bool(JsonObject o, String key) {
        if (!o.has(key)) return false;
        if (!o.get(key).isJsonPrimitive() || !o.get(key).getAsJsonPrimitive().isBoolean())
            throw new IllegalArgumentException("Expected boolean: " + key);
        return o.get(key).getAsBoolean();
    }

    public boolean hasTrack() { return !title.isBlank(); }

    public boolean differentTrack(MediaState previous) {
        return hasTrack() && (!trackId.equals(previous.trackId) || !title.equals(previous.title)
                || !artist.equals(previous.artist));
    }
}
