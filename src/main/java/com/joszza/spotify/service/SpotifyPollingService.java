package com.joszza.spotify.service;

import com.joszza.spotify.windows.WindowsMediaManager;
import com.joszza.spotify.api.SpotifyNowPlayingInfo;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.joszza.spotify.ui.SpotifyHudSupport;
import com.joszza.spotify.service.SpotifyControlsRegistry;
import com.joszza.spotify.ui.SpotifyControlsPage;
import com.joszza.spotify.ui.SpotifyNowPlayingHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyPollingService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long API_POLL_INTERVAL_SECONDS = 3L;
    private static final long PROGRESS_TICK_SECONDS = 1L;
    private static final Map<UUID, SpotifyNowPlayingInfo> LAST_INFO = new ConcurrentHashMap<>();
    private static volatile boolean started;

    private SpotifyPollingService() {}

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            SpotifyPollingService::pollAllPlayers,
            API_POLL_INTERVAL_SECONDS,
            API_POLL_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            SpotifyPollingService::refreshProgressTicks,
            PROGRESS_TICK_SECONDS,
            PROGRESS_TICK_SECONDS,
            TimeUnit.SECONDS
        );
        LOGGER.atInfo().log("Spotify polling started (API every %ss, progress every %ss)", API_POLL_INTERVAL_SECONDS, PROGRESS_TICK_SECONDS);
    }

    public static void refreshPlayerNow(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> updatePlayer(ref, store, true));
    }

    public static void clearPlayer(@Nonnull UUID playerUuid) {
        LAST_INFO.remove(playerUuid);
        SpotifyAlbumArtService.clearPlayer(playerUuid);
    }

    private static void pollAllPlayers() {
        try {
            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                world.execute(() -> updatePlayer(ref, store, true));
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Spotify API polling tick failed");
        }
    }

    private static void refreshProgressTicks() {
        try {
            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) {
                    continue;
                }
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                world.execute(() -> updatePlayer(ref, store, false));
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Spotify progress tick failed");
        }
    }

    private static void updatePlayer(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, boolean fetchApi) {
        if (!ref.isValid()) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }

        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        if (!state.canShowHud()) {
            LAST_INFO.remove(playerRef.getUuid());
            if (SpotifyHudSupport.isHudActive(player)) {
                SpotifyHudSupport.removeHud(player, playerRef);
            }
            return;
        }

        SpotifyNowPlayingInfo info;
        if (fetchApi) {
            String previousTrack = state.getLastTrack();
            String previousArtist = state.getLastArtist();
            if (state.getMusicSource().isWindows()) {
                info = WindowsMediaManager.get().getStatus();
                state.setLastVolumePercent(WindowsMediaManager.get().getVolumePercent());
            } else {
                info = SpotifyNowPlayingInfo.fetch(state);
            }
            LAST_INFO.put(playerRef.getUuid(), info);
            state.setNowPlaying(info.getTrackName(), info.getArtistName(), info.getStatus().name());
            maybeNotifyTrackChange(playerRef, state, previousTrack, previousArtist, info);
        } else {
            info = LAST_INFO.get(playerRef.getUuid());
            if (info == null) {
                return;
            }
            if (state.getMusicSource().isWindows()) {
                info = WindowsMediaManager.get().getStatus();
                LAST_INFO.put(playerRef.getUuid(), info);
                state.setLastVolumePercent(WindowsMediaManager.get().getVolumePercent());
                state.setNowPlaying(info.getTrackName(), info.getArtistName(), info.getStatus().name());
            }
        }

        // Custom pages and HUD share one CustomUI channel. Never send page selectors at the HUD
        // (or HUD selectors while a page is open) — that disconnects the client.
        var openPage = player.getPageManager().getCustomPage();
        if (openPage != null) {
            SpotifyControlsPage controls = SpotifyControlsRegistry.get(playerRef.getUuid());
            if (controls != null && openPage == controls) {
                controls.refreshFromPolling(state);
            }
            return;
        }

        // Safe window: flush any cover that was waiting on UI / rebuild cooldown.
        if (state.isHudAlbumArtVisible()) {
            SpotifyAlbumArtService.tryFlush(playerRef, player);
        }

        SpotifyNowPlayingHud hud = SpotifyHudSupport.obtainHud(player, playerRef);
        if (fetchApi) {
            hud.refresh(playerRef, state, info);
        } else {
            hud.refreshProgress(state, info);
        }
    }

    private static void maybeNotifyTrackChange(
        @Nonnull PlayerRef playerRef,
        @Nonnull SpotifyPlayerComponent state,
        @Nullable String previousTrack,
        @Nullable String previousArtist,
        @Nonnull SpotifyNowPlayingInfo info
    ) {
        if (!state.isTrackChangeNotify()) {
            return;
        }
        String track = info.getTrackName();
        if (track == null || track.isBlank()) {
            return;
        }
        if (info.getStatus() != SpotifyNowPlayingInfo.Status.PLAYING
            && info.getStatus() != SpotifyNowPlayingInfo.Status.PAUSED) {
            return;
        }
        String artist = info.getArtistName();
        if (Objects.equals(previousTrack, track) && Objects.equals(previousArtist, artist)) {
            return;
        }
        if (previousTrack == null || previousTrack.isBlank()) {
            return;
        }
        Message title = Message.raw(track);
        Message body = artist == null || artist.isBlank()
            ? Message.translation("spotify.spotify.hud.playing")
            : Message.translation("spotify.spotify.hud.artist").param("artist", artist);
        NotificationUtil.sendNotification(playerRef.getPacketHandler(), title, body, NotificationStyle.Default);
    }
}
