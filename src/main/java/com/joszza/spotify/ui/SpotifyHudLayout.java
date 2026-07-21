package com.joszza.spotify.ui;

import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;

/**
 * Keeps HUD geometry paired across S/M/L: cover, titles and full-width progress stay inside the panel.
 */
public final class SpotifyHudLayout {
    public static final int DEFAULT_OFFSET = 24;

    private SpotifyHudLayout() {}

    public static void applyRootAnchor(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        builder.setObject("#SpotifyHudRoot.Anchor", buildRootAnchor(state));
    }

    public static void applyAppearance(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        applyRootAnchor(builder, state);
        SpotifyHudScale scale = state.getHudScale();
        boolean albumOn = state.isHudAlbumArtVisible();
        int rowHeight = albumOn ? scale.getCoverSize() : scale.getTextOnlyRowHeight();
        int gap = scale.getCoverGap();

        builder.setObject("#MainRow.Anchor", heightBottom(rowHeight, scale.getMainRowBottomGap()));
        builder.set("#AlbumArtBox.Visible", albumOn);
        if (albumOn) {
            builder.setObject("#AlbumArtBox.Anchor", box(scale.getCoverSize(), scale.getCoverSize(), gap));
            builder.setObject("#AlbumArt.Anchor", size(scale.getCoverSize(), scale.getCoverSize()));
        }
        builder.setObject("#TextColumn.Anchor", heightOnly(rowHeight));
        builder.setObject("#TrackLine.Anchor", heightBottom(scale.getTrackRowHeight(), 2));
        builder.setObject("#ArtistLine.Anchor", heightOnly(scale.getArtistRowHeight()));
        builder.setObject("#ProgressSection.Anchor", heightOnly(scale.getProgressSectionHeight()));
        builder.setObject("#TimeBadge.Anchor", heightBottom(Math.max(10, scale.getTimeFontSize() + 2), 2));
        builder.setObject("#TrackProgressBar.Anchor", progressAnchor(scale.getProgressHeight()));

        builder.set("#TrackLine.Style.FontSize", scale.getTrackFontSize());
        builder.set("#ArtistLine.Style.FontSize", scale.getArtistFontSize());
        builder.set("#TimeBadge.Style.FontSize", scale.getTimeFontSize());
        builder.set("#TrackLine.Style.TextColor", SpotifyHudColors.track(state));
        builder.set("#ArtistLine.Style.TextColor", SpotifyHudColors.artist(state));
        builder.set("#TimeBadge.Style.TextColor", SpotifyHudColors.time(state));
        builder.set("#TrackLine.Style.RenderBold", true);
        builder.set("#ArtistLine.Style.RenderBold", false);
        builder.set("#TimeBadge.Style.RenderBold", false);
        builder.set("#TrackLine.Style.Wrap", false);
        builder.set("#ArtistLine.Style.Wrap", false);
        builder.set("#ProgressSection.Visible", state.isHudProgressVisible());
    }

    @Nonnull
    private static Anchor box(int width, int height, int right) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        anchor.setRight(Value.of(right));
        return anchor;
    }

    @Nonnull
    private static Anchor size(int width, int height) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(width));
        anchor.setHeight(Value.of(height));
        return anchor;
    }

    @Nonnull
    private static Anchor heightOnly(int height) {
        Anchor anchor = new Anchor();
        anchor.setHeight(Value.of(height));
        return anchor;
    }

    @Nonnull
    private static Anchor heightBottom(int height, int bottom) {
        Anchor anchor = new Anchor();
        anchor.setHeight(Value.of(height));
        anchor.setBottom(Value.of(bottom));
        return anchor;
    }

    @Nonnull
    private static Anchor progressAnchor(int height) {
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(0));
        anchor.setRight(Value.of(0));
        anchor.setHeight(Value.of(height));
        return anchor;
    }

    @Nonnull
    public static Anchor buildRootAnchor(@Nonnull SpotifyPlayerComponent state) {
        SpotifyHudPosition position = state.getHudPosition();
        int offsetX = clampOffset(state.getHudOffsetX());
        int offsetY = clampOffset(state.getHudOffsetY());

        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(state.getHudScale().getPanelWidth()));
        switch (position) {
            case BOTTOM_RIGHT -> {
                anchor.setBottom(Value.of(offsetY));
                anchor.setRight(Value.of(offsetX));
            }
            case TOP_LEFT -> {
                anchor.setTop(Value.of(offsetY));
                anchor.setLeft(Value.of(offsetX));
            }
            case TOP_RIGHT -> {
                anchor.setTop(Value.of(offsetY));
                anchor.setRight(Value.of(offsetX));
            }
            case BOTTOM_LEFT -> {
                anchor.setBottom(Value.of(offsetY));
                anchor.setLeft(Value.of(offsetX));
            }
        }
        return anchor;
    }

    private static int clampOffset(int value) {
        return Math.max(8, Math.min(200, value));
    }
}
