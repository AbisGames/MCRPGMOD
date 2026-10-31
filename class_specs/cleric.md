# Class: Cleric

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Cleric' (p. 64). -->

## 1. Basics

- **Id:** cleric
- **Display name:** Cleric
- **Description:** A militant champion of the gods who drives back the undead with holy symbols and righteous steel.
- **Icon item:** totem_of_undying
- **Selectable by players:** yes
- **Base health:** 26

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.15 |
| Strength     | 4 | x1 |
| Dexterity    | 0 | x0.85 |
| Agility      | 0 | x0.7 |
| Intelligence | 2 | x1 |
| Wisdom       | 6 | x1.3 |
| Endurance    | 3 | x1.15 |
| Luck         | 1 | x0.85 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Conviction
- **Description:** Unshakeable belief. Empowers holy strikes and steadies the mind.
- **What each point gives:** +1% damage against undead
- **Max points:** 100

### Resource bar

- **Name:** Grace
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** holy white-gold (#FFF1B8)

## 4. Starting kit

- 1 × stone_axe
- 1 × shield
- 1 × chainmail_chestplate
- 1 × golden_apple
- 6 × bread

## 5. Passives

- Conviction: immune to Darkness (warden) and Nausea effects
- +20% damage against undead mobs
- Icon of Faith: while holding a totem_of_undying or golden item in the off-hand, +10% damage and -10% damage taken from undead

### Special passives

- Empowered Symbol: +1% healing done and received per 2 Conviction
- +0.05 armor per Conviction point

## 6. Weakness

- **What players read:** The cleric's purity draws the hunger of the damned.
- **What it actually does:** takes 20% more damage from wither skeletons, the Wither and the Wither effect

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | neutral |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | mastery |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
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

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Mending

- Shared skill - full definition in `priest.md`

### Skill: Crusade

- **Shared or class-only:** class-only
- **Trained by:** killing undead mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Conviction
- **Tier II bonus:** +1 Conviction
- **Tier III bonus:** +10% Grace regen
- **Tier IV bonus:** +1 Conviction
- **Tier V bonus:** +2 Conviction

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** right side of the web (Faith quarter), ring 2, leaning toward the War quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| cleric_start | Cleric | start | - | - | - | +5% damage to undead |
| cleric_scripture | Scripture | small | 1 | 1 | cleric_start | +1 armor |
| cleric_censure | Censure | small | 1 | 1 | cleric_start | +8% damage to undead |
| cleric_bulwark | Bulwark of Faith | notable | 2 | 10 | cleric_scripture | -10% damage taken from undead |
| cleric_holy_wrath | Holy Wrath | notable | 2 | 12 | cleric_censure | undead you hit are set on fire for 2s |
| cleric_crusader | Crusader | keystone | 3 | 25 | cleric_bulwark, cleric_holy_wrath | +30% damage to undead, but -10% damage to everything else |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Icon of Faith | start | - | - | - | - |
| conviction | Conviction | small | 1 | 1 | start | +2 Conviction |
| blessed_symbol | Blessed Symbol | small | 1 | 5 | start | Icon of Faith also accepts any shield with a banner pattern |
| empowered_symbol | Empowered Symbol | notable | 2 | 15 | conviction | healing you receive is +15% while holding your icon |
| turn_undead | Turn Undead | notable | 2 | 20 | blessed_symbol | unlocks Turn Undead (undead flee for 5s) |
| divine_power | Divine Power | keystone | 3 | 35 | empowered_symbol, turn_undead | Icon of Faith bonuses double; without an icon in hand you deal -10% damage |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| cleric_stat_start | Cleric | start | - | - | - | - |
| cleric_devout | Devout | small | 2 | 1 | cleric_stat_start | +2 Wisdom, +1 Strength |
| cleric_armored | Armored in Faith | small | 2 | 5 | cleric_devout | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Turn Undead** (undead within 8 blocks flee, 20 Grace), **Holy Strike** (empower next hit, +1d6-style bonus vs undead).
- Book talents: Conviction, Icon of Faith, Empowered Symbol, Divine Power.
- **Co-design question:** Conviction + Grace. Is the totem/golden-item 'holy symbol' fun, or should there be a custom holy symbol trinket?
