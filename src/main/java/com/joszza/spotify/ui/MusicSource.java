package com.joszza.spotify.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum MusicSource {
    SPOTIFY;

    @Nonnull
    public static MusicSource fromString(@Nullable String raw) {
        // Legacy "WINDOWS" values from older installs map to Spotify (Windows bridge removed).
        return SPOTIFY;
    }
}
