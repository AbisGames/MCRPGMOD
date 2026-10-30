# Biome boss development

## Checkpoint: common framework, before playable combat

Branch: `biome-bosses`. Verified worldgen merged at `9f170c9` without changing
any of the 27 original geometry files. See `WORLDGEN_TESTING.md` for server coverage.

Implemented foundation:
- Morvath entity registration and attributes, persistent mob, multiplayer tracking boss bar.
- Server-owned combat clock with telegraph/recovery/cooldown and a latched half-health phase.
- Entity serialization of phase, ability, remaining ticks, cooldown and resolved impact.
- Native client renderer converts preserved Bedrock box geometry into model parts;
  idle/walk/head movement and two original, reproducibly generated basalt texture drafts.
- Four GameTests exercise interrupted windup, reload after impact, phase latching,
  invalid-state bounds, real registered entity attributes and compressed entity NBT disk roundtrip.
- Per-dimension encounter SavedData reserves arenas across chunk unload, preserves owner and
  terminal completion tombstones, and rejects duplicate completion after disk roundtrip.
- Summoning service checks the actual 3D biome, loaded arena, collision/headroom and sturdy
  player-built floor; spawn failure rolls back its reservation. No ingredients are consumed here.
- Managed bosses cancel after 600 occupied-world ticks without eligible nearby players; owner
  cancellation and terminal/missing records discard the entity without loot. Death reward
  processing claims the terminal record once before issuing any drops.

This checkpoint does **not** implement damaging attacks, ritual items/altar,
finished loot, finished animation or reviewed final artwork. Summoning-service positive
biome/clearance cases and live cancellation still need focused integration coverage.
Morvath is available only through developer commands. The other 26 bosses remain
geometry prototypes and are deliberately not registered as finished entities.
Do not distribute this checkpoint as a completed boss update.

Local Java 25 compilation of main and GameTest source sets passes. The dedicated
`bosses.yml` workflow runs the complete existing lightweight test set, boss tests,
and player provenance/renewal disk regression. First CI run: [41022507755](https://github.com/48018390/MCRPGMOD-Development/actions/runs/41022507755),
commit `26dcac0`: **all 22 required tests passed**, including the original three boss tests.
Encounter extensions are checked in a subsequent run.
Client render appearance and multiplayer behavior still require in-game verification.

Next: persistent encounter registry and safe cancellation; server attack effects and
telegraphs; ritual altar/sigil transaction; complete Morvath art/animations and loot.
