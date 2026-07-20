package com.joszza.spotify.ui;

import com.joszza.spotify.data.SpotifyPlayerComponent;
import com.joszza.spotify.oauth.SpotifyOAuthService;
import com.joszza.spotify.service.SpotifyPollingService;
import com.joszza.spotify.windows.WindowsMediaManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Settings panel: source, connect, HUD options. Playback lives in {@link SpotifyControlsPage}. */
public final class SpotifyControlPage extends InteractiveCustomUIPage<SpotifyControlPage.PageData> {
    private boolean templateAppended;

    public SpotifyControlPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("Spotify/SpotifyControlPage.ui");
            templateAppended = true;
        }

        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());

        commandBuilder.set("#PanelTitle.TextSpans", Message.translation("spotify.spotify.ui.panel.title"));
        commandBuilder.set("#SourceLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.source"));
        commandBuilder.set("#SetupSpotifyButton.TextSpans", Message.translation("spotify.spotify.ui.panel.setup"));
        commandBuilder.set("#SetupUrlLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.setupUrlHint"));
        commandBuilder.set("#SetupUrlSection.Visible", false);
        commandBuilder.set("#SetupUrlInput.Value", "");
        commandBuilder.set("#DisplayLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.display"));
        commandBuilder.set("#HudCheckLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.hudCheck"));
        commandBuilder.set("#ProgressCheckLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.progressCheck"));
        commandBuilder.set("#TrackNotifyCheckLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.trackNotifyCheck"));
        commandBuilder.set("#PositionLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.position"));
        commandBuilder.set("#SizeLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.size"));
        commandBuilder.set("#PosBottomLeft.TextSpans", Message.translation("spotify.spotify.ui.panel.posBottomLeft"));
        commandBuilder.set("#PosBottomRight.TextSpans", Message.translation("spotify.spotify.ui.panel.posBottomRight"));
        commandBuilder.set("#PosTopLeft.TextSpans", Message.translation("spotify.spotify.ui.panel.posTopLeft"));
        commandBuilder.set("#PosTopRight.TextSpans", Message.translation("spotify.spotify.ui.panel.posTopRight"));
        commandBuilder.set("#SizeSmall.TextSpans", Message.translation("spotify.spotify.ui.panel.sizeSmall"));
        commandBuilder.set("#SizeMedium.TextSpans", Message.translation("spotify.spotify.ui.panel.sizeMedium"));
        commandBuilder.set("#SizeLarge.TextSpans", Message.translation("spotify.spotify.ui.panel.sizeLarge"));
        commandBuilder.set("#ColorsLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.colors"));
        commandBuilder.set("#ColorTrackLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.colorTrack"));
        commandBuilder.set("#ColorArtistLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.colorArtist"));
        commandBuilder.set("#ColorTimeLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.colorTime"));
        commandBuilder.set("#ApplyColorsButton.TextSpans", Message.translation("spotify.spotify.ui.panel.applyColors"));
        commandBuilder.set("#ResetColorsButton.TextSpans", Message.translation("spotify.spotify.ui.panel.resetColors"));
        commandBuilder.set("#OpenControlsButton.TextSpans", Message.translation("spotify.spotify.ui.panel.openControls"));
        commandBuilder.set("#CloseButton.TextSpans", Message.translation("spotify.spotify.ui.panel.close"));

        applyState(commandBuilder, state);

        bind(eventBuilder, "Setup", "#SetupSpotifyButton");
        bind(eventBuilder, "SourceSpotify", "#SourceSpotify");
        bind(eventBuilder, "SourceWindows", "#SourceWindows");
        bind(eventBuilder, "PosBottomLeft", "#PosBottomLeft");
        bind(eventBuilder, "PosBottomRight", "#PosBottomRight");
        bind(eventBuilder, "PosTopLeft", "#PosTopLeft");
        bind(eventBuilder, "PosTopRight", "#PosTopRight");
        bind(eventBuilder, "SizeSmall", "#SizeSmall");
        bind(eventBuilder, "SizeMedium", "#SizeMedium");
        bind(eventBuilder, "SizeLarge", "#SizeLarge");
        bind(eventBuilder, "ResetColors", "#ResetColorsButton");
        bind(eventBuilder, "OpenControls", "#OpenControlsButton");
        bind(eventBuilder, "Close", "#CloseButton");
        bindCheck(eventBuilder, "ToggleHud", "#HudCheck");
        bindCheck(eventBuilder, "ToggleProgress", "#ProgressCheck");
        bindCheck(eventBuilder, "ToggleTrackNotify", "#TrackNotifyCheck");
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ApplyColorsButton",
            new EventData()
                .append("Action", "ApplyColors")
                .append("@TrackColor", "#ColorTrackInput.Value")
                .append("@ArtistColor", "#ColorArtistInput.Value")
                .append("@TimeColor", "#ColorTimeInput.Value"),
            false
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) {
            return;
        }
        switch (data.action) {
            case "Close" -> close();
            case "Setup" -> startBrowserSetup(ref, store);
            case "SourceSpotify" -> setMusicSource(ref, store, MusicSource.SPOTIFY);
            case "SourceWindows" -> setMusicSource(ref, store, MusicSource.WINDOWS);
            case "ToggleHud" -> setHudEnabled(ref, store, data.checked);
            case "PosBottomLeft" -> setPosition(ref, store, SpotifyHudPosition.BOTTOM_LEFT);
            case "PosBottomRight" -> setPosition(ref, store, SpotifyHudPosition.BOTTOM_RIGHT);
            case "PosTopLeft" -> setPosition(ref, store, SpotifyHudPosition.TOP_LEFT);
            case "PosTopRight" -> setPosition(ref, store, SpotifyHudPosition.TOP_RIGHT);
            case "SizeSmall" -> setScale(ref, store, SpotifyHudScale.SMALL);
            case "SizeMedium" -> setScale(ref, store, SpotifyHudScale.MEDIUM);
            case "SizeLarge" -> setScale(ref, store, SpotifyHudScale.LARGE);
            case "ToggleProgress" -> setProgressVisible(ref, store, data.checked);
            case "ToggleTrackNotify" -> setTrackNotify(ref, store, data.checked);
            case "ApplyColors" -> applyColors(ref, store, data.trackColor, data.artistColor, data.timeColor);
            case "ResetColors" -> resetColors(ref, store);
            case "OpenControls" -> openControls(ref, store);
            default -> {}
        }
    }

    private void openControls(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        close();
        player.getPageManager().openCustomPage(ref, store, new SpotifyControlsPage(playerRef));
        SpotifyPollingService.refreshPlayerNow(ref, store);
    }

    private void setMusicSource(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull MusicSource source
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setMusicSource(source);
        if (source.isWindows()) {
            state.setHudEnabled(true);
            if (WindowsMediaManager.isOsWindows() && !WindowsMediaManager.get().isReady()) {
                WindowsMediaManager.get().start();
            }
        }
        refreshHud(ref, store);
        pushState(ref, store);
    }

    private void startBrowserSetup(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (playerRef == null || uuidComponent == null) {
            return;
        }
        SpotifyOAuthService.SetupBeginResult result = SpotifyOAuthService.beginSetup(playerRef, uuidComponent);
        if (result == null) {
            return;
        }
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#SetupUrlSection.Visible", true);
        builder.set("#SetupUrlInput.Value", result.url());
        builder.set(
            "#StatusLine.TextSpans",
            Message.translation(
                result.copiedToClipboard()
                    ? "spotify.spotify.oauth.copiedToClipboard"
                    : "spotify.spotify.oauth.copyFailed"
            )
        );
        sendUpdate(builder, null, false);
    }

    private void setHudEnabled(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nullable Boolean checked
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (state == null || player == null || playerRef == null || checked == null) {
            return;
        }
        state.setHudEnabled(checked);
        if (!checked) {
            SpotifyHudSupport.removeHud(player, playerRef);
        }
        pushState(ref, store);
    }

    private void setPosition(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SpotifyHudPosition position
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudPosition(position);
        refreshHud(ref, store);
        pushState(ref, store);
    }

    private void setScale(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SpotifyHudScale scale
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudScale(scale);
        refreshHud(ref, store);
        pushState(ref, store);
    }

    private void setProgressVisible(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nullable Boolean checked
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null || checked == null) {
            return;
        }
        state.setHudProgressVisible(checked);
        refreshHud(ref, store);
        pushState(ref, store);
    }

    private void setTrackNotify(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nullable Boolean checked
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null || checked == null) {
            return;
        }
        state.setTrackChangeNotify(checked);
        pushState(ref, store);
    }

    private void applyColors(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nullable String trackRaw,
        @Nullable String artistRaw,
        @Nullable String timeRaw
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        String track = SpotifyHudColors.parseOrNull(trackRaw);
        String artist = SpotifyHudColors.parseOrNull(artistRaw);
        String time = SpotifyHudColors.parseOrNull(timeRaw);
        if ((trackRaw != null && !trackRaw.isBlank() && track == null)
            || (artistRaw != null && !artistRaw.isBlank() && artist == null)
            || (timeRaw != null && !timeRaw.isBlank() && time == null)) {
            UICommandBuilder builder = new UICommandBuilder();
            builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.colorsInvalid"));
            sendUpdate(builder, null, false);
            return;
        }
        state.setHudTextColors(
            blankMeansKeep(trackRaw, track, state.getHudTrackColor()),
            blankMeansKeep(artistRaw, artist, state.getHudArtistColor()),
            blankMeansKeep(timeRaw, time, state.getHudTimeColor())
        );
        pushState(ref, store);
    }

    @Nullable
    private static String blankMeansKeep(
        @Nullable String raw,
        @Nullable String parsed,
        @Nullable String previous
    ) {
        if (raw == null || raw.isBlank()) {
            return previous;
        }
        return parsed;
    }

    private void resetColors(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudTextColors(null, null, null);
        pushState(ref, store);
    }

    private static void refreshHud(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // Deferred until the settings page closes — live HUD updates kick the client.
    }

    private void pushState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        UICommandBuilder builder = new UICommandBuilder();
        applyState(builder, state);
        sendUpdate(builder, null, false);
    }

    private static void applyState(@Nonnull UICommandBuilder builder, @Nullable SpotifyPlayerComponent state) {
        MusicSource source = state != null ? state.getMusicSource() : MusicSource.SPOTIFY;

        if (source.isWindows()) {
            if (WindowsMediaManager.get().isReady()) {
                builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.windowsReady"));
            } else {
                builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.windowsNotReady"));
            }
        } else if (state != null && state.hasCredentials()) {
            builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.connected"));
        } else {
            builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.notConnected"));
        }

        builder.set("#SourceSpotify.TextSpans", Message.translation("spotify.spotify.ui.panel.sourceSpotify"));
        builder.set("#SourceWindows.TextSpans", Message.translation("spotify.spotify.ui.panel.sourceWindows"));
        builder.set("#SourceSpotify.Disabled", source == MusicSource.SPOTIFY);
        builder.set("#SourceWindows.Disabled", source == MusicSource.WINDOWS);
        builder.set("#SetupSpotifyButton.Visible", source == MusicSource.SPOTIFY);

        boolean hudOn = state != null && state.isHudEnabled();
        boolean progressOn = state == null || state.isHudProgressVisible();
        boolean notifyOn = state == null || state.isTrackChangeNotify();
        builder.set("#HudCheck.Value", hudOn);
        builder.set("#ProgressCheck.Value", progressOn);
        builder.set("#TrackNotifyCheck.Value", notifyOn);

        SpotifyHudPosition pos = state != null ? state.getHudPosition() : SpotifyHudPosition.BOTTOM_LEFT;
        builder.set("#PosBottomLeft.Disabled", pos == SpotifyHudPosition.BOTTOM_LEFT);
        builder.set("#PosBottomRight.Disabled", pos == SpotifyHudPosition.BOTTOM_RIGHT);
        builder.set("#PosTopLeft.Disabled", pos == SpotifyHudPosition.TOP_LEFT);
        builder.set("#PosTopRight.Disabled", pos == SpotifyHudPosition.TOP_RIGHT);

        SpotifyHudScale scale = state != null ? state.getHudScale() : SpotifyHudScale.MEDIUM;
        builder.set("#SizeSmall.Disabled", scale == SpotifyHudScale.SMALL);
        builder.set("#SizeMedium.Disabled", scale == SpotifyHudScale.MEDIUM);
        builder.set("#SizeLarge.Disabled", scale == SpotifyHudScale.LARGE);

        builder.set(
            "#ColorTrackInput.Value",
            state != null && state.getHudTrackColor() != null
                ? state.getHudTrackColor()
                : SpotifyHudColors.DEFAULT_TRACK
        );
        builder.set(
            "#ColorArtistInput.Value",
            state != null && state.getHudArtistColor() != null
                ? state.getHudArtistColor()
                : SpotifyHudColors.DEFAULT_ARTIST
        );
        builder.set(
            "#ColorTimeInput.Value",
            state != null && state.getHudTimeColor() != null
                ? state.getHudTimeColor()
                : SpotifyHudColors.DEFAULT_TIME
        );
    }

    private static void bind(@Nonnull UIEventBuilder eventBuilder, @Nonnull String action, @Nonnull String selector) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            selector,
            new EventData().append("Action", action),
            false
        );
    }

    private static void bindCheck(
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull String action,
        @Nonnull String selector
    ) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            selector,
            new EventData().append("Action", action).append("@Checked", selector + ".Value"),
            false
        );
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("@Checked", Codec.BOOLEAN), (d, v) -> d.checked = v, d -> d.checked)
            .add()
            .append(new KeyedCodec<>("@TrackColor", Codec.STRING), (d, v) -> d.trackColor = v, d -> d.trackColor)
            .add()
            .append(new KeyedCodec<>("@ArtistColor", Codec.STRING), (d, v) -> d.artistColor = v, d -> d.artistColor)
            .add()
            .append(new KeyedCodec<>("@TimeColor", Codec.STRING), (d, v) -> d.timeColor = v, d -> d.timeColor)
            .add()
            .build();

        @Nullable
        private String action;
        @Nullable
        private Boolean checked;
        @Nullable
        private String trackColor;
        @Nullable
        private String artistColor;
        @Nullable
        private String timeColor;
    }
}
