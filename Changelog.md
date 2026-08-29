# Changelog

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