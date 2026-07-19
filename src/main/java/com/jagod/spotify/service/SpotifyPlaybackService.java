package com.jagod.spotify.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagod.spotify.api.SpotifyAuth;
import com.jagod.spotify.api.SpotifyHttpClient;
import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.hypixel.hytale.logger.HytaleLogger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyPlaybackService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String PLAYER_API = "https://api.spotify.com/v1/me/player";

    private SpotifyPlaybackService() {}

    public static int next(@Nonnull SpotifyPlayerComponent state) {
        return authorizedRequest(state, "POST", "/next");
    }

    public static int previous(@Nonnull SpotifyPlayerComponent state) {
        return authorizedRequest(state, "POST", "/previous");
    }

    public static int pause(@Nonnull SpotifyPlayerComponent state) {
        return authorizedRequest(state, "PUT", "/pause");
    }

    public static int play(@Nonnull SpotifyPlayerComponent state) {
        return authorizedRequest(state, "PUT", "/play");
    }

    public static int setVolume(@Nonnull SpotifyPlayerComponent state, int volumePercent) {
        int clamped = Math.max(0, Math.min(100, volumePercent));
        String token = SpotifyAuth.ensureAccessToken(state);
        if (token == null) {
            return -1;
        }
        Map<String, String> headers = Map.of("Authorization", "Bearer " + token);
        String deviceId = resolveActiveDeviceId(token);
        String url = PLAYER_API + "/volume?volume_percent=" + clamped;
        if (deviceId != null && !deviceId.isBlank()) {
            url += "&device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
        }
        return SpotifyHttpClient.putEmpty(url, headers);
    }

    public static int getVolumePercent(@Nonnull SpotifyPlayerComponent state) {
        String token = SpotifyAuth.ensureAccessToken(state);
        if (token == null) {
            return -1;
        }
        String body = SpotifyHttpClient.getString(PLAYER_API, Map.of("Authorization", "Bearer " + token));
        if (body == null || body.isBlank()) {
            return -1;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("device") || !root.get("device").isJsonObject()) {
                return -1;
            }
            JsonObject device = root.getAsJsonObject("device");
            if (!device.has("volume_percent") || device.get("volume_percent").isJsonNull()) {
                return -1;
            }
            return Math.max(0, Math.min(100, device.get("volume_percent").getAsInt()));
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("Failed to parse Spotify volume");
            return -1;
        }
    }

    private static int authorizedRequest(@Nonnull SpotifyPlayerComponent state, @Nonnull String method, @Nonnull String path) {
        String token = SpotifyAuth.ensureAccessToken(state);
        if (token == null) {
            return -1;
        }
        Map<String, String> headers = Map.of("Authorization", "Bearer " + token);
        String deviceId = resolveActiveDeviceId(token);
        String url = PLAYER_API + path;
        if (deviceId != null && !deviceId.isBlank()) {
            url += "?device_id=" + URLEncoder.encode(deviceId, StandardCharsets.UTF_8);
        } else {
            LOGGER.atFine().log("Spotify playback %s without device_id (no active player returned by /me/player)", path);
        }
        return switch (method) {
            case "POST" -> SpotifyHttpClient.postEmpty(url, headers);
            case "PUT" -> SpotifyHttpClient.putEmpty(url, headers);
            default -> -1;
        };
    }

    @Nullable
    private static String resolveActiveDeviceId(@Nonnull String token) {
        String body = SpotifyHttpClient.getString(PLAYER_API, Map.of("Authorization", "Bearer " + token));
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("device") || !root.get("device").isJsonObject()) {
                return null;
            }
            JsonObject device = root.getAsJsonObject("device");
            if (!device.has("id") || device.get("id").isJsonNull()) {
                return null;
            }
            return device.get("id").getAsString();
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("Failed to parse active Spotify device");
            return null;
        }
    }
}
