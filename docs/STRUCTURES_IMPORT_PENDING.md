# Extended singleplayer structure pack (pending import)

An offline generated pack has been produced with:
- 439 Minecraft gzip-NBT structure templates (108 initial biome/type pieces + 331 authored structure-pool assets).
- 22 individually ornamented regional capital templates, modeled at 176x72x176 blocks.
- Furnished regional villages, linked village Jigsaw pools, four-room dungeons, mineral halls and loot chests.
- 54 regional dungeon/mine loot table definitions, 27 mine ore-node configurations.
- 70 collision-resolved named settlement locations documented in `generated/LANDMARK_PLACEMENT_PLAN.json`.
- Structural validator result: 439 NBT files and 8,676,309 block placements checked; 824 JSON files parsed, all Jigsaw pool NBT references exist.

**These assets are not yet in this Git branch.** The downloadable full overlay and
source generators were created in the ChatGPT conversation on 2026-10-27; the
asset package must be imported to `src/main/resources/` and tested in
Minecraft 26.1.2 before changing this status.

Release blockers: hero capitals are procedural architectural models, not final
artist-authored full cities; fixed-capital placements currently only have a
coordinate plan, not active runtime placement; mine and dungeon refresh remains
unwired to persistent server state; GameTest for the new asset pack not run.

Never claim singleplayer completion solely because the NBT format validator passed.
