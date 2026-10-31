# Class: Hexer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Hexer' (p. 89). -->

## 1. Basics

- **Id:** hexer
- **Display name:** Hexer
- **Description:** A curse-slinger who delights in misfortune - warping enemies, sapping their strength and making them hurt.
- **Icon item:** fermented_spider_eye
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 0 | x0.7 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x0.85 |
| Intelligence | 8 | x1.15 |
| Wisdom       | 7 | x1.3 |
| Endurance    | 1 | x0.85 |
| Luck         | 3 | x1.15 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Malice
- **Description:** The spite you pour into every curse.
- **What each point gives:** +0.5% chance your hits curse a mob
- **Max points:** 100

### Resource bar

- **Name:** Spite
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** curse purple (#6A1B7A)

## 4. Starting kit

- 2 × fermented_spider_eye
- 1 × splash_potion of weakness
- 1 × stone_sword
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Evil Eye: 12% chance on hit to curse a mob for 8s with one of: Desire (stops attacking), Lethargy (Slowness II) or Pain (2 damage per second)
- Exacting Curse: +15% damage to cursed mobs
- Harmful splash potions you throw last 25% longer

### Special passives

- Curse chance +0.3% per Malice
- Per 10 Malice, curses last 1s longer

## 6. Weakness

- **What players read:** Curses rebound on their maker.
- **What it actually does:** while a curse you placed is active, you take 10% more damage

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

### Skill: Maledictions

- **Shared or class-only:** class-only
- **Trained by:** cursing mobs with Evil Eye
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Malice
- **Tier II bonus:** +1 Malice
- **Tier III bonus:** +10% Spite regen
- **Tier IV bonus:** +1 Malice
- **Tier V bonus:** +2 Malice

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Witch

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| hexer_start | Hexer | start | - | - | - | +3% curse chance |
| hexer_wicked | Wicked | small | 1 | 1 | hexer_start | +5% curse chance |
| hexer_lingering_hate | Lingering Hate | small | 1 | 1 | hexer_start | curses last 3s longer |
| hexer_withering_gaze | Withering Gaze | notable | 2 | 12 | hexer_wicked | Pain curse also gives Weakness |
| hexer_hex_ward | Hex Ward | notable | 2 | 15 | hexer_lingering_hate | you can't be hit by witches' potions |
| hexer_doom_hex | Doom Hex | keystone | 3 | 30 | hexer_withering_gaze, hexer_hex_ward | cursed mobs that die explode in a small Instant Damage cloud, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Evil Eye | start | - | - | - | - |
| spiteful | Spiteful | small | 1 | 1 | start | +2 Malice |
| desire | Desire | small | 1 | 5 | start | Desire-cursed mobs fight other mobs |
| lethargy | Lethargy | notable | 2 | 15 | spiteful | Lethargy also stops jumping |
| pain | Pain | notable | 2 | 20 | desire | Pain curse deals 3 per second |
| arch_hexer | Arch-Hexer | keystone | 3 | 35 | lethargy, pain | you can curse 3 mobs at once; Spite regen -25% |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| hexer_stat_start | Hexer | start | - | - | - | - |
| hexer_malicious | Malicious | small | 2 | 1 | hexer_stat_start | +2 Intelligence, +1 Luck |
| hexer_bitter_will | Bitter Will | small | 2 | 5 | hexer_malicious | +2 Wisdom, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Hex** (choose a curse and apply it to a target at range, 20 Spite).
- Book talents: Evil Eye (Desire/Lethargy/Pain), Exacting Curse. Tradition: Curse.
