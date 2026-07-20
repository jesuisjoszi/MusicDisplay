package com.joszza.spotify.oauth;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.annotation.Nonnull;

/** Copies text to the system clipboard without spawning OS commands. */
public final class SpotifySetupClipboard {
    private SpotifySetupClipboard() {}

    public static boolean tryCopy(@Nonnull String text) {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
