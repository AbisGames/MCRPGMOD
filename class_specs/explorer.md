# Class: Explorer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Explorer' (p. 87). -->

## 1. Basics

- **Id:** explorer
- **Display name:** Explorer
- **Description:** A survivor of the world's edges who shrugs off hardship, keeps the group going and digs deep when luck runs out.
- **Icon item:** compass
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.1 |
| Strength     | 3 | x1 |
| Dexterity    | 4 | x1.1 |
| Agility      | 6 | x1.25 |
| Intelligence | 2 | x0.75 |
| Wisdom       | 2 | x1 |
| Endurance    | 3 | x0.9 |
| Luck         | 1 | x0.9 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Wayfinding
- **Description:** Endurance and experience from far journeys.
- **What each point gives:** +0.3% movement speed outdoors
- **Max points:** 100

### Resource bar

- **Name:** Endurance
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** map tan (#C8A86B)

## 4. Starting kit

- 1 × compass
- 1 × map
- 1 × stone_pickaxe
- 1 × leather_chestplate
- 8 × cooked_cod
- 4 × torch

## 5. Passives

- Preternatural Senses: hostile mobs within 12 blocks glow while you sneak
- Perseverance: Poison, Hunger and Slowness wear off 50% faster
- Respite: sleeping heals you and players within 16 blocks fully
- Driven: 10% chance that a hit that would leave you under 2 hearts leaves you at 2 hearts instead

### Special passives

- Hunger drain -0.3% per Wayfinding
- Per 10 Wayfinding, +1 block glow range

## 6. Weakness

- **What players read:** Always moving on - never settled.
- **What it actually does:** -25% Endurance regen within 64 blocks of your bed/spawn point

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | neutral |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | proficient |
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

### Skill: Survival

- Shared skill - full definition in `ranger.md`

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Pathfinding

- **Shared or class-only:** class-only
- **Trained by:** discovering new chunks / travelling long distances
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Wayfinding
- **Tier II bonus:** +1 Wayfinding
- **Tier III bonus:** +10% Endurance regen
- **Tier IV bonus:** +1 Wayfinding
- **Tier V bonus:** +2 Wayfinding

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Ranger

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| explorer_start | Explorer | start | - | - | - | -10% hunger drain |
| explorer_trail_legs | Trail Legs | small | 1 | 1 | explorer_start | +5% movement speed outdoors |
| explorer_iron_stomach | Iron Stomach | small | 1 | 1 | explorer_start | immune to food poisoning effects |
| explorer_pack_mule | Pack Mule | notable | 2 | 12 | explorer_trail_legs | +10% knockback resistance |
| explorer_cartographer | Cartographer | notable | 2 | 15 | explorer_iron_stomach | maps you make explore 25% faster |
| explorer_far_horizon | Far Horizon | keystone | 3 | 30 | explorer_pack_mule, explorer_cartographer | +15% movement speed outdoors, but -15% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Perseverance | start | - | - | - | - |
| hardship | Hardship | small | 1 | 1 | start | +2 Wayfinding |
| respite | Respite | small | 1 | 5 | start | Respite also gives Regeneration I for 30s |
| driven | Driven | notable | 2 | 15 | hardship | Driven chance 20% |
| survivor | Survivor | notable | 2 | 20 | respite | immune to freezing |
| trailblazer | Trailblazer | keystone | 3 | 35 | driven, survivor | players within 16 blocks get +10% speed outdoors; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| explorer_stat_start | Explorer | start | - | - | - | - |
| explorer_hardy | Hardy | small | 2 | 1 | explorer_stat_start | +2 Vitality, +1 Endurance |
| explorer_fleet | Fleet | small | 2 | 5 | explorer_hardy | +2 Agility, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Preternatural Senses, Perseverance, Respite, Driven.
