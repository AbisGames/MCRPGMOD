# Class: Executioner

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Executioner' (p. 87). -->

## 1. Basics

- **Id:** executioner
- **Display name:** Executioner
- **Description:** A master of the killing blow. Every swing can end a fight - at the cost of leaving themselves open.
- **Icon item:** iron_axe
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.1 |
| Strength     | 7 | x1.2 |
| Dexterity    | 5 | x1.1 |
| Agility      | 3 | x0.9 |
| Intelligence | 1 | x0.9 |
| Wisdom       | 0 | x0.8 |
| Endurance    | 4 | x1 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Severance
- **Description:** Anatomical knowledge of where to cut.
- **What each point gives:** +0.5% critical damage
- **Max points:** 100

### Resource bar

- **Name:** Dread
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** black hood (#1C1C1C)

## 4. Starting kit

- 1 × iron_axe
- 1 × leather_helmet
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Execute: fully charged hits deal +20%; if a hit takes a mob below 15% health, it dies
- Exacting Strike: critical hits deal maximum damage (+25% critical damage)
- After an Execute kill, mobs within 6 blocks get Weakness I for 3s

### Special passives

- Execute threshold +0.1% per Severance
- Per 10 Severance, +3% damage to mobs below half health

## 6. Weakness

- **What players read:** All-in swings leave you wide open.
- **What it actually does:** for 2s after a fully charged hit, you take 20% more damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | mastery |
| Spears | neutral |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
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

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Backstab

- Shared skill - full definition in `rogue.md`

### Skill: Headsman

- **Shared or class-only:** class-only
- **Trained by:** killing mobs with Execute
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Severance
- **Tier II bonus:** +1 Severance
- **Tier III bonus:** +10% Dread regen
- **Tier IV bonus:** +1 Severance
- **Tier V bonus:** +2 Severance

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Assassin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| executioner_start | Executioner | start | - | - | - | +5% critical damage |
| executioner_clean_cut | Clean Cut | small | 1 | 1 | executioner_start | +6% critical damage |
| executioner_black_hood | Black Hood | small | 1 | 1 | executioner_start | mobs notice you 10% later |
| executioner_merciless | Merciless | notable | 2 | 12 | executioner_clean_cut | Execute threshold 20% |
| executioner_terrifying | Terrifying | notable | 2 | 15 | executioner_black_hood | Weakness after kill becomes Weakness II |
| executioner_headsman_s_axe | Headsman's Axe | keystone | 3 | 30 | executioner_merciless, executioner_terrifying | Execute threshold 25%, but -15% damage to mobs above half health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Execute | start | - | - | - | - |
| anatomy | Anatomy | small | 1 | 1 | start | +2 Severance |
| steady | Steady | small | 1 | 5 | start | exposure after charged hits lasts 1s |
| exacting_strike | Exacting Strike | notable | 2 | 15 | anatomy | +10% critical damage |
| death_sentence | Death Sentence | notable | 2 | 20 | steady | unlocks Sentence ability |
| inevitable | Inevitable | keystone | 3 | 35 | exacting_strike, death_sentence | executed mobs drop +1 Looting; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| executioner_stat_start | Executioner | start | - | - | - | - |
| executioner_grim | Grim | small | 2 | 1 | executioner_stat_start | +2 Strength, +1 Dexterity |
| executioner_cold | Cold | small | 2 | 5 | executioner_grim | +2 Dexterity, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Sentence** (mark a mob; your next hit on it deals triple damage, 30 Dread).
- Book talents: Execute, Exacting Strike.
