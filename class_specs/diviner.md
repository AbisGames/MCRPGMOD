# Class: Diviner

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Diviner' (p. 85). -->

## 1. Basics

- **Id:** diviner
- **Display name:** Diviner
- **Description:** A seer of Divination magic who glimpses the next few seconds and bends fortune around them.
- **Icon item:** ender_eye
- **Selectable by players:** yes
- **Base health:** 26

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x1 |
| Strength     | 0 | x0.75 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x0.85 |
| Intelligence | 7 | x1.15 |
| Wisdom       | 8 | x1.25 |
| Endurance    | 1 | x0.85 |
| Luck         | 4 | x1.15 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Foresight
- **Description:** How clearly you see what comes next.
- **What each point gives:** +0.3% chance an attack against you misses
- **Max points:** 100

### Resource bar

- **Name:** Omens
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** pale violet (#C3A6FF)

## 4. Starting kit

- 1 × spyglass
- 1 × compass
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Premonitions: after sleeping, you get 3 'premonitions' - each automatically turns a hit against you into a miss
- Omens: +1 Luck; while you have a premonition left, mobs can't surprise-hit you (no backstab bonus) and you see their outlines within 8 blocks
- Structures within 64 blocks are hinted in chat when you look at a compass

### Special passives

- Miss chance +0.3% per Foresight
- Per 20 Foresight, +1 premonition

## 6. Weakness

- **What players read:** Diviners watch the future - and miss the present.
- **What it actually does:** takes 15% more damage from the first hit of every fight

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
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
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Augury

- **Shared or class-only:** class-only
- **Trained by:** using up premonitions and spending Omens
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Foresight
- **Tier II bonus:** +1 Foresight
- **Tier III bonus:** +10% Omens regen
- **Tier IV bonus:** +1 Foresight
- **Tier V bonus:** +2 Foresight

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Oracle

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| diviner_start | Diviner | start | - | - | - | +1 Luck |
| diviner_third_eye | Third Eye | small | 1 | 1 | diviner_start | +5% miss chance |
| diviner_lucky_star | Lucky Star | small | 1 | 1 | diviner_start | +1 Luck |
| diviner_read_the_signs | Read the Signs | notable | 2 | 12 | diviner_third_eye | loot chests show their contents' rarity colour |
| diviner_second_sight | Second Sight | notable | 2 | 15 | diviner_lucky_star | invisible mobs glow |
| diviner_doomsayer | Doomsayer | keystone | 3 | 30 | diviner_read_the_signs, diviner_second_sight | +2 premonitions, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Premonitions | start | - | - | - | - |
| scrying | Scrying | small | 1 | 1 | start | +2 Foresight |
| omens | Omens | small | 1 | 5 | start | premonitions also work on arrows |
| ill_fortune | Ill Fortune | notable | 2 | 15 | scrying | mobs within 8 blocks have 10% chance to miss everyone |
| twist_of_fate | Twist of Fate | notable | 2 | 20 | omens | unlocks Twist Fate ability |
| oracle_of_ages | Oracle of Ages | keystone | 3 | 35 | ill_fortune, twist_of_fate | premonitions refill every 10 minutes; Omens regen -30% |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| diviner_stat_start | Diviner | start | - | - | - | - |
| diviner_far_seeing | Far-Seeing | small | 2 | 1 | diviner_stat_start | +2 Wisdom, +1 Luck |
| diviner_reasoned | Reasoned | small | 2 | 5 | diviner_far_seeing | +2 Intelligence, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Twist Fate** (force one attack against an ally to miss, 30 Omens), **Scry** (see through a player's eyes for 5s).
- Book talents: Omens, Premonitions. Tradition: Divination.
