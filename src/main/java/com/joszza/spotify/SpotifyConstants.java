package com.joszza.spotify;

public final class SpotifyConstants {
    public static final String HUD_KEY = "MusicDisplay";
    public static final String COMMAND_ROOT = "musicdisplay";
    public static final String COMMAND_ALIAS_SHORT = "msd";

    /** Must match Spotify Dashboard Redirect URI exactly (localhost ≠ 127.0.0.1). */
    public static final String SETUP_HOST = "127.0.0.1";
    public static final int SETUP_PORT = 8888;
    public static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    public static final String SETUP_PATH = "/setup";
    public static final String SUBMIT_PATH = "/submit";
    public static final String CALLBACK_PATH = "/callback";

    public static final String SPOTIFY_SCOPES =
        "user-read-currently-playing user-read-playback-state user-modify-playback-state";
    public static final long OAUTH_SESSION_TTL_MS = 15L * 60L * 1000L;

    public static final String ALBUM_ART_SLOT_PATH = "UI/Custom/Spotify/CoverSlot.png";
    public static final String ALBUM_ART_FALLBACK = "UI/Custom/Pages/Memories/MissingIcon.png";
    public static final String ALBUM_ART_PLACEHOLDER = ALBUM_ART_SLOT_PATH;

    private SpotifyConstants() {}
}
