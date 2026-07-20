package com.joszza.spotify.windows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.joszza.spotify.SpotifyPlugin;
import com.joszza.spotify.api.SpotifyNowPlayingInfo;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Windows SMTC bridge (same approach as Minecraft musicdisplay).
 * Extracts and launches {@code SMTCBridge.exe}, reads JSON status lines from stdout.
 * Works only when the Hytale server runs on the same Windows PC as the media player.
 */
public final class WindowsMediaManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final WindowsMediaManager INSTANCE = new WindowsMediaManager();
    private static final String RESOURCE_PATH = "/native/SMTCBridge.exe";

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MusicDisplay-SMTC");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean bridgeReady;
    private volatile SpotifyNowPlayingInfo currentInfo = SpotifyNowPlayingInfo.idle();
    private volatile int volumePercent = 50;
    @Nullable
    private volatile Path artworkPath;
    private long lastVolumeCmdMs;
    @Nullable
    private Process bridgeProcess;
    @Nullable
    private OutputStream bridgeStdin;
    @Nullable
    private Path bridgeExePath;

    private WindowsMediaManager() {}

    @Nonnull
    public static WindowsMediaManager get() {
        return INSTANCE;
    }

    public static boolean isOsWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    public boolean isReady() {
        return bridgeReady;
    }

    @Nonnull
    public SpotifyNowPlayingInfo getStatus() {
        return currentInfo;
    }

    public void start() {
        if (!isOsWindows()) {
            LOGGER.atInfo().log("Windows media source skipped (not Windows)");
            return;
        }
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            try {
                extractBridge();
                launchBridge();
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("Failed to start SMTC bridge");
                running.set(false);
                bridgeReady = false;
            }
        });
    }

    public void stop() {
        running.set(false);
        bridgeReady = false;
        Process process = bridgeProcess;
        if (process != null) {
            try {
                process.destroyForcibly();
            } catch (Exception ignored) {
            }
            bridgeProcess = null;
        }
        bridgeStdin = null;
    }

    public void playPause() {
        sendCommand("playpause");
    }

    public void next() {
        sendCommand("next");
    }

    public void previous() {
        sendCommand("previous");
    }

    public int getVolumePercent() {
        return volumePercent;
    }

    @Nullable
    public Path getArtworkPath() {
        return artworkPath;
    }

    public void setVolume(int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        volumePercent = clamped;
        long now = System.currentTimeMillis();
        if (now - lastVolumeCmdMs < 100L) {
            return;
        }
        lastVolumeCmdMs = now;
        sendCommand("volume " + clamped);
    }

    private void extractBridge() throws Exception {
        SpotifyPlugin plugin = SpotifyPlugin.get();
        if (plugin == null) {
            throw new IllegalStateException("Plugin not loaded");
        }
        Path dataDir = plugin.getDataDirectory();
        Files.createDirectories(dataDir);
        bridgeExePath = dataDir.resolve("SMTCBridge.exe");
        try (InputStream in = WindowsMediaManager.class.getResourceAsStream(RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE_PATH);
            }
            Files.copy(in, bridgeExePath, StandardCopyOption.REPLACE_EXISTING);
        }
        LOGGER.atInfo().log("Extracted SMTC bridge to %s", bridgeExePath);
    }

    private void launchBridge() {
        if (bridgeExePath == null) {
            return;
        }
        try {
            Path artPath = bridgeExePath.getParent().resolve("windows-media-art.png");
            ProcessBuilder pb = new ProcessBuilder(bridgeExePath.toAbsolutePath().toString(), artPath.toAbsolutePath().toString());
            pb.redirectErrorStream(false);
            bridgeProcess = pb.start();
            bridgeStdin = bridgeProcess.getOutputStream();
            bridgeReady = true;
            LOGGER.atInfo().log("SMTC bridge started (pid %s)", bridgeProcess.pid());

            Thread stderr = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(
                    new InputStreamReader(bridgeProcess.getErrorStream(), StandardCharsets.UTF_8)
                )) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        LOGGER.atFine().log("[SMTC] %s", line);
                    }
                } catch (Exception ignored) {
                }
            }, "MusicDisplay-SMTC-err");
            stderr.setDaemon(true);
            stderr.start();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(bridgeProcess.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while (running.get() && (line = reader.readLine()) != null) {
                    try {
                        parseStatusLine(line);
                    } catch (Exception e) {
                        LOGGER.atFine().log("Bad SMTC status line: %s", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("SMTC bridge process error");
        } finally {
            bridgeReady = false;
            if (running.get()) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                if (running.get()) {
                    LOGGER.atInfo().log("Restarting SMTC bridge...");
                    launchBridge();
                }
            }
        }
    }

    private void parseStatusLine(@Nonnull String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String title = stringOrEmpty(obj, "Title");
        String artist = stringOrEmpty(obj, "Artist");
        boolean playing = obj.has("IsPlaying") && obj.get("IsPlaying").getAsBoolean();
        double positionSeconds = obj.has("PositionSeconds") ? obj.get("PositionSeconds").getAsDouble() : 0.0;
        double durationSeconds = obj.has("DurationSeconds") ? obj.get("DurationSeconds").getAsDouble() : 0.0;
        if (obj.has("VolumePercent") && !obj.get("VolumePercent").isJsonNull()) {
            volumePercent = Math.max(0, Math.min(100, obj.get("VolumePercent").getAsInt()));
        }
        String art = stringOrEmpty(obj, "ArtworkPath");
        artworkPath = art.isBlank() ? null : Path.of(art);

        if (title.isBlank() && artist.isBlank()) {
            currentInfo = SpotifyNowPlayingInfo.idle();
            return;
        }

        long progressMs = Math.max(0L, (long) (positionSeconds * 1000.0));
        long durationMs = Math.max(0L, (long) (durationSeconds * 1000.0));
        currentInfo = SpotifyNowPlayingInfo.of(
            playing ? SpotifyNowPlayingInfo.Status.PLAYING : SpotifyNowPlayingInfo.Status.PAUSED,
            title.isBlank() ? "Unknown" : title,
            artist.isBlank() ? "Unknown" : artist,
            null,
            artworkPath,
            progressMs,
            durationMs
        );
    }

    @Nonnull
    private static String stringOrEmpty(@Nonnull JsonObject obj, @Nonnull String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    private void sendCommand(@Nonnull String command) {
        OutputStream stdin = bridgeStdin;
        if (!bridgeReady || stdin == null) {
            return;
        }
        try {
            stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (Exception e) {
            LOGGER.atWarning().log("Failed to send SMTC command '%s': %s", command, e.getMessage());
        }
    }
}
