package com.jagod.spotify.oauth;

import com.jagod.spotify.SpotifyConstants;
import java.awt.Desktop;
import java.net.URI;
import javax.annotation.Nonnull;

public final class SpotifyBrowserLauncher {
    private SpotifyBrowserLauncher() {}

    public static void open(@Nonnull String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] {"cmd", "/c", "start", "", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] {"open", url});
            } else {
                Runtime.getRuntime().exec(new String[] {"xdg-open", url});
            }
        } catch (Exception ignored) {
        }
    }

    @Nonnull
    public static String setupUrl(@Nonnull String sessionId) {
        return "http://127.0.0.1:" + SpotifyConstants.SETUP_PORT
            + SpotifyConstants.SETUP_PATH + "?session=" + sessionId;
    }
}
