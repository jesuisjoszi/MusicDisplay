package com.joszza.spotify.assets;

import com.hypixel.hytale.common.util.ArrayUtil;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.setup.AssetFinalize;
import com.hypixel.hytale.protocol.packets.setup.AssetInitialize;
import com.hypixel.hytale.protocol.packets.setup.AssetPart;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.joszza.spotify.SpotifyConstants;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** HyUI-style cover push into a pack-bundled slot for one player. */
public final class DynamicCoverSender {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PART_SIZE = 2_621_440;
    /** Soft throttle — rapid rebuilds can hitch / disconnect the client. */
    private static final long MIN_REBUILD_INTERVAL_MS = 1_200L;

    @Nullable
    private static volatile String slotHash;

    private static final ConcurrentMap<UUID, Long> LAST_REBUILD_MS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<UUID, byte[]> DEFERRED_PNG = new ConcurrentHashMap<>();

    private DynamicCoverSender() {}

    @Nonnull
    public static String slotPath() {
        return SpotifyConstants.ALBUM_ART_SLOT_PATH;
    }

    @Nullable
    public static String resolveSlotHash() {
        String cached = slotHash;
        if (cached != null) {
            return cached;
        }
        CommonAsset baseline = CommonAssetRegistry.getByName(SpotifyConstants.ALBUM_ART_SLOT_PATH);
        if (baseline == null) {
            LOGGER.atWarning().log(
                "Cover slot missing from common assets: %s (is the MusicDisplay asset pack loaded?)",
                SpotifyConstants.ALBUM_ART_SLOT_PATH
            );
            return null;
        }
        slotHash = baseline.getHash();
        return slotHash;
    }

    /**
     * Push cover bytes. If rebuild cooldown is active, queues the PNG and returns {@code false}
     * (caller should keep the cover pending and call {@link #flushDeferred} later).
     */
    public static boolean sendPngToPlayer(
        @Nonnull UUID playerUuid,
        @Nonnull PacketHandler handler,
        @Nonnull byte[] pngBytes
    ) {
        if (pngBytes.length < 8) {
            return false;
        }
        String hash = resolveSlotHash();
        if (hash == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_REBUILD_MS.get(playerUuid);
        if (last != null && now - last < MIN_REBUILD_INTERVAL_MS) {
            DEFERRED_PNG.put(playerUuid, pngBytes);
            return false;
        }
        return pushNow(playerUuid, handler, hash, pngBytes, now);
    }

    public static boolean flushDeferred(@Nonnull UUID playerUuid, @Nonnull PacketHandler handler) {
        byte[] png = DEFERRED_PNG.get(playerUuid);
        if (png == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_REBUILD_MS.get(playerUuid);
        if (last != null && now - last < MIN_REBUILD_INTERVAL_MS) {
            return false;
        }
        String hash = resolveSlotHash();
        if (hash == null) {
            DEFERRED_PNG.remove(playerUuid);
            return false;
        }
        DEFERRED_PNG.remove(playerUuid);
        return pushNow(playerUuid, handler, hash, png, now);
    }

    public static boolean hasDeferred(@Nonnull UUID playerUuid) {
        return DEFERRED_PNG.containsKey(playerUuid);
    }

    public static void clearPlayer(@Nonnull UUID playerUuid) {
        LAST_REBUILD_MS.remove(playerUuid);
        DEFERRED_PNG.remove(playerUuid);
    }

    private static boolean pushNow(
        @Nonnull UUID playerUuid,
        @Nonnull PacketHandler handler,
        @Nonnull String hash,
        @Nonnull byte[] pngBytes,
        long nowMs
    ) {
        try {
            CommonAsset cover = new BlobCommonAsset(SpotifyConstants.ALBUM_ART_SLOT_PATH, hash, pngBytes);
            writeAsset(handler, cover);
            handler.writeNoCache(new RequestCommonAssetsRebuild());
            LAST_REBUILD_MS.put(playerUuid, nowMs);
            DEFERRED_PNG.remove(playerUuid);
            return true;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Failed to send album cover asset");
            return false;
        }
    }

    private static void writeAsset(@Nonnull PacketHandler handler, @Nonnull CommonAsset asset) {
        byte[] allBytes = asset.getBlob().join();
        byte[][] parts = ArrayUtil.split(allBytes, PART_SIZE);
        ToClientPacket[] packets = new ToClientPacket[1 + parts.length];
        packets[0] = new AssetInitialize(asset.toPacket(), allBytes.length);
        for (int i = 0; i < parts.length; i++) {
            packets[1 + i] = new AssetPart(parts[i]);
        }
        handler.write(packets, new AssetFinalize());
    }
}
