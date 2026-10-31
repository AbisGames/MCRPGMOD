# Class: Acrobat

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Acrobat' (p. 79). -->

## 1. Basics

- **Id:** acrobat
- **Display name:** Acrobat
- **Description:** A tumbler and cat-burglar who moves like water - over walls, past enemies and off any height.
- **Icon item:** feather
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.85 |
| Strength     | 2 | x1.15 |
| Dexterity    | 6 | x1.15 |
| Agility      | 9 | x1.3 |
| Intelligence | 1 | x0.85 |
| Wisdom       | 0 | x0.7 |
| Endurance    | 3 | x1 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Poise
- **Description:** Perfect body control.
- **What each point gives:** +0.3% movement speed and -1% fall damage
- **Max points:** 100

### Resource bar

- **Name:** Flow
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** aqua (#36D6C3)

## 4. Starting kit

- 1 × stone_sword
- 1 × leather_boots
- 4 × slime_ball
- 6 × bread

## 5. Passives

- +15% movement speed
- Acrobatics: no fall damage below 8 blocks; landing from any fall can't break your legs below 2 hearts
- You can walk through other players and pass by mobs without being pushed
- No slowdown from soul sand, honey, cobwebs or when swimming

### Special passives

- Mobility: per 10 Poise, +5% chance a melee hit against you misses
- Jump height +0.05 blocks per 10 Poise

## 6. Weakness

- **What players read:** Acrobats rely on space and speed. Trapped, they flounder.
- **What it actually does:** +25% damage taken while Slowed or standing in a 1-block-wide space

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | neutral |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 1 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Tumbling

- **Shared or class-only:** class-only
- **Trained by:** taking fall damage and dodging attacks
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Poise
- **Tier II bonus:** +1 Poise
- **Tier III bonus:** +0.5 safe-fall blocks
- **Tier IV bonus:** +1 Poise
- **Tier V bonus:** +2 Poise

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Scout

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| acrobat_start | Acrobat | start | - | - | - | +5% movement speed |
| acrobat_light_landing | Light Landing | small | 1 | 1 | acrobat_start | -20% fall damage |
| acrobat_fleet | Fleet | small | 1 | 1 | acrobat_start | +5% movement speed |
| acrobat_wall_run | Wall Run | notable | 2 | 12 | acrobat_light_landing | sprinting into a wall lets you run up it for 2 blocks |
| acrobat_untouchable | Untouchable | notable | 2 | 15 | acrobat_fleet | +8% dodge chance |
| acrobat_wind_dancer | Wind Dancer | keystone | 3 | 30 | acrobat_wall_run, acrobat_untouchable | double-jump in mid-air once per 5s, but -2 armor |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Acrobatics | start | - | - | - | - |
| somersault | Somersault | small | 1 | 1 | start | +2 Poise |
| kip_up | Kip-up | small | 1 | 5 | start | no slowdown after being knocked back |
| mobility | Mobility | notable | 2 | 15 | somersault | your movement never triggers mob 'attack of opportunity' style knockback |
| catfall | Catfall | notable | 2 | 20 | kip_up | fall damage halved again |
| living_wind | Living Wind | keystone | 3 | 35 | mobility, catfall | +20% movement speed while Flow is above half; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| acrobat_stat_start | Acrobat | start | - | - | - | - |
| acrobat_limber | Limber | small | 2 | 1 | acrobat_stat_start | +2 Agility, +1 Dexterity |
| acrobat_balanced | Balanced | small | 2 | 5 | acrobat_limber | +2 Dexterity, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Dash** (short burst forward, 10 Flow), **Vault** (leap over a mob, 15 Flow).
- Book talents: Acrobatics, Mobility.
