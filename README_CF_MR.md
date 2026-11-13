# S-Lib

S-Lib is a utility and config library. My other mods use it for their configs and config screens, so on its own it adds nothing to the game. If a mod you installed lists S-Lib as a dependency, drop it in your mods folder.

## Dependencies

- Fabric only: [Fabric API](https://modrinth.com/mod/fabric-api)
- **Optional:** [YACL](https://modrinth.com/mod/yacl) for the in-game config screens. Without YACL everything still works, you will need to edit the config files manually.
- **Optional, Fabric only:** [Mod Menu](https://modrinth.com/mod/modmenu)

## Versions

| Minecraft        | Fabric | Forge | NeoForge     |
|------------------|--------|-------|--------------|
| 1.20 - 1.20.1    | Yes    | Yes   |              |
| 1.20.2 - 1.20.4  | Yes    |       | 1.20.4 only  |
| 1.20.5 - 1.20.6  | Yes    |       | Yes          |
| 1.21 - 1.21.1    | Yes    |       | Yes          |
| 1.21.2 - 1.21.4  | Yes    |       | Yes          |
| 1.21.5           | Yes    |       | Yes          |
| 1.21.6 - 1.21.8  | Yes    |       | Yes          |
| 1.21.9 - 1.21.10 | Yes    |       | Yes          |
| 1.21.11          | Yes    |       | Yes          |
| 26.1 - 26.1.2    | Yes    |       | Yes          |
| 26.2             | Yes    |       | Yes          |
| 26.3             | Yes    |       | Yes          |

## For players

Each mod keeps its settings in a `.json5` file in your `config` folder. If you mess up an option when editing, only that option goes back to its default. The old file gets backed up first.

With YACL installed (and if the mod supports it), the config button opens a settings screen in game.

In multiplayer, server-managed settings are automagically sent to you when you join, and they stay greyed out unless the server lets you change them (usually operators only). In a LAN world the host can always change them. On a server without S-Lib, those settings just use their defaults.

## For mod developers

- Configs are annotation-based POJOs with support for range clamping, comments, and more
  - `@Sync` decides whether an option is kept locally or synced to the server
- Configs can be registered with a one-liner, and an entire YACL screen can easily be built with a few more lines
- YACL controllers for colors, enum and items/tags dropdowns.
- An item matcher (ids, tags, regex, `!` to exclude) and some smaller helpers

See the [GitHub README](https://github.com/Stalemated/s-lib) for the full documentation, and the [S-Lib Test Mod](https://github.com/Stalemated/s-lib_test_mod) for real code examples.
