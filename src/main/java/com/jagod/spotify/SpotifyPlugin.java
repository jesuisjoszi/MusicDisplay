package com.jagod.spotify;

import com.jagod.spotify.command.SpotifyCommand;
import com.jagod.spotify.data.SpotifyPlayerComponent;
import com.jagod.spotify.oauth.SpotifyOAuthService;
import com.jagod.spotify.service.SpotifyControlsRegistry;
import com.jagod.spotify.service.SpotifyPollingService;
import com.jagod.spotify.ui.SpotifyHudSupport;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.AssetPackRegisterEvent;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SpotifyPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nullable
    private static volatile SpotifyPlugin instance;

    public SpotifyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Nullable
    public static SpotifyPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;
        SpotifyPlayerComponent.register(this.getEntityStoreRegistry());
        LOGGER.atInfo().log("MusicDisplay setup complete");
    }

    @Override
    protected void start() {
        ensureAssetPackLoaded();

        this.getCommandRegistry().registerCommand(new SpotifyCommand());

        this.getEventRegistry().registerGlobal(EventPriority.NORMAL, PlayerReadyEvent.class, event -> {
            Player player = event.getPlayer();
            if (player == null || player.getWorld() == null || player.getReference() == null) {
                return;
            }
            player.getWorld().execute(() -> {
                Ref<EntityStore> ref = player.getReference();
                Store<EntityStore> store = ref.getStore();
                SpotifyCommand.ensureComponent(store, ref);
                SpotifyPlayerComponent state = store.getComponent(ref, SpotifyPlayerComponent.getComponentType());
                if (state != null && state.isHudEnabled() && state.hasCredentials()) {
                    SpotifyPollingService.refreshPlayerNow(ref, store);
                }
            });
        });

        this.getEventRegistry().registerGlobal(EventPriority.NORMAL, PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            SpotifyPollingService.clearPlayer(playerRef.getUuid());
            SpotifyControlsRegistry.unregister(playerRef.getUuid());
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                SpotifyHudSupport.removeHud(player, playerRef);
            }
        });

        SpotifyPollingService.start();
        LOGGER.atInfo().log("MusicDisplay started — use /spotify to open the control panel");
    }

    @Override
    protected void shutdown() {
        SpotifyOAuthService.shutdown();
    }

    private void ensureAssetPackLoaded() {
        if (!this.getManifest().includesAssetPack()) {
            return;
        }
        String packId = new PluginIdentifier(this.getManifest()).toString();
        AssetPack pack = AssetModule.get().getAssetPack(packId);
        if (pack == null) {
            LOGGER.atWarning().log("Spotify asset pack %s not found in AssetModule", packId);
            return;
        }
        HytaleServer.get()
            .getEventBus()
            .<Void, AssetPackRegisterEvent>dispatchFor(AssetPackRegisterEvent.class)
            .dispatch(new AssetPackRegisterEvent(pack));
        CommonAssetModule commonAssets = CommonAssetModule.get();
        if (commonAssets != null) {
            commonAssets.loadCommonAssets(pack, System.nanoTime());
            if (Universe.get().getPlayerCount() > 0) {
                Universe.get().broadcastPacketNoCache(new RequestCommonAssetsRebuild());
            }
        }
    }
}
