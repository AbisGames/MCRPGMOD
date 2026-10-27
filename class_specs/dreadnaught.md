# Class: Dreadnaught

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Dreadnaught' (p. 85). -->

## 1. Basics

- **Id:** dreadnaught
- **Display name:** Dreadnaught
- **Description:** A master of heavy armor - near invulnerable and impossible to shove around.
- **Icon item:** netherite_chestplate
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 7 |
| Strength     | 6 |
| Dexterity    | 1 |
| Agility      | 0 |
| Intelligence | 0 |
| Wisdom       | 2 |
| Endurance    | 9 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Plate
- **Description:** Expertise in heavy armor.
- **What each point gives:** +0.1 armor toughness while wearing heavy armor
- **Max points:** 100

### Resource bar

- **Name:** Fortitude
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** iron grey (#6B6F75)

## 4. Starting kit

- 1 × iron_chestplate
- 1 × iron_helmet
- 1 × iron_sword
- 6 × bread

## 5. Passives

- Iron Clad: heavy armor gives +1 armor per piece and no movement penalty
- Immovable: immune to knockback while standing on solid ground
- Weapon Resistance: while wearing 4 heavy pieces, take 20% less damage from weapons and projectiles

### Special passives

- Weapon Resistance +0.2% per Plate
- Per 10 Plate, armor durability loss -5%

## 6. Weakness

- **What players read:** Plate is heavy. Water is deep.
- **What it actually does:** sinks in water, can't swim upward while wearing 3+ heavy pieces, and takes 25% more drowning damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | proficient |
| Light armor (leather, turtle) | clumsy |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Ironclad

- **Shared or class-only:** class-only
- **Trained by:** taking damage while wearing 4 heavy armor pieces
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Plate
- **Tier II bonus:** +1 Plate
- **Tier III bonus:** +1 armor
- **Tier IV bonus:** +1 Plate
- **Tier V bonus:** +2 Plate

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| dreadnaught_start | Dreadnaught | start | - | - | - | +1 armor |
| dreadnaught_riveted | Riveted | small | 1 | 1 | dreadnaught_start | +1 armor toughness |
| dreadnaught_padded | Padded | small | 1 | 1 | dreadnaught_start | +3 max health |
| dreadnaught_deflection | Deflection | notable | 2 | 12 | dreadnaught_riveted | +10% projectile damage reduction |
| dreadnaught_anchored | Anchored | notable | 2 | 15 | dreadnaught_padded | immune to Slowness |
| dreadnaught_walking_fortress | Walking Fortress | keystone | 3 | 30 | dreadnaught_deflection, dreadnaught_anchored | +30% damage reduction, but -15% movement speed and -15% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Iron Clad | start | - | - | - | - |
| heavy_plate | Heavy Plate | small | 1 | 1 | start | +2 Plate |
| immovable | Immovable | small | 1 | 5 | start | also immune to wind charges and explosions' knockback |
| weapon_resistance | Weapon Resistance | notable | 2 | 15 | heavy_plate | +10% more weapon resistance |
| unbreakable | Unbreakable | notable | 2 | 20 | immovable | armor never drops below 10% durability while worn |
| colossus | Colossus | keystone | 3 | 35 | weapon_resistance, unbreakable | +4 armor, but you can't sprint |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| dreadnaught_stat_start | Dreadnaught | start | - | - | - | - |
| dreadnaught_ox_strong | Ox-Strong | small | 2 | 1 | dreadnaught_stat_start | +2 Strength, +1 Endurance |
| dreadnaught_iron_body | Iron Body | small | 2 | 5 | dreadnaught_ox_strong | +2 Endurance, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Iron Clad, Immovable, Weapon Resistance.
