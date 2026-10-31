# Class: Sharpshooter

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Sharpshooter' (p. 95). -->

## 1. Basics

- **Id:** sharpshooter
- **Display name:** Sharpshooter
- **Description:** A master archer who lines up the perfect shot and threads arrows through any gap.
- **Icon item:** bow
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.2 |
| Strength     | 3 | x0.65 |
| Dexterity    | 10 | x1.35 |
| Agility      | 4 | x1.2 |
| Intelligence | 1 | x1 |
| Wisdom       | 1 | x0.8 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Precision
- **Description:** Steady hands and a keen eye.
- **What each point gives:** +0.6% bow and crossbow damage
- **Max points:** 100

### Resource bar

- **Name:** Focus
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** fletching green (#5C8C3A)

## 4. Starting kit

- 1 × bow
- 1 × crossbow
- 48 × arrow
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Deadly Aim: holding a fully drawn bow for 2 extra seconds makes the shot deal +50% damage and never miss (no spread)
- Perfect Shot: arrows ignore 20% of armor and fly 20% faster
- +15% bow and crossbow damage

### Special passives

- Deadly Aim bonus +1% per Precision
- Per 10 Precision, aim time -0.2s

## 6. Weakness

- **What players read:** Sharpshooters are lost in close quarters.
- **What it actually does:** -30% melee damage and -10% armor while a mob is within 3 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | mastery |
| Crossbows | mastery |
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

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Marksmanship

- **Shared or class-only:** class-only
- **Trained by:** landing Deadly Aim shots
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Precision
- **Tier II bonus:** +1 Precision
- **Tier III bonus:** +10% Focus regen
- **Tier IV bonus:** +1 Precision
- **Tier V bonus:** +2 Precision

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Ranger

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sharpshooter_start | Sharpshooter | start | - | - | - | +5% bow damage |
| sharpshooter_steady_breath | Steady Breath | small | 1 | 1 | sharpshooter_start | Deadly Aim time -0.5s |
| sharpshooter_piercing | Piercing | small | 1 | 1 | sharpshooter_start | arrows pierce 1 mob |
| sharpshooter_headshot | Headshot | notable | 2 | 12 | sharpshooter_steady_breath | +20% damage on hits to the upper part of a mob |
| sharpshooter_long_shot | Long Shot | notable | 2 | 15 | sharpshooter_piercing | +10% damage per 16 blocks of distance |
| sharpshooter_eagle_eye | Eagle Eye | keystone | 3 | 30 | sharpshooter_headshot, sharpshooter_long_shot | +30% ranged damage, but you can't hold a melee weapon in your off-hand or main hand without -50% |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Deadly Aim | start | - | - | - | - |
| calm | Calm | small | 1 | 1 | start | +2 Precision |
| quick_nock | Quick Nock | small | 1 | 5 | start | +15% draw speed |
| perfect_shot | Perfect Shot | notable | 2 | 15 | calm | armor ignore 35% |
| called_shot | Called Shot | notable | 2 | 20 | quick_nock | unlocks Called Shot ability |
| unerring | Unerring | keystone | 3 | 35 | perfect_shot, called_shot | Deadly Aim shots always crit; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sharpshooter_stat_start | Sharpshooter | start | - | - | - | - |
| sharpshooter_steady_hands | Steady Hands | small | 2 | 1 | sharpshooter_stat_start | +2 Dexterity, +1 Wisdom |
| sharpshooter_sniper_s_patience | Sniper's Patience | small | 2 | 5 | sharpshooter_steady_hands | +2 Dexterity, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Called Shot** (mark a target; next arrow deals triple damage, 25 Focus).
- Book talents: Deadly Aim, Perfect Shot.
