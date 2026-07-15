package com.jagod.spotify.oauth;

import com.jagod.spotify.SpotifyConstants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyOAuthService {
    private static final SpotifyOAuthServer SERVER = new SpotifyOAuthServer();

    private SpotifyOAuthService() {}

    public record SetupBeginResult(@Nonnull String url, boolean copiedToClipboard) {}

    public static void shutdown() {
        SERVER.shutdown();
    }

    @Nullable
    public static SetupBeginResult beginSetup(@Nonnull PlayerRef playerRef, @Nonnull UUIDComponent uuidComponent) {
        if (!SERVER.ensureRunning()) {
            playerRef.sendMessage(Message.translation("spotify.spotify.oauth.serverFailed"));
            return null;
        }

        SpotifyOAuthSession session = SERVER.createSession(uuidComponent.getUuid());
        String url = setupUrl(session.getSessionId());
        boolean copied = SpotifySetupClipboard.tryCopy(url);
        return new SetupBeginResult(url, copied);
    }

    @Nonnull
    private static String setupUrl(@Nonnull String sessionId) {
        return "http://127.0.0.1:" + SpotifyConstants.SETUP_PORT
            + SpotifyConstants.SETUP_PATH + "?session=" + sessionId;
    }
}
