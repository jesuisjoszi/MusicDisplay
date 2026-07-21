package com.joszza.spotify.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.joszza.spotify.SpotifyConstants;
import com.joszza.spotify.api.SpotifyHttpClient;
import com.joszza.spotify.assets.DynamicCoverSender;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

/**
 * Album covers via HyUI-style dynamic asset slots.
 * HUD always binds the slot path; bytes are pushed only when safe (no Custom UI + soft cooldown).
 */
public final class SpotifyAlbumArtService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int COVER_PIXEL_SIZE = 64;

    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> READY_KEY_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PENDING_KEY_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingCover> WAITING = new ConcurrentHashMap<>();

    private record PendingCover(@Nonnull String key, @Nonnull byte[] png) {}

    private SpotifyAlbumArtService() {}

    @Nonnull
    public static String slotPath() {
        return DynamicCoverSender.slotPath();
    }

    /**
     * Always returns the slot path so AssetImage stays bound. Download starts in background when needed.
     * Client shows pack default / previous cover until rebuild lands.
     */
    @Nonnull
    public static String resolveRemote(@Nonnull PlayerRef playerRef, @Nullable String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return SpotifyConstants.ALBUM_ART_FALLBACK;
        }
        UUID uuid = playerRef.getUuid();
        if (!Objects.equals(READY_KEY_BY_PLAYER.get(uuid), imageUrl)) {
            requestRemote(uuid, imageUrl);
        }
        return DynamicCoverSender.slotPath();
    }

    @Nonnull
    public static String resolveLocalFile(@Nonnull PlayerRef playerRef, @Nullable Path file, @Nonnull String cacheKey) {
        if (file == null || !Files.isRegularFile(file)) {
            return SpotifyConstants.ALBUM_ART_FALLBACK;
        }
        String key = "file:" + cacheKey;
        UUID uuid = playerRef.getUuid();
        if (!Objects.equals(READY_KEY_BY_PLAYER.get(uuid), key)) {
            requestLocal(uuid, file, key);
        }
        return DynamicCoverSender.slotPath();
    }

    /** Push waiting / deferred covers when Custom UI is closed. */
    public static void tryFlush(@Nonnull PlayerRef playerRef, @Nonnull Player player) {
        if (player.getPageManager().getCustomPage() != null) {
            return;
        }
        UUID uuid = playerRef.getUuid();

        PendingCover waiting = WAITING.get(uuid);
        if (waiting != null) {
            if (Objects.equals(PENDING_KEY_BY_PLAYER.get(uuid), waiting.key())) {
                if (push(uuid, playerRef, waiting.key(), waiting.png())) {
                    WAITING.remove(uuid, waiting);
                    return;
                }
            } else {
                WAITING.remove(uuid, waiting);
            }
        }

        if (DynamicCoverSender.hasDeferred(uuid)
            && DynamicCoverSender.flushDeferred(uuid, playerRef.getPacketHandler())) {
            String pending = PENDING_KEY_BY_PLAYER.get(uuid);
            if (pending != null) {
                READY_KEY_BY_PLAYER.put(uuid, pending);
            }
        }
    }

    public static void clearPlayer(@Nonnull UUID playerUuid) {
        READY_KEY_BY_PLAYER.remove(playerUuid);
        PENDING_KEY_BY_PLAYER.remove(playerUuid);
        WAITING.remove(playerUuid);
        DynamicCoverSender.clearPlayer(playerUuid);
    }

    public static void syncFromDataDirectory() {
        DynamicCoverSender.resolveSlotHash();
    }

    private static void requestRemote(@Nonnull UUID uuid, @Nonnull String imageUrl) {
        String flightKey = uuid + "|" + imageUrl;
        if (!IN_FLIGHT.add(flightKey)) {
            return;
        }
        PENDING_KEY_BY_PLAYER.put(uuid, imageUrl);
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                byte[] raw = SpotifyHttpClient.getBytes(imageUrl, Map.of());
                if (raw == null || raw.length == 0) {
                    return;
                }
                byte[] png = toPng(raw);
                if (png == null) {
                    return;
                }
                queueOrDeliver(uuid, imageUrl, png);
            } catch (Exception e) {
                LOGGER.atFine().withCause(e).log("Cover download failed: %s", imageUrl);
            } finally {
                IN_FLIGHT.remove(flightKey);
            }
        });
    }

    private static void requestLocal(@Nonnull UUID uuid, @Nonnull Path file, @Nonnull String key) {
        String flightKey = uuid + "|" + key;
        if (!IN_FLIGHT.add(flightKey)) {
            return;
        }
        PENDING_KEY_BY_PLAYER.put(uuid, key);
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                byte[] raw = Files.readAllBytes(file);
                byte[] png = toPng(raw);
                if (png == null) {
                    return;
                }
                queueOrDeliver(uuid, key, png);
            } catch (Exception e) {
                LOGGER.atFine().withCause(e).log("Local cover load failed");
            } finally {
                IN_FLIGHT.remove(flightKey);
            }
        });
    }

    private static void queueOrDeliver(@Nonnull UUID playerUuid, @Nonnull String key, @Nonnull byte[] png) {
        if (!Objects.equals(PENDING_KEY_BY_PLAYER.get(playerUuid), key)) {
            return;
        }
        PlayerRef online = findOnline(playerUuid);
        if (online == null || !online.isValid()) {
            WAITING.put(playerUuid, new PendingCover(key, png));
            return;
        }
        if (isCustomUiOpen(online)) {
            WAITING.put(playerUuid, new PendingCover(key, png));
            return;
        }
        if (!push(playerUuid, online, key, png)) {
            WAITING.put(playerUuid, new PendingCover(key, png));
        }
    }

    private static boolean push(
        @Nonnull UUID playerUuid,
        @Nonnull PlayerRef playerRef,
        @Nonnull String key,
        @Nonnull byte[] png
    ) {
        if (!Objects.equals(PENDING_KEY_BY_PLAYER.get(playerUuid), key)) {
            return true;
        }
        if (isCustomUiOpen(playerRef)) {
            return false;
        }
        boolean ok = DynamicCoverSender.sendPngToPlayer(playerUuid, playerRef.getPacketHandler(), png);
        if (ok) {
            READY_KEY_BY_PLAYER.put(playerUuid, key);
            WAITING.remove(playerUuid);
        }
        return ok;
    }

    private static boolean isCustomUiOpen(@Nonnull PlayerRef playerRef) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return false;
            }
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            return player != null && player.getPageManager().getCustomPage() != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static byte[] toPng(@Nonnull byte[] raw) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(raw));
            if (source == null) {
                return null;
            }
            BufferedImage square = toFixedSquare(source, COVER_PIXEL_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(square, "png", out)) {
                return null;
            }
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    @Nonnull
    private static BufferedImage toFixedSquare(@Nonnull BufferedImage source, int size) {
        int w = Math.max(1, source.getWidth());
        int h = Math.max(1, source.getHeight());
        int crop = Math.min(w, h);
        int x = (w - crop) / 2;
        int y = (h - crop) / 2;
        BufferedImage cropped = source.getSubimage(x, y, crop, crop);

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            );
            float radius = size * 0.18f;
            java.awt.geom.RoundRectangle2D clip =
                new java.awt.geom.RoundRectangle2D.Float(0, 0, size, size, radius * 2f, radius * 2f);
            g.setClip(clip);
            g.drawImage(cropped, 0, 0, size, size, null);
        } finally {
            g.dispose();
        }
        return out;
    }

    @Nullable
    private static PlayerRef findOnline(@Nonnull UUID uuid) {
        for (PlayerRef player : Universe.get().getPlayers()) {
            if (uuid.equals(player.getUuid())) {
                return player;
            }
        }
        return null;
    }
}
