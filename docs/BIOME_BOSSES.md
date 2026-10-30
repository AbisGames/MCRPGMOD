# Biome boss development

## Current checkpoint: server framework

Branch `biome-bosses`; verified production code `93beb03`.
Worldgen checkpoint `b0f3443` was merged at `9f170c9`. All 27 original `.geo.json`
blobs still match `7aafa4f` exactly. See `WORLDGEN_TESTING.md` for worldgen coverage.

Implemented:
- Shared metadata catalog for all 27 regions; only Morvath is registered as an entity.
- Entity attributes, persistent mob, native multiplayer tracking boss bar.
- Server combat clock: windup/recovery/cooldown and a latched half-health phase.
- Entity serialization and synchronization of phase, ability, remaining ticks,
  cooldown and resolved impact. An impact cannot repeat after normal reload.
- Per-dimension encounter SavedData: owner, arena reservation and terminal tombstones.
  Active reservations survive chunk unload. Completion can be claimed once.
- Summoning service checks the actual 3D biome, loaded arena, collision/headroom and
  a sturdy player-built floor. It never modifies blocks. Failed spawn rolls back
  its reservation; callers must consume ingredients only after SUCCESS.
- Managed bosses cancel after 600 loaded entity ticks without eligible nearby players.
  Owner cancellation, missing/terminal records and permanent entity removal clean up
  safely, including when NoAI is set. Chunk unload preserves the encounter.
  Leaving the dimension cancels it. Terminal records gate all death reward processing.
- Native client renderer converts preserved Bedrock box geometry into model parts;
  idle/walk/head movement and two original, reproducibly generated basalt texture drafts.
  Prototype scale is already baked into geometry and is not multiplied again.

## Runtime verification

[Run 41024349874](https://github.com/48018390/MCRPGMOD-Development/actions/runs/41024349874),
commit `93beb03`: **all 25 required GameTests passed**, including six boss tests:

1. Interrupted windup/reload after impact: no early or duplicate effect, cooldown completes.
2. Half-health phase remains latched after healing/reload, transition runs once.
3. Registered entity attributes and combat NBT survive compressed disk roundtrip.
4. Arena/owner reservation and terminal completion survive compressed disk roundtrip;
   overlapping encounters, repeated completion and terminal UUID reuse are rejected.
5. Actual mock server player and Ashlands biome fixture: positive spawn on a player-built
   floor; wrong biome, same XZ at a different Y, solid obstruction and duplicate rejection;
   non-owner cancellation rejection and owner cancellation/entity cleanup. Floor survives.
6. Chunk unload retains reservation; abandoned NoAI entity cleans up after reload;
   permanent removal releases its arena without awarding a completion.

Existing class, worldgen catalogue/access safety and player provenance/ore renewal
reload regressions also passed in this run. Tests live only in the GameTest source set.
The published development JAR was checked for all 27 geometries, both draft textures,
expected common/client classes and absence of GameTest classes.

Earlier successful checkpoints: `26dcac0` / run41022507755 (22 tests),
`d0c1d36` / run41022987435 (23 tests). Later fixture debugging corrected an obsolete
ChunkPos constructor and a FULL-only ticket that did not simulate entities; assertions
were retained, and the final positive fixture uses an entity-ticking ticket.

## Still unfinished

Morvath is **not a completed playable fight**. There are no damaging attack effects,
ritual altar/sigil items, finished loot, full combat animations or reviewed final artwork.
The clock enumerates attacks but does not execute them yet. The summoning service is an
internal API; command spawning remains for development. Other 26 bosses are unregistered
geometry prototypes. Client appearance, FPS, a real multiplayer play session and crash
atomicity across independent world files have not been verified.

Next implementation: Morvath Infernal Cleave, marked Meteor Rain and temporary
non-terrain Volcanic Eruption; phase visuals/animations and loot; then altar/sigil
transaction and player-facing summoning. Remaining boss batches, rare ores and armor
upgrade service follow the user-requested phase order.
