package com.joszza.spotify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyProfile {
    private static final String ME_API = "https://api.spotify.com/v1/me";

    private SpotifyProfile() {}

    @Nullable
    public static String fetchProduct(@Nonnull SpotifyPlayerComponent state) {
        String token = SpotifyAuth.ensureAccessToken(state);
        if (token == null) {
            return null;
        }
        String body = SpotifyHttpClient.getString(ME_API, Map.of("Authorization", "Bearer " + token));
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            return root.has("product") ? root.get("product").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isPremium(@Nonnull SpotifyPlayerComponent state) {
        String product = fetchProduct(state);
        return "premium".equalsIgnoreCase(product);
    }
}
