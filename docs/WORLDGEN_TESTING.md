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
