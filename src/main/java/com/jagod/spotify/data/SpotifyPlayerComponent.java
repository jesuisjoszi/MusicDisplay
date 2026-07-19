package com.jagod.spotify.data;

import com.jagod.spotify.ui.MusicSource;
import com.jagod.spotify.ui.SpotifyHudLayout;
import com.jagod.spotify.ui.SpotifyHudPosition;
import com.jagod.spotify.ui.SpotifyHudScale;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Per-player Spotify credentials, token cache, and HUD preferences. */
public final class SpotifyPlayerComponent implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<SpotifyPlayerComponent> CODEC = BuilderCodec.builder(
            SpotifyPlayerComponent.class,
            SpotifyPlayerComponent::new
        )
        .append(new KeyedCodec<>("ClientId", Codec.STRING), (c, v) -> c.clientId = blankToNull(v), c -> c.clientId)
        .add()
        .append(new KeyedCodec<>("ClientSecret", Codec.STRING), (c, v) -> c.clientSecret = blankToNull(v), c -> c.clientSecret)
        .add()
        .append(new KeyedCodec<>("RefreshToken", Codec.STRING), (c, v) -> c.refreshToken = blankToNull(v), c -> c.refreshToken)
        .add()
        .append(new KeyedCodec<>("AccessToken", Codec.STRING), (c, v) -> c.accessToken = blankToNull(v), c -> c.accessToken)
        .add()
        .append(
            new KeyedCodec<>("AccessTokenExpiresAt", Codec.LONG),
            (c, v) -> c.accessTokenExpiresAt = v != null ? v : 0L,
            c -> c.accessTokenExpiresAt
        )
        .add()
        .append(new KeyedCodec<>("HudEnabled", Codec.BOOLEAN), (c, v) -> c.hudEnabled = v != null && v, c -> c.hudEnabled)
        .add()
        .append(new KeyedCodec<>("HudPosition", Codec.STRING), (c, v) -> c.hudPosition = SpotifyHudPosition.fromString(v), c -> c.hudPosition.name())
        .add()
        .append(new KeyedCodec<>("HudOffsetX", Codec.INTEGER), (c, v) -> c.hudOffsetX = v != null ? v : SpotifyHudLayout.DEFAULT_OFFSET, c -> c.hudOffsetX)
        .add()
        .append(new KeyedCodec<>("HudOffsetY", Codec.INTEGER), (c, v) -> c.hudOffsetY = v != null ? v : SpotifyHudLayout.DEFAULT_OFFSET, c -> c.hudOffsetY)
        .add()
        .append(new KeyedCodec<>("HudScale", Codec.STRING), (c, v) -> c.hudScale = SpotifyHudScale.fromString(v), c -> c.hudScale.name())
        .add()
        .append(new KeyedCodec<>("HudProgressVisible", Codec.BOOLEAN), (c, v) -> c.hudProgressVisible = v == null || v, c -> c.hudProgressVisible)
        .add()
        .append(new KeyedCodec<>("HudTrackColor", Codec.STRING), (c, v) -> c.hudTrackColor = blankToNull(v), c -> c.hudTrackColor)
        .add()
        .append(new KeyedCodec<>("HudArtistColor", Codec.STRING), (c, v) -> c.hudArtistColor = blankToNull(v), c -> c.hudArtistColor)
        .add()
        .append(new KeyedCodec<>("HudTimeColor", Codec.STRING), (c, v) -> c.hudTimeColor = blankToNull(v), c -> c.hudTimeColor)
        .add()
        .append(new KeyedCodec<>("MusicSource", Codec.STRING), (c, v) -> c.musicSource = MusicSource.fromString(v), c -> c.musicSource.name())
        .add()
        .append(new KeyedCodec<>("LastTrack", Codec.STRING), (c, v) -> c.lastTrack = blankToNull(v), c -> c.lastTrack)
        .add()
        .append(new KeyedCodec<>("LastArtist", Codec.STRING), (c, v) -> c.lastArtist = blankToNull(v), c -> c.lastArtist)
        .add()
        .append(new KeyedCodec<>("LastStatus", Codec.STRING), (c, v) -> c.lastStatus = blankToNull(v), c -> c.lastStatus)
        .add()
        .append(new KeyedCodec<>("GrantedScopes", Codec.STRING), (c, v) -> c.grantedScopes = blankToNull(v), c -> c.grantedScopes)
        .add()
        .build();

    @Nullable
    private static volatile ComponentType<EntityStore, SpotifyPlayerComponent> componentType;

    @Nullable
    private String clientId;
    @Nullable
    private String clientSecret;
    @Nullable
    private String refreshToken;
    @Nullable
    private String accessToken;
    private long accessTokenExpiresAt;
    private boolean hudEnabled;
    @Nonnull
    private SpotifyHudPosition hudPosition = SpotifyHudPosition.BOTTOM_LEFT;
    private int hudOffsetX = SpotifyHudLayout.DEFAULT_OFFSET;
    private int hudOffsetY = SpotifyHudLayout.DEFAULT_OFFSET;
    @Nonnull
    private SpotifyHudScale hudScale = SpotifyHudScale.MEDIUM;
    private boolean hudProgressVisible = true;
    @Nullable
    private String hudTrackColor;
    @Nullable
    private String hudArtistColor;
    @Nullable
    private String hudTimeColor;
    @Nonnull
    private MusicSource musicSource = MusicSource.SPOTIFY;
    @Nullable
    private String lastTrack;
    @Nullable
    private String lastArtist;
    @Nullable
    private String lastStatus;
    @Nullable
    private String grantedScopes;

    public static void register(@Nonnull ComponentRegistryProxy<EntityStore> registry) {
        componentType = registry.registerComponent(SpotifyPlayerComponent.class, "SpotifyPlayer", CODEC);
    }

    @Nonnull
    public static ComponentType<EntityStore, SpotifyPlayerComponent> getComponentType() {
        ComponentType<EntityStore, SpotifyPlayerComponent> type = componentType;
        if (type == null) {
            throw new IllegalStateException("SpotifyPlayerComponent not registered");
        }
        return type;
    }

    public static void ensurePresent(@Nonnull Ref<EntityStore> playerRef, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (commandBuffer.getComponent(playerRef, getComponentType()) == null) {
            commandBuffer.addComponent(playerRef, getComponentType(), new SpotifyPlayerComponent());
        }
    }

    @Nullable
    public String getClientId() {
        return clientId;
    }

    @Nullable
    public String getClientSecret() {
        return clientSecret;
    }

    @Nullable
    public String getRefreshToken() {
        return refreshToken;
    }

    @Nullable
    public String getAccessToken() {
        return accessToken;
    }

    public long getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    @Nonnull
    public SpotifyHudPosition getHudPosition() {
        return hudPosition;
    }

    public int getHudOffsetX() {
        return hudOffsetX;
    }

    public int getHudOffsetY() {
        return hudOffsetY;
    }

    @Nonnull
    public SpotifyHudScale getHudScale() {
        return hudScale;
    }

    public boolean isHudProgressVisible() {
        return hudProgressVisible;
    }

    @Nullable
    public String getHudTrackColor() {
        return hudTrackColor;
    }

    @Nullable
    public String getHudArtistColor() {
        return hudArtistColor;
    }

    @Nullable
    public String getHudTimeColor() {
        return hudTimeColor;
    }

    @Nonnull
    public MusicSource getMusicSource() {
        return musicSource;
    }

    public void setMusicSource(@Nonnull MusicSource musicSource) {
        this.musicSource = musicSource;
    }

    public boolean canShowHud() {
        if (!hudEnabled) {
            return false;
        }
        if (musicSource.isWindows()) {
            return true;
        }
        return hasCredentials();
    }

    @Nullable
    public String getLastTrack() {
        return lastTrack;
    }

    @Nullable
    public String getLastArtist() {
        return lastArtist;
    }

    @Nullable
    public String getLastStatus() {
        return lastStatus;
    }

    @Nullable
    public String getGrantedScopes() {
        return grantedScopes;
    }

    public boolean hasPlaybackScope() {
        return grantedScopes != null && grantedScopes.contains("user-modify-playback-state");
    }

    public void setGrantedScopes(@Nullable String grantedScopes) {
        this.grantedScopes = blankToNull(grantedScopes);
    }

    public boolean hasCredentials() {
        return clientId != null && clientSecret != null && refreshToken != null;
    }

    public void setCredentials(@Nonnull String clientId, @Nonnull String clientSecret, @Nonnull String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.accessToken = null;
        this.accessTokenExpiresAt = 0L;
        this.grantedScopes = null;
        this.hudEnabled = true;
    }

    public void setAccessToken(@Nonnull String accessToken, long expiresAtEpochMs) {
        this.accessToken = accessToken;
        this.accessTokenExpiresAt = expiresAtEpochMs;
    }

    public void clearAccessToken() {
        this.accessToken = null;
        this.accessTokenExpiresAt = 0L;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public void setHudPosition(@Nonnull SpotifyHudPosition hudPosition) {
        this.hudPosition = hudPosition;
    }

    public void setHudOffset(int offsetX, int offsetY) {
        this.hudOffsetX = offsetX;
        this.hudOffsetY = offsetY;
    }

    public void setHudScale(@Nonnull SpotifyHudScale hudScale) {
        this.hudScale = hudScale;
    }

    public void setHudProgressVisible(boolean hudProgressVisible) {
        this.hudProgressVisible = hudProgressVisible;
    }

    public void setHudTextColors(@Nullable String trackColor, @Nullable String artistColor, @Nullable String timeColor) {
        this.hudTrackColor = trackColor;
        this.hudArtistColor = artistColor;
        this.hudTimeColor = timeColor;
    }

    public void setNowPlaying(@Nullable String track, @Nullable String artist, @Nonnull String status) {
        this.lastTrack = track;
        this.lastArtist = artist;
        this.lastStatus = status;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        SpotifyPlayerComponent copy = new SpotifyPlayerComponent();
        copy.clientId = clientId;
        copy.clientSecret = clientSecret;
        copy.refreshToken = refreshToken;
        copy.accessToken = accessToken;
        copy.accessTokenExpiresAt = accessTokenExpiresAt;
        copy.hudEnabled = hudEnabled;
        copy.hudPosition = hudPosition;
        copy.hudOffsetX = hudOffsetX;
        copy.hudOffsetY = hudOffsetY;
        copy.hudScale = hudScale;
        copy.hudProgressVisible = hudProgressVisible;
        copy.hudTrackColor = hudTrackColor;
        copy.hudArtistColor = hudArtistColor;
        copy.hudTimeColor = hudTimeColor;
        copy.musicSource = musicSource;
        copy.lastTrack = lastTrack;
        copy.lastArtist = lastArtist;
        copy.lastStatus = lastStatus;
        copy.grantedScopes = grantedScopes;
        return copy;
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
