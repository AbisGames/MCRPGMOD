# Worldgen verification

## Checkpoint — 2026-10-30

Worldgen is **verified for the server fixture coverage below**. Client visual/FPS
and natural-distribution limits remain explicit. Work is confined to
`48018390/MCRPGMOD-Development`, branch `worldgen-and-ores`.

| Scope | Runtime evidence | Result |
| --- | --- | --- |
| 22 capitals, 63 pieces each | [40733045807](https://github.com/48018390/MCRPGMOD-Development/actions/runs/40733045807), `28c3f5c` | All assemblies, foundations, roads and disk reload passed |
| 48 fixed settlements | Same broad run | All placements, contents, support and disk reload passed |
| 27 representative native villages/outposts | [41011624642](https://github.com/48018390/MCRPGMOD-Development/actions/runs/41011624642), `f966e28` | All 27 passed; no close stalls |
| Sky islands/End portal, unique sites, capital-core terrain | Broad run, all three seeds | Passed |
| Player provenance and ore renewal disk reload | Broad and subsequent native runs, all three seeds | Passed |
| Focused surface/deep/underwater mine+dungeon regressions | [41011529452](https://github.com/48018390/MCRPGMOD-Development/actions/runs/41011529452), `f966e28` | All six examples and protection suites passed |
| All 54 native mine/dungeon types | [41019418456](https://github.com/48018390/MCRPGMOD-Development/actions/runs/41019418456), `db0aea9` | All 54 passed; 18 per seed |
| Final provenance/ore reload regression | Same native run, all three seeds | All 19 required tests passed in each suite |

Catalogues are distributed by index modulo three over seeds **0, 123 and
987654321**: 22 full capital assemblies, 48 settlements, 27 native settlement
types and 54 expeditions in total. This is not a claim that every complete
assembly was placed on every seed. Terrain-core, sky and protection suites run
on all three seeds. Later production changes require their own relevant checks;
a previous green run does not automatically verify a newer commit.

Boss framework, Morvath, rituals, rare ores and upgrades are subsequent phases.
The 27 geometry prototypes remain on `biome-bosses`.

## Run the checks

Java 25, Minecraft 26.1.2 and NeoForge 26.1.2.124 are required.

```sh
python tools/validate_worldgen.py
python tools/validate_biome_assets.py
python tools/worldgen/validate_biome_mines.py
```

The CI workflow generates special landmark NBT and copies it into the mod
resources before `./gradlew build --no-daemon`. Follow those preparation steps
when building locally. Check the resulting binary, not just source JSON:

```sh
python tools/worldgen/validate_packaged_mod.py build/libs/pipican-0.1.0.jar
```

Run a selected real-server suite:

```sh
./gradlew runGameTestServer --no-daemon -PworldgenSuite=capital -PworldgenTestSeed=0 -PworldgenTarget=hallvard
./gradlew runGameTestServer --no-daemon -PworldgenSuite=expeditions -PworldgenTestSeed=123 -PworldgenTarget=umbral_chasm
./gradlew runGameTestServer --no-daemon -PworldgenSuite=villages -PworldgenTestSeed=987654321
./gradlew runGameTestServer --no-daemon -PworldgenSuite=protection -PworldgenTestSeed=0
```

On Windows use `gradlew.bat`. Available ordinary suites are `sky`, `terrain`,
`capital`, `settlements`, `villages`, `protection`, `unique` and `expeditions`.
Run all three catalogue shards to cover every entry. An empty native target
selects the seed's shard; an explicit region selects that region directly.
The optional `terrain_probe` suite requires one of the three capital targets
and is excluded from the default `all` selection.

### Workflows

- `worldgen.yml`: manual `focused_only=true` first checks Hallvard/Skelvik/Bogmere
  with their regression seeds. `focused_only=false` runs the full matrix.
  `probe_terrain=true` substitutes a density diagnostic for the first focused
  capital check; a diagnostic pass does not establish full assembly success.
- `expedition-access.yml`: `suite=expeditions` or `villages`. With
  `all_regions=true`, the three seed jobs cover the entire selected catalogue.
  Otherwise they target Heartlands, Umbral Chasm and Shimmering Reefs. Each job
  additionally runs the provenance/ore disk-reload suite.

Logs/reports are retained as diagnostic artifacts. The native workflow also
uploads a compiled test JAR from seed 0. A compiled-JAR artifact can exist even
when a runtime test failed: inspect the suite conclusions and logs separately.
Gradle dependencies are cached; sources, packaging and server tests still run.

## What the fixtures exercise

Tests create real temporary RPG `ServerLevel` instances. The harness replaces
GameTestServer's hard-coded seed/structure options only inside the fixture and
restores them afterwards. The production preset/noise generator, native biome
selection, registered structures, templates, processors and actual blocks run.
Test-only data and code are excluded from the shipped mod JAR.

### Capitals and fixed settlements

Each capital places all 25 districts and 38 road pieces through the production
placement service. Tests inspect foundations, a conservative walking connection
between district entrances, terminal progress and actual disk persistence,
including a reload after the first eleven pieces. The other 48 authored sites
verify placed content, support, saved completion and prevention of rebuilding.

Production capital ticks prepare neighboring chunks using expiring loading
tickets and place one near-player tile per interval. The synchronous fixture
finishes its requested chunks for deterministic inspection; it is not a client
performance benchmark. Template cache entries are released after direct placement.

Foundation Y is sampled from the real saved terrain, then retained for the city.
New cities use a centre-first tile order; legacy progress preserves its existing
order. Natural grading is bounded by a 24-block cut and 12-block foundation fill.
The complete grading/headroom volume is preflighted before mutation; inventories,
player provenance and artificial obstructions defer placement. A rejected
template restores any collected natural grading edits.

### Native settlements, mines and dungeons

Fixtures deliberately force a selected valid native start in an eligible biome
outside authored reservations. They install it before decoration, then generate
real chunks. Neighboring STRUCTURE_STARTS are checked first so the forced
fixture cannot overwrite an already selected natural structure. This controls
coverage; it does not measure naturally weighted placement frequency.

Village/outpost fixtures cover one representative type per each of 27 regions,
including all five exotic regions. They inspect actual furniture and support,
not every authored subbiome variant.

All 27 mine and 27 dungeon types inspect real interior blocks and support. Mine
checks reject foreign regional ore, post natural break events, test cooldown
and renewal, and place the actual ore BlockItem to reject player-ore renewal
exploits. Dungeon checks require the trial spawner and three regional reward
loot tables; decorative storage chests do not count as rewards.

`ExpeditionFoundationProcessor` extends bottom-floor blocks through air/fluid
to existing support within the current decoration chunk. Native access uses
`ExpeditionPoolElement`, which reserves a 16-block front corridor in the piece
bounds. `ExpeditionAccessProcessor` preflights and builds that route and its
outer escape ladder, clipped to each reserved decoration chunk. It retains
player provenance, inventory and artificial-block rejection; natural sculk,
moss and lush-cave growth are recognized explicitly. The room NBT is unchanged.

Generation/collision bounds include the access corridor. Interior support and
ore/reward checks use actual template bounds, excluding the added corridor and
StructureStart's terrain-adaptation padding. The pool codec is round-tripped,
and portal coverage is checked in all four rotations. Existing serialized
vanilla pool elements remain supported. These changes apply to new starts;
old structures are not silently regenerated or given retroactive exits.

The entrance search requires an actual walking/swimming/climbing route from the
rotated doorway to exterior terrain. It uses native collision shapes, a
0.6-block-wide/two-block-high body, a 1.25-block step bound, breathing room and
lava rejection. Foliage and air gaps below canopies do not hide exterior ground;
solid rock ceilings still reject underground endpoints. A paired foliage/stone
fixture checks this distinction. Search is bounded to 100,000 visited positions
and a local 32-block horizontal radius, so it can conservatively reject a long
winding natural cave exit beyond that area. This is not a full interior navmesh.

### Protection and persistence

Per-dimension provenance records player placements, multi-block placements,
ordinary excavation, piston movement and fluid-source changes. Player-placed
natural-looking ore remains ineligible for renewal. Due renewal nodes in
unloaded or pending-load chunks remain queued without forcing synchronous loads.
Tests save/reopen worlds and verify provenance, cooldowns, queue state and ore.

Provenance is initialized on server level load, cached by level identity, and
read by generation workers through a concurrent position set. Workers do not
load saved-data files. An unavailable cache rejects the access edit plan.
Access safety tests require an unblocked baseline before inserting protected
air, inventories or artificial blocks, and verify that natural-growth samples
survive placement. Appended edits stay inside the decoration box.

Temporary-world closure drains chunk work and queued server/POI callbacks while
storage remains open, reaches a short quiescent interval, saves, then closes.
A close watchdog records stalled stacks and fails the disposable JVM after
180 seconds. A server progress watchdog reports stalled server/worker stacks
and exits with failure after five minutes without a fixture/chunk checkpoint.
Neither watchdog suppresses assertions or converts a timeout into success.

## Remaining limits

- Historical player edits before provenance tracking cannot be reconstructed.
- Full redstone timing and provenance of every derived fluid flow are not covered.
- New terrain fixes do not rewrite already generated terrain or personal saves.
- Controlled native fixtures do not establish natural weighted distribution or
  exhaustive placement of every subbiome variant.
- Visual terrain quality, all interior navigation, client rendering and FPS
  require client inspection; server placement checks do not prove those results.
- Static catalogues, registry decoding, packaged NBT and runtime generation are
  separate checks. The packaged validator currently checks 1,439 structure
  templates, including legacy assets; that count alone does not prove placement.

The aquifer regression illustrates the distinction: Hallvard/Skelvik had
negative final density but still contained high stone because preliminary
aquifer surface estimates remained vanilla. Regional density now participates
in that preliminary surface, and the corrected complete assemblies passed
actual server tests. Spawn registries also reject wrong mob categories; the
13 erroneous ocelot entries were moved to CREATURE without changing weights.
