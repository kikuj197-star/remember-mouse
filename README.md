# Remember Mouse

A Fabric mod for Minecraft 26.1+ that remembers your cursor position when closing inventory/container UIs and restores it when you reopen.

## Features

- **Global cursor memory** — all container types (inventory, chest, furnace, anvil, etc.) share one cursor position. Close a chest with the cursor on a slot, open inventory, cursor lands in the same spot.
- **Multi-page backpack support** — on servers with paginated GUIs, the cursor stays hovering over page-turn buttons after clicking.
- **No visible flicker** — cursor is restored before it becomes visible, not after.

## Requirements

- Minecraft 26.1.2+
- Fabric Loader 0.19.3+
- Java 25+

## Building

```bash
./gradlew build
```

Output jar is at `build/libs/remember-mouse-1.0.0.jar`.

## How it works

| Mixin | Target | Hook | Purpose |
|---|---|---|---|
| `MinecraftMixin` | `Minecraft.setScreen()` | HEAD | Save cursor when closing any container screen |
| `ScreenMixin` | `Screen.init()` | TAIL | Restore cursor before first frame renders |
| `MouseHandlerMixin` | `MouseHandler.releaseMouse()` | RETURN | Fallback if releaseMouse centers the cursor |

All state lives in `RememberMouse` (not in mixin classes) to comply with Fabric Mixin static field rules.

## License

MIT
