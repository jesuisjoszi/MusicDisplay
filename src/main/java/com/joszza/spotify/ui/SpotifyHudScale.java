package com.joszza.spotify.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Paired HUD sizes. Main row (cover + titles) and progress block scale together for S/M/L.
 */
public enum SpotifyHudScale {
    // panelW, cover, gap, trackFont, artistFont, timeFont, barH, trackRow, artistRow, progressBlock, maxTrack, maxArtist
    SMALL(240, 40, 8, 11, 9, 8, 3, 18, 14, 16, 20, 24),
    MEDIUM(300, 48, 10, 14, 11, 9, 4, 22, 18, 18, 28, 32),
    LARGE(360, 60, 12, 16, 12, 10, 5, 28, 22, 22, 36, 40);

    private final int panelWidth;
    private final int coverSize;
    private final int coverGap;
    private final int trackFontSize;
    private final int artistFontSize;
    private final int timeFontSize;
    private final int progressHeight;
    private final int trackRowHeight;
    private final int artistRowHeight;
    private final int progressSectionHeight;
    private final int maxTrackChars;
    private final int maxArtistChars;

    SpotifyHudScale(
        int panelWidth,
        int coverSize,
        int coverGap,
        int trackFontSize,
        int artistFontSize,
        int timeFontSize,
        int progressHeight,
        int trackRowHeight,
        int artistRowHeight,
        int progressSectionHeight,
        int maxTrackChars,
        int maxArtistChars
    ) {
        this.panelWidth = panelWidth;
        this.coverSize = coverSize;
        this.coverGap = coverGap;
        this.trackFontSize = trackFontSize;
        this.artistFontSize = artistFontSize;
        this.timeFontSize = timeFontSize;
        this.progressHeight = progressHeight;
        this.trackRowHeight = trackRowHeight;
        this.artistRowHeight = artistRowHeight;
        this.progressSectionHeight = progressSectionHeight;
        this.maxTrackChars = maxTrackChars;
        this.maxArtistChars = maxArtistChars;
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getCoverSize() {
        return coverSize;
    }

    public int getCoverGap() {
        return coverGap;
    }

    /** Space between main row and progress block. */
    public int getMainRowBottomGap() {
        return Math.max(4, coverGap / 2);
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

    public int getTrackRowHeight() {
        return trackRowHeight;
    }

    public int getArtistRowHeight() {
        return artistRowHeight;
    }

    public int getProgressSectionHeight() {
        return progressSectionHeight;
    }

    public int getMaxTrackChars() {
        return maxTrackChars;
    }

    public int getMaxArtistChars() {
        return maxArtistChars;
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
