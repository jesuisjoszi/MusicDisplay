package com.jagod.spotify.oauth;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class SpotifyOAuthService {
    private static final SpotifyOAuthServer SERVER = new SpotifyOAuthServer();

    private SpotifyOAuthService() {}

    public static void shutdown() {
        SERVER.shutdown();
    }

    public static boolean beginSetup(@Nonnull PlayerRef playerRef, @Nonnull UUIDComponent uuidComponent) {
        if (!SERVER.ensureRunning()) {
            playerRef.sendMessage(Message.translation("spotify.spotify.oauth.serverFailed"));
            return false;
        }

        SpotifyOAuthSession session = SERVER.createSession(uuidComponent.getUuid());
        String url = SpotifyBrowserLauncher.setupUrl(session.getSessionId());
        SpotifyBrowserLauncher.open(url);
        playerRef.sendMessage(Message.translation("spotify.spotify.oauth.browserOpened").param("url", url));
        return true;
    }
}
