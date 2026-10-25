# MenuFlow

A lightweight client-side Fabric mod for Minecraft 26.3 that remembers cursor positions across inventory and container screens, and safely converts compatible Hypixel SkyBlock UI left-clicks into middle-clicks so display items are not picked up.

![License](https://img.shields.io/badge/license-MIT-green)
![Minecraft](https://img.shields.io/badge/Minecraft-26.3-blue)
![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.5-orange)
![Java](https://img.shields.io/badge/Java-25-red)

## Features

- **Global cursor memory** — all container types (inventory, chest, furnace, anvil, etc.) share one cursor position. Close a chest with the cursor on a slot, open inventory, cursor lands in the same spot.
- **Multi-page backpack support** — on servers with paginated GUIs, the cursor stays hovering over page-turn buttons after clicking.
- **Safe SkyBlock UI clicks** — left clicks on compatible page buttons, NPC shop offers, menu options, and blank GUI fillers are sent as middle clicks so display items are not picked up locally. NPC shops with a sell hopper also use middle clicks for inventory sales. Real items and click-sensitive screens such as Storage, Loadouts, Sacks, HOTM/HOTF template selection, Inferno Minions, Garden traps, The Hex, and anvils keep their normal click behavior.
- **No visible flicker** — cursor is restored before it becomes visible, not after.
- **Toggle on/off** — `/menuflow toggle` to enable/disable the mod
- **Memory time window** — set how long a saved position stays valid before expiring, or keep it permanent
- **Tab completion** — all `/menuflow` subcommands support tab completion
- **No Fabric API dependency** — only Fabric Loader is required

## Commands

| Command | Description |
|---|---|
| `/menuflow toggle` | Enable/disable cursor memory and safe UI clicks |
| `/menuflow window` | Show current memory window setting |
| `/menuflow window <seconds>` | Set memory window (0 = permanent, default) |

The former `/remembermouse` command remains available as a compatibility alias.

## Configuration

All settings are saved to `config/menuflow/menuflow.json`. Existing settings from
`config/remember-mouse/remember-mouse.json` are imported automatically on first launch.

```json
{
  "enabled": true,
  "memoryWindowSeconds": 0
}
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle for cursor memory and safe SkyBlock UI click replacement |
| `memoryWindowSeconds` | `0` | How long a saved position stays valid. `0` = permanent |

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.5+
- Java 25+
- Fabric API is not required

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) ≥ 0.19.5 for Minecraft 26.3
2. Download `menuflow-2.0.2+mc26.3.jar` from [Releases](https://github.com/kikuj197-star/remember-mouse/releases)
3. Place the jar in `mods/` folder
4. Launch Minecraft

## Building from Source

```bash
# Requires Java 25 and Gradle 9.6
git clone https://github.com/kikuj197-star/remember-mouse.git
cd remember-mouse
./gradlew build
# Output: build/libs/menuflow-2.0.2+mc26.3.jar
```

## How it works

| Mixin | Target | Hook | Purpose |
|---|---|---|---|
| `MinecraftMixin` | `Gui.setScreen()` | HEAD/RETURN | Save cursor when closing any container screen; pre-compute pending target |
| `ScreenMixin` | `Screen.init()` | TAIL | Restore cursor before first frame renders |
| `MouseHandlerMixin` | `MouseHandler.releaseMouse()` | RETURN | SDL-compatible fallback if releaseMouse centers the cursor |
| `AbstractContainerScreenMixin` | `AbstractContainerScreen.slotClicked()` | Before packet send | Replace SkyBlock UI item clicks with middle clicks while preserving real-item clicks |
| `ClientPacketListenerMixin` | `ClientPacketListener` | Multiple | Command interception, tab completion, server command tree injection |

All state lives in `RememberMouse` (not in mixin classes) to comply with Fabric Mixin static field rules.

## License

MIT — see [LICENSE](LICENSE) for details.
