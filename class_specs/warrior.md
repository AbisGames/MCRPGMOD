# Class: Warrior

<!-- Source: Shadow of the Demon Lord core rulebook, novice path 'Warrior' (p. 58). -->

## 1. Basics

- **Id:** warrior
- **Display name:** Warrior
- **Description:** Trained in every weapon and toughened by countless fights. The best pure fighter of the novice classes.
- **Icon item:** iron_sword
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.2 |
| Strength     | 4 | x1.45 |
| Dexterity    | 2 | x1 |
| Agility      | 1 | x1 |
| Intelligence | 0 | x0.8 |
| Wisdom       | 0 | x0.55 |
| Endurance    | 3 | x1.2 |
| Luck         | 0 | x0.8 |
| Special stat (section 3) | 5 | |

## 3. Special stat

- **Name:** Grit
- **Description:** Stubborn battlefield toughness. Feeds the Warrior's weapon damage and Stamina.
- **What each point gives:** +0.5% weapon damage
- **Max points:** 100

### Resource bar

- **Name:** Stamina
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** orange (#E8892B)

## 4. Starting kit

- 1 × stone_sword
- 1 × shield
- 1 × leather_chestplate
- 8 × bread

## 5. Passives

- +1 attack damage with any weapon (Weapon Training)
- +5% weapon damage, +1% more every 10 class levels (Combat Prowess)
- +5% knockback resistance

### Special passives

- Critical hits deal +1% extra damage per 2 Grit (Forceful Strike)
- +0.1 max health per Grit point

## 6. Weakness

- **What players read:** Warriors trust steel, not sorcery. Magic slips past their guard.
- **What it actually does:** takes 15% more magic damage (potions, evoker fangs, guardian beams, Wither effect)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 0 |
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
- **Tier III bonus:** +5% sword attack speed
- **Tier IV bonus:** +3% sword damage
- **Tier V bonus:** +6% sword damage

### Skill: Axemanship

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with axes
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% axe damage
- **Tier II bonus:** +3% axe damage
- **Tier III bonus:** +10% shield-disable time when you break a shield
- **Tier IV bonus:** +3% axe damage
- **Tier V bonus:** +6% axe damage

### Skill: Archery

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with arrows from a bow
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% bow damage
- **Tier II bonus:** +5% draw speed
- **Tier III bonus:** +3% bow damage
- **Tier IV bonus:** +5% draw speed
- **Tier V bonus:** +6% bow damage

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

### Skill: Battle-Hardened

- **Shared or class-only:** class-only
- **Trained by:** killing hostile mobs while below half health
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Grit
- **Tier II bonus:** +1 Grit
- **Tier III bonus:** +2 max health
- **Tier IV bonus:** +1 Grit
- **Tier V bonus:** +2 Grit

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** left side of the web (the War quarter), ring 1

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| warrior_start | Warrior | start | - | - | - | +1 attack damage |
| warrior_drill | Weapon Drill | small | 1 | 1 | warrior_start | +4% weapon damage |
| warrior_hardy | Hardy | small | 1 | 1 | warrior_start | +2 max health |
| warrior_footwork | Footwork | small | 1 | 5 | warrior_drill | +4% attack speed |
| warrior_veteran | Veteran | notable | 2 | 10 | warrior_hardy, warrior_footwork | +1 armor, +8% weapon damage |
| warrior_combat_mastery | Combat Mastery | keystone | 3 | 25 | warrior_veteran | your fully-charged melee hits also strike one other mob within 2 blocks for 50% damage, but -10% damage with bows and crossbows |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Catch Your Breath | start | - | - | - | - |
| second_wind | Second Wind | small | 1 | 1 | start | +2 Grit |
| forceful | Forceful Strike | small | 1 | 5 | start | critical hits deal +10% damage |
| expertise | Combat Expertise | notable | 2 | 15 | second_wind, forceful | every 4th hit on the same mob deals +25% damage |
| grit_heart | Iron Lungs | notable | 2 | 20 | second_wind | +50% Stamina regen while below half health |
| grit_mastery | Grit | keystone | 3 | 35 | expertise, grit_heart | Catch Your Breath can be used twice per rest; -10% max Stamina |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| warrior_stat_start | Warrior | start | - | - | - | - |
| warrior_tough | Tough | small | 2 | 1 | warrior_stat_start | +2 Vitality, +1 Endurance |
| warrior_strong | Strong Arm | small | 2 | 5 | warrior_stat_start | +2 Strength, +1 Dexterity |

## 12. Anything else

- **Power tier: Novice.** Book levels 1, 2, 5, 8. Baseline power: 14 stat points, 5 special points, 3 trinket slots. Selectable from the start.
- Book talents kept as active abilities for later: **Catch Your Breath** (heal ~25% max health, once per sleep/rest, 15 Stamina).
- Book talent 'Combat Expertise / Mastery' (extra attack or extra damage) is approximated by the special-tree nodes.
- Firearms flavour from the book (pistols) waits for the planned gun system.
- **Co-design question:** Grit + Stamina are my proposal. Keep, or rename/rework?
