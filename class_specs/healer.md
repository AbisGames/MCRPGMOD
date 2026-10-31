# Class: Healer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Healer' (p. 89). -->

## 1. Basics

- **Id:** healer
- **Display name:** Healer
- **Description:** A master of Life magic whose touch closes wounds, cures poison and keeps whole parties standing.
- **Icon item:** glistering_melon_slice
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 6 | x1.15 |
| Strength     | 2 | x1 |
| Dexterity    | 1 | x0.85 |
| Agility      | 1 | x0.85 |
| Intelligence | 5 | x1.15 |
| Wisdom       | 8 | x1.3 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.7 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Vitality Flow
- **Description:** The life energy you can pour into others.
- **What each point gives:** +1% healing you give and receive
- **Max points:** 100

### Resource bar

- **Name:** Lifeblood
- **Max at 0 points:** 70
- **Extra max per special-stat point:** 5
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** life green (#5BE07A)

## 4. Starting kit

- 2 × glistering_melon_slice
- 1 × splash_potion of healing
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Far Healing: other players within 10 blocks regenerate 1 HP every 4s while you have over 25% Lifeblood (drains 1 per heal)
- Unassailable Vitality: immune to Poison, Hunger and Wither; sleeping heals you fully
- Empowered Healing: Instant Health and Regeneration potions you throw or drink are 30% stronger

### Special passives

- Far Healing tick -1% interval per Vitality Flow
- Per 10 Vitality Flow, +1 block healing range

## 6. Weakness

- **What players read:** Healers commit to mending, never to harming.
- **What it actually does:** -25% damage dealt with all weapons

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | proficient |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | clumsy |
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

### Skill: Mending

- Shared skill - full definition in `priest.md`

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Lifebinding

- **Shared or class-only:** class-only
- **Trained by:** healing other players
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Vitality Flow
- **Tier II bonus:** +1 Vitality Flow
- **Tier III bonus:** +10% Lifeblood regen
- **Tier IV bonus:** +1 Vitality Flow
- **Tier V bonus:** +2 Vitality Flow

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Priest

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| healer_start | Healer | start | - | - | - | +10% healing given |
| healer_gentle_hands | Gentle Hands | small | 1 | 1 | healer_start | +10% healing given |
| healer_physician | Physician | small | 1 | 1 | healer_start | your potions last 20% longer |
| healer_triage | Triage | notable | 2 | 12 | healer_gentle_hands | players below 30% health heal twice as fast from you |
| healer_purify | Purify | notable | 2 | 15 | healer_physician | players near you lose Poison twice as fast |
| healer_saint | Saint | keystone | 3 | 30 | healer_triage, healer_purify | healing doubled, but you deal -30% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Far Healing | start | - | - | - | - |
| lifespring | Lifespring | small | 1 | 1 | start | +2 Vitality Flow |
| wide_mend | Wide Mend | small | 1 | 5 | start | +3 blocks healing range |
| empowered_healing | Empowered Healing | notable | 2 | 15 | lifespring | unlocks Mass Heal ability |
| unassailable_vitality | Unassailable Vitality | notable | 2 | 20 | wide_mend | also immune to Nausea and Slowness |
| resurrection | Resurrection | keystone | 3 | 35 | empowered_healing, unassailable_vitality | once per day, a player who dies near you is saved at 4 HP; -20% max Lifeblood |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| healer_stat_start | Healer | start | - | - | - | - |
| healer_compassionate | Compassionate | small | 2 | 1 | healer_stat_start | +2 Wisdom, +1 Vitality |
| healer_learned_healer | Learned Healer | small | 2 | 5 | healer_compassionate | +2 Intelligence, +1 Wisdom |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Heal** (target player heals 8, 15 Lifeblood), **Mass Heal** (all players within 8 blocks heal 6, 50 Lifeblood), **Cure** (remove harmful effects, 20 Lifeblood).
- Book talents: Far Healing, Empowered Healing, Unassailable Vitality. Tradition: Life.
