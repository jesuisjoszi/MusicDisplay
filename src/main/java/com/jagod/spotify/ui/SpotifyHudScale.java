package com.jagod.spotify.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum SpotifyHudScale {
    SMALL(360, 48, 11, 9, 8, 4),
    MEDIUM(440, 64, 14, 11, 9, 5),
    LARGE(520, 80, 17, 13, 10, 6);

    private final int panelWidth;
    private final int coverSize;
    private final int trackFontSize;
    private final int artistFontSize;
    private final int timeFontSize;
    private final int progressHeight;

    SpotifyHudScale(
        int panelWidth,
        int coverSize,
        int trackFontSize,
        int artistFontSize,
        int timeFontSize,
        int progressHeight
    ) {
        this.panelWidth = panelWidth;
        this.coverSize = coverSize;
        this.trackFontSize = trackFontSize;
        this.artistFontSize = artistFontSize;
        this.timeFontSize = timeFontSize;
        this.progressHeight = progressHeight;
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getCoverSize() {
        return coverSize;
    }

    public int getTrackFontSize() {
        return trackFontSize;
    }

    public int getArtistFontSize() {
        return artistFontSize;
    }

    public int getTimeFontSize() {
        return timeFontSize;
    }

    public int getProgressHeight() {
        return progressHeight;
    }

    @Nonnull
    public static SpotifyHudScale fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
