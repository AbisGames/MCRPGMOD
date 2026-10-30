# Worldgen server integration tests

Use Java 25 and the Gradle wrapper. No personal Minecraft save is used.

```powershell
python tools/validate_worldgen.py
python tools/validate_biome_assets.py
python tools/worldgen/validate_biome_mines.py
.\gradlew.bat build --no-daemon
python tools/worldgen/validate_packaged_mod.py build/libs/pipican-0.1.0.jar
.\gradlew.bat runGameTestServer --no-daemon -PworldgenTestSeed=0
.\gradlew.bat runGameTestServer --no-daemon -PworldgenTestSeed=123
.\gradlew.bat runGameTestServer --no-daemon -PworldgenTestSeed=987654321
```

For a focused regression while debugging:

```powershell
.\gradlew.bat runGameTestServer --no-daemon -PworldgenTestSeed=0 -PworldgenSuite=capital -PworldgenTarget=aldermere
.\gradlew.bat runGameTestServer --no-daemon -PworldgenTestSeed=0 -PworldgenSuite=expeditions -PworldgenTarget=heartlands
```

CI runs focused capital regressions first, followed by regional settlements and the full matrix in
separate Java processes per integration suite, retaining each suite's logs. A
focused pass cannot make the workflow green without the full integration run.

The manually dispatched workflow has a `focused_only` input. Its default `true`
runs the three previously failing capital/seed pairs only: Hallvard/0,
Skelvik/123 and Bogmere/987654321. This is an explicit verification checkpoint;
it does not establish full worldgen completion. Dispatch with `focused_only=false`
to run the complete matrix. Push and pull-request runs retain the full matrix.

The GameTest run has a 4 GB maximum heap. CI runs the three seeds as independent
jobs and retains logs and build reports even when a test fails. The generated
structure workflow also rebuilds the authored assets before running all three
seeds. Publishing generated assets waits for every seed job to pass.

The asset sequence is full generation, full-catalogue validation, then special
landmark integration. Repeating full generation clears only the four known
special NBT outputs from the generated directory before recreating the base
catalogue; it never removes templates from source resources or Minecraft saves.

## Actual world generation

`WorldgenIntegrationTests` creates real RPG `ServerLevel` instances using the
registered RPG preset, noise settings and biome source. Each test uses a fresh
temporary directory below the GameTest world's `rpg-integration` directory.
Directories are retained for investigation; the harness never deletes saves.

Vanilla's GameTest server hardcodes seed 0 and disables structure generation.
The harness temporarily replaces that server instance's world-generation
settings for the selected seed and restores them in a `finally` block. Passing
only the `ServerLevel` biome-zoom seed would not test different noise seeds.
This reflection is test-only and is not packaged in the mod JAR.

The integration suites assert:

- Generated terrain and building clearance on the seven authored sky islands.
- Actual placement of the Sky Citadel and all six floating villages.
- Activation of the End portal by inserting eyes through vanilla item logic.
- Generated solid terrain at every capital's centre for each seed.
- All 22 capital assemblies across the three jobs, with 63 pieces per capital,
  real disk save/reload after eleven pieces, foundation checks and a walking
  search from the keep approach to the 25 district entrances.
- The complete, initially unlit Nether portal in Morvath's central keep.
- Actual Blackspire and underground Umbral City placement.
- All other 48 authored sites across the three seeds, including actual storage
  placement, foundations and completion/block persistence after disk reload.
- Player placement provenance and pending ore renewal across disk save/reload,
  including a previously loaded, then unloaded queued chunk.

The capital and regional expedition catalogues are distributed by catalogue
index modulo three. Run all three commands to cover every entry. A failure in
one capital or expedition is collected and reported without skipping the
remaining entries in that job.

## Regional mine and dungeon fixtures

`RegionalExpeditionTests` covers all 27 mine and 27 dungeon types across the
three jobs. It asks the registered native structure to generate a valid start
in an eligible biome outside authored reservations. It installs that start in
a protochunk before references and decoration, then generates real full
chunks. This exercises native structure pieces, terrain generation and actual
placed blocks, rather than merely decoding NBT or calculating coordinates.

Mine checks inspect regional ore blocks, reject foreign regional ores, post
server break events and place the actual ore BlockItem through item-use logic.
They test the configured cooldown, renewal and
the player-placed-ore exploit inside a registered generated mine. Dungeon
checks inspect the placed trial spawner and three regional reward loot tables;
decorative storage chests are excluded from the reward count. Foundation
samples use actual piece bounds, excluding the terrain-adaptation padding
added by StructureStart to its overall bounding box.

Native expedition pools use `ExpeditionFoundationProcessor` to extend solid
bottom-floor blocks through air and fluid down to existing support. Its writes
stay within the chunk decoration box. It stops at existing non-replaceable
blocks and block entities. Noise adaptation alone did not support every column
under these large templates on uneven terrain and ocean floors.

The `villages` suite covers one native regional settlement per region (27 in
total), preferring a village and falling back to another regional settlement
where no village is authored. The five exotic village types are included.
These fixtures inspect real placed furniture and foundations. They are
representative coverage, not exhaustive placement of every subbiome variant.

**These fixtures deliberately force the selected valid start for coverage.**
They do not measure natural weighted structure-set frequency or prove that
every naturally selected placement has an accessible entrance. Selected mine
and dungeon fixtures include the local entrance-access check below; broader
distribution and detailed interior navigation need separate checks.

## Safety and limits

Placement checks retain block-entity and obstruction protection. New player
placements, multi-block placements, piston movement, fluid sources and ordinary excavation are recorded in
per-dimension saved data. Natural mining nodes remain eligible for renewal;
player-placed ore does not. Protected player positions deliberately defer
authored placement instead of being overwritten.

Authored terrain grading separates a maximum 24-block natural cut from a
12-block foundation fill. The complete grading volume is checked before any
block changes, including headroom above short road pieces. Player provenance,
inventories and non-natural obstructions still defer placement. Grading keeps
the already-saved city elevation, so partially assembled cities stay aligned;
an invalid/empty template restores the cleared natural blocks on failure.

Old saves do not contain historical provenance for natural-looking blocks
placed before this tracking existed. Piston provenance is checked using the
native resolver/event; full redstone timing and derived fluid-flow provenance are not covered
by the current placement-history tests. The protection system
must not be described as a complete historical reconstruction of user edits.

Static catalogue validation and JAR/NBT checks are separate from runtime
generation. A green formatting check alone does not establish worldgen success.
The packaged-JAR validator checks every structure entry, including compression,
palette indices, sampled block coordinates and special landmark inclusion.

The walking check is a conservative two-block clearance search around the
capital ground floor. It does not validate every room, staircase or artistic
detail. Visual terrain quality, full interior navigation and client FPS require
a Minecraft client inspection. The synchronous fixture's mass chunk generation
is not a client performance benchmark; production capital ticks prepare chunks
asynchronously using expiring loading tickets.

## Capital aquifer regression

The 2026-10-28 terrain probe reproduced Hallvard seed 0 at (-3929,-12705)
and Skelvik seed 123 at (8921,-21095). Their noise densities were respectively
-11.18 and -19.39, but the generated columns still contained stone at Y=129
and Y=158. Final-density terracing alone does not constrain aquifer barrier
stone: the aquifer was using the unchanged vanilla preliminary surface.

The RPG preliminary surface now includes the same regional elevation density.
Its search starts at Y=432 to include regional mountains and floating islands;
regional elevation ends above Y=430 and vanilla terrain ends below that.
Aquifers remain enabled. This changes new terrain, not already generated
chunks. Verify the three focused capital assemblies after the diagnostic probe;
a green probe alone does not prove full assembly or worldgen completion.

The expedition fixture additionally searches actual placed blocks from the
rotated nine-block front doorway to terrain at least eight blocks outside,
with two-block clearance and an open-sky breathing position. It permits
one-block walking steps, water swimming and climbable blocks, rejects lava,
and bounds the local search to 100,000 visited positions. It can conservatively
reject a long winding cave exit outside that local search volume. It is an
entrance-access check, not a full interior navigation or distribution test.

## Verified checkpoint ? 2026-10-28

Worldgen is **partially verified**. Commit `27d93f8` passed the actual NeoForge
server focused regression in the personal repository:
[run 40731587424](https://github.com/48018390/MCRPGMOD-Development/actions/runs/40731587424).

| Seed | Capital | Result |
| --- | --- | --- |
| 0 | Hallvard | 63 pieces, foundations, road connectivity, disk reload; all 18 required tests passed |
| 123 | Skelvik | 63 pieces, foundations, road connectivity, disk reload; all 18 required tests passed |
| 987654321 | Bogmere | 63 pieces, foundations, road connectivity, disk reload; all 18 required tests passed |

The earlier failing density probe is retained in
[run 40730694226](https://github.com/48018390/MCRPGMOD-Development/actions/runs/40730694226).
No excavation bound was increased for the aquifer fix. These successes do not
verify every capital, settlement, expedition entrance or sky island. Those
require the full matrix, including the entrance test added in `0fe0e10`.
Boss framework, Morvath, rituals, rare ores and upgrades remain subsequent phases.

## Deep expedition access

Focused entrance run
[40733574565](https://github.com/48018390/MCRPGMOD-Development/actions/runs/40733574565)
passed Heartlands seed 0 and Shimmering Reefs seed 987654321, but reproduced
inaccessible Umbral Chasm mine and dungeon entrances at seed 123. Both deep
structures were at floor Y=-21 without a route to open terrain.

`ExpeditionAccessProcessor` adds a front-boundary ladder and supporting casing
when the outside terrain differs from doorway height by more than four blocks.
It connects that height to the actual outside surface, including underwater
and elevated terrain, using only the template's X/Z footprint and the current
chunk decoration box. It checks player provenance, non-natural blocks and block
entities before returning local edits. State rotation is left to native template
placement. Flat, already reachable entrances remain unchanged. This processor
is attached only to the 54 mine/dungeon pools, never the village pools.

Changes affect newly generated expeditions. Existing structures do not acquire
a shaft retroactively. The focused access workflow runs three representative
regions separately from the full worldgen workflow; neither scope replaces the
other. The access fix must pass the server regression before being called verified.

`expedition_access_safety` separately exercises the edit-plan preflight in an
actual test level: protected air, inventories and artificial blocks must reject
the local shaft plan without a world mutation. Appended edits must stay within
the decoration box, and empty data-marker processing must remain empty.

Synchronous fixture work has a wall-clock progress watchdog. After two minutes
without a fixture/chunk checkpoint it records server and worker stacks; after
five minutes without progress it terminates only the disposable GameTest JVM
with exit code 4. Each capital tile, native structure chunk and world-close
checkpoint refreshes progress. This turns a stalled fixture into diagnosable
failure, without bypassing assertions or treating the suite as successful.

Player provenance is initialized on server level load and cached by level
identity. Native access workers read that cache and a concurrent position set;
they never load saved-data files. An unavailable cache rejects the shaft plan.
Temporary integration levels explicitly perform the same initialization before
requesting generated chunks. Existing saved provenance is still serialized in
the original format and disk-reload assertions remain enabled.

Server logs exposed 13 Pipican biomes with ocelots incorrectly assigned to the
monster category. Their entries now use the registered creature category,
preserving spawn weights and pack sizes. The registry test checks every
Pipican spawn entry against its EntityType category to prevent this regression.

## Completed broad-run evidence reviewed 2026-10-30

[Run 40733045807](https://github.com/48018390/MCRPGMOD-Development/actions/runs/40733045807)
completed at `28c3f5c`. All 22 capital assemblies and all 48 authored settlements
passed their three distributed seed shards, including disk reloads. All three
seed jobs passed sky/End portal, player-protection/ore reload, unique landmark
and capital-core terrain steps. This remains a partial worldgen checkpoint:
expedition entrances failed, one village shard failed, and one focused village
step timed out. The newer provenance/access implementation requires its own
regressions; these broad results do not verify later code automatically.

The native fixture close trace identified a queued POI callback waiting on
SectionStorage while draining server tasks. Test closure now always drains both
chunk work and queued server callbacks to a short quiescent interval before
closing a temporary world's storage. Save/reload assertions remain unchanged.
