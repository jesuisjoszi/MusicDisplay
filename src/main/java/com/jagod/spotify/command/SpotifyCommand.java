package com.jagod.spotify.command;

import com.jagod.spotify.SpotifyConstants;
import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.service.SpotifyPlaybackSupport;
import com.jagod.spotify.service.SpotifyPollingService;
import com.jagod.spotify.ui.SpotifyControlPage;
import com.jagod.spotify.ui.SpotifyControlsPage;
import com.jagod.spotify.ui.SpotifyHudSupport;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Root {@code /musicdisplay} (alias {@code /msd}) command. Bare command opens settings;
 * playback and HUD actions are subcommands.
 */
public final class SpotifyCommand extends SpotifyAccessiblePlayerCommand {
    public SpotifyCommand() {
        super(SpotifyConstants.COMMAND_ROOT, "spotify.spotify.command.desc");
        this.addAliases(SpotifyConstants.COMMAND_ALIAS_SHORT);

        this.addSubCommand(new ControlsSubCommand());
        this.addSubCommand(new PlaybackSubCommand("next", SpotifyPlaybackSupport.Action.NEXT));
        this.addSubCommand(new PlaybackSubCommand("skip", SpotifyPlaybackSupport.Action.NEXT));
        this.addSubCommand(new PlaybackSubCommand("prev", SpotifyPlaybackSupport.Action.PREVIOUS));
        this.addSubCommand(new PlaybackSubCommand("previous", SpotifyPlaybackSupport.Action.PREVIOUS));
        this.addSubCommand(new PlaybackSubCommand("play", SpotifyPlaybackSupport.Action.PLAY));
        this.addSubCommand(new PlaybackSubCommand("pause", SpotifyPlaybackSupport.Action.PAUSE));
        this.addSubCommand(new PlaybackSubCommand("stop", SpotifyPlaybackSupport.Action.PAUSE));
        this.addSubCommand(new ToggleHudSubCommand());
        this.addSubCommand(new HideHudSubCommand());
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        ensureComponent(store, ref);
        openControlPage(store, ref, playerRef, player);
    }

    private static void openControlPage(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull Player player
    ) {
        if (player.getPageManager().getCustomPage() != null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new SpotifyControlPage(playerRef));
    }

    private static void openControlsPage(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull Player player
    ) {
        if (player.getPageManager().getCustomPage() != null) {
            playerRef.sendMessage(Message.translation("spotify.spotify.command.closeOtherUi"));
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new SpotifyControlsPage(playerRef));
        SpotifyPollingService.refreshPlayerNow(ref, store);
    }

    private static void runPlayback(
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull SpotifyPlaybackSupport.Action action
    ) {
        SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
        if (state == null) {
            return;
        }
        SpotifyPlaybackSupport.Result result = SpotifyPlaybackSupport.run(state, action);
        SpotifyPlaybackSupport.apply(playerRef, ref, store, state, result);
    }

    public static void ensureComponent(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (store.getComponent(ref, SpotifyPlayerComponent.getComponentType()) == null) {
            store.addComponent(ref, SpotifyPlayerComponent.getComponentType(), new SpotifyPlayerComponent());
        }
    }

    private static final class ControlsSubCommand extends SpotifyAccessiblePlayerCommand {
        ControlsSubCommand() {
            super("controls", "spotify.spotify.command.controls.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            ensureComponent(store, ref);
            openControlsPage(store, ref, playerRef, player);
        }
    }

    private static final class PlaybackSubCommand extends SpotifyAccessiblePlayerCommand {
        @Nonnull
        private final SpotifyPlaybackSupport.Action action;

        PlaybackSubCommand(@Nonnull String name, @Nonnull SpotifyPlaybackSupport.Action action) {
            super(name, "spotify.spotify.command.playback.desc");
            this.action = action;
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            ensureComponent(store, ref);
            runPlayback(store, ref, playerRef, action);
        }
    }

    private static final class ToggleHudSubCommand extends SpotifyAccessiblePlayerCommand {
        ToggleHudSubCommand() {
            super("toggle", "spotify.spotify.command.toggle.desc");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            ensureComponent(store, ref);
            SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
            if (state == null) {
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
        }
    }

    private static final class HideHudSubCommand extends SpotifyAccessiblePlayerCommand {
        HideHudSubCommand() {
            super("hide", "spotify.spotify.command.hide.desc");
            this.addAliases("off");
        }

        @Override
        protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            ensureComponent(store, ref);
            SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
            if (state != null) {
                state.setHudEnabled(false);
            }
            SpotifyHudSupport.removeHud(player, playerRef);
            playerRef.sendMessage(Message.translation("spotify.spotify.command.disabled"));
        }
    }
}
