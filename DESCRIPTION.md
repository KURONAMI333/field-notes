# Field Notes

> Auto-record your achievements with timestamp, location, and context — your own field journal that builds itself as you play.

## What it does

Every time you earn an advancement (vanilla or modded), Field Notes captures the moment in a personal chronicle:

- ⏰ **Timestamp** (real-time + in-world day)
- 📍 **Coordinates + dimension + biome**
- 🏆 **Advancement icon, title, description, frame type** (task / goal / challenge)

Open your chronicle two ways:
- Press **K** (rebindable; defaults to K to avoid JourneyMap's J binding)
- Right-click the **Field Notes** journal item

Browse via a **spread-page book viewer** styled after Patchouli — 4 entries per spread, vanilla page-turn arrows, search by keyword, filter by frame type. Biome and dimension names auto-localize to your client language (e.g. `Day 0 · 砂漠` in Japanese).

## Getting the journal

The journal item is the door to your chronicle. You can get it three ways:

1. **Auto-granted on world join** via the "Welcome to Field Notes" advancement — appears in your inventory the first time you spawn
2. **Craft it**: shapeless recipe `book + leather` *or* `book + rabbit_hide`
3. **Creative tab**: Tools & Utilities

## Why?

Long playthroughs blur together. You remember "I beat the Ender Dragon" but not "I was in a swamp at world day 47, in the rain, with iron armor and a bow". Field Notes makes those details persistent — your own quiet record of how the journey unfolded.

## Design philosophy

Field Notes is **observation only**. It does not change advancement requirements, difficulty, rewards, or any gameplay mechanic. It surfaces information the game already tracks internally — same philosophy as AppleSkin for hunger.

Works with **any mod that uses the vanilla advancement system** (Twilight Forest, Botania, Apotheosis, Cobblemon, ...) — no per-mod integration needed.

## Supported Loaders / Versions (v0.2.0)

| Minecraft | NeoForge | Forge | Fabric |
|---|:---:|:---:|:---:|
| 1.21.1 | ✅ | — | ✅ |
| 1.20.1 |  —  | ✅ | ✅ |

> *Forge 1.21.1 build was dropped in v0.2.0 — Patchouli has no Forge 1.21.1 release. The NeoForge 1.21.1 build covers the same Minecraft version (NeoForge is the de-facto loader for 1.21+).*

## Dependencies

- **Patchouli** is required (it ships the book texture this mod renders). Same dependency used by Botania, Hex Casting, and others.
- Fabric users: also requires Fabric API.

## Installation

1. Install your loader (NeoForge / Forge / Fabric) for your MC version
2. Install **Patchouli** for your loader/version
3. **Fabric only**: install Fabric API
4. Drop `fieldnotes-X.Y.Z-{loader}-{mc}.jar` into `mods/`
5. Join a world — the Welcome advancement grants you a Journal automatically

## License

MIT — modpack inclusion welcome, no credit required.

## Credits

- Author: KURONAMI
- Source: [github.com/KURONAMI333/field-notes](https://github.com/KURONAMI333/field-notes)
- Book texture: [Patchouli](https://github.com/VazkiiMods/Patchouli) by Vazkii
