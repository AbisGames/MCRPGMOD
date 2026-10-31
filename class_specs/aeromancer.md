# Class: Aeromancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Aeromancer' (p. 79). -->

## 1. Basics

- **Id:** aeromancer
- **Display name:** Aeromancer
- **Description:** Bonded to a wind genie, the aeromancer rides the air, throws gusts and eventually flies.
- **Icon item:** wind_charge
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 0 | x0.9 |
| Dexterity    | 2 | x1 |
| Agility      | 4 | x1.1 |
| Intelligence | 7 | x1.2 |
| Wisdom       | 6 | x1.1 |
| Endurance    | 2 | x0.8 |
| Luck         | 2 | x0.9 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Gale
- **Description:** The strength of your bond with the wind genie.
- **What each point gives:** +0.5% movement speed and +1% wind charge knockback
- **Max points:** 100

### Resource bar

- **Name:** Breath of Wind
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** pale sky (#BFE8FF)

## 4. Starting kit

- 8 × wind_charge
- 1 × leather_chestplate
- 2 × feather
- 6 × bread

## 5. Passives

- Air Walk: after throwing a wind charge, Slow Falling for 3s
- +50% wind charge knockback and they don't hurt you
- No fall damage while Breath of Wind is above 25%
- Arrows shot at you are 15% likely to be blown aside

### Special passives

- +0.2% arrow-deflect chance per Gale point
- Per 20 Gale, +1 extra jump block height

## 6. Weakness

- **What players read:** Aeromancers belong to the open sky.
- **What it actually does:** -20% damage and no Breath of Wind regen underground (no sky access) and underwater

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Windcalling

- **Shared or class-only:** class-only
- **Trained by:** throwing wind charges and falling long distances safely
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Gale
- **Tier II bonus:** +1 Gale
- **Tier III bonus:** +10% Breath regen
- **Tier IV bonus:** +1 Gale
- **Tier V bonus:** +2 Gale

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Sorcerer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| aeromancer_start | Aeromancer | start | - | - | - | +5% movement speed |
| aeromancer_updraft | Updraft | small | 1 | 1 | aeromancer_start | +1 jump height |
| aeromancer_tailwind | Tailwind | small | 1 | 1 | aeromancer_start | +6% movement speed |
| aeromancer_deflecting_winds | Deflecting Winds | notable | 2 | 12 | aeromancer_updraft | +10% arrow-deflect chance |
| aeromancer_skyborn | Skyborn | notable | 2 | 15 | aeromancer_tailwind | -30% fall damage at all times |
| aeromancer_eye_of_the_storm | Eye of the Storm | keystone | 3 | 30 | aeromancer_deflecting_winds, aeromancer_skyborn | you can glide like with an elytra without one, but -3 armor |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Air Walk | start | - | - | - | - |
| zephyr | Zephyr | small | 1 | 1 | start | +2 Gale |
| gust | Gust | small | 1 | 5 | start | wind charges you throw deal +2 damage |
| cyclone | Cyclone | notable | 2 | 15 | zephyr | wind charges pull light mobs in before pushing them out |
| flight | Flight | notable | 2 | 20 | gust | unlocks Flight ability |
| lord_of_the_air | Lord of the Air | keystone | 3 | 35 | cyclone, flight | Flight costs 30% less Breath; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| aeromancer_stat_start | Aeromancer | start | - | - | - | - |
| aeromancer_airy | Airy | small | 2 | 1 | aeromancer_stat_start | +2 Agility, +1 Intelligence |
| aeromancer_clear_mind | Clear Mind | small | 2 | 5 | aeromancer_airy | +2 Wisdom, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Gust** (cone knockback, 15 Breath), **Flight** (creative-style flight that drains Breath; book level 10 talent).
- Book talents: Air Walk, Flight. Tradition: Air.
