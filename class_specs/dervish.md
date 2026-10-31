# Class: Dervish

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Dervish' (p. 84). -->

## 1. Basics

- **Id:** dervish
- **Display name:** Dervish
- **Description:** A whirling two-weapon fighter who dances across the battlefield, parrying with one blade and striking with the other.
- **Icon item:** golden_sword
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 4 |
| Dexterity    | 7 |
| Agility      | 6 |
| Intelligence | 0 |
| Wisdom       | 0 |
| Endurance    | 3 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Rhythm
- **Description:** The dance of two blades.
- **What each point gives:** +0.5% attack speed while dual-wielding
- **Max points:** 100

### Resource bar

- **Name:** Whirl
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** saffron (#F2A71B)

## 4. Starting kit

- 2 × iron_sword
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Ambidexterity: weapons in your off-hand also swing (off-hand hit every 2nd attack)
- Off-Hand Parry: while holding a weapon in both hands, +2 armor and 10% chance to parry melee hits
- Two Weapon Mastery: hitting the same mob with both hands in a row deals +20% on the second hit

### Special passives

- Parry chance +0.3% per Rhythm
- Per 10 Rhythm, +3% movement speed while dual-wielding

## 6. Weakness

- **What players read:** No hand free for a shield or a bow.
- **What it actually does:** shields count as forbidden and -25% damage with bows and crossbows

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | mastery |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | forbidden |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
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

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Whirling

- **Shared or class-only:** class-only
- **Trained by:** landing both-hand combos
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Rhythm
- **Tier II bonus:** +1 Rhythm
- **Tier III bonus:** +10% Whirl regen
- **Tier IV bonus:** +1 Rhythm
- **Tier V bonus:** +2 Rhythm

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| dervish_start | Dervish | start | - | - | - | +3% attack speed |
| dervish_twin_strike | Twin Strike | small | 1 | 1 | dervish_start | +5% damage while dual-wielding |
| dervish_light_feet | Light Feet | small | 1 | 1 | dervish_start | +5% movement speed |
| dervish_blade_dance | Blade Dance | notable | 2 | 12 | dervish_twin_strike | each hit gives +1% speed for 3s (stacks 10) |
| dervish_deflect | Deflect | notable | 2 | 15 | dervish_light_feet | +5% parry chance |
| dervish_storm_of_steel | Storm of Steel | keystone | 3 | 30 | dervish_blade_dance, dervish_deflect | off-hand swings every attack, but -3 armor |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Ambidexterity | start | - | - | - | - |
| flow | Flow | small | 1 | 1 | start | +2 Rhythm |
| counter | Counter | small | 1 | 5 | start | a successful parry makes your next hit a crit |
| off_hand_parry | Off-Hand Parry | notable | 2 | 15 | flow | +1 armor while dual-wielding |
| two_weapon_mastery | Two Weapon Mastery | notable | 2 | 20 | counter | combo bonus +15% |
| whirling_death | Whirling Death | keystone | 3 | 35 | off_hand_parry, two_weapon_mastery | unlocks Whirl ability; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| dervish_stat_start | Dervish | start | - | - | - | - |
| dervish_graceful | Graceful | small | 2 | 1 | dervish_stat_start | +2 Agility, +1 Dexterity |
| dervish_ambidextrous | Ambidextrous | small | 2 | 5 | dervish_graceful | +2 Dexterity, +1 Strength |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Whirl** (spin, hitting all mobs around you with both weapons, 25 Whirl).
- Book talents: Ambidexterity, Off-Hand Parry, Two Weapon Mastery.
