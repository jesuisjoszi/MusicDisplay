package com.jagod.spotify.api;

import com.hypixel.hytale.logger.HytaleLogger;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Minimal outbound HTTP for Spotify Web API. */
public final class SpotifyHttpClient {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private SpotifyHttpClient() {}

    @Nullable
    public static String getString(@Nonnull String url, @Nonnull Map<String, String> headers) {
        try {
            HttpURLConnection http = open(url, "GET", headers);
            int code = http.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? http.getInputStream() : http.getErrorStream();
            if (in == null) {
                http.disconnect();
                return null;
            }
            String body = new String(readAll(in), StandardCharsets.UTF_8);
            http.disconnect();
            if (code < 200 || code >= 300) {
                LOGGER.atFine().log("Spotify GET %s failed: HTTP %s", url, code);
                return null;
            }
            return body;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Spotify GET %s failed", url);
            return null;
        }
    }

    public static int postEmpty(@Nonnull String url, @Nonnull Map<String, String> headers) {
        return requestEmpty(url, "POST", headers);
    }

    public static int putEmpty(@Nonnull String url, @Nonnull Map<String, String> headers) {
        return requestEmpty(url, "PUT", headers);
    }

    private static int requestEmpty(@Nonnull String url, @Nonnull String method, @Nonnull Map<String, String> headers) {
        try {
            HttpURLConnection http = open(url, method, headers);
            http.setRequestProperty("Content-Length", "0");
            if ("POST".equals(method) || "PUT".equals(method)) {
                try (OutputStream os = http.getOutputStream()) {
                    os.write(new byte[0]);
                }
            }
            int code = http.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? http.getInputStream() : http.getErrorStream();
            String body = "";
            if (in != null) {
                body = new String(readAll(in), StandardCharsets.UTF_8);
            }
            http.disconnect();
            if (code < 200 || code >= 300) {
                LOGGER.atWarning().log("Spotify %s %s failed: HTTP %s %s", method, url, code, body);
            }
            return code;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("%s %s failed", method, url);
            return -1;
        }
    }

    public static int getResponseCode(@Nonnull String url, @Nonnull Map<String, String> headers) {
        try {
            HttpURLConnection http = open(url, "GET", headers);
            int code = http.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? http.getInputStream() : http.getErrorStream();
            if (in != null) {
                readAll(in);
            }
            http.disconnect();
            return code;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Spotify GET %s failed", url);
            return -1;
        }
    }

    @Nullable
    public static byte[] getBytes(@Nonnull String url, @Nonnull Map<String, String> headers) {
        try {
            HttpURLConnection http = open(url, "GET", headers);
            int code = http.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? http.getInputStream() : http.getErrorStream();
            if (in == null) {
                http.disconnect();
                return null;
            }
            byte[] body = readAll(in);
            http.disconnect();
            if (code < 200 || code >= 300) {
                LOGGER.atFine().log("GET bytes %s failed: HTTP %s", url, code);
                return null;
            }
            return body;
        } catch (Exception e) {
            LOGGER.atFine().withCause(e).log("GET bytes %s failed", url);
            return null;
        }
    }

    @Nullable
    public static String postForm(
        @Nonnull String url,
        @Nonnull Map<String, String> headers,
        @Nonnull String formBody
    ) {
        try {
            HttpURLConnection http = open(url, "POST", headers);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
            byte[] body = formBody.getBytes(StandardCharsets.UTF_8);
            http.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = http.getOutputStream()) {
                os.write(body);
            }
            int code = http.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? http.getInputStream() : http.getErrorStream();
            String response = in != null ? new String(readAll(in), StandardCharsets.UTF_8) : "";
            http.disconnect();
            if (code < 200 || code >= 300) {
                LOGGER.atWarning().log("Spotify POST %s failed: HTTP %s %s", url, code, response);
                return null;
            }
            return response;
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("Spotify POST %s failed", url);
            return null;
        }
    }

    @Nonnull
    public static String basicAuth(@Nonnull String clientId, @Nonnull String clientSecret) {
        String raw = clientId + ":" + clientSecret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Nonnull
    private static HttpURLConnection open(@Nonnull String url, @Nonnull String method, @Nonnull Map<String, String> headers)
        throws Exception {
        HttpURLConnection http = (HttpURLConnection) URI.create(url).toURL().openConnection();
        http.setRequestMethod(method);
        http.setConnectTimeout(CONNECT_TIMEOUT_MS);
        http.setReadTimeout(READ_TIMEOUT_MS);
        http.setDoInput(true);
        if ("POST".equals(method) || "PUT".equals(method)) {
            http.setDoOutput(true);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            http.setRequestProperty(entry.getKey(), entry.getValue());
        }
        return http;
    }

    @Nonnull
    private static byte[] readAll(@Nonnull InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
