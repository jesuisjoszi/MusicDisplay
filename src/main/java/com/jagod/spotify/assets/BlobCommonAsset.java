package com.jagod.spotify.assets;

import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

/** In-memory common asset (HyUI-style dynamic image blob). */
public final class BlobCommonAsset extends CommonAsset {
    @Nonnull
    private final byte[] data;

    public BlobCommonAsset(@Nonnull String name, @Nonnull String hash, @Nonnull byte[] data) {
        super(name, hash, data);
        this.data = data;
    }

    @Override
    protected CompletableFuture<byte[]> getBlob0() {
        return CompletableFuture.completedFuture(data);
    }
}
