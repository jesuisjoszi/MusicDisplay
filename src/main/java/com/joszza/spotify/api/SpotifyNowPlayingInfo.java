package com.joszza.spotify.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import java.nio.file.Path;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyNowPlayingInfo {
    public enum Status {
        PLAYING,
        PAUSED,
        IDLE,
        ERROR
    }

    @Nonnull
    private final Status status;
    @Nullable
    private final String trackName;
    @Nullable
    private final String artistName;
    @Nullable
    private final String errorMessage;
    @Nullable
    private final String albumArtUrl;
    @Nullable
    private final Path localArtPath;
    private final long progressMs;
    private final long durationMs;
    private final long fetchedAtMs;

    private SpotifyNowPlayingInfo(
        @Nonnull Status status,
        @Nullable String trackName,
        @Nullable String artistName,
        @Nullable String errorMessage,
        @Nullable String albumArtUrl,
        @Nullable Path localArtPath,
        long progressMs,
        long durationMs,
        long fetchedAtMs
    ) {
        this.status = status;
        this.trackName = trackName;
        this.artistName = artistName;
        this.errorMessage = errorMessage;
        this.albumArtUrl = albumArtUrl;
        this.localArtPath = localArtPath;
        this.progressMs = progressMs;
        this.durationMs = durationMs;
        this.fetchedAtMs = fetchedAtMs;
    }

    @Nonnull
    public static SpotifyNowPlayingInfo idle() {
        return new SpotifyNowPlayingInfo(Status.IDLE, null, null, null, null, null, 0L, 0L, System.currentTimeMillis());
    }

    @Nonnull
    public static SpotifyNowPlayingInfo error(@Nonnull String message) {
        return new SpotifyNowPlayingInfo(Status.ERROR, null, null, message, null, null, 0L, 0L, System.currentTimeMillis());
    }

    @Nonnull
    public static SpotifyNowPlayingInfo of(
        @Nonnull Status status,
        @Nonnull String trackName,
        @Nonnull String artistName,
        long progressMs,
        long durationMs
    ) {
        return of(status, trackName, artistName, null, null, progressMs, durationMs);
    }

    @Nonnull
    public static SpotifyNowPlayingInfo of(
        @Nonnull Status status,
        @Nonnull String trackName,
        @Nonnull String artistName,
        @Nullable String albumArtUrl,
        @Nullable Path localArtPath,
        long progressMs,
        long durationMs
    ) {
        return new SpotifyNowPlayingInfo(
            status,
            trackName,
            artistName,
            null,
            albumArtUrl,
            localArtPath,
            progressMs,
            durationMs,
            System.currentTimeMillis()
        );
    }

    @Nonnull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public String getTrackName() {
        return trackName;
    }

    @Nullable
    public String getArtistName() {
        return artistName;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Nullable
    public String getAlbumArtUrl() {
        return albumArtUrl;
    }

    @Nullable
    public Path getLocalArtPath() {
        return localArtPath;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public float getProgressRatio() {
        return getProgressRatio(System.currentTimeMillis());
    }

    public float getProgressRatio(long nowMs) {
        if (durationMs <= 0L) {
            return 0f;
        }
        long current = progressMs;
        if (status == Status.PLAYING) {
            current += Math.max(0L, nowMs - fetchedAtMs);
        }
        return Math.min(1f, Math.max(0f, current / (float) durationMs));
    }

    @Nonnull
    public String formatElapsed(long nowMs) {
        return formatTime((long) (getProgressRatio(nowMs) * durationMs));
    }

    @Nonnull
    public String formatRemaining(long nowMs) {
        if (durationMs <= 0L) {
            return "0:00";
        }
        long elapsed = (long) (getProgressRatio(nowMs) * durationMs);
        return formatTime(Math.max(0L, durationMs - elapsed));
    }

    @Nonnull
    public String formatProgressClock(long nowMs) {
        if (durationMs <= 0L) {
            return formatElapsed(nowMs);
        }
        return formatElapsed(nowMs) + "/" + formatTime(durationMs);
    }

    @Nonnull
    public static SpotifyNowPlayingInfo fetch(@Nonnull SpotifyPlayerComponent state) {
        if (!state.hasCredentials()) {
            return error("Missing Spotify credentials");
        }
        String accessToken = SpotifyAuth.ensureAccessToken(state);
        if (accessToken == null) {
            return error("Could not refresh Spotify token");
        }

        int code = SpotifyHttpClient.getResponseCode(
            "https://api.spotify.com/v1/me/player/currently-playing",
            Map.of("Authorization", "Bearer " + accessToken)
        );

        if (code == 204 || code == 404) {
            return idle();
        }
        if (code == 401) {
            state.clearAccessToken();
            accessToken = SpotifyAuth.ensureAccessToken(state);
            if (accessToken == null) {
                return error("Spotify token expired");
            }
            code = SpotifyHttpClient.getResponseCode(
                "https://api.spotify.com/v1/me/player/currently-playing",
                Map.of("Authorization", "Bearer " + accessToken)
            );
            if (code == 204 || code == 404) {
                return idle();
            }
        }
        if (code != 200) {
            return error("Spotify API HTTP " + code);
        }

        String body = SpotifyHttpClient.getString(
            "https://api.spotify.com/v1/me/player/currently-playing",
            Map.of("Authorization", "Bearer " + accessToken)
        );
        if (body == null || body.isBlank()) {
            return idle();
        }
        return parseBody(body);
    }

    @Nonnull
    private static SpotifyNowPlayingInfo parseBody(@Nonnull String body) {
        try {
            long fetchedAt = System.currentTimeMillis();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            boolean playing = !root.has("is_playing") || root.get("is_playing").getAsBoolean();
            long progressMs = root.has("progress_ms") ? root.get("progress_ms").getAsLong() : 0L;
            JsonObject item = root.has("item") && root.get("item").isJsonObject() ? root.getAsJsonObject("item") : null;
            if (item == null) {
                return idle();
            }
            String track = item.has("name") ? item.get("name").getAsString() : "Unknown track";
            String artist = parseArtists(item);
            long durationMs = item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L;
            String artUrl = parseAlbumArtUrl(item);
            return new SpotifyNowPlayingInfo(
                playing ? Status.PLAYING : Status.PAUSED,
                track,
                artist,
                null,
                artUrl,
                null,
                progressMs,
                durationMs,
                fetchedAt
            );
        } catch (Exception e) {
            return error("Invalid Spotify response");
        }
    }

    @Nullable
    private static String parseAlbumArtUrl(@Nonnull JsonObject item) {
        JsonArray images = null;
        if (item.has("album") && item.get("album").isJsonObject()) {
            JsonObject album = item.getAsJsonObject("album");
            if (album.has("images") && album.get("images").isJsonArray()) {
                images = album.getAsJsonArray("images");
            }
        }
        if (images == null && item.has("images") && item.get("images").isJsonArray()) {
            images = item.getAsJsonArray("images");
        }
        if (images == null || images.isEmpty()) {
            return null;
        }
        String best = null;
        int bestSize = Integer.MAX_VALUE;
        for (JsonElement element : images) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject image = element.getAsJsonObject();
            if (!image.has("url") || image.get("url").isJsonNull()) {
                continue;
            }
            String url = image.get("url").getAsString();
            int height = image.has("height") && !image.get("height").isJsonNull() ? image.get("height").getAsInt() : 640;
            if (height >= 64 && height < bestSize) {
                bestSize = height;
                best = url;
            } else if (best == null) {
                best = url;
            }
        }
        return best;
    }

    @Nonnull
    private static String parseArtists(@Nonnull JsonObject item) {
        if (!item.has("artists") || !item.get("artists").isJsonArray()) {
            return "Unknown artist";
        }
        JsonArray artists = item.getAsJsonArray("artists");
        StringBuilder sb = new StringBuilder();
        for (JsonElement element : artists) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject artist = element.getAsJsonObject();
            if (!artist.has("name")) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(artist.get("name").getAsString());
        }
        return sb.isEmpty() ? "Unknown artist" : sb.toString();
    }

    @Nonnull
    private static String formatTime(long ms) {
        long totalSeconds = Math.max(0L, ms / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
