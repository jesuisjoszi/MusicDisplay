package com.jagod.spotify.oauth;

import com.jagod.spotify.SpotifyConstants;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Translations for the browser-based Spotify OAuth setup page.
 * Separate from in-game .lang files because the setup page is served over HTTP.
 */
public final class SpotifySetupWebI18n {
    public static final String DEFAULT_LANG = "en";
    private static final Map<String, LangPack> PACKS = new LinkedHashMap<>();

    static {
        register("en", "English", pack(
            "Spotify for Hytale",
            "Connect your Spotify account to show now-playing info in-game.",
            "Invalid or expired setup session. Return to Hytale and click <strong>Setup Spotify</strong> again.",
            "Client ID and Client Secret are required.",
            "Setup session expired. Return to Hytale and click <strong>Setup Spotify</strong> again.",
            "Spotify authorization failed",
            "Invalid callback. Return to Hytale and try again.",
            "Could not exchange authorization code for tokens. Verify Client ID, Secret, and Redirect URI in the Spotify Dashboard.",
            "Token error",
            "Connected!",
            "Return to Hytale. The now-playing overlay should appear automatically.",
            "Setup failed",
            "Expected Redirect URI",
            "Step 1 — Spotify Developer Dashboard",
            "Open the <a href=\"https://developer.spotify.com/dashboard\" target=\"_blank\" rel=\"noopener\">Spotify Developer Dashboard</a>.",
            "Select your app, or create a new one.",
            "Go to <strong>Settings</strong> → <strong>Redirect URIs</strong>.",
            "Delete any <code>localhost</code> entries — Spotify no longer accepts them as secure.",
            "Add this Redirect URI exactly, then click <strong>Save</strong>:",
            "If you still get redirect errors, also add <code>http://127.0.0.1/callback</code> and Save again.",
            "Step 2 — Enter credentials",
            "Paste your Client ID and Client Secret from the same Spotify app.",
            "Client ID",
            "Client Secret",
            "Connect Spotify Account",
            "redirect_uri errors usually mean the URI above is missing in the dashboard, you did not click Save, or the Client ID belongs to a different app.",
            "Language"
        ));
        register("pl", "Polski", pack(
            "Spotify dla Hytale",
            "Połącz konto Spotify, aby wyświetlać aktualny utwór w grze.",
            "Nieprawidłowa lub wygasła sesja. Wróć do Hytale i kliknij <strong>Połącz Spotify</strong> ponownie.",
            "Client ID i Client Secret są wymagane.",
            "Sesja wygasła. Wróć do Hytale i kliknij <strong>Połącz Spotify</strong> ponownie.",
            "Autoryzacja Spotify nie powiodła się",
            "Nieprawidłowy callback. Wróć do Hytale i spróbuj ponownie.",
            "Nie udało się wymienić kodu autoryzacji na tokeny. Sprawdź Client ID, Secret i Redirect URI w Spotify Dashboard.",
            "Błąd tokenu",
            "Połączono!",
            "Wróć do Hytale. Overlay z aktualnym utworem powinien pojawić się automatycznie.",
            "Konfiguracja nie powiodła się",
            "Oczekiwany Redirect URI",
            "Krok 1 — Spotify Developer Dashboard",
            "Otwórz <a href=\"https://developer.spotify.com/dashboard\" target=\"_blank\" rel=\"noopener\">Spotify Developer Dashboard</a>.",
            "Wybierz swoją aplikację lub utwórz nową.",
            "Przejdź do <strong>Settings</strong> → <strong>Redirect URIs</strong>.",
            "Usuń wpisy z <code>localhost</code> — Spotify nie uznaje ich za bezpieczne.",
            "Dodaj dokładnie ten Redirect URI i kliknij <strong>Save</strong>:",
            "Jeśli nadal masz błąd redirect_uri, dodaj też <code>http://127.0.0.1/callback</code> i zapisz ponownie.",
            "Krok 2 — Wprowadź dane",
            "Wklej Client ID i Client Secret z tej samej aplikacji Spotify.",
            "Client ID",
            "Client Secret",
            "Połącz konto Spotify",
            "Błędy redirect_uri zwykle oznaczają brak URI w dashboardzie, brak kliknięcia Save lub Client ID z innej aplikacji.",
            "Język"
        ));
        register("de", "Deutsch", pack(
            "Spotify für Hytale",
            "Verbinde dein Spotify-Konto, um den aktuellen Titel im Spiel anzuzeigen.",
            "Ungültige oder abgelaufene Sitzung. Kehre zu Hytale zurück und klicke erneut auf <strong>Setup Spotify</strong>.",
            "Client ID und Client Secret sind erforderlich.",
            "Sitzung abgelaufen. Kehre zu Hytale zurück und klicke erneut auf <strong>Setup Spotify</strong>.",
            "Spotify-Autorisierung fehlgeschlagen",
            "Ungültiger Callback. Kehre zu Hytale zurück und versuche es erneut.",
            "Autorisierungscode konnte nicht in Tokens umgewandelt werden. Überprüfe Client ID, Secret und Redirect URI im Spotify Dashboard.",
            "Token-Fehler",
            "Verbunden!",
            "Kehre zu Hytale zurück. Das Now-Playing-Overlay sollte automatisch erscheinen.",
            "Einrichtung fehlgeschlagen",
            "Erwartete Redirect URI",
            "Schritt 1 — Spotify Developer Dashboard",
            "Öffne das <a href=\"https://developer.spotify.com/dashboard\" target=\"_blank\" rel=\"noopener\">Spotify Developer Dashboard</a>.",
            "Wähle deine App oder erstelle eine neue.",
            "Gehe zu <strong>Settings</strong> → <strong>Redirect URIs</strong>.",
            "Lösche alle <code>localhost</code>-Einträge — Spotify akzeptiert sie nicht mehr als sicher.",
            "Füge genau diese Redirect URI hinzu und klicke auf <strong>Save</strong>:",
            "Bei redirect-Fehlern füge auch <code>http://127.0.0.1/callback</code> hinzu und speichere erneut.",
            "Schritt 2 — Zugangsdaten eingeben",
            "Füge Client ID und Client Secret derselben Spotify-App ein.",
            "Client ID",
            "Client Secret",
            "Spotify-Konto verbinden",
            "redirect_uri-Fehler bedeuten meist eine fehlende URI im Dashboard, fehlendes Speichern oder eine Client ID aus einer anderen App.",
            "Sprache"
        ));
        register("fr", "Français", pack(
            "Spotify pour Hytale",
            "Connectez votre compte Spotify pour afficher le titre en cours dans le jeu.",
            "Session invalide ou expirée. Retournez dans Hytale et cliquez à nouveau sur <strong>Setup Spotify</strong>.",
            "Client ID et Client Secret sont requis.",
            "Session expirée. Retournez dans Hytale et cliquez à nouveau sur <strong>Setup Spotify</strong>.",
            "Échec de l'autorisation Spotify",
            "Callback invalide. Retournez dans Hytale et réessayez.",
            "Impossible d'échanger le code d'autorisation. Vérifiez Client ID, Secret et Redirect URI dans le Spotify Dashboard.",
            "Erreur de jeton",
            "Connecté !",
            "Retournez dans Hytale. L'overlay now-playing devrait apparaître automatiquement.",
            "Échec de la configuration",
            "Redirect URI attendue",
            "Étape 1 — Spotify Developer Dashboard",
            "Ouvrez le <a href=\"https://developer.spotify.com/dashboard\" target=\"_blank\" rel=\"noopener\">Spotify Developer Dashboard</a>.",
            "Sélectionnez votre application ou créez-en une nouvelle.",
            "Allez dans <strong>Settings</strong> → <strong>Redirect URIs</strong>.",
            "Supprimez les entrées <code>localhost</code> — Spotify ne les accepte plus comme sécurisées.",
            "Ajoutez exactement cette Redirect URI puis cliquez sur <strong>Save</strong> :",
            "En cas d'erreur redirect_uri, ajoutez aussi <code>http://127.0.0.1/callback</code> et enregistrez à nouveau.",
            "Étape 2 — Saisir les identifiants",
            "Collez votre Client ID et Client Secret de la même application Spotify.",
            "Client ID",
            "Client Secret",
            "Connecter le compte Spotify",
            "Les erreurs redirect_uri signifient généralement une URI manquante, l'absence de Save, ou un Client ID d'une autre application.",
            "Langue"
        ));
        register("es", "Español", pack(
            "Spotify para Hytale",
            "Conecta tu cuenta de Spotify para mostrar la canción actual en el juego.",
            "Sesión inválida o expirada. Vuelve a Hytale y haz clic en <strong>Setup Spotify</strong> de nuevo.",
            "Client ID y Client Secret son obligatorios.",
            "Sesión expirada. Vuelve a Hytale y haz clic en <strong>Setup Spotify</strong> de nuevo.",
            "Error de autorización de Spotify",
            "Callback inválido. Vuelve a Hytale e inténtalo de nuevo.",
            "No se pudo intercambiar el código de autorización. Verifica Client ID, Secret y Redirect URI en el Spotify Dashboard.",
            "Error de token",
            "¡Conectado!",
            "Vuelve a Hytale. El overlay de reproducción debería aparecer automáticamente.",
            "Error de configuración",
            "Redirect URI esperada",
            "Paso 1 — Spotify Developer Dashboard",
            "Abre el <a href=\"https://developer.spotify.com/dashboard\" target=\"_blank\" rel=\"noopener\">Spotify Developer Dashboard</a>.",
            "Selecciona tu app o crea una nueva.",
            "Ve a <strong>Settings</strong> → <strong>Redirect URIs</strong>.",
            "Elimina entradas con <code>localhost</code> — Spotify ya no las acepta como seguras.",
            "Añade exactamente esta Redirect URI y haz clic en <strong>Save</strong>:",
            "Si sigue fallando, añade también <code>http://127.0.0.1/callback</code> y guarda de nuevo.",
            "Paso 2 — Introduce credenciales",
            "Pega tu Client ID y Client Secret de la misma app de Spotify.",
            "Client ID",
            "Client Secret",
            "Conectar cuenta de Spotify",
            "Los errores redirect_uri suelen significar URI faltante, no guardar, o Client ID de otra app.",
            "Idioma"
        ));
    }

    private SpotifySetupWebI18n() {}

    @Nonnull
    public static String normalizeLang(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_LANG;
        }
        String code = raw.trim().toLowerCase(Locale.ROOT);
        if (code.length() >= 2 && code.contains("-")) {
            code = code.substring(0, code.indexOf('-'));
        }
        return PACKS.containsKey(code) ? code : DEFAULT_LANG;
    }

    @Nonnull
    public static LangPack get(@Nullable String raw) {
        return PACKS.get(normalizeLang(raw));
    }

    @Nonnull
    public static Set<String> supportedLangCodes() {
        return PACKS.keySet();
    }

    @Nonnull
    public static String languageSwitcher(@Nonnull String sessionId, @Nonnull String currentLang) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"lang-bar\"><span class=\"lang-label\">")
            .append(htmlEscape(get(currentLang).languageLabel()))
            .append("</span>");
        for (Map.Entry<String, LangPack> entry : PACKS.entrySet()) {
            String code = entry.getKey();
            String label = entry.getValue().displayName();
            String cls = code.equals(currentLang) ? "lang-btn active" : "lang-btn";
            sb.append("<a class=\"")
                .append(cls)
                .append("\" href=\"")
                .append(SpotifyConstants.SETUP_PATH)
                .append("?session=")
                .append(urlEscape(sessionId))
                .append("&lang=")
                .append(code)
                .append("\">")
                .append(htmlEscape(label))
                .append("</a>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static void register(@Nonnull String code, @Nonnull String displayName, @Nonnull LangPack pack) {
        PACKS.put(code, pack.withDisplayName(displayName));
    }

    @Nonnull
    private static LangPack pack(String... values) {
        return new LangPack(values);
    }

    @Nonnull
    private static String htmlEscape(@Nonnull String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Nonnull
    private static String urlEscape(@Nonnull String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    public record LangPack(
        String pageTitle,
        String subtitle,
        String invalidSession,
        String credentialsRequired,
        String sessionExpired,
        String authFailedPrefix,
        String invalidCallback,
        String tokenExchangeFailed,
        String tokenErrorPrefix,
        String connectedTitle,
        String connectedBody,
        String errorTitle,
        String expectedRedirectLabel,
        String step1Title,
        String step1a,
        String step1b,
        String step1c,
        String step1d,
        String step1e,
        String step1f,
        String step2Title,
        String step2a,
        String clientIdLabel,
        String clientSecretLabel,
        String connectButton,
        String redirectHint,
        String languageLabel,
        String displayName
    ) {
        LangPack(String... values) {
            this(
                values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8],
                values[9], values[10], values[11], values[12], values[13], values[14], values[15], values[16], values[17],
                values[18], values[19], values[20], values[21], values[22], values[23], values[24], values[25], values[26],
                values[0]
            );
        }

        @Nonnull
        LangPack withDisplayName(@Nonnull String name) {
            return new LangPack(
                pageTitle, subtitle, invalidSession, credentialsRequired, sessionExpired, authFailedPrefix,
                invalidCallback, tokenExchangeFailed, tokenErrorPrefix, connectedTitle, connectedBody, errorTitle,
                expectedRedirectLabel, step1Title, step1a, step1b, step1c, step1d, step1e, step1f, step2Title, step2a,
                clientIdLabel, clientSecretLabel, connectButton, redirectHint, languageLabel, name
            );
        }
    }
}
