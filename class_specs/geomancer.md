# Class: Geomancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Geomancer' (p. 88). -->

## 1. Basics

- **Id:** geomancer
- **Display name:** Geomancer
- **Description:** Bonded to an earth genie, the geomancer's skin turns to stone - they absorb blows and walk through rock.
- **Icon item:** cobblestone
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 3 |
| Dexterity    | 1 |
| Agility      | 0 |
| Intelligence | 6 |
| Wisdom       | 6 |
| Endurance    | 3 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Bedrock
- **Description:** How much of the stone has become part of you.
- **What each point gives:** +0.1 armor and +0.5% mining speed
- **Max points:** 100

### Resource bar

- **Name:** Earthen Defense
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** slate (#6F6A5E)

## 4. Starting kit

- 1 × stone_pickaxe
- 16 × cobblestone
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Earthen Defense: your resource bar absorbs damage first while you stand on stone, dirt, sand or gravel (1 point per damage)
- Earth Walker: no fall damage when landing on stone or dirt; +20% mining speed
- -30% suffocation damage and immune to falling-block damage

### Special passives

- Absorption efficiency +0.5% per Bedrock
- Per 10 Bedrock, +1 armor while standing on stone

## 6. Weakness

- **What players read:** Stone sinks and cracks in the cold and the water.
- **What it actually does:** -25% movement speed while swimming and takes 25% more freezing damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | proficient |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
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

### Skill: Mining

- **Shared or class-only:** shared
- **Trained by:** mining ores and stone
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +3% mining speed
- **Tier II bonus:** +3% mining speed
- **Tier III bonus:** +5% chance of extra ore drop
- **Tier IV bonus:** +3% mining speed
- **Tier V bonus:** +10% chance of extra ore drop

### Skill: Stoneheart

- **Shared or class-only:** class-only
- **Trained by:** absorbing damage with Earthen Defense
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Bedrock
- **Tier II bonus:** +1 Bedrock
- **Tier III bonus:** +10% Earthen Defense regen
- **Tier IV bonus:** +1 Bedrock
- **Tier V bonus:** +2 Bedrock

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Druid

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| geomancer_start | Geomancer | start | - | - | - | +1 armor |
| geomancer_granite_skin | Granite Skin | small | 1 | 1 | geomancer_start | +1 armor |
| geomancer_prospector | Prospector | small | 1 | 1 | geomancer_start | +10% mining speed |
| geomancer_bulwark_of_stone | Bulwark of Stone | notable | 2 | 12 | geomancer_granite_skin | +15% knockback resistance on stone |
| geomancer_ore_sense | Ore Sense | notable | 2 | 15 | geomancer_prospector | ores within 6 blocks glow while you sneak |
| geomancer_living_mountain | Living Mountain | keystone | 3 | 30 | geomancer_bulwark_of_stone, geomancer_ore_sense | +4 armor and +20% knockback resistance, but -15% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Earthen Defense | start | - | - | - | - |
| rooted | Rooted | small | 1 | 1 | start | +2 Bedrock |
| stone_fist | Stone Fist | small | 1 | 5 | start | unarmed and pickaxe hits +20% damage |
| earth_walker | Earth Walker | notable | 2 | 15 | rooted | unlocks Earth Glide ability |
| quake | Quake | notable | 2 | 20 | stone_fist | unlocks Quake ability |
| avatar_of_earth | Avatar of Earth | keystone | 3 | 35 | earth_walker, quake | Earthen Defense works anywhere; -20% movement speed in water and air |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| geomancer_stat_start | Geomancer | start | - | - | - | - |
| geomancer_granite | Granite | small | 2 | 1 | geomancer_stat_start | +2 Vitality, +1 Strength |
| geomancer_grounded | Grounded | small | 2 | 5 | geomancer_granite | +2 Wisdom, +1 Intelligence |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Earth Glide** (walk through stone for 3s, 40 Earthen Defense), **Quake** (stun nearby mobs, 30 Earthen Defense).
- Book talents: Earthen Defense, Earth Walker. Tradition: Earth.
