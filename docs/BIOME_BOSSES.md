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
- Three GameTests exercise interrupted windup, reload after impact, phase latching,
  invalid-state bounds, real registered entity attributes and entity NBT roundtrip.

This checkpoint does **not** implement damaging attacks, encounter ownership/arena
constraints, ritual summoning, loot, finished animation or reviewed final artwork.
Morvath is available only through developer commands. The other 26 bosses remain
geometry prototypes and are deliberately not registered as finished entities.
Do not distribute this checkpoint as a completed boss update.

Local Java 25 compilation of main and GameTest source sets passes. The dedicated
`bosses.yml` workflow runs the complete existing lightweight test set, boss tests,
and player provenance/renewal disk regression. CI results are recorded after execution.
Client render appearance and multiplayer behavior still require in-game verification.

Next: persistent encounter registry and safe cancellation; server attack effects and
telegraphs; ritual altar/sigil transaction; complete Morvath art/animations and loot.
