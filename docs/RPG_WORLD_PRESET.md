# Experimental RPG World preset

Select `Pipican RPG World` in the world type list once enabled.

Uses `pipican:regional` biome source for region/subregion placement within
the 50k logical coordinate bounds. Outside the authored world, the fallback
biome is plains; world border still requires a separate server-side setting.

IMPORTANT: This is experimental: it uses vanilla Overworld height and noise
terrain, NOT extended -64..511 or custom capital-adaptive terrain. The biome
definitions presently have empty features and spawn settings, so these biomes
will not contain their intended forests/ores/mobs until biome generation
features are authored. Unique seeds are not yet derived from the world seed;
the preset currently supplies seed=0.

The structure pools JSON remains an authored manifest, not actual jigsaw
registrations. No real castle, dungeon or mine templates are placed by this
preset. Run GameTests before using it for actual survival saves.
