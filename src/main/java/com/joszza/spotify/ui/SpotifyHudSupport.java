package com.joszza.spotify.ui;

import com.joszza.spotify.SpotifyConstants;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class SpotifyHudSupport {
    private SpotifyHudSupport() {}

    @Nonnull
    public static SpotifyNowPlayingHud obtainHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        CustomUIHud existing = player.getHudManager().getCustomHud(SpotifyConstants.HUD_KEY);
        if (existing instanceof SpotifyNowPlayingHud hud) {
            return hud;
        }
        SpotifyNowPlayingHud created = new SpotifyNowPlayingHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, created);
        return created;
    }

    public static void removeHud(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getHudManager().removeCustomHud(playerRef, SpotifyConstants.HUD_KEY);
    }

    public static boolean isHudActive(@Nonnull Player player) {
        return player.getHudManager().getCustomHud(SpotifyConstants.HUD_KEY) instanceof SpotifyNowPlayingHud;
    }
}
