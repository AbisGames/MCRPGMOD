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

## What bonuses are possible right now

Write these in plain words ("+2 armor", "+10% sword damage", "takes 50% more fire damage").

| You can say... | Example |
|---|---|
| **+/- any attribute**, flat or % | "+2 armor", "+4 max health", "+10% movement speed", "+5% attack speed", "+1 attack damage", "+0.5 armor toughness", "+20% knockback resistance", "+1 block reach", "-10% fall damage", "+0.1 scale", "+2 luck" (loot luck) |
| **+ points in a stat** | "+2 strength", "+1 special stat" |
| **Permanent potion effect** | "night vision", "haste I", "water breathing", "fire resistance" |
| **Deal more/less damage** | "+10% damage", "+15% damage with swords", "+20% fire damage", "-30% damage with bows" |
| **Take more/less damage** | "takes 50% more fire damage", "-20% fall damage taken", "-10% projectile damage taken" |
| **Grows with something** | "+0.25 armor per special-stat point", "+0.1 attack damage per class level", "+1% damage per 5 strength" |

**Damage kinds** for "deal/take damage": fire, fall, projectile, explosion, magic, freezing, drowning, lightning,
wither, melee, or "all".

**Skills can be trained by:** melee hits (any weapon, or only swords/axes/...), ranged hits (bows, crossbows,
tridents), kills (with any or specific weapons), blocking with a shield, taking damage while wearing certain armor,
moving (walking, sprinting, sneaking, swimming, climbing, gliding: "every N blocks"), jumping.

## Not possible yet

Fine to write these in section 12 ("Anything else"). Claude will leave them out and list them, until the system
supports them.

- Active abilities / spells / dodge roll (and spending the resource bar)
- Magic damage, healing power, health regen, crit chance as stats (Intelligence, Wisdom, Endurance and Luck
  give nothing yet)
- Different stat growth per class (e.g. a mage getting more out of Intelligence)
- Stat requirements on gear
