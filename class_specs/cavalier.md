# Class: Cavalier

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Cavalier' (p. 82). -->

## 1. Basics

- **Id:** cavalier
- **Display name:** Cavalier
- **Description:** A mounted warrior who fights best in the saddle - devastating charges and a steed that's hard to bring down.
- **Icon item:** saddle
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.25 |
| Strength     | 6 | x1.15 |
| Dexterity    | 3 | x1 |
| Agility      | 2 | x0.85 |
| Intelligence | 1 | x0.75 |
| Wisdom       | 2 | x1 |
| Endurance    | 5 | x1.15 |
| Luck         | 2 | x0.85 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Horsemanship
- **Description:** Your bond and skill with mounts.
- **What each point gives:** +1% mount speed and +1% mount max health
- **Max points:** 100

### Resource bar

- **Name:** Charge
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** royal blue (#2D4FA8)

## 4. Starting kit

- 1 × saddle
- 1 × lead
- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 4 × apple
- 4 × bread

## 5. Passives

- Combat Riding: +20% damage while mounted
- Master Rider: your mount gets +4 armor and +10% speed
- Devastating Charge: sprinting or riding at full speed into a mob adds +20% damage to the hit (+40% mounted)
- You never fall off when your mount is hit

### Special passives

- Mount armor +1 per 10 Horsemanship
- Mounted damage +0.5% per Horsemanship

## 6. Weakness

- **What players read:** Off the horse, a cavalier is just a soldier with a limp.
- **What it actually does:** -15% damage and -10% movement speed while not mounted

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | mastery |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | mastery |
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

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Riding

- **Shared or class-only:** shared
- **Trained by:** travelling on horses, camels, donkeys, pigs and striders
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +5% mount speed
- **Tier II bonus:** +2 mount max health
- **Tier III bonus:** +5% mount jump
- **Tier IV bonus:** +2 mount max health
- **Tier V bonus:** +10% mount speed

### Skill: Lancer

- **Shared or class-only:** class-only
- **Trained by:** hitting mobs while mounted
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Horsemanship
- **Tier II bonus:** +1 Horsemanship
- **Tier III bonus:** +10% Charge regen
- **Tier IV bonus:** +1 Horsemanship
- **Tier V bonus:** +2 Horsemanship

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| cavalier_start | Cavalier | start | - | - | - | +5% mount speed |
| cavalier_saddle_born | Saddle-Born | small | 1 | 1 | cavalier_start | +5% mounted damage |
| cavalier_barding | Barding | small | 1 | 1 | cavalier_start | mount +2 armor |
| cavalier_lance_charge | Lance Charge | notable | 2 | 12 | cavalier_saddle_born | spears deal +15% damage while mounted |
| cavalier_warhorse | Warhorse | notable | 2 | 15 | cavalier_barding | mount +6 max health |
| cavalier_knight_errant | Knight Errant | keystone | 3 | 30 | cavalier_lance_charge, cavalier_warhorse | mounted damage +25%, but -25% damage on foot |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Combat Riding | start | - | - | - | - |
| spurs | Spurs | small | 1 | 1 | start | +2 Horsemanship |
| trample | Trample | small | 1 | 5 | start | your mount damages small mobs it runs into |
| devastating_charge | Devastating Charge | notable | 2 | 15 | spurs | unlocks Charge ability |
| master_rider | Master Rider | notable | 2 | 20 | trample | mount gets Regeneration I while you ride |
| bonded_steed | Bonded Steed | keystone | 3 | 35 | devastating_charge, master_rider | your mount respawns at your bed a day after dying; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| cavalier_stat_start | Cavalier | start | - | - | - | - |
| cavalier_rider_s_seat | Rider's Seat | small | 2 | 1 | cavalier_stat_start | +2 Dexterity, +1 Strength |
| cavalier_knightly | Knightly | small | 2 | 5 | cavalier_rider_s_seat | +2 Strength, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Charge** (mount dashes forward, first mob hit takes heavy damage + knockback, 25 Charge).
- Book talents: Combat Riding, Devastating Charge, Master Rider.
