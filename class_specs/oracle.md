# Class: Oracle

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Oracle' (p. 67). -->

## 1. Basics

- **Id:** oracle
- **Display name:** Oracle
- **Description:** A vessel for spirits and gods. When possessed, the oracle becomes fearless, tough and all-seeing - at a price to the mind.
- **Icon item:** ender_eye
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1.1 |
| Strength     | 2 | x0.9 |
| Dexterity    | 1 | x0.9 |
| Agility      | 2 | x1 |
| Intelligence | 3 | x1.1 |
| Wisdom       | 6 | x1.2 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Communion
- **Description:** How deeply the presence within you can reach.
- **What each point gives:** +0.5 Ecstasy max and +0.2% damage while in Divine Ecstasy
- **Max points:** 100

### Resource bar

- **Name:** Ecstasy
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.4
- **Extra regen per second per special-stat point:** 0.04
- **Starts full when the class is chosen:** no
- **Colour:** violet flame (#9B5CFF)

## 4. Starting kit

- 1 × stone_sword
- 1 × spyglass
- 1 × leather_helmet
- 6 × bread

## 5. Passives

- The Sight: invisible mobs and players within 16 blocks glow for you
- Visions: you see the outline of hostile mobs through walls within 6 blocks while sneaking
- Divine Ecstasy (toggle, drains 3 Ecstasy/s): +6 max health, immune to Weakness, Slowness and fear-type effects, +10% damage

### Special passives

- Divine Ecstasy lasts longer: drain -1% per 2 Communion
- Avatar: per 20 Communion, +1 armor while in Divine Ecstasy

## 6. Weakness

- **What players read:** The entity inside leaves cracks in the mind.
- **What it actually does:** when Divine Ecstasy ends: Nausea for 6s and Slowness I for 4s

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Mending

- Shared skill - full definition in `priest.md`

### Skill: Trance

- **Shared or class-only:** class-only
- **Trained by:** seconds spent in Divine Ecstasy
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Communion
- **Tier II bonus:** +1 Communion
- **Tier III bonus:** +10% Ecstasy regen
- **Tier IV bonus:** +1 Communion
- **Tier V bonus:** +2 Communion

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** right side of the web (Faith quarter), ring 2, leaning toward the Power quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| oracle_start | Oracle | start | - | - | - | +1 max health |
| oracle_vessel | Vessel | small | 1 | 1 | oracle_start | +2 max health |
| oracle_whispers | Whispers | small | 1 | 1 | oracle_start | glow range of The Sight +8 blocks |
| oracle_steady_mind | Steady Mind | notable | 2 | 10 | oracle_vessel | the after-effects of Ecstasy last half as long |
| oracle_prophet | Prophet | notable | 2 | 12 | oracle_whispers | +10% damage to mobs that glow for you |
| oracle_avatar | Living Avatar | keystone | 3 | 25 | oracle_steady_mind, oracle_prophet | Divine Ecstasy bonuses +50%, but you can't leave it early and it drains 50% faster |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Divine Ecstasy | start | - | - | - | - |
| open_vessel | Open Vessel | small | 1 | 1 | start | +2 Communion |
| fearless | Fearless | small | 1 | 5 | start | +15% knockback resistance in Ecstasy |
| commune | Commune with the Gods | notable | 2 | 15 | open_vessel | unlocks Commune ability |
| avatar | Avatar | notable | 2 | 20 | fearless | +1 armor and +10% attack speed in Ecstasy |
| possessed | Fully Possessed | keystone | 3 | 35 | commune, avatar | Ecstasy also gives Regeneration I; after-effects add Blindness 3s |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| oracle_stat_start | Oracle | start | - | - | - | - |
| oracle_mystic | Mystic | small | 2 | 1 | oracle_stat_start | +2 Wisdom, +1 Luck |
| oracle_hardy_host | Hardy Host | small | 2 | 5 | oracle_mystic | +2 Vitality, +1 Strength |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Divine Ecstasy is written as a passive toggle; if toggles aren't possible yet, treat it as a future active ability.
- Active ability for later: **Commune** (ask the server a hint: e.g. points you toward the nearest structure, 30 Ecstasy).
- The book's Insanity mechanic is approximated by the post-Ecstasy debuffs.
- **Co-design question:** Communion + Ecstasy (fills slowly, spent on trances). Good, or would you like an actual 'Insanity' meter for Faith classes?
