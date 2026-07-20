package com.joszza.spotify.oauth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.joszza.spotify.SpotifyConstants;
import com.joszza.spotify.api.SpotifyHttpClient;
import com.joszza.spotify.command.SpotifyCommand;
import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.joszza.spotify.service.SpotifyPollingService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Local OAuth server modeled after Minecraft musicdisplay's SpotifyManager.
 * @see <a href="https://github.com/realmichaelstetson/musicdisplay">realmichaelstetson/musicdisplay</a>
 */
public final class SpotifyOAuthServer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final SpotifyOAuthSessionStore sessionStore = new SpotifyOAuthSessionStore();
    @Nullable
    private HttpServer server;

    public boolean ensureRunning() {
        if (server != null) {
            return true;
        }
        try {
            // Bind all interfaces on the port, same as musicdisplay (InetSocketAddress(PORT)).
            HttpServer created = HttpServer.create(new InetSocketAddress(SpotifyConstants.SETUP_PORT), 0);
            created.createContext(SpotifyConstants.SETUP_PATH, this::handleSetup);
            created.createContext(SpotifyConstants.SUBMIT_PATH, this::handleSubmit);
            created.createContext(SpotifyConstants.CALLBACK_PATH, this::handleCallback);
            created.start();
            server = created;
            LOGGER.atInfo().log("Spotify OAuth server on port %s, redirect URI: %s", SpotifyConstants.SETUP_PORT, SpotifyConstants.REDIRECT_URI);
            return true;
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("Could not start Spotify OAuth server on port %s", SpotifyConstants.SETUP_PORT);
            return false;
        }
    }

    public void shutdown() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    @Nonnull
    public SpotifyOAuthSession createSession(@Nonnull java.util.UUID playerUuid) {
        return sessionStore.create(playerUuid);
    }

    private void handleSetup(@Nonnull HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }
        String query = exchange.getRequestURI().getRawQuery();
        String sessionId = queryParam(query, "session");
        String lang = SpotifySetupWebI18n.normalizeLang(queryParam(query, "lang"));
        if (sessionId == null || sessionStore.get(sessionId) == null) {
            sendHtml(exchange, 400, errorPage(SpotifySetupWebI18n.get(lang).invalidSession(), lang));
            return;
        }
        sendHtml(exchange, 200, setupPage(sessionId, lang, null));
    }

    private void handleSubmit(@Nonnull HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = parseForm(body);
        String sessionId = form.get("session");
        String clientId = trim(form.get("clientId"));
        String clientSecret = trim(form.get("clientSecret"));

        String lang = SpotifySetupWebI18n.normalizeLang(form.get("lang"));
        SpotifyOAuthSession session = sessionId != null ? sessionStore.get(sessionId) : null;
        if (session == null) {
            sendHtml(exchange, 400, errorPage(SpotifySetupWebI18n.get(lang).sessionExpired(), lang));
            return;
        }
        if (clientId == null || clientSecret == null) {
            sendHtml(exchange, 400, setupPage(sessionId, lang, SpotifySetupWebI18n.get(lang).credentialsRequired()));
            return;
        }

        session.setClientCredentials(clientId, clientSecret);

        String authorizeUrl = buildAuthorizeUrl(clientId, sessionId);
        LOGGER.atInfo().log("Spotify authorize redirect_uri=%s client_id=%s", SpotifyConstants.REDIRECT_URI, clientId);

        exchange.getResponseHeaders().set("Location", authorizeUrl);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void handleCallback(@Nonnull HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendText(exchange, 405, "Method not allowed");
            return;
        }

        String query = exchange.getRequestURI().getRawQuery();
        String lang = SpotifySetupWebI18n.DEFAULT_LANG;
        String error = queryParam(query, "error");
        if (error != null) {
            String description = queryParam(query, "error_description");
            String reason = description != null ? error + ": " + description : error;
            SpotifySetupWebI18n.LangPack pack = SpotifySetupWebI18n.get(lang);
            sendHtml(exchange, 400, errorPage(pack.authFailedPrefix() + ": " + htmlEscape(reason), lang));
            return;
        }

        String code = queryParam(query, "code");
        String sessionId = queryParam(query, "state");
        SpotifyOAuthSession session = sessionId != null ? sessionStore.get(sessionId) : null;
        if (code == null || session == null || session.getClientId() == null || session.getClientSecret() == null) {
            sendHtml(exchange, 400, errorPage(SpotifySetupWebI18n.get(lang).invalidCallback(), lang));
            return;
        }

        String tokenBody =
            "grant_type=authorization_code"
                + "&code=" + code
                + "&redirect_uri=" + URLEncoder.encode(SpotifyConstants.REDIRECT_URI, StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode(session.getClientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(session.getClientSecret(), StandardCharsets.UTF_8);

        String tokenResponse = SpotifyHttpClient.postForm(
            "https://accounts.spotify.com/api/token",
            Map.of(),
            tokenBody
        );

        if (tokenResponse == null || tokenResponse.isBlank()) {
            sendHtml(exchange, 500, errorPage(SpotifySetupWebI18n.get(lang).tokenExchangeFailed(), lang));
            return;
        }

        JsonObject json = JsonParser.parseString(tokenResponse).getAsJsonObject();
        if (!json.has("refresh_token") || !json.has("access_token")) {
            String spotifyError = json.has("error") ? json.get("error").getAsString() : "unknown";
            String spotifyDesc = json.has("error_description") ? json.get("error_description").getAsString() : "";
            SpotifySetupWebI18n.LangPack pack = SpotifySetupWebI18n.get(lang);
            sendHtml(exchange, 500, errorPage(pack.tokenErrorPrefix() + ": " + spotifyError + " " + spotifyDesc, lang));
            return;
        }

        String refreshToken = json.get("refresh_token").getAsString();
        String accessToken = json.get("access_token").getAsString();
        String grantedScopes = json.has("scope") ? json.get("scope").getAsString() : SpotifyConstants.SPOTIFY_SCOPES;
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
        long expiresAt = System.currentTimeMillis() + expiresIn * 1000L;

        applyCredentialsToPlayer(
            session.getPlayerUuid(),
            session.getClientId(),
            session.getClientSecret(),
            refreshToken,
            accessToken,
            expiresAt,
            grantedScopes
        );
        sessionStore.remove(sessionId);
        sendHtml(exchange, 200, successPage(lang));
    }

    @Nonnull
    private static String buildAuthorizeUrl(@Nonnull String clientId, @Nonnull String sessionId) {
        return "https://accounts.spotify.com/authorize"
            + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&response_type=code"
            + "&redirect_uri=" + URLEncoder.encode(SpotifyConstants.REDIRECT_URI, StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode(SpotifyConstants.SPOTIFY_SCOPES, StandardCharsets.UTF_8)
            + "&show_dialog=true"
            + "&state=" + URLEncoder.encode(sessionId, StandardCharsets.UTF_8);
    }

    private void applyCredentialsToPlayer(
        @Nonnull java.util.UUID playerUuid,
        @Nonnull String clientId,
        @Nonnull String clientSecret,
        @Nonnull String refreshToken,
        @Nonnull String accessToken,
        long accessTokenExpiresAt,
        @Nullable String grantedScopes
    ) {
        PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
        if (playerRef == null) {
            LOGGER.atWarning().log("Spotify OAuth completed but player %s is offline", playerUuid);
            return;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (!ref.isValid()) {
                return;
            }
            SpotifyCommand.ensureComponent(store, ref);
            SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
            if (state == null) {
                return;
            }
            state.setCredentials(clientId, clientSecret, refreshToken);
            state.setAccessToken(accessToken, accessTokenExpiresAt);
            state.setGrantedScopes(grantedScopes);
            state.setHudEnabled(true);
            SpotifyPollingService.refreshPlayerNow(ref, store);
            playerRef.sendMessage(Message.translation("spotify.spotify.ui.setup.saved"));
            if (!state.hasPlaybackScope()) {
                playerRef.sendMessage(Message.translation("spotify.spotify.command.missingScope"));
            } else if (!com.joszza.spotify.api.SpotifyProfile.isPremium(state)) {
                playerRef.sendMessage(Message.translation("spotify.spotify.command.premiumRequired"));
            }
        });
    }

    @Nonnull
    private static String setupPage(@Nonnull String sessionId, @Nonnull String lang, @Nullable String error) {
        SpotifySetupWebI18n.LangPack t = SpotifySetupWebI18n.get(lang);
        String errorBlock = error == null ? "" : "<p class=\"error\">" + htmlEscape(error) + "</p>";
        String langSwitcher = SpotifySetupWebI18n.languageSwitcher(sessionId, lang);
        return pageShell(
            t.pageTitle(),
            lang,
            """
              <div class="hero">
                <div class="badge">HYTALE MOD</div>
                <h1>%s</h1>
                <p class="subtitle">%s</p>
              </div>
              %s
              %s
              <div class="card">
                <h2>%s</h2>
                <ol>
                  <li>%s</li>
                  <li>%s</li>
                  <li>%s</li>
                  <li>%s</li>
                  <li>%s<br><span class="uri">%s</span></li>
                  <li>%s</li>
                </ol>
              </div>
              <div class="card">
                <h2>%s</h2>
                <p>%s</p>
                <form method="post" action="%s">
                  <input type="hidden" name="session" value="%s">
                  <input type="hidden" name="lang" value="%s">
                  <label for="clientId">%s</label>
                  <input id="clientId" name="clientId" required autocomplete="off" spellcheck="false">
                  <label for="clientSecret">%s</label>
                  <input id="clientSecret" name="clientSecret" required autocomplete="off" spellcheck="false">
                  <button type="submit">%s</button>
                </form>
                <p class="warn">%s</p>
              </div>
            """.formatted(
                htmlEscape(t.pageTitle()),
                htmlEscape(t.subtitle()),
                langSwitcher,
                errorBlock,
                htmlEscape(t.step1Title()),
                t.step1a(),
                htmlEscape(t.step1b()),
                t.step1c(),
                t.step1d(),
                t.step1e(),
                SpotifyConstants.REDIRECT_URI,
                t.step1f(),
                htmlEscape(t.step2Title()),
                htmlEscape(t.step2a()),
                SpotifyConstants.SUBMIT_PATH,
                htmlEscape(sessionId),
                htmlEscape(lang),
                htmlEscape(t.clientIdLabel()),
                htmlEscape(t.clientSecretLabel()),
                htmlEscape(t.connectButton()),
                htmlEscape(t.redirectHint())
            )
        );
    }

    @Nonnull
    private static String successPage(@Nonnull String lang) {
        SpotifySetupWebI18n.LangPack t = SpotifySetupWebI18n.get(lang);
        return pageShell(
            t.connectedTitle(),
            lang,
            """
              <div class="card center">
                <div class="success-icon">✓</div>
                <h1>%s</h1>
                <p>%s</p>
              </div>
            """.formatted(htmlEscape(t.connectedTitle()), htmlEscape(t.connectedBody()))
        );
    }

    @Nonnull
    private static String errorPage(@Nonnull String message, @Nonnull String lang) {
        SpotifySetupWebI18n.LangPack t = SpotifySetupWebI18n.get(lang);
        return pageShell(
            t.errorTitle(),
            lang,
            """
              <div class="card">
                <h1 class="error-title">%s</h1>
                <p>%s</p>
                <p>%s: <code>%s</code></p>
              </div>
            """.formatted(
                htmlEscape(t.errorTitle()),
                message,
                htmlEscape(t.expectedRedirectLabel()),
                SpotifyConstants.REDIRECT_URI
            )
        );
    }

    @Nonnull
    private static String pageShell(@Nonnull String title, @Nonnull String lang, @Nonnull String body) {
        return """
            <!DOCTYPE html>
            <html lang="%s">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>%s</title>
              <style>
                :root {
                  --bg-deep: #0a0e14;
                  --bg-card: #121820;
                  --bg-input: #0d1218;
                  --accent: #5ce1e6;
                  --accent-dim: #2a8f96;
                  --spotify: #1db954;
                  --text: #e8eaed;
                  --muted: #8b9aab;
                  --warn: #e8c468;
                  --error: #ff7b7b;
                  --border: #1e2a38;
                }
                * { box-sizing: border-box; }
                body {
                  font-family: "Segoe UI", system-ui, sans-serif;
                  background: radial-gradient(ellipse at top, #14202c 0%%, var(--bg-deep) 55%%);
                  color: var(--text);
                  margin: 0;
                  min-height: 100vh;
                  padding: 32px 20px 48px;
                }
                .wrap { max-width: 720px; margin: 0 auto; }
                .hero { text-align: center; margin-bottom: 28px; }
                .badge {
                  display: inline-block;
                  font-size: 11px;
                  letter-spacing: 0.14em;
                  color: var(--accent);
                  border: 1px solid var(--accent-dim);
                  border-radius: 999px;
                  padding: 6px 14px;
                  margin-bottom: 14px;
                }
                h1 { margin: 0 0 10px; font-size: 2rem; color: var(--text); }
                h2 { margin-top: 0; font-size: 1.1rem; color: var(--accent); }
                .subtitle { color: var(--muted); margin: 0; line-height: 1.6; }
                .card {
                  background: var(--bg-card);
                  border: 1px solid var(--border);
                  border-radius: 14px;
                  padding: 24px 26px;
                  margin-bottom: 18px;
                  box-shadow: 0 12px 40px #00000055;
                }
                .card.center { text-align: center; padding: 40px 28px; }
                .success-icon {
                  width: 64px; height: 64px; line-height: 64px;
                  border-radius: 50%%; margin: 0 auto 16px;
                  background: #1db95433; color: var(--spotify);
                  font-size: 2rem; font-weight: 700;
                }
                ol { line-height: 1.75; color: var(--muted); padding-left: 1.2rem; }
                ol li { margin-bottom: 6px; }
                label { display: block; margin: 14px 0 6px; font-weight: 600; color: var(--text); }
                input {
                  width: 100%%; padding: 12px 14px; border-radius: 10px;
                  border: 1px solid var(--border); background: var(--bg-input);
                  color: var(--text); font-size: 15px;
                }
                input:focus { outline: 2px solid var(--accent-dim); border-color: var(--accent); }
                button {
                  margin-top: 18px; width: 100%%;
                  background: linear-gradient(135deg, var(--spotify), #169c46);
                  color: #041208; border: 0; border-radius: 999px;
                  padding: 14px 24px; font-weight: 700; font-size: 15px;
                  cursor: pointer;
                }
                button:hover { filter: brightness(1.08); }
                .error, .error-title { color: var(--error); }
                code, .uri {
                  background: #0a1018; padding: 4px 8px; border-radius: 6px;
                  font-family: Consolas, monospace; color: var(--accent);
                  word-break: break-all;
                }
                .warn { color: var(--warn); font-size: 14px; line-height: 1.5; }
                .lang-bar {
                  display: flex; flex-wrap: wrap; gap: 8px; align-items: center;
                  justify-content: center; margin-bottom: 18px;
                }
                .lang-label { color: var(--muted); font-size: 13px; margin-right: 4px; }
                .lang-btn {
                  color: var(--muted); text-decoration: none; font-size: 13px;
                  border: 1px solid var(--border); border-radius: 999px;
                  padding: 5px 12px;
                }
                .lang-btn:hover { color: var(--text); border-color: var(--accent-dim); }
                .lang-btn.active {
                  color: var(--accent); border-color: var(--accent);
                  background: #5ce1e618;
                }
                a { color: var(--accent); }
              </style>
            </head>
            <body>
              <div class="wrap">%s</div>
            </body>
            </html>
            """.formatted(lang, htmlEscape(title), body);
    }

    @Nonnull
    private static Map<String, String> parseForm(@Nonnull String body) {
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            map.put(decode(pair.substring(0, idx)), decode(pair.substring(idx + 1)));
        }
        return map;
    }

    @Nullable
    private static String queryParam(@Nullable String query, @Nonnull String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String part : query.split("&")) {
            int idx = part.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            if (key.equals(decode(part.substring(0, idx)))) {
                return decode(part.substring(idx + 1));
            }
        }
        return null;
    }

    @Nonnull
    private static String decode(@Nonnull String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Nullable
    private static String trim(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nonnull
    private static String htmlEscape(@Nonnull String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void sendHtml(@Nonnull HttpExchange exchange, int status, @Nonnull String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendText(@Nonnull HttpExchange exchange, int status, @Nonnull String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
