# Changelog

## 1.0.2+1.21.1

### Fix
- Updated isModLoaded() in NeoForge to get the loading mod list (Fixes crashes with mixin plugins)

## 1.0.1+1.21.1

### Fix
- Changed AttributeGetter methods from private to public

## 1.0.0+1.21.1

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