# MusicDisplay

A Hytale server mod that shows your live Spotify track on a customizable in-game HUD overlay, with playback controls via `/spotify`.

![Hytale](https://img.shields.io/badge/Hytale-%3E%3D0.5.0--pre.0-blue)
![Java](https://img.shields.io/badge/Java-25-orange)
![License](https://img.shields.io/badge/License-MIT-green)

## Features

- **Now-playing HUD** — track, artist, elapsed/total time, optional progress bar
- **Customizable layout** — corner, size, offsets, hex text colors
- **In-game settings** — `/spotify` opens connect + HUD options
- **Playback controls** — `/spotify controls` or chat commands (`play`, `pause`, `next`, …)
- **Per-player OAuth** — each player connects their own Spotify account
- **Languages** — English, Polish, German, French, Spanish

## Requirements

- Hytale server `>=0.5.0-pre.0 <0.6.0`
- JDK **25** (for building)
- Spotify account (Premium required for skip/play/pause commands)
- Port **8888** free on the player's PC during first-time setup

## Installation

1. Download `MusicDisplay-x.x.x.jar` from [Releases](https://github.com/jesuisjoszi/MusicDisplay/releases) or build locally.
2. Copy the JAR into your mods folder:
   ```
   %AppData%\Roaming\Hytale\UserData\Mods\
   ```
3. Restart the server and run `/spotify` in-game.

## First-time Spotify setup

1. Run `/spotify` and click **Setup Spotify** — the setup URL is copied to your clipboard.
2. Paste it in your browser with **Ctrl+V**.
3. In the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard), add this Redirect URI:
   ```
   http://127.0.0.1:8888/callback
   ```
4. Paste your **Client ID** and **Client Secret** into the browser setup page.
5. Approve all Spotify permissions and return to the game.

## Commands

| Command | Description |
|---------|-------------|
| `/spotify` | Settings panel (connect + HUD layout) |
| `/spotify controls` | Playback UI (prev / play / pause / next) |
| `/spotify play` | Resume playback |
| `/spotify pause` | Pause playback |
| `/spotify next` / `skip` | Next track |
| `/spotify prev` / `previous` | Previous track |
| `/spotify toggle` | Show or hide HUD |
| `/spotify hide` / `off` | Hide HUD |

Commands work for all players — no OP required.

## Build from source

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
.\gradlew.bat jar
```

Output: `build/libs/MusicDisplay-0.1.0.jar`

## Project layout

```
src/main/java/com/jagod/spotify/   # Plugin logic
src/main/resources/
  manifest.json                    # Mod metadata
  icon-256.png                     # Mod icon
  Common/UI/Custom/Spotify/        # HUD + settings UI
  Server/Languages/                # In-game translations
```

## Credits

- OAuth flow inspired by [musicdisplay](https://github.com/realmichaelstetson/musicdisplay) (Minecraft)
- Built with the [hytale-mod](https://maven.hytale-modding.info/) Gradle plugin

## License

MIT — see [LICENSE](LICENSE).
