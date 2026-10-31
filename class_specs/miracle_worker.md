# Class: Miracle Worker

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Miracle Worker' (p. 92). -->

## 1. Basics

- **Id:** miracle_worker
- **Display name:** Miracle Worker
- **Description:** Faith so absolute their body becomes a vessel of the god - stigmata, a voice heard for miles, miraculous healing.
- **Icon item:** beacon
- **Selectable by players:** yes
- **Base health:** 36

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 7 | x1.35 |
| Strength     | 3 | x1 |
| Dexterity    | 1 | x0.85 |
| Agility      | 1 | x0.85 |
| Intelligence | 3 | x1 |
| Wisdom       | 8 | x1.15 |
| Endurance    | 2 | x1.15 |
| Luck         | 1 | x0.65 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Devotion
- **Description:** Absolute conviction.
- **What each point gives:** +1% healing given and +0.1 max health
- **Max points:** 100

### Resource bar

- **Name:** Miracle
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** divine white (#FFFDE8)

## 4. Starting kit

- 1 × golden_apple
- 1 × book
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Stigmata: once Miracle is full, you can take on the appearance of your god (toggle, drains 4/s): mobs within 8 blocks get Slowness and Weakness, you get +15% damage and Regeneration I
- Tongues: your chat messages can be read by all players within 1000 blocks when you start them with '!'
- Worker of Miracles: sneaking + right-clicking a player with an empty hand heals them 6 HP (spends 20 Miracle)

### Special passives

- Stigmata radius +1 per 10 Devotion
- Worker of Miracles heals +1 per 10 Devotion

## 6. Weakness

- **What players read:** Every miracle costs the body.
- **What it actually does:** each Stigmata or Worker of Miracles use lowers your max health by 2 until you sleep (stacks)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
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

### Skill: Miracles

- **Shared or class-only:** class-only
- **Trained by:** healing others with Worker of Miracles
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Devotion
- **Tier II bonus:** +1 Devotion
- **Tier III bonus:** +10% Miracle regen
- **Tier IV bonus:** +1 Devotion
- **Tier V bonus:** +2 Devotion

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Oracle

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| miracle_worker_start | Miracle Worker | start | - | - | - | +2 max health |
| miracle_worker_blessed_body | Blessed Body | small | 1 | 1 | miracle_worker_start | +3 max health |
| miracle_worker_halo | Halo | small | 1 | 1 | miracle_worker_start | you glow and light the area |
| miracle_worker_holy_presence | Holy Presence | notable | 2 | 12 | miracle_worker_blessed_body | Stigmata Slowness becomes Slowness II |
| miracle_worker_laying_of_hands | Laying of Hands | notable | 2 | 15 | miracle_worker_halo | Worker of Miracles also cures Poison and Wither |
| miracle_worker_saint_among_us | Saint Among Us | keystone | 3 | 30 | miracle_worker_holy_presence, miracle_worker_laying_of_hands | miracles cost half, but max health penalty is 3 per use |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Stigmata | start | - | - | - | - |
| conviction | Conviction | small | 1 | 1 | start | +2 Devotion |
| booming_voice | Booming Voice | small | 1 | 5 | start | Tongues messages carry server-wide |
| worker_of_miracles | Worker of Miracles | notable | 2 | 15 | conviction | heal amount 10 |
| divine_visage | Divine Visage | notable | 2 | 20 | booming_voice | Stigmata also gives Resistance I |
| living_god | Living God | keystone | 3 | 35 | worker_of_miracles, divine_visage | Stigmata lasts twice as long; you can't sleep while it's active |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| miracle_worker_stat_start | Miracle Worker | start | - | - | - | - |
| miracle_worker_zealous | Zealous | small | 2 | 1 | miracle_worker_stat_start | +2 Wisdom, +1 Vitality |
| miracle_worker_martyr_s_body | Martyr's Body | small | 2 | 5 | miracle_worker_zealous | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Stigmata, Tongues, Worker of Miracles. Book costs (Insanity, -5 Health until rest) represented by the weakness.
