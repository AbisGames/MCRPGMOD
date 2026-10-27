# Class: Druid

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Druid' (p. 65). -->

## 1. Basics

- **Id:** druid
- **Display name:** Druid
- **Description:** A guardian of the Old Faith who moves through the wilds untouched and speaks for beasts and green things.
- **Icon item:** oak_sapling
- **Selectable by players:** yes
- **Base health:** 26

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 2 |
| Dexterity    | 1 |
| Agility      | 3 |
| Intelligence | 2 |
| Wisdom       | 6 |
| Endurance    | 2 |
| Luck         | 0 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Wildness
- **Description:** Your bond with the untamed land.
- **What each point gives:** +0.5% movement speed in natural biomes (forest, plains, jungle, taiga, swamp...)
- **Max points:** 100

### Resource bar

- **Name:** Verdance
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** leaf green (#2FA84F)

## 4. Starting kit

- 1 × wooden_spear (or stick if spears don't exist yet)
- 1 × leather_chestplate
- 4 × bone_meal
- 8 × sweet_berries
- 4 × bread

## 5. Passives

- Druid Mysteries: no slowdown from powder snow, soul sand, cobwebs or honey; berry bushes and cacti don't hurt you
- You never trample farmland
- Rotten flesh, spider eyes and poisonous potatoes give no bad effects
- +15% bone meal effect

### Special passives

- +1% regeneration per 2 Wildness while standing on grass, dirt, sand or leaves
- Passive animals within 16 blocks never flee from you; per 20 Wildness, +1 heart to animals you tame

## 6. Weakness

- **What players read:** Druids shun forged iron and the stink of cities.
- **What it actually does:** -20% damage with iron, diamond and netherite weapons, and -25% Verdance regen inside villages

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | neutral |
| Spears | mastery |
| Maces | neutral |
| Tridents | proficient |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | neutral |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | wooden and stone weapons of any kind: at least proficient |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Herbalism

- Shared skill - full definition in `witch.md`

### Skill: Spear Training

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with spears (or tridents in melee if spears aren't in the game)
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% spear damage
- **Tier II bonus:** +0.25 reach
- **Tier III bonus:** +3% spear damage
- **Tier IV bonus:** +5% attack speed
- **Tier V bonus:** +6% spear damage

### Skill: Beast Bond

- **Shared or class-only:** shared
- **Trained by:** taming and breeding animals
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** tamed animals +2 max health
- **Tier II bonus:** +10% breeding speed
- **Tier III bonus:** tamed animals +5% damage
- **Tier IV bonus:** tamed animals +2 max health
- **Tier V bonus:** tamed animals +10% damage

### Skill: Old Ways

- **Shared or class-only:** class-only
- **Trained by:** planting saplings and growing trees with bone meal
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Wildness
- **Tier II bonus:** +1 Wildness
- **Tier III bonus:** +10% Verdance regen
- **Tier IV bonus:** +1 Wildness
- **Tier V bonus:** +2 Wildness

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** right side of the web (Faith quarter), ring 2, leaning toward the Trickery quarter (next to the Ranger)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| druid_start | Druid | start | - | - | - | +3% movement speed |
| druid_bark | Barkskin | small | 1 | 1 | druid_start | +1 armor |
| druid_green | Green Thumb | small | 1 | 1 | druid_start | +10% chance of double crop drops |
| druid_wild_heart | Wild Heart | notable | 2 | 10 | druid_bark | +2 max health in natural biomes |
| druid_herd | Herd Warden | notable | 2 | 12 | druid_green | tamed animals near you take -15% damage |
| druid_circle | Stone Circle | keystone | 3 | 25 | druid_wild_heart, druid_herd | +30% regeneration outdoors, but -20% max health underground (no sky) |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Druid Mysteries | start | - | - | - | - |
| weather_eye | Weather Eye | small | 1 | 1 | start | +2 Wildness; you get a warning message 30s before rain/thunder starts |
| trackless | Trackless | small | 1 | 5 | start | your steps never trigger sculk sensors or pressure plates |
| tree_walker | Tree Walker | notable | 2 | 15 | weather_eye | unlocks Tree Walk ability |
| resist_elements | Resist Elements | notable | 2 | 20 | trackless | -25% fire, freezing and lightning damage |
| grand_druid | Grand Druid | keystone | 3 | 35 | tree_walker, resist_elements | Verdance regen doubles in forests; it stops completely in the Nether and the End |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| druid_stat_start | Druid | start | - | - | - | - |
| druid_earthbound | Earthbound | small | 2 | 1 | druid_stat_start | +2 Wisdom, +1 Vitality |
| druid_fleet | Fleet as the Deer | small | 2 | 5 | druid_earthbound | +2 Agility, +1 Endurance |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active ability for later: **Tree Walk** (step into a log and come out of another log within 24 blocks, 10 Verdance).
- Book talents: Druid Mysteries, Tree Walker, Resist Elements.
- Minecraft 1.21.11 added spears; if your version doesn't have them, map 'Spears' to tridents/sticks.
- **Decided:** Wildness + Verdance. The 'no metal' weakness stays.
