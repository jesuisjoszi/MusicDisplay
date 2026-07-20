package com.joszza.spotify.service;

import com.joszza.spotify.api.SpotifyProfile;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.joszza.spotify.windows.WindowsMediaManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyPlaybackSupport {
    public enum Action {
        NEXT,
        PREVIOUS,
        PLAY,
        PAUSE,
        TOGGLE
    }

    @Nonnull
    public static Result setVolume(@Nonnull SpotifyPlayerComponent state, int volumePercent) {
        int clamped = Math.max(0, Math.min(100, volumePercent));
        state.setLastVolumePercent(clamped);
        if (state.getMusicSource().isWindows()) {
            WindowsMediaManager media = WindowsMediaManager.get();
            if (!WindowsMediaManager.isOsWindows() || !media.isReady()) {
                return Result.WINDOWS_UNAVAILABLE;
            }
            media.setVolume(clamped);
            return Result.OK;
        }
        if (!state.hasCredentials()) {
            return Result.NOT_CONNECTED;
        }
        if (!state.hasPlaybackScope()) {
            return Result.MISSING_SCOPE;
        }
        if (!SpotifyProfile.isPremium(state)) {
            return Result.PREMIUM_REQUIRED;
        }
        return mapCode(SpotifyPlaybackService.setVolume(state, clamped), state);
    }

    public static int readVolumePercent(@Nonnull SpotifyPlayerComponent state) {
        if (state.getMusicSource().isWindows()) {
            return WindowsMediaManager.get().getVolumePercent();
        }
        int fromApi = SpotifyPlaybackService.getVolumePercent(state);
        if (fromApi >= 0) {
            state.setLastVolumePercent(fromApi);
            return fromApi;
        }
        return state.getLastVolumePercent();
    }

    public enum Result {
        OK,
        NOT_CONNECTED,
        NO_DEVICE,
        FORBIDDEN,
        MISSING_SCOPE,
        PREMIUM_REQUIRED,
        WINDOWS_UNAVAILABLE,
        API_ERROR
    }

    private SpotifyPlaybackSupport() {}

    @Nonnull
    public static Result run(@Nonnull SpotifyPlayerComponent state, @Nonnull Action action) {
        if (state.getMusicSource().isWindows()) {
            return runWindows(action);
        }
        Result gate = gateSpotify(state);
        if (gate != null) {
            return gate;
        }

        int code = switch (action) {
            case NEXT -> SpotifyPlaybackService.next(state);
            case PREVIOUS -> SpotifyPlaybackService.previous(state);
            case PLAY -> SpotifyPlaybackService.play(state);
            case PAUSE -> SpotifyPlaybackService.pause(state);
            case TOGGLE -> toggle(state);
        };

        return mapCode(code, state);
    }

    @Nullable
    private static Result gateSpotify(@Nonnull SpotifyPlayerComponent state) {
        if (!state.hasCredentials()) {
            return Result.NOT_CONNECTED;
        }
        if (!state.hasPlaybackScope()) {
            return Result.MISSING_SCOPE;
        }
        if (!SpotifyProfile.isPremium(state)) {
            return Result.PREMIUM_REQUIRED;
        }
        return null;
    }

    @Nonnull
    private static Result runWindows(@Nonnull Action action) {
        WindowsMediaManager media = WindowsMediaManager.get();
        if (!WindowsMediaManager.isOsWindows() || !media.isReady()) {
            return Result.WINDOWS_UNAVAILABLE;
        }
        switch (action) {
            case NEXT -> media.next();
            case PREVIOUS -> media.previous();
            case PLAY, PAUSE, TOGGLE -> media.playPause();
        }
        return Result.OK;
    }

    public static void apply(
        @Nonnull PlayerRef playerRef,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SpotifyPlayerComponent state,
        @Nonnull Result result
    ) {
        switch (result) {
            case OK -> SpotifyPollingService.refreshPlayerNow(ref, store);
            case NOT_CONNECTED -> playerRef.sendMessage(Message.translation("spotify.spotify.command.noAuth"));
            case NO_DEVICE -> playerRef.sendMessage(Message.translation("spotify.spotify.command.noDevice"));
            case FORBIDDEN -> playerRef.sendMessage(Message.translation("spotify.spotify.command.forbidden"));
            case MISSING_SCOPE -> playerRef.sendMessage(Message.translation("spotify.spotify.command.missingScope"));
            case PREMIUM_REQUIRED -> playerRef.sendMessage(Message.translation("spotify.spotify.command.premiumRequired"));
            case WINDOWS_UNAVAILABLE -> playerRef.sendMessage(Message.translation("spotify.spotify.command.windowsUnavailable"));
            case API_ERROR -> playerRef.sendMessage(Message.translation("spotify.spotify.command.apiError"));
        }
    }

    private static int toggle(@Nonnull SpotifyPlayerComponent state) {
        String status = state.getLastStatus();
        if ("PLAYING".equalsIgnoreCase(status)) {
            return SpotifyPlaybackService.pause(state);
        }
        return SpotifyPlaybackService.play(state);
    }

    @Nonnull
    private static Result mapCode(int code, @Nullable SpotifyPlayerComponent state) {
        if (code == 204 || code == 200) {
            return Result.OK;
        }
        if (code == 404) {
            return Result.NO_DEVICE;
        }
        if (code == 403) {
            if (state != null && !SpotifyProfile.isPremium(state)) {
                return Result.PREMIUM_REQUIRED;
            }
            if (state != null && !state.hasPlaybackScope()) {
                return Result.MISSING_SCOPE;
            }
            return Result.FORBIDDEN;
        }
        if (code == 401 || code == -1) {
            return Result.NOT_CONNECTED;
        }
        return Result.API_ERROR;
    }
}
