package com.jagod.spotify.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum MusicSource {
    SPOTIFY,
    WINDOWS;

    @Nonnull
    public static MusicSource fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return SPOTIFY;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SPOTIFY;
        }
    }

    public boolean isWindows() {
        return this == WINDOWS;
    }
}
