# Class: Ranger

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Ranger' (p. 69). -->

## 1. Basics

- **Id:** ranger
- **Display name:** Ranger
- **Description:** A wilderness hunter who marks prey, never gets lost and never gets surprised.
- **Icon item:** bow
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 2 |
| Dexterity    | 6 |
| Agility      | 3 |
| Intelligence | 1 |
| Wisdom       | 2 |
| Endurance    | 2 |
| Luck         | 0 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Instinct
- **Description:** The hunter's sixth sense.
- **What each point gives:** +0.5% damage against your marked prey
- **Max points:** 100

### Resource bar

- **Name:** Quarry
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** forest green (#4C7A2E)

## 4. Starting kit

- 1 × bow
- 32 × arrow
- 1 × stone_axe
- 1 × leather_chestplate
- 1 × compass
- 6 × cooked_porkchop

## 5. Passives

- Hunt Prey: the last mob you hit with an arrow becomes your prey for 60s; you deal +15% damage to it and it glows for you
- Alertness: you can't be backstabbed (no bonus damage from mobs hitting you from behind) and hostile mobs within 8 blocks make a sound cue
- Expert Guide: other players within 16 blocks get +5% movement speed while travelling outdoors
- Natural Lore: +20% damage against animals, spiders and beast-like mobs (wolves, hoglins, ravagers)

### Special passives

- +0.5% Hunt Prey duration per Instinct point
- Master Hunter: per 10 Instinct, prey can't become invisible to you

## 6. Weakness

- **What players read:** Rangers belong under open sky.
- **What it actually does:** -15% damage and -20% Quarry regen underground (no sky access below Y=40)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | mastery |
| Crossbows | proficient |
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
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Beast Bond

- Shared skill - full definition in `druid.md`

### Skill: Survival

- **Shared or class-only:** shared
- **Trained by:** killing mobs outdoors at night
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** -5% hunger drain
- **Tier II bonus:** +1 max health
- **Tier III bonus:** -5% hunger drain
- **Tier IV bonus:** +1 max health
- **Tier V bonus:** eating cooked meat gives Regeneration I for 3s

### Skill: Tracking

- **Shared or class-only:** class-only
- **Trained by:** killing your marked prey
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Instinct
- **Tier II bonus:** +1 Instinct
- **Tier III bonus:** +10% Quarry regen
- **Tier IV bonus:** +1 Instinct
- **Tier V bonus:** +2 Instinct

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** left side of the web (War quarter), ring 2, leaning toward the Trickery quarter (next to the Druid)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| ranger_start | Ranger | start | - | - | - | +5% bow damage |
| ranger_steady | Steady Aim | small | 1 | 1 | ranger_start | +6% bow damage |
| ranger_woodcraft | Woodcraft | small | 1 | 1 | ranger_start | +3 max health outdoors |
| ranger_volley | Volley | notable | 2 | 10 | ranger_steady | 10% chance a bow shot fires a second arrow |
| ranger_stalker | Stalker | notable | 2 | 12 | ranger_woodcraft | +10% movement speed toward your prey |
| ranger_apex | Apex Predator | keystone | 3 | 25 | ranger_volley, ranger_stalker | +30% damage to prey, but -15% damage to everything else |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Hunt Prey | start | - | - | - | - |
| alertness | Alertness | small | 1 | 1 | start | +2 Instinct |
| natural_lore | Natural Lore | small | 1 | 5 | start | +10% damage to animals and beasts |
| expert_tracker | Expert Tracker | notable | 2 | 15 | alertness | your prey glows through walls up to 32 blocks |
| relentless | Relentless Pursuit | notable | 2 | 20 | natural_lore | when prey flees, you get Speed I for 3s |
| master_hunter | Master Hunter | keystone | 3 | 35 | expert_tracker, relentless | you can mark prey in melee too and prey bonus +10%; Hunt Prey ends instantly if you hit another mob |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| ranger_stat_start | Ranger | start | - | - | - | - |
| ranger_eagle_eye | Eagle Eye | small | 2 | 1 | ranger_stat_start | +2 Dexterity, +1 Agility |
| ranger_rugged | Rugged | small | 2 | 5 | ranger_eagle_eye | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active ability for later: **Mark Prey** at range (15 Quarry), **Find the Way** (points toward your bed/spawn, 10 Quarry).
- Book talents: Alertness, Hunt Prey, Natural Lore, Expert Guide, Expert Tracker, Master Hunter, Relentless Pursuit.
- **Co-design question:** Instinct + Quarry. Should prey be marked automatically (current) or only with an ability?
