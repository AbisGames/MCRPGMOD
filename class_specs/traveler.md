# Class: Traveler

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Traveler' (p. 97). -->

## 1. Basics

- **Id:** traveler
- **Display name:** Traveler
- **Description:** A master of Teleportation who blinks across the battlefield and arrives with a thunderclap.
- **Icon item:** ender_pearl
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 0 | x0.8 |
| Dexterity    | 3 | x0.9 |
| Agility      | 5 | x1.1 |
| Intelligence | 7 | x1.2 |
| Wisdom       | 5 | x1.1 |
| Endurance    | 1 | x0.9 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Displacement
- **Description:** How far reality will bend for you.
- **What each point gives:** +1% ender pearl range and +0.3% movement speed
- **Max points:** 100

### Resource bar

- **Name:** Waypoint
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** ender teal (#1FB39A)

## 4. Starting kit

- 8 × ender_pearl
- 1 × leather_boots
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Hasty Escape: when you take a hit below half health, 25% chance to blink 6 blocks away (20s cooldown)
- Ender pearls deal no damage to you and have 30% shorter cooldown
- Spatial Disturbance: after teleporting, mobs near your arrival point get Slowness I for 2s

### Special passives

- Blink distance +0.1 per Displacement
- Per 10 Displacement, Hasty Escape chance +5%

## 6. Weakness

- **What players read:** Teleporting scatters your senses.
- **What it actually does:** after any teleport (pearl or blink), Nausea for 2s

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 2 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Wayfaring

- **Shared or class-only:** class-only
- **Trained by:** teleporting with pearls and blinks
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Displacement
- **Tier II bonus:** +1 Displacement
- **Tier III bonus:** +10% Waypoint regen
- **Tier IV bonus:** +1 Displacement
- **Tier V bonus:** +2 Displacement

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Scout

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| traveler_start | Traveler | start | - | - | - | +5% movement speed |
| traveler_light_traveller | Light Traveller | small | 1 | 1 | traveler_start | pearl cooldown -10% |
| traveler_fleetfoot | Fleetfoot | small | 1 | 1 | traveler_start | +5% movement speed |
| traveler_far_traveler | Far Traveler | notable | 2 | 12 | traveler_light_traveller | +30% pearl range |
| traveler_steady_arrival | Steady Arrival | notable | 2 | 15 | traveler_fleetfoot | no Nausea after teleporting |
| traveler_planeswalker | Planeswalker | keystone | 3 | 30 | traveler_far_traveler, traveler_steady_arrival | Hasty Escape always triggers, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Hasty Escape | start | - | - | - | - |
| fold_space | Fold Space | small | 1 | 1 | start | +2 Displacement |
| thunderclap | Thunderclap | small | 1 | 5 | start | arrivals knock mobs back |
| blink | Blink | notable | 2 | 15 | fold_space | unlocks Blink ability |
| recall | Recall | notable | 2 | 20 | thunderclap | unlocks Recall ability |
| gatekeeper | Gatekeeper | keystone | 3 | 35 | blink, recall | your pearls can carry a nearby player with you; +50% pearl cooldown |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| traveler_stat_start | Traveler | start | - | - | - | - |
| traveler_restless | Restless | small | 2 | 1 | traveler_stat_start | +2 Agility, +1 Luck |
| traveler_spatial_mind | Spatial Mind | small | 2 | 5 | traveler_restless | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Blink** (teleport 8 blocks where you look, 20 Waypoint), **Recall** (return to a marked spot, 60 Waypoint).
- Book talents: Hasty Escape, Far Traveler, Spatial Disturbance. Tradition: Teleportation.
