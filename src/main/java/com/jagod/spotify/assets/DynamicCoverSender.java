package com.jagod.spotify.assets;

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
import com.jagod.spotify.SpotifyConstants;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * HyUI-style cover push: overwrite a pack-bundled slot path and send asset packets to one player.
 * Based on Elliesaur/HyUI {@code DynamicImageAsset.sendToPlayer} (and SimpleClaims).
 */
public final class DynamicCoverSender {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int PART_SIZE = 2_621_440;

    @Nullable
    private static volatile String slotHash;

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

    public static boolean sendPngToPlayer(@Nonnull PacketHandler handler, @Nonnull byte[] pngBytes) {
        String hash = resolveSlotHash();
        if (hash == null) {
            return false;
        }
        try {
            CommonAsset baseline = CommonAssetRegistry.getByName(SpotifyConstants.ALBUM_ART_SLOT_PATH);
            if (baseline != null) {
                // Clear slot first (same as HyUI), then push the real cover bytes.
                writeAsset(handler, baseline);
            }
            CommonAsset cover = new BlobCommonAsset(SpotifyConstants.ALBUM_ART_SLOT_PATH, hash, pngBytes);
            writeAsset(handler, cover);
            handler.writeNoCache(new RequestCommonAssetsRebuild());
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
