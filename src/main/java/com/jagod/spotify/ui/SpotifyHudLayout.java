package com.jagod.spotify.ui;

import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import javax.annotation.Nonnull;

public final class SpotifyHudLayout {
    public static final int DEFAULT_OFFSET = 24;
    public static final int PANEL_WIDTH = 440;

    private SpotifyHudLayout() {}

    public static void applyRootAnchor(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        builder.setObject("#SpotifyHudRoot.Anchor", buildRootAnchor(state));
    }

    public static void applyAppearance(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        applyRootAnchor(builder, state);
        SpotifyHudScale scale = state.getHudScale();
        builder.setObject("#AlbumArtBox.Anchor", buildCoverBoxAnchor(scale.getCoverSize()));
        builder.setObject("#AlbumArt.Anchor", buildCoverImageAnchor(scale.getCoverSize()));
        builder.set("#TrackLine.Style.FontSize", scale.getTrackFontSize());
        builder.set("#ArtistLine.Style.FontSize", scale.getArtistFontSize());
        builder.set("#TrackLine.Style.TextColor", SpotifyHudColors.track(state));
        builder.set("#ArtistLine.Style.TextColor", SpotifyHudColors.artist(state));
        builder.set("#TrackLine.Style.RenderBold", true);
        builder.set("#ArtistLine.Style.RenderBold", false);
        builder.set("#TimeBadge.Style.FontSize", scale.getTimeFontSize());
        builder.set("#TimeBadge.Style.TextColor", SpotifyHudColors.time(state));
        builder.set("#TimeBadge.Style.RenderBold", false);
        builder.setObject("#TrackProgressBar.Anchor", buildProgressAnchor(scale.getProgressHeight()));
        builder.set("#ProgressSection.Visible", state.isHudProgressVisible());
    }

    @Nonnull
    private static Anchor buildCoverBoxAnchor(int size) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(size));
        anchor.setHeight(Value.of(size));
        anchor.setRight(Value.of(12));
        return anchor;
    }

    @Nonnull
    private static Anchor buildCoverImageAnchor(int size) {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(size));
        anchor.setHeight(Value.of(size));
        return anchor;
    }

    @Nonnull
    private static Anchor buildProgressAnchor(int height) {
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
