# Biome bosses and progression checkpoint

Branch `biome-bosses`; current implementation checkpoint `8ff718e` plus the
uncommitted design-loader validation change described below.
Work is only in `48018390/MCRPGMOD-Development`. The old repository, `main` and
`worldgen-and-ores` were not modified. Existing worldgen algorithms and all 27
original geometry Git blobs are preserved. Rare deposits are independent additions.

## Implemented

Morvath has a basic swing, Infernal Cleave, Meteor Rain and Volcanic Eruption;
server-authoritative damage, fixed telegraphs, cooldown/recovery, persistent marks and
hazards, half-health transition and stronger phase-two behavior. Hazards never place
lava/fire or destroy terrain. Projectile visuals use falling particle trails.

Craftable reusable Ritual Altar/Core and Morvath sigil validate actual biome/Y,
dimension build bounds, loaded space, a safe 13x13 floor and 11x11x5 clearance.
One sigil is consumed only after successful spawning. Owner sneak-use cancellation,
cleanup, reservation persistence and terminal reward gating are implemented. Valid
completion grants one shared bundle: heart, eight Cinderite, base 150 XP and 25%
UUID-stable Infernium. See [playtest guide](MORVATH_PLAYTEST.md).

Native attachments add sword/shoulders/animated emissive eyes without editing the
original geometry. Native poses cover idle/walk/basic/three abilities/hurt/summon/
transition/death. These are art drafts pending actual in-world visual review.

Twelve rare materials, eight natural ore blocks and six regional pickaxes are registered.
All eight natural deposit types were observed in generated RPG/End chunks. Server
mining identity checks and tool-conditioned loot prevent wrong-tool/explosion bypasses.
Voidsteel/Celestium/Bloodcrystal are materials only until their planned bosses work.
See [resource rules and coverage](RARE_RESOURCES.md).

The blacksmith backend supports four tiers with +1..+10, monotonic revisions, safe
material transactions, evolution and set metadata while preserving item components.
Only Ashlands is an active material profile. No NPC interface or set-effect/class
rewrite is introduced. See [backend contract](BLACKSMITH_UPGRADES.md).

All 27 bosses have distinct design metadata: stats, sigil, three mechanics/counterplay,
phase behavior, rewards and model/animation mapping. The other 26 remain disabled
plans; only Morvath has a functional entity/ability binding. Runtime loading now rejects
duplicate sigils, invalid ingredient totals/heights, fake implementation markers,
incomplete/planned animations presented as live, and mismatched reward state. See
[design catalog](BOSS_DESIGNS.md).

## Verification

- `93beb03`, run41024349874: original 25/25 framework tests.
- `63fd915`, run41028428224: 28/28 tests with real Morvath damage/phase/altar/loot.
- `9a3a729`, run41030392276: passed with build-boundary and player-message fixes.
- `4d4c6fe`, run41066796125: 31/31 tests, including mining and feature safety.
- `27f951a`, run41068590614: 34/34 focused tests, then 35/35 with all eight native deposits
  and three blacksmith tests (forty upgrades and three evolutions, metadata preservation).
- `ba49c6e`, run41089596445: 35/35 focused tests including the complete design catalog.
- `8ff718e` plus current loader validation: local `build` and 36/36 required NeoForge
  GameTests, including live patron, resource-generation, upgrade and design validation.

The prior `ba49c6e` JAR passed the packaged-asset validator: original model hashes,
required textures/models/classes/recipes/profile metadata, no GameTest code/data.
Native-generation coverage is seed 0; it is not a statistical frequency or all-seed claim.
Normal disk reload is tested; crash atomicity across separate save files is not guaranteed.

## Client visual QA

Local SDK preparation was repaired in a task-local tool copy by replacing the older
bundled ECJ with the existing ECJ 3.44 compiler. No Windows security setting or release
CI compiler was changed. Minecraft 26.1.2 / NeoForge 26.1.2.124 opened successfully on
AMD OpenGL 3.3 in the isolated `run-client-boss` directory.

An opt-in GameTest-source client QA scene prepared a guarded isolated test arena and
visual-immunity player; it is excluded from distributed JARs. All ten native poses were
inspected in the approved isolated test world, and sword grip, armor overlap, animation
timing, phase transition and death-floor clearance were corrected. See
[animation QA notes](MORVATH_ANIMATION_QA.md) and the final captures in
`outputs/morvath-animation-qa/`. This is singleplayer visual QA, not final artwork or a
claim of measured FPS, survival balance or real multiplayer validation.

Latest local validation artifact: `build/libs/pipican-0.1.0.jar`. The older checkpoint
artifact link/hash above must not be presented as containing the newest design-loader
guard until the current artifact is copied and validated.
