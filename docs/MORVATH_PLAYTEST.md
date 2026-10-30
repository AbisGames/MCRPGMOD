# Morvath development playtest

Minecraft 26.1.2 / NeoForge 26.1.2.124 / Java 25. Use a separate test world.
Verified worldgen was preserved. The other 26 bosses are not registered as finished encounters.

## Crafting

Ritual Altar:

```
Obsidian        Amethyst Shard  Obsidian
Crying Obsidian Ender Pearl     Crying Obsidian
Obsidian        Diamond         Obsidian
```

Ritual Core:

```
Amethyst Shard Gold Ingot  Amethyst Shard
Gold Ingot     Ender Pearl Gold Ingot
Amethyst Shard Gold Ingot  Amethyst Shard
```

Morvath sigil (`C` Cinderite, `D` existing Ashlands dungeon core
`pipican:ashlands_boss_core`, `R` Ritual Core, `M` Magma Cream):

```
CCC
CDC
DRM
```

## Arena and interaction

Place the altar in an actual Ashlands biome, at Y -64..448 and within the
world's build limits, at least five blocks below the world ceiling. Biome lookup includes Y; the same XZ in another biome is rejected.
The spawn point is **six blocks north of the altar**, at its base Y.
Build a solid **13x13 floor**, centered on that spawn point, with **11x11x5 clear
space** above it. Fluids, magma and campfire floors are rejected. No terrain is edited.
Surrounding chunks within 24 blocks must already be loaded.

Use the sigil on the altar. A successful spawn consumes exactly one, including
creative mode. Wrong biome, blocked/unsafe floor, another active encounter or rejected
spawn consume none. The altar is reusable after completion/cancellation.
Sneak-use it with an empty hand to cancel your nearest owned encounter within 64 blocks.
Cancellation grants no rewards or refund. Another player cannot cancel your fight.

Fight within 24 blocks of the encounter center. A boss displaced over 32 blocks
cancels safely. After 600 loaded entity ticks with no living, non-creative,
non-spectating players nearby, it cancels. Chunk unload pauses and preserves the
fight rather than granting rewards or releasing a live reservation.

## Combat

600 health, 12 armor. Basic swing plus three telegraphed attacks:

- Infernal Cleave: 30-tick windup, fixed heading, configurable 6-block/120-degree
  cone, 14 damage (18 in phase two), knockback and two seconds of ignition.
  Dodge behind or outside the marked cone. Range/half-angle are server-configurable
  in `pipican-bosses-server.toml` and are snapshotted per attack.
- Meteor Rain: 50-tick warning, fixed position markers, falling flame trails,
  one server-authoritative area impact. Leave the circles. Overlapping impacts
  cannot hit a player twice in the same cast. At most six markers per cast;
  phase two adds adjacent markers.
- Volcanic Eruption: 45-tick warning, temporary areas pulse every 20 ticks.
  Phase one: 80 ticks/4 damage; phase two: 120 ticks/7 damage. No lava blocks,
  explosions that edit terrain or fire placement.

At half health a single transition interrupts the current windup. Phase two
persists through healing/reload, moves faster and reduces attack cooldowns.
Normal game armor/difficulty/fire resistance rules apply. No class selection is required.

## Rewards

One shared ground-drop bundle per valid completed encounter, available to the group
under normal Minecraft pickup rules; **not one bundle per participant**:

- One Ashen King's Heart.
- Eight Cinderite.
- 150 XP before other mods' experience hooks and normal game rules.
- One Infernium with a deterministic 25% roll from encounter UUID hash modulo four.
  Reloading never rerolls the encounter.

The terminal encounter claim gates all reward processing. Administrative cancellation,
invalid/unmanaged command-spawned bosses and replay of a pre-death save grant no rewards.
Normal reload behavior is tested; power-loss atomicity across independently saved world
files is not guaranteed.

## Visual QA limits

Native model attachments add a sword, shoulder plates and animated emissive eyes,
without editing any of the 27 prototype geometry files. Native poses cover idle,
walk, basic swing, cleave, meteor, eruption, summoning, hurt, transition and death.
These remain unreviewed art drafts until client inspection establishes alignment,
scale, clipping and readable animation silhouettes.

Local client preparation initially failed in NeoForm SDK compilation. The isolated QA
runtime now uses a task-local copy with ECJ 3.44 and Minecraft opens successfully.
This does not modify the release build compiler or Windows security settings.

For the opt-in development fixture, run the client with `-PclientQa=true`; its save root
is explicitly checked and only integrated singleplayer is accepted. The GameTest source
prepares a test platform, altar and four sigils with visual-immunity effects. The first
boss pauses in its summoning pose for inspection; hotbar 9 toggles AI and hotbar 8
triggers phase two. This fixture never ships in the mod JAR. It is for visual inspection,
not proof of player damage/balance. Actual damage is covered by server GameTests.

`-PclientGameDir=...` selects an isolated directory; default is `run-client-boss`.
On 2026-10-30 the approved isolated world was inspected on NeoForge 26.1.2.124.
Using a sigil on the altar consumed exactly one (4 -> 3), spawned Morvath and showed
his boss bar, full model, sword, shoulder plates and emissive eyes/cracks. Resuming AI
showed approach, sword poses and fire particles. The phase-two control was confirmed
by the server log (phase=2); its transition silhouette was not captured. The test-only
kill control removed the bar and the Heart/XP were picked up. This is not a full
survival fight or multiplayer validation. Hotbar 7 applies hurt; 6 finishes the test boss.

Coplanar outer cuff/boot cubes caused visible surface fighting. Only those Morvath
runtime shells now expand by 0.035 model pixels; the 27 source geometries are unchanged.
The new client showed separated cuffs/boots, but the prototype still needs more
angles, full animation review, body seam inspection and sword/art polish. No measured
FPS or certification of all ten animation silhouettes is claimed. Native capture:
`run-client-boss/screenshots/2026-10-30_21.47.44.png`. The test world was saved and quit.
