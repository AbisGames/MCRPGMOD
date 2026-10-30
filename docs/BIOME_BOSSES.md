# Biome bosses progress

Branch: `biome-bosses`. Worldgen remains at its verified implementation; no worldgen
production files were modified. Original checkpoint `93beb03` / 25 tests is retained
in history. All 27 original geometry Git blobs are preserved (CRLF conversion normalized).

## This session

- `194d1a5`: actual cleave/meteor/eruption damage, telegraphs, fixed marks, persistent
  impact state and temporary hazards. Actual damage/reload/dodge GameTest passed.
- `8c532d0`: craftable altar/core/Morvath sigil, real block interaction and spawn rollback,
  summoning animation, guaranteed heart/8 Cinderite/XP and 25% Infernium. Nine boss tests,
  28 total required tests passed in run41027720654.
- `daf607e`: 13x13 safe floor/11x11x5 clearance, owner cancellation through altar,
  native sword/shoulders, emissive animated eyes/cracks and native combat poses.
  Run41028128907 passed.
- `63fd915`: complete basic-swing/three-ability loop and stronger phase-two hazards.
  Actual phase-two damage/duration and hazard-pulse reload assertions; run41028428224 passed.
- `b1ebad1`: packaged-JAR assets/geometry hash validation, reward assertions, per-attack
  balance snapshots, isolated client QA directory. Local compilation passed; run41029291366 is queued for a runner at this checkpoint.
  Do not call this commit runtime verified yet.
- `9a3a729`: validates full arena height against dimension build limits and translates
  rejection messages for players; local main/test compilation passes, CI pending.

New block: `pipican:ritual_altar` (also an item).
New items: `pipican:ritual_core`, `pipican:morvath_sigil`, `pipican:ashen_kings_heart`,
`pipican:infernium`. No additional entities beyond the existing Morvath registration.
Ashlands dungeon ingredient is the already registered `pipican:ashlands_boss_core`.
No NPC/class-system implementation was overwritten.

See [playtest instructions](MORVATH_PLAYTEST.md) for exact recipes, platform size,
biome/height rules, telegraphs, cancellation and deterministic multiplayer rewards.

## Coverage and limits

GameTests cover real registered entities/attributes, combat state and compressed disk
roundtrip, phase latching, actual player damage/cone dodge, meteor mark dodge and reload,
temporary zones/phase-two damage and duration, no immediate replayed hazard pulse,
actual crafting recipes, real altar block interaction, forced rejected-spawn rollback,
exact sigil consumption, repeat interaction, owner cancellation, one-time loot,
pre-death reload replay, no cancellation loot, cleanup/NoAI/unload and existing regressions.
Packaged mod validation checks 27 original models, texture/model/class presence,
exact sigil ingredients and exclusion of GameTest code/data.

Local ECJ Java25 main/test compilation succeeds. Local client launch failed before
Minecraft started in NeoForm SDK compilation (package-info write error). ZIP preflight
succeeded; no client screenshots or visual/FPS/multiplayer play session are claimed.
Art/animation code is present, but final artwork/alignment is not verified. Meteor
projectile visuals are server-issued falling particle trails, not a separate projectile entity.
Normal save/reload is covered; crash atomicity across separate save files is not guaranteed.

## Remaining milestones

Finish client render/playtest and polish Morvath art before declaring the encounter final.
Infernium is implemented as a boss crafting material; the other 11 rare resources,
regional pickaxes/mining gates/natural deposits and blacksmith upgrade backend are pending.
The 26 other bosses remain unregistered geometry prototypes with catalog metadata;
full unique ability/loot/sigil/animation configurations and implementations are pending.

The supported NeoForm Eclipse-compiler alternative was tried once with a local-only
init script. Its bundled compiler rejected flexible constructor bodies as a disabled
preview feature while rebuilding Minecraft sources. This did not reach mod/client
execution either. No compiler-preview workaround or Windows security change was applied.

## Distributable checkpoint

`63fd915` passed all 28 required tests in run41028428224. Its distributable also passed
`tools/bosses/validate_boss_jar.py` locally. SHA256:
`425b27b21bc2ee98e05e459ae01dd08b95a55ae13b6e1e50ca2e13d240279d80`.
This is the server-verified playable development candidate; client artwork is unreviewed.
Do not confuse it with the older framework-only JAR or the newer queued `b1ebad1` candidate.
