# Regional surface palettes (26.1.2)

The RPG dimension's original vanilla Overworld noise routing and cavern
features are retained. A conditional top-surface rule adds regional terrain
materials for the 120 authored subregions plus five fantasy biomes. The rule
only affects solid `stone_depth=floor` under `above_preliminary_surface`.

This is a visual terrain identity pass, not fully custom macro-geomorphology:
river routing, plateau geology, dynamic castle valleys and floating islands
still need dedicated noise/density work. Existing world chunks will not be
retroactively changed. Always test in a new RPG world save.
