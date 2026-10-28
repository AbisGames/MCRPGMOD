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

The GameTest run has a 4 GB maximum heap. CI runs the three seeds as independent
jobs and retains logs and build reports even when a test fails. The generated
structure workflow also rebuilds the authored assets before running the tests.

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

Mine checks inspect regional ore blocks, reject foreign regional ores and post
real server break/place events. They test the configured cooldown, renewal and
the player-placed-ore exploit inside a registered generated mine. Dungeon
checks inspect the placed trial spawner and three chests.

**These fixtures deliberately force the selected valid start for coverage.**
They do not measure natural weighted structure-set frequency or prove that
every naturally selected placement has an accessible entrance. Distribution,
outer terrain access and detailed interior navigation need separate checks.

## Safety and limits

Placement checks retain block-entity and obstruction protection. New player
placements, multi-block placements and ordinary excavation are recorded in
per-dimension saved data. Natural mining nodes remain eligible for renewal;
player-placed ore does not. Protected player positions deliberately defer
authored placement instead of being overwritten.

Old saves do not contain historical provenance for natural-looking blocks
placed before this tracking existed. Piston movement and fluid provenance are
not covered by the current placement-history tests. The protection system
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
