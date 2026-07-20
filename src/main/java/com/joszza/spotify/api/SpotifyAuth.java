package com.joszza.spotify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyAuth {
    private SpotifyAuth() {}

    @Nullable
    public static String ensureAccessToken(@Nonnull SpotifyPlayerComponent state) {
        long now = System.currentTimeMillis();
        if (state.getAccessToken() != null && state.getAccessTokenExpiresAt() > now + 30_000L) {
            return state.getAccessToken();
        }
        String clientId = state.getClientId();
        String clientSecret = state.getClientSecret();
        String refreshToken = state.getRefreshToken();
        if (clientId == null || clientSecret == null || refreshToken == null) {
            return null;
        }

        String form =
            "grant_type=refresh_token&refresh_token="
                + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        String response = SpotifyHttpClient.postForm(
            "https://accounts.spotify.com/api/token",
            Map.of("Authorization", SpotifyHttpClient.basicAuth(clientId, clientSecret)),
            form
        );
        if (response == null || response.isBlank()) {
            return null;
        }

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (!json.has("access_token")) {
            return null;
        }
        String accessToken = json.get("access_token").getAsString();
        long expiresInSec = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
        state.setAccessToken(accessToken, now + expiresInSec * 1000L);
        return accessToken;
    }
}
