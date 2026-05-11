# Field Notes — Changelog

## v0.2.0 (2026-05-11) — The Chronicle Update

### Major: New spread-page UI
- **Spread-page book viewer** with Patchouli's `book_brown.png` texture
- **4 entries per spread** (2 per page) with proper journal-style hierarchy
- **Vanilla `PageButton`** (curled-arrow sprite) for page navigation
- **Auto-localized biome & dimension names** via vanilla translation keys
  (e.g. `Day 0 · 砂漠` in Japanese, `Day 0 · Desert` in English)
- Per-entry layout: `No. ##` → title (frame-colored, bold) → coords → biome → wall-clock
- Search box + frame-type filter still live below the spread

### New: Journal item
- Right-click the **Field Notes** item to open the chronicle viewer
- Item appears in the **Tools & Utilities** creative tab
- Shapeless recipe: `book + leather` *or* `book + rabbit_hide`

### New: Welcome advancement
- Automatic Journal grant on world join via the `fieldnotes:welcome` advancement
- The advancement itself becomes the first entry in your chronicle — recursive bootstrap

### Fixes
- Removed the pause-screen blur that made the book "look behind glass"
- Fixed keybinding collision with JourneyMap (J → K, rebindable)
- Title text now wraps to two lines when long instead of getting truncated
- Page indicator and "Field Notes" header no longer clip into the top frame

### Dependencies
- **Patchouli is now required** (book artwork). Same dep used by Botania, Hex Casting, etc.
- Forge 1.21.1 build dropped — Patchouli has no Forge 1.21.1 release;
  use the NeoForge build instead.

### Loader / version coverage (v0.2.0)
| Loader | 1.20.1 | 1.21.1 |
|---|---|---|
| NeoForge | — | ✅ |
| Forge | ✅ | (dropped) |
| Fabric | ✅ | ✅ |

## v0.1.0 (initial release)
- Auto-record advancements with timestamp, location, biome
- List-style viewer with search + filter
- JSON persistence per player UUID
