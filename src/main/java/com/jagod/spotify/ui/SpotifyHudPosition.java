package com.jagod.spotify.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum SpotifyHudPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT;

    @Nonnull
    public static SpotifyHudPosition fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return BOTTOM_LEFT;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return BOTTOM_LEFT;
        }
    }
}
