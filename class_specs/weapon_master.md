# Class: Weapon Master

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Weapon Master' (p. 98). -->

## 1. Basics

- **Id:** weapon_master
- **Display name:** Weapon Master
- **Description:** A warrior who has devoted their life to one favored weapon - and is legendary with it.
- **Icon item:** diamond_sword
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 7 |
| Dexterity    | 6 |
| Agility      | 2 |
| Intelligence | 0 |
| Wisdom       | 1 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Mastery
- **Description:** Devotion to your favored weapon.
- **What each point gives:** +0.6% damage with your favored weapon
- **Max points:** 100

### Resource bar

- **Name:** Focus
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** polished steel (#B8C4CF)

## 4. Starting kit

- 1 × iron_sword
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Favored Weapon: when you sleep, the weapon type in your main hand becomes your favored weapon type (sword, axe, spear, mace, trident, bow or crossbow)
- With your favored weapon: +2 armor, +20% damage, +10% attack speed
- Weapon Specialization: favored weapon critical hits deal +20%

### Special passives

- Favored damage +0.6% per Mastery
- Per 10 Mastery, favored weapon loses durability 5% slower

## 6. Weakness

- **What players read:** Devotion to one weapon means neglect of the rest.
- **What it actually does:** -20% damage with every weapon type that isn't your favored one

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | proficient |
| Bows | proficient |
| Crossbows | proficient |
| Shields | neutral |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
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

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Devotion

- **Shared or class-only:** class-only
- **Trained by:** killing mobs with your favored weapon
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Mastery
- **Tier II bonus:** +1 Mastery
- **Tier III bonus:** +10% Focus regen
- **Tier IV bonus:** +1 Mastery
- **Tier V bonus:** +2 Mastery

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| weapon_master_start | Weapon Master | start | - | - | - | +3% damage |
| weapon_master_drilled | Drilled | small | 1 | 1 | weapon_master_start | +5% favored damage |
| weapon_master_guard | Guard | small | 1 | 1 | weapon_master_start | +1 armor with favored weapon |
| weapon_master_flow_state | Flow State | notable | 2 | 12 | weapon_master_drilled | +5% attack speed with favored weapon |
| weapon_master_signature_move | Signature Move | notable | 2 | 15 | weapon_master_guard | every 5th hit with favored weapon is a crit |
| weapon_master_one_true_weapon | One True Weapon | keystone | 3 | 30 | weapon_master_flow_state, weapon_master_signature_move | +30% favored damage, but other weapons are forbidden-tier |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Favored Weapon | start | - | - | - | - |
| practice | Practice | small | 1 | 1 | start | +2 Mastery |
| swift_change | Swift Change | small | 1 | 5 | start | you can change favored weapon once per day without sleeping |
| weapon_specialization | Weapon Specialization | notable | 2 | 15 | practice | +10% favored crit damage |
| masterstroke | Masterstroke | notable | 2 | 20 | swift_change | unlocks Masterstroke ability |
| living_legend | Living Legend | keystone | 3 | 35 | weapon_specialization, masterstroke | favored weapon can't break; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| weapon_master_stat_start | Weapon Master | start | - | - | - | - |
| weapon_master_trained_body | Trained Body | small | 2 | 1 | weapon_master_stat_start | +2 Strength, +1 Dexterity |
| weapon_master_perfect_form | Perfect Form | small | 2 | 5 | weapon_master_trained_body | +2 Dexterity, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Masterstroke** (a guaranteed max-damage critical with the favored weapon, 30 Focus).
- Book talents: Favored Weapon, Weapon Specialization.
