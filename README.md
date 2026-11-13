# S-Lib

**S-Lib** is a lightweight utility and config library for Minecraft modding.

---

> **Example mod:** Check out the [S-Lib Test Mod](https://github.com/Stalemated/s-lib_test_mod) repository for complete config usage examples and GUI generation using [YACL](https://github.com/isXander/YetAnotherConfigLib).

---

## Quick Start

Build the config in a static field and register it from your mod initializer, so it loads at a known point:

```java
public static final SyncedConfigManager<MyConfig> CONFIG = SLibConfig.synced(MOD_ID, MyConfig.class).build();

public static void init() {
    CONFIG.register();
}
```

`SLibConfig.local(MOD_ID, MyConfig.class)` does the same for a config that never touches the network. Inside the initializer, `register()` on the builder builds and registers in one call.

Create its screen with the YACL builder:

```java
public final class MyConfigScreen {
    public static Screen create(Screen parent) {
        return builder(parent, CONFIG).generateScreen();
    }

    static YaclScreenBuilder builder(Screen parent, ConfigManager<MyConfig> config) {
        return YaclScreenBuilder.create(parent, Component.literal("My Mod"), MOD_ID)
                .category(config, "general", YaclCategoryBuilder::autoGenerate);
    }
}
```

Then add the config button from your client init:

```java
SLibConfigScreens.register(MOD_ID, () -> MyConfigScreen::create);
```

---

## Features

### Annotation-Driven Configs

- Register synced or local configs in a single line of code using `SLibConfig.synced(modId, Config.class)` or `SLibConfig.local(modId, Config.class)`.
- Network `@Sync` Options:
    - `OVERRIDE_CLIENT`: server authoritative config, syncs to the client. Config broadcasts on login and updates in real time. Supports configurable permission checks (e.g., OP-only).
      - The config is sent again when `/op` or `/deop` changes a player's operator status and, on Minecraft 26.2 and later, when the host changes who may use commands in World Options or in the LAN options. Call `sendConfigToPlayer(player)` to resend it yourself, for example after a permission plugin changes what a player may edit.
    - `INFORM_SERVER`: local config, informs server of changes. The server reads each player's values with `informedConfigOf(player)`.
    - `NONE`: local config, no network sync
- Newly added fields are automatically populated with defaults, obsolete keys are removed, and values are clamped to the ranges set with `@RangeInt`, `@RangeLong`, `@RangeFloat` and `@RangeDouble`. Each range annotation also takes a `step` used by YACL sliders, and on a `@Nest` list or map it clamps the contents.
- Insert comments using the `@Comment` annotation on top of fields, directly in your config class.
  - Backups are automatically created if file corruption or invalid syntax is detected, preserving the broken config if you accidentally make a mistake.
- Use the `@Nest` annotation to allow usage of nested classes for more complex config structures.
- Use the `@Ignore` annotation to ignore a field entirely.
- Rename a key without losing saved values: `@SerializedName(value = "new_name", alternate = {"old_name"})` reads the old key and writes the new one.
- Use `onChange(listener)` to react to changes on the builder or on the manager. The manager returns a `Subscription` you can `unsubscribe()`.
- Built-in type adapters compatible with serialization for AWT's `Color`, Minecraft's `TextColor`, `ResourceLocation` (`Identifier` from 1.21.11 on), `Pattern`, and `UUID` classes.

### Config Screens (YACL)

- `YaclScreenBuilder` builds a whole screen from your config classes. `autoGenerate()` adds every option, and `group`, `nested`, `nestedList`, `nestedMap`, `listOption` and `mapPairsOption` lay them out by hand. Edits stay in a draft until the player saves, and then each config is saved once.
- `nestedList` and `nestedMap` sections have an Add Entry button, and every entry has a Remove Entry button. A map section also has a New Entry Key field for the key of the new entry.
- An open screen reloads when the server changes its values or the player's permission, or when another player saves them on a LAN world. It also reloads after Save, Undo, Add Entry, and Remove Entry.
- ID fields (`ResourceLocation`/`Identifier` without `@ItemDropdown`) only accept text that is a valid id, so a space or a capital letter never goes in. An empty id is removed when the config is saved.
- Translation keys are snake_case: `<prefix>.category.*`, `<prefix>.group.*`, `<prefix>.option.*` and `<prefix>.description.*`. A description uses the same key plus `.desc` and shows nothing when the key is missing.
- Use `@Title` to add a section header above a field (`<prefix>.group.<key>`).
- Use `@ItemDropdown` on `String` or `ResourceLocation`/`Identifier` fields (or collections of them) to get the item dropdown controller. `@ItemDropdown(allowTags = false)` suggests items only, without `#` tags.
- Use `@SharedDescription` to point several options at one `<prefix>.description.<key>` tooltip.
- Use `@EnabledIf` to enable an option only while another option holds a value:

  ```java
  public boolean particles = true;

  @EnabledIf("particles")
  public int particleCount = 20;

  public Mode mode = Mode.FADE;

  @EnabledIf(value = "mode", equals = {"FADE", "SCROLL"})
  public int fadeTicks = 10;
  ```

  - The toggle is a boolean or enum option, named by its full key (`category.field` inside a `@Nest`).
  - The option follows the value shown on the screen before it is saved.
  - It works on options, lists and `mapPairsOption`, not on the entries of `nestedList`, `nestedMap`, `nestedMapLists` or `listElement`.
  - A misspelled key, a value the toggle cannot hold, or a cycle fails when the screen is built.
- Sliders show as many decimals as their `step`. `@Format("%.0f%%")` replaces the slider text. It needs a range annotation on the same field, and anywhere else the config fails when it is created.
- `SLibConfigScreens.register(modId, () -> MyConfigScreen::create)` adds the config button to Mod Menu on Fabric and to the mod list on Forge and NeoForge. Without YACL it does nothing.
  - The factory is a supplier, and S-Lib only calls it when YACL is installed, so your screen class never loads without YACL.
  - On Mod Menu 15.0.2 and older (Minecraft 1.20.1 to 1.21.8), a `modmenu` entrypoint of your own hides this screen, so remove it.
- Edit or create one element of a list in its own screen:

  ```java
  ConfigManager<Entry> edited = CONFIG.editListElement(cfg -> cfg.entries, entry, Entry::new);
  ConfigManager<Entry> created = CONFIG.addListElement(cfg -> cfg.entries, Entry::new);
  ```

  The returned manager works with `category(...)` and `livePreview(...)`, and saving it writes the element into `CONFIG`. Take `entry` from `CONFIG.get()` after the last save, because an element that is no longer in the list is rejected. Code that must react to the save goes in `CONFIG.onChange(...)`.
- `nestedMapLists(mapKey, NestListBinding.of(getter, setter))` shows one list per map entry. `withKeys(keys, newEntry)` adds keys that are not in the map yet (every registry entry, for example), `withEntryNames(key -> name)` names each list, and `withDescription(component)` sets their tooltip.
- `SLibTesting.assertScreenBuilds(builder, langFile)` builds a screen in a JUnit test and fails with the list of keys missing from your lang file:

  ```java
  @Test
  void theConfigScreenBuilds() throws IOException {
      SLibTesting.assertScreenBuilds(
              MyConfigScreen.builder(null, new InMemoryConfigManager<>(new MyConfig(), MyConfig::new, null)),
              Path.of("src/main/resources/assets/mymod/lang/en_us.json"));
  }
  ```

  The test needs Minecraft, YACL, and S-Lib on its classpath. With Loom (Groovy DSL):

  ```groovy
  dependencies {
      testImplementation files(sourceSets.main.compileClasspath)
      testImplementation "org.junit.jupiter:junit-jupiter:6.1.3"
      testRuntimeOnly "org.junit.platform:junit-platform-launcher"
  }

  test {
      useJUnitPlatform()
  }
  ```

### Custom YACL Controllers
Custom controllers for [YACL](https://github.com/isXander/YetAnotherConfigLib):
- **Advanced Color Controller:** Supports hex codes (`#AARRGGBB` and `#RRGGBB`), Minecraft color names, and legacy formatting codes (`&a`, `&3`).
- **Item or Tag Controller:** Autocomplete dropdown for matching exact items, tags (`#c:swords`), namespaces (`minecraft:*`), and regex patterns.
- **Simple Dropdowns:** Dropdown selectors for Enums and Strings.

### Conditional Mixins
Apply a compat mixin only when the mods it needs are loaded. Point your mixin config at S-Lib's plugin:

```json
{
  "package": "com.example.mymod.mixin",
  "plugin": "com.stalemated.lib.mixin.ConditionalMixinPlugin",
  "mixins": ["compat.OtherModScreenMixin"]
}
```

Then mark each compat mixin with the mods it needs:

```java
@RequiresMod("othermod")
@Mixin(targets = "com.othermod.client.OtherModScreen")
public abstract class OtherModScreenMixin {
}
```

With several ids the mixin needs all of them. A mixin without `@RequiresMod` always applies.

### General Utilities
- **Target Matcher Engine:** Item matching via exact IDs, tags (`#c:swords`), namespaces (`minecraft:*`), every item (`*`), regex (`regex:.*_sword`), and negation (`!minecraft:stick`).
- **Platform Helpers:** Abstractions for loader-specific methods, both as a general utility (`PlatformHelper`) and for network-related features (`NetworkHelper`).
- **Math and Color Utils:** Easing functions (lerp, ease-out, clamp), text gradient generators, color utilities, and more.
- **Resource Ids:** `ResourceIds.of(namespace, path)` and `ResourceIds.parse(id)` create identifiers on every supported Minecraft version.

---

## Building from Source

Building needs JDK 25.

#### 1. Clone the repository
```bash
git clone https://github.com/Stalemated/s-lib.git
cd s-lib
```

#### 2. Publish to Maven Local
Windows:
```bash
gradlew.bat publishToMavenLocal
```

Linux & macOS:
```bash
./gradlew publishToMavenLocal
```

Once published, other local projects will be able to resolve `com.stalemated.lib:s-lib-<loader>:<version>+<minecraft>` (for example `com.stalemated.lib:s-lib-fabric:4.0.0+1.21.1`) as a dependency during their build process.

In an Architectury or multi-loader project, compile the common module against the Fabric artifact:

```groovy
modCompileOnly("com.stalemated.lib:s-lib-fabric:<version>+<minecraft>")
```

---

## Platform Support

| Minecraft        | Fabric | Forge | NeoForge |
|------------------|--------|-------|----------|
| 1.20 - 1.20.1    | Yes    | Yes   |          |
| 1.20.2 - 1.20.4  | Yes    |       | 1.20.4   |
| 1.20.5 - 1.20.6  | Yes    |       | Yes      |
| 1.21 - 1.21.1    | Yes    |       | Yes      |
| 1.21.2 - 1.21.4  | Yes    |       | Yes      |
| 1.21.5           | Yes    |       | Yes      |
| 1.21.6 - 1.21.8  | Yes    |       | Yes      |
| 1.21.9 - 1.21.10 | Yes    |       | Yes      |
| 1.21.11          | Yes    |       | Yes      |
| 26.1 - 26.1.2    | Yes    |       | Yes      |
| 26.2             | Yes    |       | Yes      |
| 26.3             | Yes    |       | Yes      |

---

## Dependencies

#### Fabric only
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

#### Optional
- [YACL](https://github.com/isXander/YetAnotherConfigLib): required only for the generated config screens.
- [Mod Menu](https://github.com/TerraformersMC/ModMenu) (Fabric): shows the config button.
