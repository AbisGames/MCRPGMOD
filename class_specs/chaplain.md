# Class: Chaplain

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Chaplain' (p. 82). -->

## 1. Basics

- **Id:** chaplain
- **Display name:** Chaplain
- **Description:** A battlefield preacher whose chants steel allies against fear and spur their blades.
- **Icon item:** book
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 4 |
| Dexterity    | 1 |
| Agility      | 1 |
| Intelligence | 2 |
| Wisdom       | 7 |
| Endurance    | 5 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Sermon
- **Description:** The power of your voice to stir the faithful.
- **What each point gives:** +0.5% Battle Chant damage bonus
- **Max points:** 100

### Resource bar

- **Name:** Chant
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** candle amber (#E7A23B)

## 4. Starting kit

- 1 × book
- 1 × iron_shovel
- 1 × shield
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Battle Chant: players within 10 blocks (including you) get +10% damage and immunity to Darkness, Nausea and Blindness
- Succor: crouching next to a downed-ish player (below 4 HP) for 3s heals them 6 HP (60s cooldown)
- Emboldening Chant: players in your chant get +5% attack speed

### Special passives

- Chant range +1 block per 10 Sermon
- Succor heal +1 per 10 Sermon

## 6. Weakness

- **What players read:** A chaplain must keep speaking. Silenced, the chant stops.
- **What it actually does:** Battle Chant switches off for 10s whenever you take more than 6 damage in one hit

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | proficient |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | mastery |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
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

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Mending

- Shared skill - full definition in `priest.md`

### Skill: Liturgy

- **Shared or class-only:** class-only
- **Trained by:** allies killing mobs inside your Battle Chant
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Sermon
- **Tier II bonus:** +1 Sermon
- **Tier III bonus:** +10% Chant regen
- **Tier IV bonus:** +1 Sermon
- **Tier V bonus:** +2 Sermon

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Cleric

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| chaplain_start | Chaplain | start | - | - | - | +1 max health |
| chaplain_psalm | Psalm | small | 1 | 1 | chaplain_start | +5% Chant damage bonus |
| chaplain_steady_voice | Steady Voice | small | 1 | 1 | chaplain_start | Battle Chant no longer stops from big hits below 10 damage |
| chaplain_field_medic | Field Medic | notable | 2 | 12 | chaplain_psalm | Succor cooldown -20s |
| chaplain_litany | Litany | notable | 2 | 15 | chaplain_steady_voice | players in your chant regenerate +10% faster |
| chaplain_holy_war | Holy War | keystone | 3 | 30 | chaplain_field_medic, chaplain_litany | chant bonuses +50%, but you deal -15% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Battle Chant | start | - | - | - | - |
| hymn | Hymn | small | 1 | 1 | start | +2 Sermon |
| choir | Choir | small | 1 | 5 | start | +3 blocks chant range |
| emboldening_chant | Emboldening Chant | notable | 2 | 15 | hymn | +5% more damage in chant |
| succor | Succor | notable | 2 | 20 | choir | Succor also removes Poison and Wither |
| voice_of_god | Voice of God | keystone | 3 | 35 | emboldening_chant, succor | chant also gives Resistance I; Chant regen -25% |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| chaplain_stat_start | Chaplain | start | - | - | - | - |
| chaplain_devout | Devout | small | 2 | 1 | chaplain_stat_start | +2 Wisdom, +1 Endurance |
| chaplain_burly_preacher | Burly Preacher | small | 2 | 5 | chaplain_devout | +2 Strength, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Battle Chant, Emboldening Chant, Succor.
