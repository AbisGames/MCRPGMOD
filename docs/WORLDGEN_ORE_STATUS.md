# Worldgen and ores: current state

Implemented: 50k x 50k macro scale, 22 authored region bounds, 120 authored
subregion definitions and sampling, 70 protected landmark center/radius
records, 331 authored pool IDs, 125 custom biome datapack definitions, five
original fantasy biome names, 27 registered ore blocks/items, 27 PNG ore
textures, 27 block models, block loot tables, renewable site state model
and deterministic structure candidate planning.

Not complete: No in-world .nbt structure pieces/pools/placement, registered
Overworld biome source, region-aware terrain and surface rules, enhanced
dimension height, vanilla biome features inside custom biomes, protected
mining interactions, persisted mine/dungeon timers, server inventories and
loot transaction rights, client/mining GUIs. No successful Minecraft 26.1.2
test has been demonstrated.

The 125 biome definitions currently have empty feature/spawner arrays and
should not be selected by a production world preset. Worldgen source JSON
files are loaded by Java helper classes but are not an automatically active
world preset. The source layout is not a game map by itself.

Run `python3 tools/validate_worldgen.py` for static checks; use
`./gradlew build` under Java 25 for compile validation when CI is available.

Do NOT merge as a fully working worldgen + ore implementation yet.
