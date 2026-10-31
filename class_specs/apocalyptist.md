# Class: Apocalyptist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Apocalyptist' (p. 80). -->

## 1. Basics

- **Id:** apocalyptist
- **Display name:** Apocalyptist
- **Description:** A delver into Forbidden magic who spreads fear and ruin. Immensely dangerous, corrupted by their own power.
- **Icon item:** wither_skeleton_skull
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 1 |
| Dexterity    | 1 |
| Agility      | 1 |
| Intelligence | 8 |
| Wisdom       | 7 |
| Endurance    | 3 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Corruption
- **Description:** The darkness eating you from inside - and the power it gives.
- **What each point gives:** +1% magic damage and +0.5% Wither damage
- **Max points:** 100

### Resource bar

- **Name:** Doom
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** sick green-black (#4E7A1E)

## 4. Starting kit

- 1 × stone_sword
- 1 × wither_rose
- 1 × leather_chestplate
- 4 × rotten_flesh
- 4 × bread

## 5. Passives

- Fearsome Casting: when you spend Doom, mobs within 8 blocks get Weakness I for 4s
- Your hits have a 15% chance to apply Wither I for 3s
- Immune to the Wither effect
- Hasten the Apocalypse: mobs you kill have a 10% chance to leave a lingering cloud of Instant Damage

### Special passives

- Wither chance +0.3% per Corruption point
- Fearsome Casting radius +1 block per 10 Corruption

## 6. Weakness

- **What players read:** Corruption marks you. The living recoil.
- **What it actually does:** villagers refuse to trade with you, animals flee from you, and healing from Regeneration/Instant Health is 25% weaker

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
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Forbidden Rites

- **Shared or class-only:** class-only
- **Trained by:** killing mobs while they have the Wither effect
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Corruption
- **Tier II bonus:** +1 Corruption
- **Tier III bonus:** +10% Doom regen
- **Tier IV bonus:** +1 Corruption
- **Tier V bonus:** +2 Corruption

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Warlock

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| apocalyptist_start | Apocalyptist | start | - | - | - | +5% magic damage |
| apocalyptist_dark_whispers | Dark Whispers | small | 1 | 1 | apocalyptist_start | +6% magic damage |
| apocalyptist_blighted | Blighted | small | 1 | 1 | apocalyptist_start | +10% Wither duration |
| apocalyptist_harbinger | Harbinger | notable | 2 | 12 | apocalyptist_dark_whispers | mobs near you take 1 damage every 3s |
| apocalyptist_plague_bearer | Plague Bearer | notable | 2 | 15 | apocalyptist_blighted | your Wither is Wither II |
| apocalyptist_end_of_days | End of Days | keystone | 3 | 30 | apocalyptist_harbinger, apocalyptist_plague_bearer | +25% magic damage, but you slowly take 1 damage every 10s |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Fearsome Casting | start | - | - | - | - |
| black_pages | Black Pages | small | 1 | 1 | start | +2 Corruption |
| terror | Terror | small | 1 | 5 | start | Fearsome Casting adds Slowness I |
| hasten_the_apocalypse | Hasten the Apocalypse | notable | 2 | 15 | black_pages | unlocks Hasten the Apocalypse ability |
| unholy_vigor | Unholy Vigor | notable | 2 | 20 | terror | +2 max health per 10 Corruption |
| avatar_of_ruin | Avatar of Ruin | keystone | 3 | 35 | hasten_the_apocalypse, unholy_vigor | doubles Fearsome Casting radius; you can't sleep in beds |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| apocalyptist_stat_start | Apocalyptist | start | - | - | - | - |
| apocalyptist_dread_intellect | Dread Intellect | small | 2 | 1 | apocalyptist_stat_start | +2 Intelligence, +1 Wisdom |
| apocalyptist_hollow_will | Hollow Will | small | 2 | 5 | apocalyptist_dread_intellect | +2 Wisdom, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Hasten the Apocalypse** (wave of green light: mobs take damage, fall prone, ground becomes slow, 40 Doom).
- Book talents: Fearsome Casting, Hasten the Apocalypse. The book also gives +1 Corruption - represented by the special stat and the villager weakness. Tradition: Forbidden.
- **Decided:** Selectable by players like every other class, even though Apocalyptists are openly evil in the book.
