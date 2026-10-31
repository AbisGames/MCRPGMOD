# Class: Stormbringer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Stormbringer' (p. 95). -->

## 1. Basics

- **Id:** stormbringer
- **Display name:** Stormbringer
- **Description:** A master of Storm magic who hurls lightning and rides the thunder.
- **Icon item:** lightning_rod
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 1 |
| Dexterity    | 2 |
| Agility      | 3 |
| Intelligence | 7 |
| Wisdom       | 7 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Tempest
- **Description:** The storm you carry inside.
- **What each point gives:** +1% lightning damage and +0.3% movement speed in rain
- **Max points:** 100

### Resource bar

- **Name:** Charge
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** lightning yellow (#FFF24A)

## 4. Starting kit

- 1 × lightning_rod
- 1 × trident
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Powered by Storm: immune to lightning; being struck refills Charge and gives +6 max health and Strength I for 60s
- Ride the Lightning: in rain or thunder, +20% speed and +15% damage
- Your hits have a 5% chance in thunderstorms to call down lightning on the target

### Special passives

- Lightning chance +0.2% per Tempest
- Per 10 Tempest, Ride the Lightning works in clear weather at half strength

## 6. Weakness

- **What players read:** The storm doesn't follow you underground.
- **What it actually does:** -15% damage and no Charge regen underground (no sky access)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | proficient |
| Bows | neutral |
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

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Stormcalling

- **Shared or class-only:** class-only
- **Trained by:** fighting in rain and thunderstorms
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Tempest
- **Tier II bonus:** +1 Tempest
- **Tier III bonus:** +10% Charge regen
- **Tier IV bonus:** +1 Tempest
- **Tier V bonus:** +2 Tempest

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Sorcerer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| stormbringer_start | Stormbringer | start | - | - | - | +5% damage in rain |
| stormbringer_static | Static | small | 1 | 1 | stormbringer_start | your hits have 10% chance to chain 2 damage to a nearby mob |
| stormbringer_thunderhead | Thunderhead | small | 1 | 1 | stormbringer_start | +10% speed in rain |
| stormbringer_conductor | Conductor | notable | 2 | 12 | stormbringer_static | lightning chance doubled while holding a trident |
| stormbringer_eye_of_the_storm | Eye of the Storm | notable | 2 | 15 | stormbringer_thunderhead | -20% damage taken in thunderstorms |
| stormbringer_storm_avatar | Storm Avatar | keystone | 3 | 30 | stormbringer_conductor, stormbringer_eye_of_the_storm | your lightning chance works in any rain, but you take 25% more fire damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Powered by Storm | start | - | - | - | - |
| crackle | Crackle | small | 1 | 1 | start | +2 Tempest |
| ride_the_lightning | Ride the Lightning | small | 1 | 5 | start | unlocks Lightning Dash ability |
| chain_lightning | Chain Lightning | notable | 2 | 15 | crackle | lightning arcs to 2 more mobs |
| call_the_storm | Call the Storm | notable | 2 | 20 | ride_the_lightning | unlocks Summon Storm ability |
| stormlord | Stormlord | keystone | 3 | 35 | chain_lightning, call_the_storm | Powered by Storm lasts 3 minutes; you can't wear iron or netherite armor |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| stormbringer_stat_start | Stormbringer | start | - | - | - | - |
| stormbringer_electric_mind | Electric Mind | small | 2 | 1 | stormbringer_stat_start | +2 Intelligence, +1 Agility |
| stormbringer_wild_will | Wild Will | small | 2 | 5 | stormbringer_electric_mind | +2 Wisdom, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Lightning Bolt** (call lightning on a target, 30 Charge), **Lightning Dash** (fly up to 10 blocks in a flash, 20 Charge), **Summon Storm** (start rain for 2 minutes, 100 Charge).
- Book talents: Ride the Lightning, Powered by Storm. Tradition: Storm.
