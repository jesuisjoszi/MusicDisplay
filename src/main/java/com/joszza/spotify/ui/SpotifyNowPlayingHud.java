package com.joszza.spotify.ui;

import com.joszza.spotify.SpotifyConstants;
import com.joszza.spotify.api.SpotifyNowPlayingInfo;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.joszza.spotify.service.SpotifyAlbumArtService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyNowPlayingHud extends CustomUIHud {
    private String lastAppearanceKey = "";
    private String lastContentKey = "";

    public SpotifyNowPlayingHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, com.joszza.spotify.SpotifyConstants.HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Spotify/SpotifyNowPlayingHud.ui");
        commandBuilder.set("#AlbumArt.FallbackTexturePath", SpotifyConstants.ALBUM_ART_FALLBACK);
        lastAppearanceKey = "";
        lastContentKey = "";
    }

    /** Full refresh (API poll / settings). Appearance only when layout options change. */
    public void refresh(
        @Nonnull PlayerRef playerRef,
        @Nonnull SpotifyPlayerComponent state,
        @Nonnull SpotifyNowPlayingInfo info
    ) {
        UICommandBuilder builder = new UICommandBuilder();
        long now = System.currentTimeMillis();
        boolean dirty = maybeApplyAppearance(builder, state);

        if (!state.getMusicSource().isWindows() && !state.hasCredentials()) {
            applyIdle(builder, Message.translation("spotify.spotify.hud.notConfigured"));
            clearCover(builder);
            lastContentKey = "idle:auth";
            update(false, builder);
            return;
        }

        if (state.getMusicSource().isWindows()
            && info.getStatus() == SpotifyNowPlayingInfo.Status.ERROR) {
            applyIdle(builder, Message.translation("spotify.spotify.hud.windowsUnavailable"));
            clearCover(builder);
            lastContentKey = "idle:win";
            update(false, builder);
            return;
        }

        switch (info.getStatus()) {
            case PLAYING, PAUSED -> {
                String contentKey = contentKey(info, state.isHudProgressVisible());
                if (!contentKey.equals(lastContentKey)) {
                    applyTrack(
                        builder,
                        state,
                        safe(info.getTrackName(), "Unknown"),
                        safe(info.getArtistName(), "Unknown"),
                        info,
                        now,
                        state.isHudProgressVisible()
                    );
                    applyCover(builder, playerRef, info);
                    lastContentKey = contentKey;
                    dirty = true;
                } else {
                    applyProgressOnly(builder, info, now, state.isHudProgressVisible());
                    dirty = true;
                }
            }
            case IDLE -> {
                if (!"idle".equals(lastContentKey)) {
                    applyIdle(builder, Message.translation("spotify.spotify.hud.idle"));
                    clearCover(builder);
                    lastContentKey = "idle";
                    dirty = true;
                }
            }
            case ERROR -> {
                builder.set("#TrackLine.TextSpans", Message.translation("spotify.spotify.hud.error"));
                builder.set("#ArtistLine.TextSpans", Message.raw(safe(info.getErrorMessage(), "")));
                builder.set("#TimeBadge.TextSpans", Message.raw(""));
                builder.set("#TimeBadge.Visible", false);
                builder.set("#ProgressSection.Visible", false);
                clearCover(builder);
                lastContentKey = "error";
                dirty = true;
            }
        }
        if (dirty) {
            update(false, builder);
        }
    }

    /** Lightweight 1s tick — only progress bar / clock, no layout or cover. */
    public void refreshProgress(
        @Nonnull SpotifyPlayerComponent state,
        @Nonnull SpotifyNowPlayingInfo info
    ) {
        if (info.getStatus() != SpotifyNowPlayingInfo.Status.PLAYING
            && info.getStatus() != SpotifyNowPlayingInfo.Status.PAUSED) {
            return;
        }
        if (!state.isHudProgressVisible() || info.getDurationMs() <= 0L) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        long now = System.currentTimeMillis();
        builder.set("#TrackProgressBar.Value", info.getProgressRatio(now));
        builder.set("#TimeBadge.TextSpans", Message.raw(info.formatProgressClock(now)));
        update(false, builder);
    }

    public void forceAppearance(@Nonnull SpotifyPlayerComponent state) {
        lastAppearanceKey = "";
        UICommandBuilder builder = new UICommandBuilder();
        maybeApplyAppearance(builder, state);
        update(false, builder);
    }

    private boolean maybeApplyAppearance(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        String key = appearanceKey(state);
        if (key.equals(lastAppearanceKey)) {
            return false;
        }
        SpotifyHudLayout.applyAppearance(builder, state);
        lastAppearanceKey = key;
        return true;
    }

    @Nonnull
    private static String appearanceKey(@Nonnull SpotifyPlayerComponent state) {
        return state.getHudScale().name()
            + "|" + state.getHudPosition().name()
            + "|" + state.getHudOffsetX()
            + "|" + state.getHudOffsetY()
            + "|" + state.isHudProgressVisible()
            + "|" + Objects.toString(state.getHudTrackColor(), "")
            + "|" + Objects.toString(state.getHudArtistColor(), "")
            + "|" + Objects.toString(state.getHudTimeColor(), "");
    }

    @Nonnull
    private static String contentKey(@Nonnull SpotifyNowPlayingInfo info, boolean progressVisible) {
        return safe(info.getTrackName(), "")
            + "|" + safe(info.getArtistName(), "")
            + "|" + info.getStatus().name()
            + "|" + Objects.toString(info.getAlbumArtUrl(), "")
            + "|" + Objects.toString(info.getLocalArtPath(), "")
            + "|" + progressVisible
            + "|" + info.getDurationMs();
    }

    private static void applyCover(
        @Nonnull UICommandBuilder builder,
        @Nonnull PlayerRef playerRef,
        @Nonnull SpotifyNowPlayingInfo info
    ) {
        String assetPath = null;
        if (info.getAlbumArtUrl() != null) {
            assetPath = SpotifyAlbumArtService.resolveRemote(playerRef, info.getAlbumArtUrl());
        } else if (info.getLocalArtPath() != null) {
            String cacheKey = safe(info.getTrackName(), "track") + "|" + safe(info.getArtistName(), "artist");
            assetPath = SpotifyAlbumArtService.resolveLocalFile(playerRef, info.getLocalArtPath(), cacheKey);
        }
        builder.set("#AlbumArt.Visible", true);
        builder.set(
            "#AlbumArt.AssetPath",
            assetPath != null ? assetPath : SpotifyAlbumArtService.placeholderPath()
        );
        builder.set("#AlbumArt.FallbackTexturePath", SpotifyConstants.ALBUM_ART_FALLBACK);
    }

    private static void clearCover(@Nonnull UICommandBuilder builder) {
        builder.set("#AlbumArt.Visible", true);
        builder.set("#AlbumArt.AssetPath", SpotifyConstants.ALBUM_ART_PLACEHOLDER);
        builder.set("#AlbumArt.FallbackTexturePath", SpotifyConstants.ALBUM_ART_FALLBACK);
    }

    private static void applyTrack(
        @Nonnull UICommandBuilder builder,
        @Nonnull SpotifyPlayerComponent state,
        @Nonnull String track,
        @Nonnull String artist,
        @Nonnull SpotifyNowPlayingInfo info,
        long nowMs,
        boolean showProgress
    ) {
        SpotifyHudScale scale = state.getHudScale();
        builder.set("#TrackLine.TextSpans", Message.raw(ellipsis(track, scale.getMaxTrackChars())));
        builder.set("#ArtistLine.TextSpans", Message.raw(ellipsis(artist, scale.getMaxArtistChars())));
        applyProgressOnly(builder, info, nowMs, showProgress);
    }

    @Nonnull
    private static String ellipsis(@Nonnull String text, int maxChars) {
        if (maxChars < 4 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars - 1) + "…";
    }

    private static void applyProgressOnly(
        @Nonnull UICommandBuilder builder,
        @Nonnull SpotifyNowPlayingInfo info,
        long nowMs,
        boolean showProgress
    ) {
        boolean progressVisible = showProgress && info.getDurationMs() > 0L;
        builder.set("#ProgressSection.Visible", progressVisible);
        if (progressVisible) {
            builder.set("#TrackProgressBar.Value", info.getProgressRatio(nowMs));
            builder.set("#TimeBadge.TextSpans", Message.raw(info.formatProgressClock(nowMs)));
            builder.set("#TimeBadge.Visible", true);
        } else {
            builder.set("#TimeBadge.Visible", false);
        }
    }

    private static void applyIdle(@Nonnull UICommandBuilder builder, @Nonnull Message message) {
        builder.set("#TrackLine.TextSpans", message);
        builder.set("#ArtistLine.TextSpans", Message.raw(""));
        builder.set("#TimeBadge.TextSpans", Message.raw(""));
        builder.set("#TimeBadge.Visible", false);
        builder.set("#ProgressSection.Visible", false);
    }

    @Nonnull
    private static String safe(@Nullable String value, @Nonnull String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
