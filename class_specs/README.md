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
| **Resource bar** | "+10% Rage regen", "-15% max Mana", "+5 max Mana", "no Mana regen", "Rage fills 25% faster", "abilities cost 10% less Mana", "your melee hits refill 2 Warfire", "every kill refills 5 Rage", "Spare Parts refill completely when you sleep", "whenever you spend Mana, gain Speed II for 3s / heal 2 HP / mobs within 8 blocks get Weakness I for 4s" |
| **Filling bars** | in "Starts full": "no (fills by 2 per hit taken and 1 per hit dealt, drains 0.5/s out of combat)", or "fills by 8 per kill" |
| **Short buffs after something** | "+15% movement speed for 2s after you take damage", "+10% speed for 2s after hitting a mob", "after a kill, +20% damage for 5s", "whenever you spend Ward, gain +3 armor for 10s" |
| **Mounts** | "+10% mount speed", "+5% mount jump", "mount +2 armor", "+2 mount max health", "your mount gets +4 armor and +10% speed" |
| **Stealth, training, sprinting** | "-10% mob detection range while sneaking", "skills train 15% faster", "+2% sprint speed", "-5% hunger from sprinting", "-10% hunger drain", "immune to lightning / fire / fall damage" |
| **Named class passives** | Define one as "Berserk: while Rage is above 75%: +6 max health, ...", then refer to it: "+15% speed while Berserk", "Berserk melee bonus +1% per 2 Fury", "Battle Stance bonuses +1% per 4 Resolve", "per 10 Vigilance, Stand Guard +5% damage", "when you go Berserk, mobs within 6 blocks get Slowness I for 3s". Skills can train "while Berserk" too. |
| **Execute, auras, duels** | "if a hit takes a mob below 15% health, it dies", "unclean mobs below 20% health die from your hits", "fully charged hits deal +20%", "players within 10 blocks (including you) get +10% damage", "other players within 4 blocks take 15% less damage", "the first mob you hit becomes your challenged foe for 30s" + "+20% damage against your challenged foe", "the mob you look at for 2s becomes scrutinised - it glows, and you deal +15% damage to it" |
| **Endings, last chances, follow-ups** | "when Berserk ends: Weakness I and Slowness I for 8s", "once per rest, when you would die, go Berserk instead with 4 HP", "once per day, when you'd die, explode in flame and heal 6 HP", "once per 5 minutes, when you'd drop below 2 hearts, heal 6 HP and gain +20% damage for 5s", "after Zeal triggers, immune to Slowness and Nausea for 5s", "hits within 5s of a Zeal trigger deal +20% damage". Abilities count as named states too: "+1 armor while in Divine Ecstasy". |
| **Favored weapon, last stand** | "when you sleep, the weapon type in your main hand becomes your favored weapon type", "with your favored weapon: +2 armor, +20% damage", "when a hit would bring you below 2 hearts, 30% chance to shrug it off entirely", "after spending Warfire, your next 3 melee hits deal +20% damage" |
| **Health regen** | "natural regeneration is 40% faster", "+10% natural regeneration" |
| **Only in some situations** | "+10% damage at night", "+2 armor while below half health", "-20% damage underground", "while Ward is above half: +20% speed", "+1 armor in forests", "-10% damage while no other player is within 16 blocks" |
| **Against some mobs / mob states** | "+20% damage against undead", "+15% damage to mobs below 30% health", "+10% damage to burning / poisoned / marked mobs", "+10% damage to mobs not targeting you", "-20% damage taken from spiders" |
| **Crits, dodging, bleeding** | "+15% critical damage", "+5% crit chance with swords", "8% chance to dodge melee attacks", "10% chance to parry melee hits", "+5% miss chance", "+10% arrow-deflect chance", "critical sword hits cause bleeding - the mob takes 1 damage per second for 4s", "+1s bleed duration", "bleed stacks up to 3 times", "bleeding mobs are Slowed", "bleeding mobs take +5% more damage from you" |
| **Knockback & drops** | "melee knockback +25%", "+5% extra mob loot chance", "+10% chance of extra ore drop", "+5% chance of double crop drops" |
| **Immunities** | "immune to poison", "immune to wither and hunger" |
| **Chance on hit / kill** | "20% chance on hit to set mobs on fire", "+5% ignite chance" / "poison chance +0.5% per X" / "charm chance doubled" (raises that chance), "10% chance a hostile mob you hit is charmed", "your hits have a 5% chance in thunderstorms to call down lightning on the target", "kills give Speed I for 5s", "when hit, 20% chance to slow the attacker" |
| **XP, potions, hunger, trading, bows** | "+10% experience", "+10% potion duration", "harmful potions last 30% shorter", "potion effects you apply last 30% longer", "you get hungry 30% faster", "-5% trade prices", "+15% crossbow reload speed", "+20% bow draw speed", "+10% sword attack speed" |
| **Grows with something** | "+0.25 armor per special-stat point", "+0.1 attack damage per class level", "+1% damage per 5 strength" |

**Situations** you can use: wet / in water / underwater / in lava / in rain / in thunderstorms, at night / in daylight,
outdoors / underground, in the Nether / the End, in forests / deserts / jungles / oceans / snowy or natural biomes,
standing on stone / dirt / sand / gravel / Nether blocks, light level N or lower/higher, in darkness, below/above X%
health, full health, your resource above/below X% or full, sprinting, sneaking, blocking, swimming, in combat, moving / standing still (for N s), mounted / on foot,
on fire, slowed / poisoned / withered / weakened / blinded, holding a shield / your staff, dual-wielding, wearing
N+ heavy/medium/light armor or leather or no armor, another player / no other player / hostile mobs within N blocks.
Combine with "and" / "or".

**Damage kinds** for "deal/take damage": fire, fall, projectile, explosion, magic, freezing, drowning, lightning,
wither, melee, or "all".

**Skills can be trained by:** melee hits (any weapon, or only swords/axes/...), ranged hits (bows, crossbows,
tridents), kills (with any or specific weapons), blocking with a shield, taking damage while wearing certain armor,
moving (walking, sprinting, sneaking, swimming, climbing, gliding, riding: "every N blocks"), jumping, crits, making mobs
bleed, dodging, spending your resource, being healed, taking (big) hits or fall damage, teleporting, enchanting, brewing,
crafting (e.g. redstone components), trading, taming, breeding, mining (ores / stone), harvesting crops, planting saplings,
bone meal, anvils, gaining levels, drinking potions, fishing, your pets or summons killing mobs. Any of them can be
limited: "killing undead mobs", "killing mobs while wet", "killing marked targets", "hitting mobs while mounted",
"damaging mobs that are not targeting you", "taking damage while Ward is above half". Several can be joined with "and".

## Active abilities

Describe them in section 12, one bold name each, with what they do and their cost:
`Active abilities: **Haste** (Speed III + Haste II for 8s, 25 Sands), **Blink** (teleport 8 blocks where you look, 20 Waypoint)`.
A tree node or a skill tier can unlock one: write "unlocks Haste ability" in its "What it gives" / tier bonus. Abilities are translated by
hand into `tools/class_builder/abilities.py`, so tell Claude when you add or change one. They can:
buff/debuff players and mobs around you or the one you look at, heal, cleanse, shield, damage (around you, in a
cone in front of you, or the mob you look at), push, dash, blink, call lightning, summon temporary helpers
(wolves, iron golems...), explode, start rain, refill the resource bar, fly (as a toggle), and give any bonus
from the table above for a while (a timed state) or while toggled on. They can also mark a mob, buff your
next hit, frighten / charm / recruit mobs, leave a decoy, make the next hit on an ally miss, teleport to an
ally / a saved waypoint / a log / through walls, find your bed or a village, see through a player's eyes,
give temporary items, steal, transmute, cast spells written in a book, build and ride the Engineer's eidolon,
bond with a player, and react to hits and kills ("for 30s, every mob you kill rises as a zombie"). Abilities can need a situation
("only in darkness", "while holding your staff"), and can start by themselves ("when Rage is above 75%").

## Not possible yet

Fine to write these in section 12 ("Anything else"). Claude will leave them out and list them, until the system
supports them.

- Magic damage, healing power, health regen, crit chance as stats (Intelligence, Wisdom, Endurance and Luck
  give nothing yet)
- Different stat growth per class (e.g. a mage getting more out of Intelligence)
- Stat requirements on gear
