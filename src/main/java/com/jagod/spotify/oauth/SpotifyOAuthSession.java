package com.jagod.spotify.oauth;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyOAuthSession {
    @Nonnull
    private final String sessionId;
    @Nonnull
    private final UUID playerUuid;
    private final long createdAtMs;

    @Nullable
    private String clientId;
    @Nullable
    private String clientSecret;

    public SpotifyOAuthSession(@Nonnull String sessionId, @Nonnull UUID playerUuid) {
        this.sessionId = sessionId;
        this.playerUuid = playerUuid;
        this.createdAtMs = System.currentTimeMillis();
    }

    @Nonnull
    public String getSessionId() {
        return sessionId;
    }

    @Nonnull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    @Nullable
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientCredentials(@Nonnull String clientId, @Nonnull String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public boolean isExpired(long nowMs) {
        return nowMs - createdAtMs > com.jagod.spotify.SpotifyConstants.OAUTH_SESSION_TTL_MS;
    }
}
