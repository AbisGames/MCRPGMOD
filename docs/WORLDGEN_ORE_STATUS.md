# Singleplayer worldgen status

Current code has the 50,000x50,000 region coordinate system, 22 macro-regions, 120 authored subregions, five added fantasy biomes and 125 biome registrations.
It includes the experimental RPG world preset (Y=-64..511), regional surface palettes, regional mountain/ocean density contribution and 27 regional ore blocks.
Twenty-seven dungeon types contain renewable vanilla trial-spawner encounters with biome-themed enemies, boss-core catalyst rewards and supporting loot tables.
The reproducible generator creates 967 binary NBT templates, 331 authored pools and 22 capital layouts of 25 architectural pieces plus 38 walkways per capital.
All 70 named settlements have incremental, persisted near-player placement in an RPG world.

Important release blockers:
1. Generated assets are modular furnished block architecture, not hand-sculpted recreations of every design reference.
2. No visual singleplayer inspection of all castles, town rooms, dungeon routes, mine reset cycles or saved-world migration has been completed.
3. Incremental city assembly can load nearby chunks and overwrite player edits; replace with safe worldgen/chunk staging before production.
4. Mine node restoration is now restricted to tagged RPG mine structures, but still needs repeat-mine gameplay tests and protection against manual template placement exploits.
5. Custom scripted RPG bosses, NPC population and player-specific dungeon loot are not ready.
6. Region-aware mountains are experimental; rivers, ravines, waterfalls, floating-island geology and all capital foundations still need visual tuning.

Validation: python3 tools/validate_worldgen.py; python3 tools/worldgen/build_full_structures.py; python3 tools/worldgen/validate_full_structures.py; ./gradlew build; ./gradlew runGameTestServer.
Do not merge into main until dedicated solo-save exploration and regression tests are finished.