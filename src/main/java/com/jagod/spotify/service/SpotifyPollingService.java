package com.jagod.spotify.service;

import com.jagod.spotify.windows.WindowsMediaManager;
import com.jagod.spotify.api.SpotifyNowPlayingInfo;
import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.ui.SpotifyHudSupport;
import com.jagod.spotify.service.SpotifyControlsRegistry;
import com.jagod.spotify.ui.SpotifyControlsPage;
import com.jagod.spotify.ui.SpotifyNowPlayingHud;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

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
            if (state.getMusicSource().isWindows()) {
                info = WindowsMediaManager.get().getStatus();
            } else {
                info = SpotifyNowPlayingInfo.fetch(state);
            }
            LAST_INFO.put(playerRef.getUuid(), info);
            state.setNowPlaying(info.getTrackName(), info.getArtistName(), info.getStatus().name());
        } else {
            info = LAST_INFO.get(playerRef.getUuid());
            if (info == null) {
                return;
            }
            if (state.getMusicSource().isWindows()) {
                info = WindowsMediaManager.get().getStatus();
                LAST_INFO.put(playerRef.getUuid(), info);
            }
        }

        SpotifyNowPlayingHud hud = SpotifyHudSupport.obtainHud(player, playerRef);
        hud.refresh(playerRef, state, info);

        SpotifyControlsPage controls = SpotifyControlsRegistry.get(playerRef.getUuid());
        if (controls != null) {
            controls.refreshFromPolling(state);
        }
    }
}
