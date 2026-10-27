# RPG Biomes: original landscape implementation

## Content added on this branch

The regional overworld is intentionally authored around **22 fixed capitals**,
not random castle placement. The capital's known subregion wins within a
warped ~500-block inner region, while surrounding biome boundaries remain
irregular. All region coordinates fit the 50,000 × 50,000 world.

- 22 authored realms and their 120 original named subregion biome entries.
- Five additional exotic biomes: Aetheris Expanse, Verdant Depths,
  Umbral Chasm, Crimson Wilds and Shimmering Reefs.
- 27 **original aesthetic palettes**, each with 4 registered decorative
  blocks: stone strata, orientable tree trunk, decaying leaves and flowering
  undergrowth (108 registrable/usable blocks).
- 108 separately generated original 16x16 PNG textures, plus blockstates,
  block/item models, loot tables, mining tags and a Regional Nature creative tab.
- 81 configured terrain/vegetation features (one tree, bloom and rock style
  per palette) and matching placed features, plus rare and macro placements.
- 120 subregion-specific floral distributions and 83 distinct tree placement
  profiles reflecting forest, grassland and barren subregion classifications.
- 27 biome-specific cliff exposure rules in the noise settings; lower,
  vegetated terrain remains grassy where the ordinary vanilla surface rule applies.
- Terrain density adjustments create landmark-adjacent ridges and valley
  silhouettes: Sylvaran sheltered valley; Durnhold granite walls; Tianmen
  terraces; Morvath volcanic rim; Qasr Zahir dunes; coastal city island ridges.
- Minecraft's base caves, vanilla resources, fauna and common flora are retained
  as a compatibility foundation. RPG ores remain restricted to their own mines.

## How to inspect the result (singleplayer focus)

In the new world screen select **Pipican RPG World** as the world type.
Creative inventory -> **Regional Nature & Landscape** exposes all 108 new
blocks, grouped by realm. Travel to a parent realm to see its strata, forests
and low vegetation. The border is 50,000 blocks; dimension bounds are
Y=-64..511. Start a **new** world when changing worldgen settings.

Static checks: `python3 tools/validate_worldgen.py` and
`python3 tools/validate_biome_assets.py`. NeoForge compile, datapack-load
and registered-feature GameTests run in GitHub Actions at Java 25.

## What these tests *do not* prove

Worldgen screenshots, frame rate measurements, large-world chunk travel,
waterfall composition, biome boundary beauty and exact elevations against
the largest castles have **not** been manually inspected in a real client.
New tree geometry is generated from configurable trunk/canopy parameters;
it is distinct by species and height, not yet sculpted by Blockbench artists.
The regional variation is procedural, not a complete hand-painted terrain
heightmap. Some vanilla trees and native plants remain in each biome.

This patch does **not** change named city placement, dungeon locations,
mine locations, renewable mining, loot, classes, armor or RPG NPC logic.
Those tasks remain separate. Do not merge unrelated branches.
