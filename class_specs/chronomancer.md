# Class: Chronomancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Chronomancer' (p. 83). -->

## 1. Basics

- **Id:** chronomancer
- **Display name:** Chronomancer
- **Description:** A master of Time magic who slips between seconds, moves faster than anyone and glimpses what's coming.
- **Icon item:** clock
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 0 |
| Dexterity    | 2 |
| Agility      | 4 |
| Intelligence | 8 |
| Wisdom       | 6 |
| Endurance    | 1 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Tempo
- **Description:** Your grip on the flow of time.
- **What each point gives:** +0.3% attack speed and +0.3% movement speed
- **Max points:** 100

### Resource bar

- **Name:** Sands
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** sand gold (#D9B45A)

## 4. Starting kit

- 1 × clock
- 1 × leather_chestplate
- 1 × stone_sword
- 6 × bread

## 5. Passives

- Quickening: whenever you spend Sands, gain Speed II for 3s and your movement doesn't provoke knockback
- Precognition: 10% chance any hit against you misses
- Potion effects on you tick down 20% slower (good ones last longer)

### Special passives

- +0.2% Precognition per Tempo point
- Per 10 Tempo, harmful effects on you last 3% shorter

## 6. Weakness

- **What players read:** Time takes its toll on those who bend it.
- **What it actually does:** you get hungry 30% faster

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
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

### Skill: Temporal Study

- **Shared or class-only:** class-only
- **Trained by:** dodging with Precognition and spending Sands
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Tempo
- **Tier II bonus:** +1 Tempo
- **Tier III bonus:** +10% Sands regen
- **Tier IV bonus:** +1 Tempo
- **Tier V bonus:** +2 Tempo

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| chronomancer_start | Chronomancer | start | - | - | - | +3% movement speed |
| chronomancer_fast_hands | Fast Hands | small | 1 | 1 | chronomancer_start | +6% attack speed |
| chronomancer_fleeting | Fleeting | small | 1 | 1 | chronomancer_start | +5% movement speed |
| chronomancer_foresight | Foresight | notable | 2 | 12 | chronomancer_fast_hands | +5% Precognition |
| chronomancer_stretch | Stretch | notable | 2 | 15 | chronomancer_fleeting | your Speed effects last 30% longer |
| chronomancer_time_thief | Time Thief | keystone | 3 | 30 | chronomancer_foresight, chronomancer_stretch | +20% attack and movement speed, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Quickening | start | - | - | - | - |
| accelerate | Accelerate | small | 1 | 1 | start | +2 Tempo |
| haste | Haste | small | 1 | 5 | start | Quickening speed lasts 2s longer |
| precognition | Precognition | notable | 2 | 15 | accelerate | unlocks Insight: after you dodge, your next hit is a guaranteed crit |
| slow | Slow | notable | 2 | 20 | haste | mobs you hit get Slowness I for 2s |
| pause | Pause | keystone | 3 | 35 | precognition, slow | unlocks Time Stop ability; hunger drains 50% faster |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| chronomancer_stat_start | Chronomancer | start | - | - | - | - |
| chronomancer_quick_witted | Quick-Witted | small | 2 | 1 | chronomancer_stat_start | +2 Intelligence, +1 Agility |
| chronomancer_patient | Patient | small | 2 | 5 | chronomancer_quick_witted | +2 Wisdom, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Haste** (Speed III + Haste II for 8s, 25 Sands), **Time Stop** (freeze mobs within 6 blocks for 3s, 60 Sands).
- Book talents: Quickening, Precognition (reroll a failure). Tradition: Time.
