package com.jagod.spotify.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import javax.annotation.Nonnull;

/** Player commands usable by everyone — no OP or custom permission nodes. */
abstract class SpotifyAccessiblePlayerCommand extends AbstractPlayerCommand {
    protected SpotifyAccessiblePlayerCommand(@Nonnull String name, @Nonnull String descriptionKey) {
        super(name, descriptionKey);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
