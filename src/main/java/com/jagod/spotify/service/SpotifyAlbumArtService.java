package com.jagod.spotify.service;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.jagod.spotify.SpotifyConstants;
import com.jagod.spotify.api.SpotifyHttpClient;
import com.jagod.spotify.assets.DynamicCoverSender;
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
 * Album covers via HyUI-style dynamic asset slots (not Icons/ItemsGenerated).
 * Downloads PNG/JPEG, converts to PNG, pushes into {@link SpotifyConstants#ALBUM_ART_SLOT_PATH}.
 */
public final class SpotifyAlbumArtService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, String> READY_KEY_BY_PLAYER = new ConcurrentHashMap<>();
    private static final Map<UUID, String> PENDING_KEY_BY_PLAYER = new ConcurrentHashMap<>();

    private SpotifyAlbumArtService() {}

    @Nonnull
    public static String placeholderPath() {
        return SpotifyConstants.ALBUM_ART_SLOT_PATH;
    }

    /**
     * @return slot asset path when this player's cover is already pushed for {@code imageUrl}; otherwise null
     *     (download may be started in background).
     */
    @Nullable
    public static String resolveRemote(@Nonnull PlayerRef playerRef, @Nullable String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        UUID uuid = playerRef.getUuid();
        String ready = READY_KEY_BY_PLAYER.get(uuid);
        if (Objects.equals(ready, imageUrl)) {
            return DynamicCoverSender.slotPath();
        }
        requestRemote(playerRef, imageUrl);
        return null;
    }

    @Nullable
    public static String resolveLocalFile(@Nonnull PlayerRef playerRef, @Nullable Path file, @Nonnull String cacheKey) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        String key = "file:" + cacheKey;
        UUID uuid = playerRef.getUuid();
        String ready = READY_KEY_BY_PLAYER.get(uuid);
        if (Objects.equals(ready, key)) {
            return DynamicCoverSender.slotPath();
        }
        if (!IN_FLIGHT.add(uuid + "|" + key)) {
            return null;
        }
        PENDING_KEY_BY_PLAYER.put(uuid, key);
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                byte[] raw = Files.readAllBytes(file);
                byte[] png = toPng(raw);
                if (png == null) {
                    return;
                }
                deliver(uuid, key, png);
            } catch (Exception e) {
                LOGGER.atFine().withCause(e).log("Local cover load failed");
            } finally {
                IN_FLIGHT.remove(uuid + "|" + key);
            }
        });
        return null;
    }

    public static void clearPlayer(@Nonnull UUID playerUuid) {
        READY_KEY_BY_PLAYER.remove(playerUuid);
        PENDING_KEY_BY_PLAYER.remove(playerUuid);
    }

    public static void syncFromDataDirectory() {
        DynamicCoverSender.resolveSlotHash();
    }

    private static void requestRemote(@Nonnull PlayerRef playerRef, @Nonnull String imageUrl) {
        UUID uuid = playerRef.getUuid();
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
                deliver(uuid, imageUrl, png);
            } catch (Exception e) {
                LOGGER.atFine().withCause(e).log("Cover download failed: %s", imageUrl);
            } finally {
                IN_FLIGHT.remove(flightKey);
            }
        });
    }

    private static void deliver(@Nonnull UUID playerUuid, @Nonnull String key, @Nonnull byte[] png) {
        if (!Objects.equals(PENDING_KEY_BY_PLAYER.get(playerUuid), key)) {
            return;
        }
        PlayerRef online = findOnline(playerUuid);
        if (online == null || !online.isValid()) {
            return;
        }
        boolean ok = DynamicCoverSender.sendPngToPlayer(online.getPacketHandler(), png);
        if (!ok) {
            return;
        }
        READY_KEY_BY_PLAYER.put(playerUuid, key);
    }

    private static final int COVER_PIXEL_SIZE = 128;

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

    /** Center-crop to square, then scale to a fixed pixel size so HUD layout never shifts. */
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
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY
            );
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
