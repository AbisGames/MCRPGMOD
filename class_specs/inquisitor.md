# Class: Inquisitor

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Inquisitor' (p. 91). -->

## 1. Basics

- **Id:** inquisitor
- **Display name:** Inquisitor
- **Description:** A cold hunter of heretics and the corrupt. Scrutinises a target, sees every weakness and breaks their will.
- **Icon item:** skeleton_skull
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 4 |
| Dexterity    | 3 |
| Agility      | 2 |
| Intelligence | 5 |
| Wisdom       | 5 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Scrutiny
- **Description:** The inquisitor's piercing gaze.
- **What each point gives:** +0.5% damage against your scrutinised target
- **Max points:** 100

### Resource bar

- **Name:** Judgment
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** bone white (#ECE6D6)

## 4. Starting kit

- 1 × iron_sword
- 1 × skeleton_skull
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Scrutiny: the mob you look at for 2s becomes scrutinised - it glows, you see its health, and you deal +15% damage to it
- Dreadful Threat: scrutinised mobs within 6 blocks get Weakness I
- Inquisitor's Judgment: illagers, witches and Nether mobs take +15% damage from you

### Special passives

- Scrutiny bonus +0.5% per point
- Per 10 Scrutiny, scrutinise 1s faster

## 6. Weakness

- **What players read:** The Inquisition is feared, not loved.
- **What it actually does:** villager trades cost 15% more and Hero of the Village doesn't apply to you

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | neutral |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | proficient |
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

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Interrogation

- **Shared or class-only:** class-only
- **Trained by:** killing scrutinised mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Scrutiny
- **Tier II bonus:** +1 Scrutiny
- **Tier III bonus:** +10% Judgment regen
- **Tier IV bonus:** +1 Scrutiny
- **Tier V bonus:** +2 Scrutiny

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Cleric

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| inquisitor_start | Inquisitor | start | - | - | - | +3% damage |
| inquisitor_piercing_gaze | Piercing Gaze | small | 1 | 1 | inquisitor_start | +5% damage to scrutinised |
| inquisitor_skull_mask | Skull Mask | small | 1 | 1 | inquisitor_start | immune to Nausea and Blindness |
| inquisitor_heretic_hunter | Heretic Hunter | notable | 2 | 12 | inquisitor_piercing_gaze | +10% damage to illagers and witches |
| inquisitor_master_torturer | Master Torturer | notable | 2 | 15 | inquisitor_skull_mask | scrutinised mobs drop +1 loot |
| inquisitor_witch_finder | Witch-Finder | keystone | 3 | 30 | inquisitor_heretic_hunter, inquisitor_master_torturer | +25% damage to scrutinised, but -15% to everything else |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Scrutiny | start | - | - | - | - |
| cold_eyes | Cold Eyes | small | 1 | 1 | start | +2 Scrutiny |
| dreadful_threat | Dreadful Threat | small | 1 | 5 | start | Weakness II |
| inquisitor_s_judgment | Inquisitor's Judgment | notable | 2 | 15 | cold_eyes | +10% judgment damage |
| expose | Expose | notable | 2 | 20 | dreadful_threat | scrutinised mobs take +10% damage from everyone |
| grand_inquisitor | Grand Inquisitor | keystone | 3 | 35 | inquisitor_s_judgment, expose | scrutinise 2 mobs at once; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| inquisitor_stat_start | Inquisitor | start | - | - | - | - |
| inquisitor_interrogator | Interrogator | small | 2 | 1 | inquisitor_stat_start | +2 Intelligence, +1 Strength |
| inquisitor_unbending | Unbending | small | 2 | 5 | inquisitor_interrogator | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Dreadful Threat** (frighten a scrutinised mob so it flees, 25 Judgment).
- Book talents: Scrutiny, Dreadful Threat, Master Torturer (interrogation - not ported), Inquisitor's Judgment.
