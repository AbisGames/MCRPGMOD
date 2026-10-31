# Class: Assassin

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Assassin' (p. 62). -->

## 1. Basics

- **Id:** assassin
- **Display name:** Assassin
- **Description:** A professional killer who strikes from the shadows. Deadly on the first hit, poor in an open fight.
- **Icon item:** wither_rose
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x0.9 |
| Strength     | 2 | x1 |
| Dexterity    | 5 | x1.1 |
| Agility      | 5 | x1.25 |
| Intelligence | 2 | x0.9 |
| Wisdom       | 0 | x0.75 |
| Endurance    | 1 | x1 |
| Luck         | 3 | x1.1 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Lethality
- **Description:** Knowing exactly where to put the knife.
- **What each point gives:** +1% damage against mobs that are not targeting you
- **Max points:** 100

### Resource bar

- **Name:** Shadow
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** smoke grey (#5A5A6E)

## 4. Starting kit

- 1 × iron_sword
- 1 × crossbow
- 8 × arrow
- 1 × leather_boots
- 1 × splash_potion of poison
- 4 × bread

## 5. Passives

- Assassinate: your first hit on a mob that isn't targeting you deals +60% damage; if that brings it below 20% health, it dies
- Quick Reflexes: +15% movement speed for 2s after you take damage
- Manufacture Poison: 10% chance on melee hit to apply Poison I for 4s

### Special passives

- +0.5% poison chance per Lethality point
- Killer's Eye: per 10 Lethality, +3% critical damage

## 6. Weakness

- **What players read:** Assassins live in the dark. Daylight makes them clumsy and obvious.
- **What it actually does:** -15% damage while standing in light level 12 or higher, and hostile mobs notice you from 20% farther away

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | neutral |
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

### Skill: Backstab

- Shared skill - full definition in `rogue.md`

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Poisoncraft

- **Shared or class-only:** class-only
- **Trained by:** poisoning mobs and brewing poison potions
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Lethality
- **Tier II bonus:** +1 Lethality
- **Tier III bonus:** +1s poison duration
- **Tier IV bonus:** +1 Lethality
- **Tier V bonus:** +2 Lethality

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom of the web (Trickery quarter), ring 2, leaning toward the War quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| assassin_start | Assassin | start | - | - | - | +5% damage to unaware mobs |
| assassin_garrote | Garrote | small | 1 | 1 | assassin_start | +8% damage to unaware mobs |
| assassin_soft_soles | Soft Soles | small | 1 | 1 | assassin_start | +10% sneaking speed |
| assassin_venom | Venom | notable | 2 | 10 | assassin_garrote | your poison also gives Slowness I |
| assassin_shadowstep | Shadowstep | notable | 2 | 12 | assassin_soft_soles | after a kill, Invisibility for 2s |
| assassin_death_mark | Death Mark | keystone | 3 | 25 | assassin_venom, assassin_shadowstep | Assassinate threshold rises to 30% health, but -20% damage to mobs that are targeting you |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Assassinate | start | - | - | - | - |
| disguise | Disguise Expertise | small | 1 | 1 | start | +2 Lethality |
| quick_reflexes | Quick Reflexes | small | 1 | 5 | start | Quick Reflexes speed boost lasts 2s longer |
| poisoner | Manufacture Poison | notable | 2 | 15 | disguise | poison applied by you is Poison II |
| killers_eye | Killer's Eye | notable | 2 | 20 | quick_reflexes | unlocks Mark Target ability |
| black_hand | The Black Hand | keystone | 3 | 35 | poisoner, killers_eye | Assassinate can trigger again 10s after a kill; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| assassin_stat_start | Assassin | start | - | - | - | - |
| assassin_precise | Precise | small | 2 | 1 | assassin_stat_start | +2 Dexterity, +1 Luck |
| assassin_silent | Silent | small | 2 | 5 | assassin_precise | +2 Agility, +1 Dexterity |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Mark Target** (Killer's Eye: mark a mob you're hidden from; +30% damage to it for 60s, 15 Shadow), **Disguise** (mobs ignore you for 10s, 25 Shadow).
- Book talents: Assassinate, Disguise Expertise, Quick Reflexes, Manufacture Poison, Killer's Eye.
- **Co-design question:** Lethality + Shadow. The book's Assassinate is an instant kill on a failed save - is the 'below 20% health' version OK?
