# Class: Pyromancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Pyromancer' (p. 94). -->

## 1. Basics

- **Id:** pyromancer
- **Display name:** Pyromancer
- **Description:** Bound to a flame genie, the pyromancer hurls fire, shrugs off flames and leaves burning enemies behind.
- **Icon item:** blaze_powder
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.75 |
| Strength     | 1 | x0.85 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x1 |
| Intelligence | 8 | x1.25 |
| Wisdom       | 6 | x1.15 |
| Endurance    | 2 | x0.85 |
| Luck         | 2 | x1.15 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Inferno
- **Description:** The heat of your genie's bond.
- **What each point gives:** +1% fire damage you deal
- **Max points:** 100

### Resource bar

- **Name:** Embers
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** flame orange (#FF7A1A)

## 4. Starting kit

- 4 × fire_charge
- 1 × flint_and_steel
- 1 × leather_chestplate
- 6 × cooked_porkchop

## 5. Passives

- Flame Blessing: immune to fire and lava damage while Embers is above 10% (each second in lava drains 5)
- Immolating Flames: 20% chance on hit to set mobs on fire; fire charges you throw deal +50% damage
- Fire you start burns 50% longer

### Special passives

- Ignite chance +0.3% per Inferno
- Per 10 Inferno, -10% Embers drain in lava

## 6. Weakness

- **What players read:** Fire fears water.
- **What it actually does:** -25% damage and no Embers regen while wet (rain or water); +25% damage taken from drowned and guardians

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | proficient |
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

### Skill: Firestarting

- **Shared or class-only:** class-only
- **Trained by:** setting mobs on fire and killing burning mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Inferno
- **Tier II bonus:** +1 Inferno
- **Tier III bonus:** +10% Embers regen
- **Tier IV bonus:** +1 Inferno
- **Tier V bonus:** +2 Inferno

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Sorcerer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| pyromancer_start | Pyromancer | start | - | - | - | +5% fire damage |
| pyromancer_kindling | Kindling | small | 1 | 1 | pyromancer_start | +5% ignite chance |
| pyromancer_heat_shimmer | Heat Shimmer | small | 1 | 1 | pyromancer_start | mobs that hit you have 15% chance to catch fire |
| pyromancer_wildfire | Wildfire | notable | 2 | 12 | pyromancer_kindling | burning mobs spread fire to adjacent mobs |
| pyromancer_ash_walker | Ash Walker | notable | 2 | 15 | pyromancer_heat_shimmer | +20% movement speed on Nether blocks |
| pyromancer_living_flame | Living Flame | keystone | 3 | 30 | pyromancer_wildfire, pyromancer_ash_walker | +30% fire damage, but you take 25% more damage from water, snow and ice mobs |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Flame Blessing | start | - | - | - | - |
| fan_the_flames | Fan the Flames | small | 1 | 1 | start | +2 Inferno |
| hotter | Hotter | small | 1 | 5 | start | burning mobs take +1 damage per second |
| immolating_flames | Immolating Flames | notable | 2 | 15 | fan_the_flames | ignite chance +10% |
| firebolt | Firebolt | notable | 2 | 20 | hotter | unlocks Firebolt ability |
| phoenix | Phoenix | keystone | 3 | 35 | immolating_flames, firebolt | once per day, when you'd die, explode in flame and heal 6 HP; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| pyromancer_stat_start | Pyromancer | start | - | - | - | - |
| pyromancer_burning_mind | Burning Mind | small | 2 | 1 | pyromancer_stat_start | +2 Intelligence, +1 Agility |
| pyromancer_hot_blood | Hot Blood | small | 2 | 5 | pyromancer_burning_mind | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Firebolt** (shoot a fireball, 15 Embers), **Flame Wave** (cone of fire, 35 Embers).
- Book talents: Flame Blessing, Immolating Flames. Tradition: Fire.
