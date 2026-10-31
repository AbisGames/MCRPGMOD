# Class: Rogue

<!-- Source: Shadow of the Demon Lord core rulebook, novice path 'Rogue' (p. 57). -->

## 1. Basics

- **Id:** rogue
- **Display name:** Rogue
- **Description:** A cunning trickster who wins with dirty tricks, speed and luck instead of fair fights.
- **Icon item:** leather_boots
- **Selectable by players:** yes
- **Base health:** 20

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x0.65 |
| Strength     | 1 | x1 |
| Dexterity    | 4 | x1.15 |
| Agility      | 4 | x1.35 |
| Intelligence | 2 | x1 |
| Wisdom       | 0 | x0.85 |
| Endurance    | 0 | x0.85 |
| Luck         | 1 | x1.15 |
| Special stat (section 3) | 5 | |

## 3. Special stat

- **Name:** Cunning
- **Description:** Knack for finding the opening. Boosts Trickery damage and the Guile bar.
- **What each point gives:** +0.5% damage against mobs that are not targeting you
- **Max points:** 100

### Resource bar

- **Name:** Guile
- **Max at 0 points:** 20
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** dark green (#3E7D3A)

## 4. Starting kit

- 1 × stone_sword
- 1 × bow
- 16 × arrow
- 1 × leather_boots
- 6 × bread

## 5. Passives

- +8% movement speed
- -20% fall damage
- Trickery: once every 4 seconds, your next hit deals +15% damage
- Exploit Opportunity: critical hits have a 10% chance to instantly reset your attack cooldown

### Special passives

- +1% Trickery damage per Cunning point
- +0.2% chance per Cunning point to get one extra item when a mob drops loot

## 6. Weakness

- **What players read:** Loud armor gives you away and slows your tricks.
- **What it actually does:** while wearing 2 or more pieces of heavy armor: -15% movement speed and Trickery is disabled

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 1
- **Free skill points at start (special tree):** 0

## 10. Skills (mastery)

### Skill: Stealth

- **Shared or class-only:** shared
- **Trained by:** sneaking within 8 blocks of a hostile mob without being targeted
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** -10% mob detection range while sneaking
- **Tier II bonus:** +5% sneaking speed
- **Tier III bonus:** -10% mob detection range while sneaking
- **Tier IV bonus:** +5% sneaking speed
- **Tier V bonus:** -15% mob detection range while sneaking

### Skill: Backstab

- **Shared or class-only:** shared
- **Trained by:** damaging mobs that are not targeting you
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +4% damage to unaware mobs
- **Tier II bonus:** +4% damage to unaware mobs
- **Tier III bonus:** +5% critical damage
- **Tier IV bonus:** +4% damage to unaware mobs
- **Tier V bonus:** +8% damage to unaware mobs

### Skill: Acrobatics

- **Shared or class-only:** shared
- **Trained by:** taking fall damage and sprint-jumping
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** -5% fall damage
- **Tier II bonus:** +2% movement speed
- **Tier III bonus:** -5% fall damage
- **Tier IV bonus:** +0.5 safe fall distance (blocks)
- **Tier V bonus:** +3% movement speed

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Roguery

- **Shared or class-only:** class-only
- **Trained by:** landing Trickery hits
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Cunning
- **Tier II bonus:** +1 Cunning
- **Tier III bonus:** +5% Trickery damage
- **Tier IV bonus:** +1 Cunning
- **Tier V bonus:** +2 Cunning

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom of the web (the Trickery quarter), ring 1

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| rogue_start | Rogue | start | - | - | - | +3% movement speed |
| rogue_quick | Quick Hands | small | 1 | 1 | rogue_start | +5% attack speed |
| rogue_light_step | Light Step | small | 1 | 1 | rogue_start | -15% fall damage |
| rogue_dirty | Dirty Tricks | small | 1 | 5 | rogue_quick | +6% damage to unaware mobs |
| rogue_slippery | Slippery | notable | 2 | 10 | rogue_light_step, rogue_dirty | +5% movement speed, 10% chance to dodge a melee hit |
| rogue_gambler | Gambler | keystone | 3 | 25 | rogue_slippery | +2 Luck and critical hits deal +20% damage, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Trickery | start | - | - | - | - |
| backstab | Backstab | small | 1 | 1 | start | +10% damage to unaware mobs |
| skirmish | Skirmish | small | 1 | 5 | start | +10% movement speed for 2s after hitting a mob |
| subterfuge | Subterfuge | notable | 2 | 15 | skirmish | mobs you hit while sneaking forget you 25% faster |
| threats | Threats | notable | 2 | 15 | backstab | hitting a mob from behind gives it Weakness I for 3s |
| rogue_cunning | Rogue Cunning | keystone | 3 | 30 | subterfuge, threats | Trickery recharges twice as fast; -10% max Guile |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| rogue_stat_start | Rogue | start | - | - | - | - |
| rogue_nimble | Nimble | small | 2 | 1 | rogue_stat_start | +2 Agility, +1 Dexterity |
| rogue_lucky | Lucky Break | small | 2 | 5 | rogue_nimble | +2 Luck |

## 12. Anything else

- **Power tier: Novice.** Book levels 1, 2, 5, 8. Baseline power: 14 stat points, 5 special points, 3 trinket slots. Selectable from the start.
- Active abilities for later: **Nimble Recovery** (heal + short dash, once per rest), **Subterfuge** (charm a mob into ignoring you, 10 Guile).
- The book's roguery talent 'Magic' (rogue dabbling in spells) could become a keystone that gives a small Mana bar - open to ideas.
- **Co-design question:** Cunning + Guile are my proposal. Keep, or rename/rework?
