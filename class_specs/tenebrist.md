# Class: Tenebrist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Tenebrist' (p. 96). -->

## 1. Basics

- **Id:** tenebrist
- **Display name:** Tenebrist
- **Description:** An embracer of Shadow magic, pale and thin, who wears darkness like a cloak even in daylight.
- **Icon item:** black_dye
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.9 |
| Strength     | 0 | x0.75 |
| Dexterity    | 3 | x1 |
| Agility      | 4 | x1.1 |
| Intelligence | 7 | x1.25 |
| Wisdom       | 6 | x1.1 |
| Endurance    | 1 | x0.9 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Umbra
- **Description:** The darkness infecting you.
- **What each point gives:** +0.5% damage and +0.3% dodge chance in low light
- **Max points:** 100

### Resource bar

- **Name:** Shade
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** shadow violet (#3A2B55)

## 4. Starting kit

- 1 × stone_sword
- 1 × black_dye
- 1 × potion of invisibility
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Cloak of Shadows: you count as being in darkness at all times - mobs detect you 30% later
- Shadow Form: in light level 7 or lower, you're invisible while sneaking and +15% damage
- Permanent Night Vision

### Special passives

- Detection reduction +0.3% per Umbra
- Per 10 Umbra, +5% dodge in darkness

## 6. Weakness

- **What players read:** The shadow eats at you in true light.
- **What it actually does:** -10% max health in daylight outdoors, and +20% fire damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
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

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Darkweaving

- **Shared or class-only:** class-only
- **Trained by:** killing mobs in light level 7 or lower
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Umbra
- **Tier II bonus:** +1 Umbra
- **Tier III bonus:** +10% Shade regen
- **Tier IV bonus:** +1 Umbra
- **Tier V bonus:** +2 Umbra

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Warlock

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| tenebrist_start | Tenebrist | start | - | - | - | -5% detection range |
| tenebrist_gloom | Gloom | small | 1 | 1 | tenebrist_start | +5% damage in darkness |
| tenebrist_dusk_step | Dusk Step | small | 1 | 1 | tenebrist_start | +10% sneaking speed |
| tenebrist_shadow_bind | Shadow Bind | notable | 2 | 12 | tenebrist_gloom | your hits in darkness give Slowness I |
| tenebrist_pallid | Pallid | notable | 2 | 15 | tenebrist_dusk_step | -15% wither and poison damage |
| tenebrist_child_of_night | Child of Night | keystone | 3 | 30 | tenebrist_shadow_bind, tenebrist_pallid | +30% damage in darkness, but -20% damage in daylight |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Cloak of Shadows | start | - | - | - | - |
| darkness_within | Darkness Within | small | 1 | 1 | start | +2 Umbra |
| snuff | Snuff | small | 1 | 5 | start | torches within 4 blocks flicker - mobs near you can't see you |
| shadow_form | Shadow Form | notable | 2 | 15 | darkness_within | unlocks Shadow Form ability |
| shadow_step | Shadow Step | notable | 2 | 20 | snuff | unlocks Shadow Step ability |
| void_touched | Void-Touched | keystone | 3 | 35 | shadow_form, shadow_step | Shadow Form works in any light; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| tenebrist_stat_start | Tenebrist | start | - | - | - | - |
| tenebrist_shadowed_mind | Shadowed Mind | small | 2 | 1 | tenebrist_stat_start | +2 Intelligence, +1 Agility |
| tenebrist_dim | Dim | small | 2 | 5 | tenebrist_shadowed_mind | +2 Agility, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Shadow Form** (become shadowy: attackers have reduced accuracy, pass through mobs, invisible in darkness, 40 Shade), **Shadow Step** (teleport between shadows, 25 Shade).
- Book talents: Cloak of Shadows, Shadow Form. Tradition: Shadow.
