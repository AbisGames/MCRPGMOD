# RPG stat / stat tree core (PIPICAN)

Branch: `stat-+-stat-tree`. This commit intentionally does **not** change `main`.

## Status
The branch originally contained only README.md and had **no** NeoForge Gradle project.
This is a pure Java 25 domain implementation, **not yet a functional in-game NeoForge mod**.
No GUI, data attachments, mob kill events, equipment hooks, or network packets have been installed.
Do not claim gameplay works until NeoForge is merged into this branch, integrated, compiled and tested.

## Agreed responsibilities
- This package: RPG kill XP (distinct from vanilla XP), stat points, stats, derived values, tree nodes, equipment requirement checking.
- Other team's system: class choice, class-specific skills and skill XP, special skill trees, dodge-roll implementation.
- Class profile supplies base HP, initial stats and per-attribute scaling.
- Equipment supplies flat stat bonuses, armor and magic resistance. The latter two can NEVER be spent on as stat attributes.
- Missing equipment requirements return false; equipment integration should apply heavy penalties, not prevent equipping.
- Movement multiplier is hard-clamped to 2.0. Dodge roll i-frame bonus is small; actual roll handler must apply it.
- Scaling values and XP thresholds are **provisional**, must be balanced collaboratively.

## Java interfaces
- `StatState`: a player's mutable points/XP/investments. Serialize as `xp`, `earnedPoints`, and `investments`; restore validates invariants.
- `XpRules`: configure entity IDs -> XP rewards; no reward by default. Initial example: zombie 5, skeleton 7, creeper 8, enderman 20.
- `StatEngine.awardKill`: server-only award; event handler must verify player kill and avoid duplicate rewards.
- `StatEngine.allocate`: server-only atomic validation of a whole proposed spend.
- `StatProfile`: constructed by class system; use the class ID to resolve profiles; no example class is registered.
- `StatCalculator`: base/equipment/effective stats and derived values; bind these to actual attributes on server.
- `StatTree`: data-driven nodes with prerequisites, cycle validation, unlocks. Store unlocked node IDs in player attachment alongside StatState. UI uses this tree independently of skill tree.

## Integration checklist (pending NeoForge project)
1. Register and serialize per-player state with NeoForge data attachments; copy on death and sync to owning client.
2. Hook living-death event with real killer attribution. Apply reward ID lookup *only on server*, once per actual kill.
3. Register packet requesting stat spend/tree unlock; server validates points and prerequisites and syncs result.
4. Apply max HP and speed from calculated derived values via stable modifiers, avoiding cumulative stacking.
5. Make dodge-roll code read the computed extra invulnerability ticks. Never grant permanent invulnerability.
6. Read equipment modifiers and requirements from equipment/trinket system. Show penalty for unmet requirements.
7. Implement pixel-font, three-panel Level Up / Character Status / Equipment UI with transparent panels over live Minecraft scene; preview before confirmation.
8. Put default rewards, thresholds, per-class stat scaling and optional tree node definitions into server configuration/data files.
9. Add integration tests for persistence, death/respawn, login sync, XP exploit resistance, and equip/unequip.

**Open design question:** In the original game design vanilla XP was for stats; the later explicit instruction supersedes it: RPG kill XP is now the only source of stat points, independent of vanilla XP.
