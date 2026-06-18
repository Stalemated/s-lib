# S-Lib

**S-Lib** is a personal utility library for Minecraft modding. It provides a common codebase for text rendering, target matching, and advanced configuration GUIs.

---

## ✨ Key Features (For Developers)

### 🧩 Advanced YACL Controllers
- **Advanced Color Controller:** A color controller that adds support for color validation of different formats (`0xFFFFFF`, `xFFFF00`, `#FF00FF`, `00FFFF`, also supports Alphas) as well as Minecraft Color names (`red`, `blue`, etc...) and legacy color codes (`&3`, `&a`, etc...).
- **Item Or Tag Controller:** A controller that lets you match to any item or tag in the game, supports namespaces and Regex as well.
- **Simple Enum and String Controllers:** YACL doesn't include a simple dropdown controller that can be used to pick an item from a list. It allows you to search and pick a specific entry, which could get annoying. This adds very simple dropdown controllers for both Enums and Strings.

### 📜 Tooltip Component
S-Lib includes a tooltip component that lets you specify an offset to render Indentations in a tooltip without adding any extra spaces to the tooltip's text.

### 🖱️ Scrolling Text Renderer
A highly optimized, reusable rendering utility (`ScrollingTextRenderer`) that allows any text or component to scroll seamlessly within a defined bounding box, complete with clipping and mouse interactions.

### 🎯 Target Matcher Framework
A powerful matching engine (`TargetMatcher`) capable of parsing and evaluating item targets via:
- Exact Item IDs (`minecraft:diamond_sword`)
- Item Tags (`#c:swords`)
- Mod Namespaces (`minecraft:*`)
- Regex Patterns (`regex:.*_sword`)
- Wildcards (`*`)

### ⛏️ Utilities
S-Lib includes several utilities, like:
- A helper to get some attributes from items
- Color utilities related to gradient calculation and color parsing from strings
- Generic math utils
- A utility to calculate perfect smooth scrolling text

### 🔄 Shared Tooltip State
A centralized state manager (`SharedTooltipState`) that allows different mods (like CTA and STS) to communicate and share tooltip dimension limits, rendering phases, and active configurations seamlessly without tight coupling.

---

## 🏗️ Building from Source

To compile S-Lib and make it available for other mods in your local workspace:

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

## 🌍 Platform Support

| Platform | Versions       |
|----------|----------------|
| Fabric   | 1.20.1, 1.21.1 |
| Forge    | 1.20.1         |
| NeoForge | 1.21.1         |

---

## 📦 Dependencies

#### Fabric only
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)

---

## 📄 License

This project is licensed under the **MIT License**.
