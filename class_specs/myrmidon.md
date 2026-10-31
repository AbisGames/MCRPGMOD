# Class: Myrmidon

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Myrmidon' (p. 93). -->

## 1. Basics

- **Id:** myrmidon
- **Display name:** Myrmidon
- **Description:** A shield-fighter who uses the shield as both wall and weapon - deflecting, bashing and shoving foes away.
- **Icon item:** shield
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 6 | x1.35 |
| Strength     | 6 | x1.2 |
| Dexterity    | 3 | x1 |
| Agility      | 1 | x1 |
| Intelligence | 0 | x0.65 |
| Wisdom       | 1 | x0.8 |
| Endurance    | 8 | x1.2 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Shieldcraft
- **Description:** Mastery of shield fighting.
- **What each point gives:** +0.5% shield bash damage and -0.2% damage taken while blocking
- **Max points:** 100

### Resource bar

- **Name:** Bulwark
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** bronze (#B07A3C)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Shield Block: shields recover from axe-disable 50% faster and block 100% of melee damage from the front
- Forceful Shield: blocking a melee attack knocks the attacker back 3 blocks
- Shield Mastery: +2 armor while holding a shield; sneak-attacking with a raised shield bashes for 4 damage

### Special passives

- Knockback +0.05 blocks per Shieldcraft
- Per 10 Shieldcraft, +1 armor with shield

## 6. Weakness

- **What players read:** The shield is a myrmidon's whole style.
- **What it actually does:** -20% damage and -2 armor without a shield in hand

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
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

### Skill: Phalanx

- **Shared or class-only:** class-only
- **Trained by:** blocking attacks
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Shieldcraft
- **Tier II bonus:** +1 Shieldcraft
- **Tier III bonus:** +10% Bulwark regen
- **Tier IV bonus:** +1 Shieldcraft
- **Tier V bonus:** +2 Shieldcraft

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| myrmidon_start | Myrmidon | start | - | - | - | +1 armor |
| myrmidon_brace | Brace | small | 1 | 1 | myrmidon_start | +10% knockback resistance |
| myrmidon_boss_strike | Boss Strike | small | 1 | 1 | myrmidon_start | shield bash +2 damage |
| myrmidon_turtle | Turtle | notable | 2 | 12 | myrmidon_brace | blocking also stops arrows from the sides |
| myrmidon_counterbash | Counterbash | notable | 2 | 15 | myrmidon_boss_strike | after a block, next hit +20% |
| myrmidon_living_wall | Living Wall | keystone | 3 | 30 | myrmidon_turtle, myrmidon_counterbash | shields can't be disabled, but -10% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Shield Block | start | - | - | - | - |
| heavy_shield | Heavy Shield | small | 1 | 1 | start | +2 Shieldcraft |
| forceful_shield | Forceful Shield | small | 1 | 5 | start | +2 knockback blocks |
| shield_bash | Shield Bash | notable | 2 | 15 | heavy_shield | unlocks Shield Charge ability |
| shield_mastery | Shield Mastery | notable | 2 | 20 | forceful_shield | +1 more armor with shield |
| aspis | Aspis | keystone | 3 | 35 | shield_bash, shield_mastery | players within 3 blocks behind you are protected from projectiles; -10% damage |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| myrmidon_stat_start | Myrmidon | start | - | - | - | - |
| myrmidon_strong_arm | Strong Arm | small | 2 | 1 | myrmidon_stat_start | +2 Strength, +1 Endurance |
| myrmidon_sturdy | Sturdy | small | 2 | 5 | myrmidon_strong_arm | +2 Endurance, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Shield Charge** (dash forward with shield raised, knocking mobs aside, 25 Bulwark).
- Book talents: Forceful Shield, Shield Block, Shield Mastery.
