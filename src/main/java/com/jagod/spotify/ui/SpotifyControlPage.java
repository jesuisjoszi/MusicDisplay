package com.jagod.spotify.ui;

import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.oauth.SpotifyOAuthService;
import com.jagod.spotify.service.SpotifyPollingService;
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

/** In-game control panel with browser-based Spotify OAuth setup and HUD settings. */
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
        commandBuilder.set("#HintLine.TextSpans", Message.translation("spotify.spotify.ui.panel.hint"));
        commandBuilder.set("#SetupSpotifyButton.TextSpans", Message.translation("spotify.spotify.ui.panel.setup"));
        commandBuilder.set("#SetupUrlLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.setupUrlHint"));
        commandBuilder.set("#SetupUrlSection.Visible", false);
        commandBuilder.set("#SetupUrlInput.Value", "");
        commandBuilder.set("#HudSettingsTitle.TextSpans", Message.translation("spotify.spotify.ui.panel.hudSettings"));
        commandBuilder.set("#PositionLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.position"));
        commandBuilder.set("#SizeLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.size"));
        commandBuilder.set("#ToggleProgressButton.TextSpans", Message.translation("spotify.spotify.ui.panel.toggleProgress"));
        commandBuilder.set("#OffsetXLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.offsetX"));
        commandBuilder.set("#OffsetYLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.offsetY"));
        commandBuilder.set("#TextColorsLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.textColors"));
        commandBuilder.set("#TrackColorLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.trackColor"));
        commandBuilder.set("#ArtistColorLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.artistColor"));
        commandBuilder.set("#TimeColorLabel.TextSpans", Message.translation("spotify.spotify.ui.panel.timeColor"));
        commandBuilder.set("#ApplyHudSettingsButton.TextSpans", Message.translation("spotify.spotify.ui.panel.applyHud"));
        commandBuilder.set("#CloseButton.TextSpans", Message.translation("spotify.spotify.ui.panel.close"));
        commandBuilder.set("#PosBottomLeft.TextSpans", Message.translation("spotify.spotify.ui.panel.posBottomLeft"));
        commandBuilder.set("#PosBottomRight.TextSpans", Message.translation("spotify.spotify.ui.panel.posBottomRight"));
        commandBuilder.set("#PosTopLeft.TextSpans", Message.translation("spotify.spotify.ui.panel.posTopLeft"));
        commandBuilder.set("#PosTopRight.TextSpans", Message.translation("spotify.spotify.ui.panel.posTopRight"));
        commandBuilder.set("#StatusLabel.TextSpans", Message.raw(""));

        applyConnectionState(commandBuilder, state);
        applyHudState(commandBuilder, state);
        applyCustomizationState(commandBuilder, state);

        if (state != null) {
            applyHudInputs(commandBuilder, state);
        }

        bind(eventBuilder, "Setup", "#SetupSpotifyButton");
        bind(eventBuilder, "ToggleHud", "#ToggleHudButton");
        bind(eventBuilder, "PosBottomLeft", "#PosBottomLeft");
        bind(eventBuilder, "PosBottomRight", "#PosBottomRight");
        bind(eventBuilder, "PosTopLeft", "#PosTopLeft");
        bind(eventBuilder, "PosTopRight", "#PosTopRight");
        bind(eventBuilder, "SizeSmall", "#SizeSmall");
        bind(eventBuilder, "SizeMedium", "#SizeMedium");
        bind(eventBuilder, "SizeLarge", "#SizeLarge");
        bind(eventBuilder, "ToggleProgress", "#ToggleProgressButton");
        bindApplyHud(eventBuilder);
        bind(eventBuilder, "Close", "#CloseButton");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) {
            return;
        }
        switch (data.action) {
            case "Close" -> close();
            case "Setup" -> startBrowserSetup(ref, store);
            case "ToggleHud" -> toggleHud(ref, store);
            case "PosBottomLeft" -> setPosition(ref, store, SpotifyHudPosition.BOTTOM_LEFT);
            case "PosBottomRight" -> setPosition(ref, store, SpotifyHudPosition.BOTTOM_RIGHT);
            case "PosTopLeft" -> setPosition(ref, store, SpotifyHudPosition.TOP_LEFT);
            case "PosTopRight" -> setPosition(ref, store, SpotifyHudPosition.TOP_RIGHT);
            case "SizeSmall" -> setScale(ref, store, SpotifyHudScale.SMALL);
            case "SizeMedium" -> setScale(ref, store, SpotifyHudScale.MEDIUM);
            case "SizeLarge" -> setScale(ref, store, SpotifyHudScale.LARGE);
            case "ToggleProgress" -> toggleProgress(ref, store);
            case "ApplyHud" -> applyHudSettings(
                ref,
                store,
                data.offsetX,
                data.offsetY,
                data.trackColor,
                data.artistColor,
                data.timeColor
            );
            default -> {}
        }
    }

    private void startBrowserSetup(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (playerRef == null || uuidComponent == null) {
            return;
        }
        SpotifyOAuthService.SetupBeginResult result = SpotifyOAuthService.beginSetup(playerRef, uuidComponent);
        if (result != null) {
            pushSetupUrl(result.url());
            pushStatus(
                Message.translation(
                    result.copiedToClipboard()
                        ? "spotify.spotify.oauth.copiedToClipboard"
                        : "spotify.spotify.oauth.copyFailed"
                )
            );
        }
    }

    private void pushSetupUrl(@Nonnull String url) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#SetupUrlSection.Visible", true);
        builder.set("#SetupUrlInput.Value", url);
        sendUpdate(builder, null, false);
    }

    private void toggleHud(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (state == null || player == null || playerRef == null) {
            return;
        }

        boolean enabled = !state.isHudEnabled();
        state.setHudEnabled(enabled);
        if (enabled) {
            playerRef.sendMessage(Message.translation("spotify.spotify.command.enabled"));
            SpotifyPollingService.refreshPlayerNow(ref, store);
        } else {
            SpotifyHudSupport.removeHud(player, playerRef);
            playerRef.sendMessage(Message.translation("spotify.spotify.command.disabled"));
        }
        pushPanelState(ref, store);
    }

    private void setPosition(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SpotifyHudPosition position) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudPosition(position);
        refreshHud(ref, store);
        pushPanelState(ref, store);
    }

    private void setScale(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull SpotifyHudScale scale) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudScale(scale);
        refreshHud(ref, store);
        pushPanelState(ref, store);
    }

    private void toggleProgress(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        state.setHudProgressVisible(!state.isHudProgressVisible());
        refreshHud(ref, store);
        pushPanelState(ref, store);
    }

    private static void refreshHud(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state != null && state.isHudEnabled()) {
            SpotifyPollingService.refreshPlayerNow(ref, store);
        }
    }

    private void applyHudSettings(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nullable String offsetXRaw,
        @Nullable String offsetYRaw,
        @Nullable String trackColorRaw,
        @Nullable String artistColorRaw,
        @Nullable String timeColorRaw
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        int offsetX = parseOffset(offsetXRaw, state.getHudOffsetX());
        int offsetY = parseOffset(offsetYRaw, state.getHudOffsetY());
        state.setHudOffset(offsetX, offsetY);

        String trackColor = parseColor(trackColorRaw);
        String artistColor = parseColor(artistColorRaw);
        String timeColor = parseColor(timeColorRaw);
        if (trackColor == INVALID_COLOR || artistColor == INVALID_COLOR || timeColor == INVALID_COLOR) {
            pushStatus(Message.translation("spotify.spotify.ui.panel.invalidColor"));
            pushPanelState(ref, store);
            return;
        }
        state.setHudTextColors(
            trackColor == RESET_COLOR ? null : trackColor,
            artistColor == RESET_COLOR ? null : artistColor,
            timeColor == RESET_COLOR ? null : timeColor
        );
        refreshHud(ref, store);
        pushStatus(Message.translation("spotify.spotify.ui.panel.hudApplied"));
        pushPanelState(ref, store);
    }

    private void pushPanelState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        UICommandBuilder builder = new UICommandBuilder();
        applyConnectionState(builder, state);
        applyHudState(builder, state);
        applyCustomizationState(builder, state);
        if (state != null) {
            applyHudInputs(builder, state);
        }
        sendUpdate(builder, null, false);
    }

    private static void applyHudInputs(@Nonnull UICommandBuilder builder, @Nonnull SpotifyPlayerComponent state) {
        builder.set("#OffsetXInput.Value", String.valueOf(state.getHudOffsetX()));
        builder.set("#OffsetYInput.Value", String.valueOf(state.getHudOffsetY()));
        builder.set("#TrackColorInput.Value", SpotifyHudColors.resolve(state.getHudTrackColor(), SpotifyHudColors.DEFAULT_TRACK));
        builder.set("#ArtistColorInput.Value", SpotifyHudColors.resolve(state.getHudArtistColor(), SpotifyHudColors.DEFAULT_ARTIST));
        builder.set("#TimeColorInput.Value", SpotifyHudColors.resolve(state.getHudTimeColor(), SpotifyHudColors.DEFAULT_TIME));
    }

    private void pushStatus(@Nonnull Message message) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#StatusLabel.TextSpans", message);
        sendUpdate(builder, null, false);
    }

    private static void applyConnectionState(@Nonnull UICommandBuilder builder, @Nullable SpotifyPlayerComponent state) {
        if (state != null && state.hasCredentials()) {
            builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.connected"));
        } else {
            builder.set("#StatusLine.TextSpans", Message.translation("spotify.spotify.ui.panel.notConnected"));
        }
    }

    private static void applyHudState(@Nonnull UICommandBuilder builder, @Nullable SpotifyPlayerComponent state) {
        if (state != null && state.isHudEnabled()) {
            builder.set("#HudStateLine.TextSpans", Message.translation("spotify.spotify.ui.panel.hudOn"));
            builder.set("#ToggleHudButton.TextSpans", Message.translation("spotify.spotify.ui.panel.toggleHudOff"));
        } else {
            builder.set("#HudStateLine.TextSpans", Message.translation("spotify.spotify.ui.panel.hudOff"));
            builder.set("#ToggleHudButton.TextSpans", Message.translation("spotify.spotify.ui.panel.toggleHudOn"));
        }
    }

    private static void applyCustomizationState(@Nonnull UICommandBuilder builder, @Nullable SpotifyPlayerComponent state) {
        if (state == null) {
            return;
        }
        if (state.isHudProgressVisible()) {
            builder.set(
                "#ToggleProgressButton.TextSpans",
                Message.translation("spotify.spotify.ui.panel.toggleProgressOff")
            );
        } else {
            builder.set(
                "#ToggleProgressButton.TextSpans",
                Message.translation("spotify.spotify.ui.panel.toggleProgressOn")
            );
        }
        builder.set(
            "#SizeSmall.TextSpans",
            Message.translation(
                state.getHudScale() == SpotifyHudScale.SMALL
                    ? "spotify.spotify.ui.panel.sizeSmallSelected"
                    : "spotify.spotify.ui.panel.sizeSmall"
            )
        );
        builder.set(
            "#SizeMedium.TextSpans",
            Message.translation(
                state.getHudScale() == SpotifyHudScale.MEDIUM
                    ? "spotify.spotify.ui.panel.sizeMediumSelected"
                    : "spotify.spotify.ui.panel.sizeMedium"
            )
        );
        builder.set(
            "#SizeLarge.TextSpans",
            Message.translation(
                state.getHudScale() == SpotifyHudScale.LARGE
                    ? "spotify.spotify.ui.panel.sizeLargeSelected"
                    : "spotify.spotify.ui.panel.sizeLarge"
            )
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

    private static void bindApplyHud(@Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ApplyHudSettingsButton",
            new EventData()
                .append("Action", "ApplyHud")
                .append("@OffsetX", "#OffsetXInput.Value")
                .append("@OffsetY", "#OffsetYInput.Value")
                .append("@TrackColor", "#TrackColorInput.Value")
                .append("@ArtistColor", "#ArtistColorInput.Value")
                .append("@TimeColor", "#TimeColorInput.Value"),
            false
        );
    }

    private static final String INVALID_COLOR = "__invalid__";
    private static final String RESET_COLOR = "__reset__";

    @Nonnull
    private static String parseColor(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return RESET_COLOR;
        }
        String normalized = SpotifyHudColors.normalize(raw.trim());
        if (normalized == null) {
            return INVALID_COLOR;
        }
        return normalized;
    }

    private static int parseOffset(@Nullable String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(8, Math.min(200, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .append(new KeyedCodec<>("@OffsetX", Codec.STRING), (d, v) -> d.offsetX = v, d -> d.offsetX)
            .add()
            .append(new KeyedCodec<>("@OffsetY", Codec.STRING), (d, v) -> d.offsetY = v, d -> d.offsetY)
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
        private String offsetX;
        @Nullable
        private String offsetY;
        @Nullable
        private String trackColor;
        @Nullable
        private String artistColor;
        @Nullable
        private String timeColor;
    }
}
