# Class: Priest

<!-- Source: Shadow of the Demon Lord core rulebook, novice path 'Priest' (p. 56). -->

## 1. Basics

- **Id:** priest
- **Display name:** Priest
- **Description:** A servant of the gods who heals friends and smites the dead. Sturdier than a magician, holier than a warrior.
- **Icon item:** golden_apple
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 3 |
| Dexterity    | 0 |
| Agility      | 0 |
| Intelligence | 1 |
| Wisdom       | 5 |
| Endurance    | 2 |
| Luck         | 0 |
| Special stat (section 3) | 5 |

## 3. Special stat

- **Name:** Faith
- **Description:** Strength of devotion. Powers healing and holy damage.
- **What each point gives:** +1% healing received from every source
- **Max points:** 100

### Resource bar

- **Name:** Devotion
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** pale gold (#F2D16B)

## 4. Starting kit

- 1 × wooden_shovel
- 1 × shield
- 2 × golden_carrot
- 6 × bread

## 5. Passives

- Prayer: other players within 8 blocks deal +5% damage
- +10% damage against undead mobs
- Natural regeneration is 20% faster

### Special passives

- +1% damage against undead per 2 Faith points
- Prayer aura range +1 block per 10 Faith points

## 6. Weakness

- **What players read:** Far from the sky, the gods' voices fade.
- **What it actually does:** in the Nether and the End: Devotion does not regenerate and Prayer is disabled

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
| Specific items / exceptions: | wooden_shovel and stone_shovel: count as maces at neutral (a priest's crook/club) |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 1
- **Free skill points at start (special tree):** 0

## 10. Skills (mastery)

### Skill: Mace Training

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with maces
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% mace damage
- **Tier II bonus:** +3% mace damage
- **Tier III bonus:** +10% fall-smash damage
- **Tier IV bonus:** +3% mace damage
- **Tier V bonus:** +6% mace damage

### Skill: Undead Bane

- **Shared or class-only:** shared
- **Trained by:** killing undead mobs (zombies, skeletons, phantoms, drowned, wither skeletons...)
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% damage to undead
- **Tier II bonus:** +3% damage to undead
- **Tier III bonus:** -5% damage taken from undead
- **Tier IV bonus:** +3% damage to undead
- **Tier V bonus:** +6% damage to undead

### Skill: Mending

- **Shared or class-only:** shared
- **Trained by:** healing damage (any source)
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% healing received
- **Tier II bonus:** +0.5 max health
- **Tier III bonus:** +3% healing received
- **Tier IV bonus:** +1 max health
- **Tier V bonus:** +6% healing received

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Devout

- **Shared or class-only:** class-only
- **Trained by:** other players within 8 blocks killing mobs while your Prayer aura is active
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Faith
- **Tier II bonus:** +1 Faith
- **Tier III bonus:** +1 Devotion regen per 10s
- **Tier IV bonus:** +1 Faith
- **Tier V bonus:** +2 Faith

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** right side of the web (the Faith quarter), ring 1

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| priest_start | Priest | start | - | - | - | +1 max health |
| priest_blessed | Blessed | small | 1 | 1 | priest_start | +5% healing received |
| priest_smite | Smite | small | 1 | 1 | priest_start | +6% damage to undead |
| priest_vigil | Vigil | small | 1 | 5 | priest_blessed | +1 armor |
| priest_sanctified | Sanctified | notable | 2 | 10 | priest_vigil, priest_smite | +10% natural regeneration, immune to Hunger effect |
| priest_martyr | Martyr | keystone | 3 | 25 | priest_sanctified | Prayer aura also gives allies +5% damage reduction, but you take 10% more damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Shared Recovery | start | - | - | - | - |
| prayer | Prayer | small | 1 | 1 | start | +2 Faith |
| divine_strike | Divine Strike | small | 1 | 5 | start | allies in your Prayer aura deal +5% more damage |
| inspiring | Inspiring Prayer | notable | 2 | 15 | prayer, divine_strike | you also gain your own Prayer bonus |
| tradition | Sacred Tradition | notable | 2 | 20 | prayer | +20% max Devotion |
| improved_recovery | Improved Shared Recovery | keystone | 3 | 35 | inspiring, tradition | Shared Recovery can be used twice per rest; Prayer aura range -2 blocks |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| priest_stat_start | Priest | start | - | - | - | - |
| priest_pious | Pious | small | 2 | 1 | priest_stat_start | +2 Wisdom, +1 Vitality |
| priest_steadfast | Steadfast | small | 2 | 5 | priest_pious | +2 Strength, +1 Endurance |

## 12. Anything else

- **Power tier: Novice.** Book levels 1, 2, 5, 8. Baseline power: 14 stat points, 5 special points, 3 trinket slots. Selectable from the start.
- Active ability for later: **Shared Recovery** (heal yourself and one nearby player, 15 Devotion, once per rest).
- The book ties priests to a religion (New God, Old Faith, Dwarfen Ancestors, Witchcraft). Could become a sub-choice later - ideas welcome.
- **Co-design question:** Faith + Devotion are my proposal. Keep, or rename/rework?
