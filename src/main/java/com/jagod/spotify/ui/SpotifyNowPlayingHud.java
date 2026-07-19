package com.jagod.spotify.ui;

import com.jagod.spotify.SpotifyConstants;
import com.jagod.spotify.api.SpotifyNowPlayingInfo;
import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.service.SpotifyAlbumArtService;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyNowPlayingHud extends CustomUIHud {
    public SpotifyNowPlayingHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, com.jagod.spotify.SpotifyConstants.HUD_KEY, 0);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.append("Spotify/SpotifyNowPlayingHud.ui");
        commandBuilder.set("#AlbumArt.FallbackTexturePath", SpotifyConstants.ALBUM_ART_FALLBACK);
    }

    public void refresh(
        @Nonnull PlayerRef playerRef,
        @Nonnull SpotifyPlayerComponent state,
        @Nonnull SpotifyNowPlayingInfo info
    ) {
        UICommandBuilder builder = new UICommandBuilder();
        long now = System.currentTimeMillis();
        SpotifyHudLayout.applyAppearance(builder, state);

        if (!state.getMusicSource().isWindows() && !state.hasCredentials()) {
            applyIdle(builder, Message.translation("spotify.spotify.hud.notConfigured"));
            clearCover(builder);
            update(false, builder);
            return;
        }

        if (state.getMusicSource().isWindows()
            && info.getStatus() == SpotifyNowPlayingInfo.Status.ERROR) {
            applyIdle(builder, Message.translation("spotify.spotify.hud.windowsUnavailable"));
            clearCover(builder);
            update(false, builder);
            return;
        }

        switch (info.getStatus()) {
            case PLAYING, PAUSED -> {
                applyTrack(
                    builder,
                    safe(info.getTrackName(), "Unknown"),
                    safe(info.getArtistName(), "Unknown"),
                    info,
                    now,
                    state.isHudProgressVisible()
                );
                applyCover(builder, playerRef, info);
            }
            case IDLE -> {
                applyIdle(builder, Message.translation("spotify.spotify.hud.idle"));
                clearCover(builder);
            }
            case ERROR -> {
                builder.set("#TrackLine.TextSpans", Message.translation("spotify.spotify.hud.error"));
                builder.set("#ArtistLine.TextSpans", Message.raw(safe(info.getErrorMessage(), "")));
                builder.set("#TimeBadge.TextSpans", Message.raw(""));
                builder.set("#TimeBadge.Visible", false);
                builder.set("#ProgressSection.Visible", false);
                clearCover(builder);
            }
        }
        update(false, builder);
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
        @Nonnull String track,
        @Nonnull String artist,
        @Nonnull SpotifyNowPlayingInfo info,
        long nowMs,
        boolean showProgress
    ) {
        builder.set("#TrackLine.TextSpans", Message.raw(track));
        builder.set("#ArtistLine.TextSpans", Message.raw(artist));
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
