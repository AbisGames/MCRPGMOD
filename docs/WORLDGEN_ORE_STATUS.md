# Worldgen + regional ores (class-independent)

## Implemented in this commit
- 50,000 x 50,000 logical map, scaling authored 30M coordinates by 1/600.
- The original 22 macro-regions and matching region-specific ores.
- Five original sub-biomes: Aetheris Expanse, Verdant Depths, Umbral Chasm, Crimson Wilds, Shimmering Reefs. One themed ore each.
- Pure deterministic warped region lookup, repeatable cell-based structure candidates.
- Server-side renewable site state machine scaffold and catalog of mine/dungeon upgrade catalysts.

## Not implemented (NOT playable yet)
- NeoForge custom biome registry, climate parameter mapping, dimension noise settings or height expansion (-64..511 target); no vanilla biome replacement.
- Region masks still include original macro rectangles with warped boundaries; exact 120 author-defined subregion weighted assignment not connected.
- Actual capital/castle, village, dungeon, quarry structures (.nbt / jigsaw pools); marker-based NPC anchors and world placement.
- Real registered ore blocks, drops, item textures, inventories, loot tables, worldgen restrictions.
- Site persistence and respawns from the server's saved-data, player entry/exit tracking, multiplayer loot rights.
- Build, dedicated server and game tests.

Do not merge this as a completed feature. The armors/tools belong on a separate future branch.
