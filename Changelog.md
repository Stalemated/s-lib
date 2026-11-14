# Changelog

## 4.0.1

### Changes

- `YaclScreenBuilder.category(ConfigCategory)` is now `category(Supplier<ConfigCategory>)`
- `ConfigProvider.getSerializer()` was removed, so synced configs can use a custom `ConfigProvider`
- `ItemOrTagControllerBuilder.create(Option)` no longer takes an item index, and `allowTags(boolean)` enables choosing tags
- `ColorUtils.toHex(TextColor)` now returns `#RRGGBB` only

### Fixes

Fixed:
- Singleplayer clients and LAN hosts not sending the `INFORM_SERVER` values until one of them changed
- A second `register()` silently reloading the config
- `modifyOptions` accepting options of another config
- Typos in `nested`, `includePrefix` and `configureOption` being ignored, and `dropdown` on an option that is not edited as text failing only when drawn
- Config screens crashing on maps without `String` keys
- Custom categories keeping listeners of old screens after Save, Undo, or a server change
- Key mappings registered after `RegisterKeyMappingsEvent` on Forge and NeoForge being lost without a warning
- `SLibConfigScreens.register` only rejecting an unknown mod id when YACL is installed
- Weapon damage and speed on 1.20.5+ ignoring the default modifiers of the item and the modifiers from enchantments

## 4.0.0

**API BREAKING RELEASE**
> S-Lib now supports Fabric, Forge, and NeoForge from Minecraft 1.20 to 26.3!
> - Every file on Modrinth and CurseForge has the version number `<version>+<minecraft>-<loader>`

### Breaking Changes

- Configs are now registered with `SLibConfig.synced(modId, Config.class)` and `SLibConfig.local(modId, Config.class)`
- The config managers, their builders, `OptionInfo` and `OptionTree` moved to `com.stalemated.lib.config`
- Implementation classes moved to `*.internal` packages, client-only code moved to `com.stalemated.lib.client`, and the enum and string dropdowns were merged into `SelectDropdownControllerBuilder`
- Sync packets use new channels, and a sync over 256 KB to a client or 32 KB to the server is logged and not sent

### New Features

- New YACL screen builder (`YaclScreenBuilder`): `autoGenerate()` builds a whole config, or groups, nested objects, lists, and maps can be customized. Edits stay in a draft until the player saves
- Nested lists and maps have Add Entry and Remove Entry buttons
- Open config screens reload when the server changes their values or the player's permission, or when another player saves on a LAN world. The reload keeps the tab, the scroll, and the open groups
- ID fields only accept valid ids, and empty ids are not saved
- `SLibConfigScreens.register` adds the config button on every loader and does nothing without YACL
- `editListElement` and `addListElement` edit or create one list element in its own screen
- `SLibTesting.assertScreenBuilds` checks in JUnit that a config screen builds and that its translations exist
- New annotations:
  - `@RangeLong`, plus a `step` on every range annotation for YACL sliders
  - `@Title`: adds a section header above a field
  - `@ItemDropdown`: shows the item dropdown on `String` and id fields
  - `@SharedDescription`: gives several options one description
  - `@EnabledIf`: enables an option while a boolean or enum option holds a given value
  - `@Format`: replaces the slider text
  - `@RequiresMod`: applies a mixin only when the mods it needs are loaded (with `ConditionalMixinPlugin`)
- Synced configs are sent again when a player's OP status changes and, on 26.2 and later, when a LAN world's host changes command access
- Config screens, saves, and syncs only process the options that changed, and the item dropdown caps and caches its matches

### Fixes

Fixed:
- Config files being half-written or overwritten without a backup: saves now go through a temp file and report failures, and a file that cannot be read or has invalid or unknown entries is backed up when it loads
- Invalid values being silently replaced with fallbacks
- NaN, infinite and overflowing numbers being accepted, and ranges not clamping the contents of lists and maps
- `@SerializedName` alternate keys not being read and `@Comment` on `@Nest` categories not being written
- Server changes not being broadcast, the LAN host not being able to edit server options, and `INFORM_SERVER` values not being sent on player connection
- Fixed the color controller crashing, and the item dropdown text covering the option name and placing its caret away from the mouse
- Fixed Save, Cancel, and Undo only updating on the tab where something changed, and config screens keeping their old size after a resize with a popup open
- Fixed Forge 1.20.1 mixins, attribute math, scrolling text, gradients, and the shared tooltip state

**_And much more!_**

## 3.0.1+1.20.1

### Fixes

- Fixed options being saved on disk but not applied to memory when multiple fields are modified at the same time in singleplayer clients
- Fixed ResolutionException due to incorrect shadowBundle declaration

## 3.0.0+1.20.1

**This update adds a brand-new and easy-to-use config system!**

### New Features

- Completely redesigned the config module from the ground up, making registering your configs possible in one line of code using `SLibConfig.syncedBuilder()` and `SLibConfig.localBuilder()`.
- Newly added config fields are automatically inserted with default values, old/useless keys are removed.
- Seven new annotations: 
  - `@Sync`:
    - `OVERRIDE_CLIENT`: server authoritative config, syncs to the client. Config broadcasts on login and updates in real time. Supports configurable permission checks (e.g., OP-only).
    - `INFORM_SERVER`: local config, informs server of changes
    - `NONE`: local config, no network sync
  - `@RangeInt`: clamps an int config field to a specified range
  - `@RangeFloat`: clamps a float config field to a specified range
  - `@RangeDouble`: clamps a double config field to a specified range
  - `@Comment`: allows for comments to be added to config fields (supports text blocks)
  - `@Ignore`: ignores a config field entirely
  - `@Nest`: allows for nested classes for more complex config structures
- Unmodded / Vanilla server compatibility: client instances safely fall back to default values for server authoritative options. Local options can be freely modified.
- The config also supports serializing common classes like AWT's `Color`, Minecraft's `TextColor`, `Identifier`, `Pattern`, and `UUID`.

### Fixes
- Fixed availability propagation in custom YACL controllers

### Testing
- Created a new test mod to test the new config system [S-Lib Test Mod](https://github.com/Stalemated/s-lib_test_mod).

## 2.1.1+1.20.1

### Fix
- Fixed Forge version not letting the user login to servers if the mod is not installed server-side and the player has a server-side mod that depends on it installed on the client
  - New behavior should allow the client to join but make the server-side features of the mod unusable

## 2.1.0+1.20.1

### Features

- Created SyncedConfigManager, a specialized ConfigManager that handles network sync between client and server configs
  - It allows for custom permissions to edit configs in game (such as OP only, etc...)
  - Same syntax as the recently added BaseConfigManager class (or even simpler)
  - Whenever a config file is edited in a server, the config applies instantly and gets automagically sent to all players

## 2.0.1+1.20.1

### Fix
- Fixed race condition in Forge making modded items not match when matching exact items using ItemStrategy

## 2.0.0+1.20.1

### Major Architecture Changes & Features

- **Serverside Support:** S-Lib is no longer a clientside exclusively mod! The entire architecture has been refactored to run on both clients and servers.
- **Cross-Platform Networking API:** Introduced `NetworkHelper`, a multi loader abstraction to send and receive packets between clients and servers.
- **Developer Experience & Cleanup:** 
  - Added complete Javadocs to `PlatformHelper` and `NetworkHelper`.
  - Cleaned up redundant multi-loader initialization classes (e.g., completely purged the functionally useless `SLibClient` class).

## 1.0.6+1.20.1

### Features

- Added an easy way to register keybinds for both Fabric and Forge (`PlatformHelper.registerKeyBinding(KeyBinding keyBinding);`)
- Added 3 different easing functions (lerp, ease out cubic, ease out expo) to MathUtils


## 1.0.5+1.20.1

### Feature

- Added color formatting (red) for the new Not Strategy in the ItemOrTagController

## 1.0.4+1.20.1

### New feature

- A new Not strategy was implemented, allowing to negate any condition from any other tag. This allows for matching all but one item, all but one namespace, or effectively negating a regex pattern (e.g. `!regex:#c:(swords|bows)` will match everything EXCEPT the `#c:swords` and the `#c:bows` tags.)

## 1.0.3+1.20.1

### New feature

- Regex strategy can now match tags (e.g. `regex:#c:(swords|bows)` will match the `#c:swords` and the `#c:bows` tags.)
  - The `#` symbol **ALWAYS** needs to be prefixed when matching tags in regex mode.

## 1.0.2+1.20.1

- Version bump to have parity with 1.21's version

## 1.0.1+1.20.1

### Fix
- Updated isModLoaded() in Forge to get the loading mod list (Fixes crashes with mixin plugins)
- Fixed mixins not being applied in Forge

## 1.0.0

**First release of S-Lib, a shared library designed to provide Stalemated's mods with common code.**

### Key Features
- **YACL Custom Controllers:** Extends *Yet Another Config Lib* with advanced, reusable UI elements:
    - *Advanced Color Pickers:* Supports hex codes, legacy formats, and visual color selectors in configuration screens.
    - *Item & Tag Autocomplete:* A dropdown selector that dynamically suggests Minecraft items and tags as you type.
    - *Simplified Dropdowns:* Clean dropdown selectors for enums and custom strings.
- **Target Matching System:** A robust system that allows mods to target Minecraft items using wildcards (`*`), namespaces (`minecraft:*`), tags (`#c:swords`), exact IDs, or regular expressions (Regex).
- **Advanced Color & Gradient Engine:** Built-in utilities to parse colors (Hex, legacy codes, or color names) and generate text gradient animations (Rainbow, Slide, and Breathing gradients).
- **Visual & Layout Helpers:**
    - *Horizontal Text Scroller:* A reusable renderer to scroll long text lines horizontally in UI menus.
    - *Indented Tooltip Components:* Custom components to support clean line indentation inside tooltips.
- **Blackboard State Manager:** A centralized state bridge (`SharedTooltipState`) to safely share rendering flags and tooltip dimensions between independent mods (like CTA's editor preview and STS's scrolling logic).
- **Cross-Platform Abstractions:** Reusable multi-loader helpers (via `PlatformHelper`) for seamless parity across Fabric, Forge, and NeoForge.