# Class: Avenger

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Avenger' (p. 80). -->

## 1. Basics

- **Id:** avenger
- **Display name:** Avenger
- **Description:** A sworn hunter of wrongdoers who marks whoever hurts them or their friends, and never lets it escape.
- **Icon item:** crossbow
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.2 |
| Strength     | 6 | x1.1 |
| Dexterity    | 3 | x1 |
| Agility      | 3 | x1 |
| Intelligence | 1 | x0.9 |
| Wisdom       | 2 | x0.9 |
| Endurance    | 5 | x1.1 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Vengeance
- **Description:** The heat of the oath you swore.
- **What each point gives:** +0.5% damage to your marked target
- **Max points:** 100

### Resource bar

- **Name:** Retribution
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** no
- **Colour:** crimson (#C2142E)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Vow of Vengeance: the next mob to damage you or a player within 16 blocks becomes marked for 60s (one at a time)
- +20% damage to your marked target and it glows for you
- When your marked target moves away, you get Speed II for 2s
- Retribution fills by 3 every time you or a nearby ally takes damage

### Special passives

- Avenger's Wrath: +0.5% damage to marked target per Vengeance
- Per 10 Vengeance, marked targets within 4 blocks get Weakness I

## 6. Weakness

- **What players read:** Avengers can't let go. Nothing else matters until the debt is paid.
- **What it actually does:** -15% damage against anything that isn't your marked target while a mark is active

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Retribution

- **Shared or class-only:** class-only
- **Trained by:** killing marked targets
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Vengeance
- **Tier II bonus:** +1 Vengeance
- **Tier III bonus:** +5 max Retribution
- **Tier IV bonus:** +1 Vengeance
- **Tier V bonus:** +2 Vengeance

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Paladin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| avenger_start | Avenger | start | - | - | - | +3% damage |
| avenger_grudge | Grudge | small | 1 | 1 | avenger_start | +6% damage to marked target |
| avenger_unyielding | Unyielding | small | 1 | 1 | avenger_start | +3 max health |
| avenger_pursuit | Pursuit | notable | 2 | 12 | avenger_grudge | +8% movement speed toward the marked target |
| avenger_righteous_fury | Righteous Fury | notable | 2 | 15 | avenger_unyielding | +1 armor per 25% missing health |
| avenger_blood_debt | Blood Debt | keystone | 3 | 30 | avenger_pursuit, avenger_righteous_fury | marks last until the target dies, but you can never mark a new one until it does |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Vow of Vengeance | start | - | - | - | - |
| sworn | Sworn | small | 1 | 1 | start | +2 Vengeance |
| hound | Hound | small | 1 | 5 | start | Speed boost lasts 2s longer |
| frightening_glare | Frightening Glare | notable | 2 | 15 | sworn | marked targets within your reach deal 15% less damage |
| avenger_s_wrath | Avenger's Wrath | notable | 2 | 20 | hound | +10% damage to marked target |
| unending_vendetta | Unending Vendetta | keystone | 3 | 35 | frightening_glare, avenger_s_wrath | killing a marked target heals you 25%; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| avenger_stat_start | Avenger | start | - | - | - | - |
| avenger_relentless | Relentless | small | 2 | 1 | avenger_stat_start | +2 Strength, +1 Endurance |
| avenger_iron_oath | Iron Oath | small | 2 | 5 | avenger_relentless | +2 Vitality, +1 Wisdom |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Declare Vengeance** (mark any mob you can see, 20 Retribution).
- Book talents: Vow of Vengeance, Avenger's Wrath.
