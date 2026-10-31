# Class: Defender

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Defender' (p. 84). -->

## 1. Basics

- **Id:** defender
- **Display name:** Defender
- **Description:** A wall of armor that protects allies, punishes anything attacking them and pins enemies in place.
- **Icon item:** shield
- **Selectable by players:** yes
- **Base health:** 36

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 7 | x1.45 |
| Strength     | 5 | x1.2 |
| Dexterity    | 1 | x1 |
| Agility      | 0 | x0.55 |
| Intelligence | 1 | x0.8 |
| Wisdom       | 3 | x1 |
| Endurance    | 8 | x1.2 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Bulwark
- **Description:** How much punishment you can soak for others.
- **What each point gives:** +0.1 armor and +0.3% knockback resistance
- **Max points:** 100

### Resource bar

- **Name:** Guard
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** steel blue (#4A6C8C)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × iron_chestplate
- 6 × bread

## 5. Passives

- Defend: players within 4 blocks of you take 15% less damage
- Retributive Strike: when a mob within 3 blocks hits another player, your next hit on it deals +30%
- Containing Strike: your melee hits give Slowness II for 1s

### Special passives

- Defend reduction +0.2% per Bulwark
- Per 10 Bulwark, +1 block Defend range

## 6. Weakness

- **What players read:** A defender's place is beside others - and their pace is theirs.
- **What it actually does:** -10% movement speed and -15% damage when no other player is within 8 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | mastery |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Protector

- **Shared or class-only:** class-only
- **Trained by:** blocking hits aimed at mobs targeting other players / taking damage near allies
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Bulwark
- **Tier II bonus:** +1 Bulwark
- **Tier III bonus:** +10% Guard regen
- **Tier IV bonus:** +1 Bulwark
- **Tier V bonus:** +2 Bulwark

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Paladin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| defender_start | Defender | start | - | - | - | +1 armor |
| defender_shield_brother | Shield Brother | small | 1 | 1 | defender_start | Defend range +2 |
| defender_plate | Plate | small | 1 | 1 | defender_start | +2 armor |
| defender_interpose | Interpose | notable | 2 | 12 | defender_shield_brother | +10% Defend reduction |
| defender_taunt | Taunt | notable | 2 | 15 | defender_plate | mobs within 6 blocks prefer to target you |
| defender_living_wall | Living Wall | keystone | 3 | 30 | defender_interpose, defender_taunt | Defend reduction doubled, but you deal -25% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Defend | start | - | - | - | - |
| stand_fast | Stand Fast | small | 1 | 1 | start | +2 Bulwark |
| hook | Hook | small | 1 | 5 | start | Containing Strike lasts 2s |
| retributive_strike | Retributive Strike | notable | 2 | 15 | stand_fast | retribution bonus +20% |
| bodyguard | Bodyguard | notable | 2 | 20 | hook | unlocks Intercept ability |
| aegis_eternal | Aegis Eternal | keystone | 3 | 35 | retributive_strike, bodyguard | you take 25% of the damage nearby allies take instead of them; -10% movement speed |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| defender_stat_start | Defender | start | - | - | - | - |
| defender_stalwart | Stalwart | small | 2 | 1 | defender_stat_start | +2 Endurance, +1 Vitality |
| defender_strong_guard | Strong Guard | small | 2 | 5 | defender_stalwart | +2 Strength, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Intercept** (dash to an ally and take the next hit for them, 20 Guard).
- Book talents: Defend, Containing Strike, Retributive Strike.
