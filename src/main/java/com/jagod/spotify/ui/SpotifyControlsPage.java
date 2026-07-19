package com.jagod.spotify.ui;

import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.service.SpotifyControlsRegistry;
import com.jagod.spotify.service.SpotifyPlaybackSupport;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyControlsPage extends InteractiveCustomUIPage<SpotifyControlsPage.PageData> {
    private boolean templateAppended;

    public SpotifyControlsPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        SpotifyControlsRegistry.register(playerRef.getUuid(), this);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("Spotify/SpotifyControlsPage.ui");
            templateAppended = true;
        }

        commandBuilder.set("#PanelTitle.TextSpans", Message.translation("spotify.spotify.controls.title"));
        commandBuilder.set("#PrevButton.TextSpans", Message.translation("spotify.spotify.controls.prev"));
        commandBuilder.set("#NextButton.TextSpans", Message.translation("spotify.spotify.controls.next"));
        commandBuilder.set("#CloseButton.TextSpans", Message.translation("spotify.spotify.controls.close"));
        commandBuilder.set("#StatusLabel.TextSpans", Message.raw(""));

        applyDisplay(commandBuilder, store.getComponent(ref, SpotifyPlayerComponent.getComponentType()));

        bind(eventBuilder, "Prev", "#PrevButton");
        bind(eventBuilder, "PlayPause", "#PlayPauseButton");
        bind(eventBuilder, "Next", "#NextButton");
        bind(eventBuilder, "Close", "#CloseButton");
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) {
            return;
        }
        switch (data.action) {
            case "Close" -> close();
            case "Prev" -> runAction(ref, store, SpotifyPlaybackSupport.Action.PREVIOUS);
            case "Next" -> runAction(ref, store, SpotifyPlaybackSupport.Action.NEXT);
            case "PlayPause" -> runAction(ref, store, SpotifyPlaybackSupport.Action.TOGGLE);
            default -> {}
        }
    }

    @Override
    public void close() {
        SpotifyControlsRegistry.unregister(playerRef.getUuid());
        super.close();
    }

    public void refreshFromPolling(@Nonnull SpotifyPlayerComponent state) {
        UICommandBuilder builder = new UICommandBuilder();
        applyDisplay(builder, state);
        sendUpdate(builder, null, false);
    }

    private void runAction(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull SpotifyPlaybackSupport.Action action
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (state == null || playerRef == null) {
            return;
        }

        SpotifyPlaybackSupport.Result result = SpotifyPlaybackSupport.run(state, action);
        SpotifyPlaybackSupport.apply(playerRef, ref, store, state, result);
        if (result == SpotifyPlaybackSupport.Result.OK) {
            pushDisplay(ref, store);
        } else {
            pushStatus(resultMessage(result));
        }
    }

    private void pushDisplay(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        UICommandBuilder builder = new UICommandBuilder();
        applyDisplay(builder, state);
        sendUpdate(builder, null, false);
    }

    private void pushStatus(@Nonnull Message message) {
        UICommandBuilder builder = new UICommandBuilder();
        builder.set("#StatusLabel.TextSpans", message);
        sendUpdate(builder, null, false);
    }

    private static void applyDisplay(@Nonnull UICommandBuilder builder, @Nullable SpotifyPlayerComponent state) {
        if (state == null || !state.hasCredentials()) {
            builder.set("#TrackLine.TextSpans", Message.translation("spotify.spotify.hud.notConfigured"));
            builder.set("#ArtistLine.TextSpans", Message.raw(""));
            builder.set("#StatusLine.TextSpans", Message.raw(""));
            builder.set("#PlayPauseButton.TextSpans", Message.translation("spotify.spotify.controls.play"));
            return;
        }

        String track = state.getLastTrack();
        String artist = state.getLastArtist();
        String status = safe(state.getLastStatus(), "IDLE");

        if (track == null || track.isBlank()) {
            builder.set("#TrackLine.TextSpans", Message.translation("spotify.spotify.hud.idle"));
        } else {
            builder.set("#TrackLine.TextSpans", Message.raw(track));
        }
        if (artist == null || artist.isBlank()) {
            builder.set("#ArtistLine.TextSpans", Message.raw(""));
        } else {
            builder.set(
                "#ArtistLine.TextSpans",
                Message.translation("spotify.spotify.hud.artist").param("artist", artist)
            );
        }

        Message statusMessage = switch (status.toUpperCase()) {
            case "PLAYING" -> Message.translation("spotify.spotify.hud.playing");
            case "PAUSED" -> Message.translation("spotify.spotify.hud.paused");
            case "ERROR" -> Message.translation("spotify.spotify.hud.error");
            default -> Message.translation("spotify.spotify.hud.idle");
        };
        builder.set("#StatusLine.TextSpans", statusMessage);

        if ("PLAYING".equalsIgnoreCase(status)) {
            builder.set("#PlayPauseButton.TextSpans", Message.translation("spotify.spotify.controls.pause"));
        } else {
            builder.set("#PlayPauseButton.TextSpans", Message.translation("spotify.spotify.controls.play"));
        }
    }

    @Nonnull
    private static Message resultMessage(@Nonnull SpotifyPlaybackSupport.Result result) {
        return switch (result) {
            case NOT_CONNECTED -> Message.translation("spotify.spotify.command.noAuth");
            case NO_DEVICE -> Message.translation("spotify.spotify.command.noDevice");
            case FORBIDDEN -> Message.translation("spotify.spotify.command.forbidden");
            case MISSING_SCOPE -> Message.translation("spotify.spotify.command.missingScope");
            case PREMIUM_REQUIRED -> Message.translation("spotify.spotify.command.premiumRequired");
            case WINDOWS_UNAVAILABLE -> Message.translation("spotify.spotify.command.windowsUnavailable");
            case API_ERROR -> Message.translation("spotify.spotify.command.apiError");
            case OK -> Message.raw("");
        };
    }

    @Nonnull
    private static String safe(@Nullable String value, @Nonnull String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void bind(@Nonnull UIEventBuilder eventBuilder, @Nonnull String action, @Nonnull String selector) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            selector,
            new EventData().append("Action", action),
            false
        );
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
            .add()
            .build();

        @Nullable
        private String action;
    }
}
