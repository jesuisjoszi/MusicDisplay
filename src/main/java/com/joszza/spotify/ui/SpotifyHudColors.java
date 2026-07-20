package com.joszza.spotify.ui;

import com.joszza.spotify.data.SpotifyPlayerComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyHudColors {
    public static final String DEFAULT_TRACK = "#e8eaed";
    public static final String DEFAULT_ARTIST = "#9aa0a6";
    public static final String DEFAULT_TIME = "#6b7280";

    private SpotifyHudColors() {}

    @Nonnull
    public static String track(@Nonnull SpotifyPlayerComponent state) {
        return resolve(state.getHudTrackColor(), DEFAULT_TRACK);
    }

    @Nonnull
    public static String artist(@Nonnull SpotifyPlayerComponent state) {
        return resolve(state.getHudArtistColor(), DEFAULT_ARTIST);
    }

    @Nonnull
    public static String time(@Nonnull SpotifyPlayerComponent state) {
        return resolve(state.getHudTimeColor(), DEFAULT_TIME);
    }

    @Nonnull
    public static String resolve(@Nullable String stored, @Nonnull String fallback) {
        return stored != null ? stored : fallback;
    }

    @Nullable
    public static String parseOrNull(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return normalize(raw.trim());
    }

    @Nullable
    public static String normalize(@Nonnull String raw) {
        String value = raw.startsWith("#") ? raw.substring(1) : raw;
        if (value.length() == 3) {
            value = ""
                + value.charAt(0)
                + value.charAt(0)
                + value.charAt(1)
                + value.charAt(1)
                + value.charAt(2)
                + value.charAt(2);
        }
        if (!value.matches("(?i)[0-9a-f]{6}")) {
            return null;
        }
        return "#" + value.toLowerCase();
    }
}
