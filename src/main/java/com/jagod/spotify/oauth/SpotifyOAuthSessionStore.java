package com.jagod.spotify.oauth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyOAuthSessionStore {
    private final Map<String, SpotifyOAuthSession> bySessionId = new ConcurrentHashMap<>();
    private final Map<UUID, String> sessionIdByPlayer = new ConcurrentHashMap<>();

    @Nonnull
    public SpotifyOAuthSession create(@Nonnull UUID playerUuid) {
        purgeExpired();
        String existingId = sessionIdByPlayer.get(playerUuid);
        if (existingId != null) {
            SpotifyOAuthSession existing = bySessionId.get(existingId);
            if (existing != null && !existing.isExpired(System.currentTimeMillis())) {
                return existing;
            }
            remove(existingId);
        }

        String sessionId = UUID.randomUUID().toString();
        SpotifyOAuthSession session = new SpotifyOAuthSession(sessionId, playerUuid);
        bySessionId.put(sessionId, session);
        sessionIdByPlayer.put(playerUuid, sessionId);
        return session;
    }

    @Nullable
    public SpotifyOAuthSession get(@Nonnull String sessionId) {
        purgeExpired();
        SpotifyOAuthSession session = bySessionId.get(sessionId);
        if (session == null || session.isExpired(System.currentTimeMillis())) {
            if (session != null) {
                remove(sessionId);
            }
            return null;
        }
        return session;
    }

    public void remove(@Nonnull String sessionId) {
        SpotifyOAuthSession session = bySessionId.remove(sessionId);
        if (session != null) {
            sessionIdByPlayer.remove(session.getPlayerUuid(), sessionId);
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, SpotifyOAuthSession> entry : bySessionId.entrySet()) {
            if (entry.getValue().isExpired(now)) {
                remove(entry.getKey());
            }
        }
    }
}
