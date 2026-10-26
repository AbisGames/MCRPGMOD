# Renewable biome dungeon encounters — singleplayer baseline

27 regional dungeons have a Trial Spawner at their final chamber. Each
spawner selects a biome-themed vanilla enemy and enters an approximately
30-minute cooldown after its waves are defeated. The final prize comes from
`pipican:rewards/<region>_trial` and includes the region's unique boss core
and a small quantity of regional ore. Accessible dungeon chests contain
secondary loot only.

Every region's catalyst is registered as a `<region>_boss_core` item, and
can be used by future custom forging/smithing without consuming the dungeon
logic in advance. Trial-spawner configurations are regular datapack JSON.

**Not yet implemented:** named bespoke boss mobs, puzzle doors, chest
ownership, custom boss animations, arena reset of traps and blocks, dedicated
boss HP bars or equipment upgrades. These vanilla trial encounters are a
functional renewable combat baseline, not the finished class-specific RPG
bosses envisioned in concept art. Singleplayer visual QA is still required.
