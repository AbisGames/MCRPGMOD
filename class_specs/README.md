# Class specs

Plain-language class designs. Claude turns each file here into the real JSON data
(class, skills, trees) under `src/main/resources/data/pipican/pipican/`.

## Workflow

1. Copy [`_TEMPLATE.md`](_TEMPLATE.md) to `class_specs/<class_id>.md`, one file per class.
   [`example_knight.md`](example_knight.md) shows a filled-in one.
2. Fill it in using words, not code. Write `?` anywhere you want a suggestion.
3. Upload/commit the files, then ask Claude: *"build the classes from class_specs"*.
4. Claude generates the JSON, runs the game tests and lists anything it had to guess or couldn't do.

The spec files stay here as the source of truth: to change a class, edit its spec and ask Claude to rebuild it.

## Rebuilding

[`tools/class_builder/`](../tools/class_builder) turns every spec into JSON and rewrites
[`BUILD_REPORT.md`](BUILD_REPORT.md) (what isn't in the game yet, grouped by missing feature). From the repo root:

```
python3 tools/class_builder/build.py class_specs src/main/resources class_specs/BUILD_REPORT.md
```

It regenerates `data/pipican/pipican/{class,skill,skill_tree}` and the special stats from scratch, so **don't
hand-edit those files**: change the spec (or the builder) and rebuild. Then run `./gradlew runGameTestServer`;
the `shipped_classes` test checks that every class loads and its three trees resolve.

[`CLASSES.md`](CLASSES.md) is the index of all classes (tiers, special stats, resources, where each one sits
in the shared tree).

## What bonuses are possible right now

Write these in plain words ("+2 armor", "+10% sword damage", "takes 50% more fire damage").

| You can say... | Example |
|---|---|
| **+/- any attribute**, flat or % | "+2 armor", "+4 max health", "+10% movement speed", "+5% attack speed", "+1 attack damage", "+0.5 armor toughness", "+20% knockback resistance", "+1 block reach", "-10% fall damage", "+0.1 scale", "+2 luck" (loot luck) |
| **+ points in a stat** | "+2 strength", "+1 special stat" |
| **Permanent potion effect** | "night vision", "haste I", "water breathing", "fire resistance" |
| **Deal more/less damage** | "+10% damage", "+15% damage with swords", "+20% fire damage", "-30% damage with bows" |
| **Take more/less damage** | "takes 50% more fire damage", "-20% fall damage taken", "-10% projectile damage taken" |
| **Resource bar** | "+10% Rage regen", "-15% max Mana", "+5 max Mana", "no Mana regen" |
| **Only in some situations** | "+10% damage at night", "+2 armor while below half health", "-20% damage underground", "while Ward is above half: +20% speed", "+1 armor in forests", "-10% damage while no other player is within 16 blocks" |
| **Grows with something** | "+0.25 armor per special-stat point", "+0.1 attack damage per class level", "+1% damage per 5 strength" |

**Situations** you can use: wet / in water / underwater / in lava / in rain / in thunderstorms, at night / in daylight,
outdoors / underground, in the Nether / the End, in forests / deserts / jungles / oceans / snowy or natural biomes,
standing on stone / dirt / sand / gravel / Nether blocks, light level N or lower/higher, in darkness, below/above X%
health, full health, your resource above/below X% or full, sprinting, sneaking, blocking, swimming, mounted / on foot,
on fire, slowed / poisoned / withered / weakened / blinded, holding a shield / your staff, dual-wielding, wearing
N+ heavy/medium/light armor or leather or no armor, another player / no other player / hostile mobs within N blocks.
Combine with "and" / "or".

**Damage kinds** for "deal/take damage": fire, fall, projectile, explosion, magic, freezing, drowning, lightning,
wither, melee, or "all".

**Skills can be trained by:** melee hits (any weapon, or only swords/axes/...), ranged hits (bows, crossbows,
tridents), kills (with any or specific weapons), blocking with a shield, taking damage while wearing certain armor,
moving (walking, sprinting, sneaking, swimming, climbing, gliding: "every N blocks"), jumping.

## Active abilities

Describe them in section 12, one bold name each, with what they do and their cost:
`Active abilities: **Haste** (Speed III + Haste II for 8s, 25 Sands), **Blink** (teleport 8 blocks where you look, 20 Waypoint)`.
A tree node can unlock one: write "unlocks Haste ability" in its "What it gives". Abilities are translated by
hand into `tools/class_builder/abilities.py`, so tell Claude when you add or change one. They can:
buff/debuff players and mobs around you or the one you look at, heal, cleanse, shield, damage (around you, in a
cone in front of you, or the mob you look at), push, dash, blink, call lightning, summon temporary helpers
(wolves, iron golems...), explode, start rain, refill the resource bar, fly (as a toggle), and give any bonus
from the table above for a while (a timed state) or while toggled on. Abilities can need a situation
("only in darkness", "while holding your staff"), and can start by themselves ("when Rage is above 75%").

## Not possible yet

Fine to write these in section 12 ("Anything else"). Claude will leave them out and list them, until the system
supports them.

- Ability mechanics beyond the list above: marking a target, charming or controlling mobs, decoys, eidolons,
  temples, grimoires, "your next hit does X", teleporting to a saved spot or between logs
- Magic damage, healing power, health regen, crit chance as stats (Intelligence, Wisdom, Endurance and Luck
  give nothing yet)
- Different stat growth per class (e.g. a mage getting more out of Intelligence)
- Stat requirements on gear
