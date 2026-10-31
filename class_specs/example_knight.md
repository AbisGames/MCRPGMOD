# Class: Knight

<!-- EXAMPLE ONLY - shows how much detail Claude needs. Not built into the mod. -->

## 1. Basics

- **Id:** knight
- **Display name:** Knight
- **Description:** A sworn defender in heavy plate. Slow, hard to kill, deadly with sword and shield.
- **Icon item:** iron_sword
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.4 |
| Strength     | 4 | x1.2 |
| Dexterity    | 1 | x0.6 |
| Agility      | 0 | x1 |
| Intelligence | 0 | x0.8 |
| Wisdom       | 1 | x1 |
| Endurance    | 3 | x1.2 |
| Luck         | 0 | x0.8 |
| Special stat (section 3) | 5 | |

## 3. Special stat

- **Name:** Valor
- **Description:** Courage under fire. Feeds the Knight's armor and Resolve.
- **What each point gives:** +0.2 armor
- **Max points:** 100

### Resource bar

- **Name:** Resolve
- **Max at 0 points:** 20
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** no
- **Colour:** gold

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 8 × bread

## 5. Passives

- +10% knockback resistance
- -5% movement speed

### Special passives

- +1% damage taken reduction per 4 Valor points
- +0.05 armor toughness per Valor point

## 6. Weakness

- **What players read:** Plate armor is clumsy in the water and the Knight is slow to adapt to ranged combat.
- **What it actually does:** takes 25% more drowning damage; -20% damage with bows and crossbows

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
| Specific items / exceptions: | golden_sword: neutral (too soft for a knight) |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 1 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 1
- **Free skill points at start (special tree):** 0

## 10. Skills (mastery)

### Skill: Swordsmanship

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with swords
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% sword damage
- **Tier II bonus:** +3% sword damage
- **Tier III bonus:** +5% attack speed
- **Tier IV bonus:** +3% sword damage
- **Tier V bonus:** +6% sword damage

### Skill: Shield Wall

- **Shared or class-only:** shared
- **Trained by:** blocking attacks with a shield
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +1 armor
- **Tier II bonus:** +1 armor
- **Tier III bonus:** -5% damage taken
- **Tier IV bonus:** +1 armor toughness
- **Tier V bonus:** -5% damage taken

### Skill: Oathbound

- **Shared or class-only:** class-only
- **Trained by:** taking damage while wearing at least 3 pieces of heavy armor
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Valor
- **Tier II bonus:** +1 Valor
- **Tier III bonus:** +2 max health
- **Tier IV bonus:** +1 Valor
- **Tier V bonus:** +2 Valor

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom-left

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| knight_start | Knight | start | - | - | - | +1 armor |
| knight_iron_skin | Iron Skin | small | 1 | 1 | knight_start | +2 armor |
| knight_blade_drill | Blade Drill | small | 1 | 1 | knight_start | +5% sword damage |
| knight_bulwark | Bulwark | notable | 2 | 10 | knight_iron_skin | +2 armor toughness, +10% knockback resistance |
| knight_unbreakable | Unbreakable | keystone | 3 | 25 | knight_bulwark, knight_blade_drill | -15% damage taken, but -10% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Oath | start | - | - | - | - |
| zeal | Zeal | small | 1 | 1 | start | +2 Valor |
| aegis | Aegis | notable | 2 | 15 | zeal | +0.1 armor per Valor point |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| knight_stat_start | Knight | start | - | - | - | - |
| knight_stout | Stout | small | 2 | 1 | knight_stat_start | +2 Vitality, +2 Endurance |
| knight_brawn | Brawn | small | 2 | 5 | knight_stout | +3 Strength |

## 12. Anything else

- Later: a "Shield Bash" active ability that costs 10 Resolve.
- Valor should feel like it matters most in long fights - open to suggestions.
