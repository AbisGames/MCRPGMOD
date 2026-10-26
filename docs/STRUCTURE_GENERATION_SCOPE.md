# Full authored model generation / honest scope

Run `python3 tools/worldgen/build_full_structures.py` then
`python3 tools/worldgen/validate_full_structures.py`. The script reads
`src/main/resources/data/pipican/worldgen/region_map.json`,
`structure_pools_manifest.json` and `settlements.json`.

The source contains exactly 331 settlement/landmark pool IDs, 70 named
settlements (22 capitals), 22 5x5 palace/district assemblies, five new fantasy
biomes, and 27 regional dungeon/mines. The generated content includes 923
distinct NBT templates with interiors and game-block palettes. Paths inside
`tools/worldgen/output/data` are ready to copy to
`src/main/resources/data`.

The capital 5x5 districts are included as separately placeable templates
and manual `.mcfunction` assembly layouts. Named capital placements at exact
coordinates are NOT yet connected to real terrain generation. Functional
NPC interaction, boss scripting, loot ownership, and renewable ore respawns
still require server runtime code and singleplayer playtests. No claim is
made that the environment mirrors concept illustrations pixel-for-pixel.

See the GitHub Actions `structure_assets.yml` workflow for build, test and
deployment. Do not merge without checking CI and loading the RPG preset.
