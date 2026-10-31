# Class: Warlock

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Warlock' (p. 74). -->

## 1. Basics

- **Id:** warlock
- **Display name:** Warlock
- **Description:** An oath-breaker who steals magic from others, turning enemy potions and spells into their own power.
- **Icon item:** ominous_bottle
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x0.85 |
| Strength     | 1 | x0.85 |
| Dexterity    | 2 | x1 |
| Agility      | 3 | x1.15 |
| Intelligence | 6 | x1.25 |
| Wisdom       | 3 | x1 |
| Endurance    | 2 | x0.75 |
| Luck         | 1 | x1.15 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Larceny
- **Description:** A talent for taking what isn't yours - especially magic.
- **What each point gives:** +0.5% chance to steal an incoming harmful effect
- **Max points:** 100

### Resource bar

- **Name:** Stolen Power
- **Max at 0 points:** 20
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.2
- **Extra regen per second per special-stat point:** 0.02
- **Starts full when the class is chosen:** no
- **Colour:** sickly teal (#2BB3A0)

## 4. Starting kit

- 1 × stone_sword
- 1 × crossbow
- 8 × arrow
- 1 × leather_chestplate
- 1 × splash_potion of weakness
- 4 × bread

## 5. Passives

- Steal Spell: 15% chance that a harmful potion effect or magic attack aimed at you is cancelled; you gain 10 Stolen Power instead
- Vanish: when you take damage, become invisible for 2s (60s cooldown)
- Elude Divination: you can't receive the Glowing effect

### Special passives

- Vanish cooldown -1% per Larceny point
- +0.5% magic damage per Larceny point while Stolen Power is above half

## 6. Weakness

- **What players read:** Stolen magic fights back against healing.
- **What it actually does:** Regeneration, Instant Health and healing from other players are 25% weaker on you

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Spell Theft

- **Shared or class-only:** class-only
- **Trained by:** stealing effects with Steal Spell
- **Tier points go to:** special tree
- **How fast it levels:** fast
- **Tier I bonus:** +1 Larceny
- **Tier II bonus:** +1 Larceny
- **Tier III bonus:** +10% max Stolen Power
- **Tier IV bonus:** +1 Larceny
- **Tier V bonus:** +2 Larceny

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom of the web (Trickery quarter), ring 2, leaning toward the Power quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| warlock_start | Warlock | start | - | - | - | -5% magic damage taken |
| warlock_filch | Filch | small | 1 | 1 | warlock_start | +5% Steal Spell chance |
| warlock_slip | Slip Away | small | 1 | 1 | warlock_start | Vanish lasts +1s |
| warlock_pact | Dark Pact | notable | 2 | 10 | warlock_filch | +12% magic damage |
| warlock_shroud | Shroud | notable | 2 | 12 | warlock_slip | Vanish also clears Slowness and Weakness |
| warlock_oathbreaker | Oathbreaker | keystone | 3 | 25 | warlock_pact, warlock_shroud | Steal Spell chance doubles, but you can no longer benefit from beacons |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Steal Spell | start | - | - | - | - |
| vanish | Vanish | small | 1 | 1 | start | +2 Larceny |
| elude | Elude Divination | small | 1 | 5 | start | also immune to Darkness effect |
| expert_thief | Expert Spell Thief | notable | 2 | 15 | vanish | stolen effects can be re-applied (see section 12) |
| vanishing_escape | Vanishing Escape | notable | 2 | 20 | elude | Vanish also teleports you up to 6 blocks away from the attacker |
| spell_thief_mastery | Spell Thief Mastery | keystone | 3 | 35 | expert_thief, vanishing_escape | Steal Spell also steals BENEFICIAL effects from mobs you hit (witches!), but -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| warlock_stat_start | Warlock | start | - | - | - | - |
| warlock_sly | Sly | small | 2 | 1 | warlock_stat_start | +2 Intelligence, +1 Agility |
| warlock_cursed_luck | Cursed Luck | small | 2 | 5 | warlock_sly | +2 Luck, -1 Wisdom, +1 Intelligence |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active ability for later: **Return to Sender** (spend Stolen Power to throw the last stolen effect at a mob as a splash, 10 Stolen Power).
- Book talents: Steal Spell, Vanish, Elude Divination, Expert Spell Thief, Spell Thief Mastery, Vanishing Escape.
- **Co-design question:** Larceny + Stolen Power (only fills by stealing, barely regenerates). Keep that 'earn it from enemies' feel?
