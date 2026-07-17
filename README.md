# Remember Mouse

A lightweight client-side Fabric mod for Minecraft 26.1+ that remembers your cursor position when closing inventory/container UIs and restores it when you reopen.

![License](https://img.shields.io/badge/license-MIT-green)
![Minecraft](https://img.shields.io/badge/Minecraft-26.1-blue)
![Fabric](https://img.shields.io/badge/Fabric%20Loader-0.19.3-orange)
![Java](https://img.shields.io/badge/Java-25-red)

## Features

- **Global cursor memory** — all container types (inventory, chest, furnace, anvil, etc.) share one cursor position. Close a chest with the cursor on a slot, open inventory, cursor lands in the same spot.
- **Multi-page backpack support** — on servers with paginated GUIs, the cursor stays hovering over page-turn buttons after clicking.
- **No visible flicker** — cursor is restored before it becomes visible, not after.
- **Toggle on/off** — `/remembermouse toggle` to enable/disable the mod
- **Memory time window** — set how long a saved position stays valid before expiring, or keep it permanent
- **Tab completion** — all `/remembermouse` subcommands support tab completion
- **Zero dependencies** — no Fabric API or external libraries required

## Commands

| Command | Description |
|---|---|
| `/remembermouse toggle` | Enable/disable cursor position memory |
| `/remembermouse window` | Show current memory window setting |
| `/remembermouse window <seconds>` | Set memory window (0 = permanent, default) |

## Configuration

All settings are saved to `config/remember-mouse/remember-mouse.json`:

```json
{
  "enabled": true,
  "memoryWindowSeconds": 0
}
```

| Setting | Default | Description |
|---|---|---|
| `enabled` | `true` | Master toggle — when false, positions are neither saved nor restored |
| `memoryWindowSeconds` | `0` | How long a saved position stays valid. `0` = permanent |

## Requirements

- Minecraft 26.1.2+
- Fabric Loader 0.19.3+
- Java 25+

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) ≥ 0.19.3 for Minecraft 26.1
2. Download `remember-mouse-1.1.0.jar` from [Releases](https://github.com/kikuj197-star/remember-mouse/releases)
3. Place the jar in `mods/` folder
4. Launch Minecraft

## Building from Source

```bash
# Requires Java 25 and Gradle 9.5.1
git clone https://github.com/kikuj197-star/remember-mouse.git
cd remember-mouse
./gradlew build
# Output: build/libs/remember-mouse-1.1.0.jar
```

## How it works

| Mixin | Target | Hook | Purpose |
|---|---|---|---|
| `MinecraftMixin` | `Minecraft.setScreen()` | HEAD | Save cursor when closing any container screen; pre-compute pending target |
| `ScreenMixin` | `Screen.init()` | TAIL | Restore cursor before first frame renders |
| `MouseHandlerMixin` | `MouseHandler.releaseMouse()` | RETURN | Fallback if releaseMouse centers the cursor |
| `ClientPacketListenerMixin` | `ClientPacketListener` | Multiple | Command interception, tab completion, server command tree injection |

All state lives in `RememberMouse` (not in mixin classes) to comply with Fabric Mixin static field rules.

## License

MIT — see [LICENSE](LICENSE) for details.
