package com.joszza.spotify.service;

import com.joszza.spotify.ui.SpotifyControlsPage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyControlsRegistry {
    private static final Map<UUID, SpotifyControlsPage> OPEN = new ConcurrentHashMap<>();

    private SpotifyControlsRegistry() {}

    public static void register(@Nonnull UUID playerUuid, @Nonnull SpotifyControlsPage page) {
        OPEN.put(playerUuid, page);
    }

    public static void unregister(@Nonnull UUID playerUuid) {
        OPEN.remove(playerUuid);
    }

    @Nullable
    public static SpotifyControlsPage get(@Nonnull UUID playerUuid) {
        return OPEN.get(playerUuid);
    }
}
