# S-Lib

**S-Lib** is a lightweight utility and config library for Minecraft modding.

---

> **Example mod:** Check out the [S-Lib Test Mod](https://github.com/Stalemated/s-lib_test_mod) repository for complete config usage examples and GUI generation using [YACL](https://github.com/isXander/YetAnotherConfigLib).

---

## Features

### Annotation-Driven Configs

- Register synced or local configs in a single line of code using `SLibConfig.syncedBuilder()` or `SLibConfig.localBuilder()`.
- Network `@Sync` Options:
    - `OVERRIDE_CLIENT`: server authoritative config, syncs to the client. Config broadcasts on login and updates in real time. Supports configurable permission checks (e.g., OP-only).
    - `INFORM_SERVER`: local config, informs server of changes
    - `NONE`: local config, no network sync
- Newly added fields are automatically populated with defaults, obsolete keys are removed, and values are clamped to specified ranges when using the annotations `@RangeInt`, `@RangeFloat`, and `@RangeDouble`.
- Insert comments using the `@Comment` annotation on top of fields, directly in your config class. 
- Backups are automatically created if file corruption or invalid syntax is detected, preserving the broken config if you accidentally make a mistake.
- Use the `@Nest` annotation to allow usage of nested classes for more complex config structures.
- Use the `@Ignore` annotation to ignore a field entirely.
- Built-in type adapters compatible with serialization for AWT's `Color`, Minecraft's `TextColor`, `Identifier`, `Pattern`, and `UUID` classes.

### Custom YACL Controllers
Custom controllers for [YACL](https://github.com/isXander/YetAnotherConfigLib):
- **Advanced Color Controller:** Supports hex codes (`#RRGGBBAA`), Minecraft color names, and legacy formatting codes (`&a`, `&3`).
- **Item or Tag Controller:** Autocomplete dropdown for matching exact items, tags (`#c:swords`), namespaces (`minecraft:*`), and regex patterns.
- **Simple Dropdowns:** Dropdown selectors for Enums and Strings.

### General Utilities
- **Target Matcher Engine:** Item matching via exact IDs, tags, namespaces, wildcards (`*`), and regex (`regex:.*_sword`).
- **Platform Helpers:** Abstractions for loader-specific methods, both as a general utility (`PlatformHelper`) and for network-related features (`NetworkHelper`).
- **Math and Color Utils:** Easing functions (lerp, ease-out, clamp), text gradient generators, color utilities, and more.

---

## Building from Source

#### 1. Clone the repository
```bash
git clone https://github.com/Stalemated/s-lib.git
cd s-lib
```

#### 2. Publish to Maven Local
```bash
# Windows
gradlew.bat publishToMavenLocal

# Linux / macOS
./gradlew publishToMavenLocal
```

Once published, other local projects will be able to resolve `com.stalemated.lib:s-lib-<loader>:<version>` as a dependency during their build process.

---

## Platform Support

| Platform | Versions       |
|----------|----------------|
| Fabric   | 1.20.1, 1.21.1 |
| Forge    | 1.20.1         |
| NeoForge | 1.21.1         |

---

## Dependencies

#### Fabric only
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

