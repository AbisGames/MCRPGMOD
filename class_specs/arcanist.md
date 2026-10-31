# Class: Arcanist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Arcanist' (p. 80). -->

## 1. Basics

- **Id:** arcanist
- **Display name:** Arcanist
- **Description:** A scholar of pure Arcana who can turn any magic into any other and recover spent power.
- **Icon item:** lapis_lazuli
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 0 |
| Dexterity    | 2 |
| Agility      | 1 |
| Intelligence | 10 |
| Wisdom       | 6 |
| Endurance    | 2 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Arcana
- **Description:** Command of magic's underlying structure.
- **What each point gives:** +0.75% max Mystic Flux and +0.5% magic damage
- **Max points:** 100

### Resource bar

- **Name:** Mystic Flux
- **Max at 0 points:** 70
- **Extra max per special-stat point:** 5
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** arcane purple (#7A4DFF)

## 4. Starting kit

- 1 × book
- 8 × lapis_lazuli
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Reclaim Arcana: whenever you spend Mystic Flux, 1-in-6 chance to get it all back
- Arcana Mastery: enchanting at a table costs 1 fewer level and 1 fewer lapis
- +10% magic damage

### Special passives

- +0.3% Reclaim chance per Arcana point
- Grindstone disenchanting gives +1% more XP per 2 Arcana

## 6. Weakness

- **What players read:** Pure theory, little practice with steel.
- **What it actually does:** -20% melee damage and takes 10% more melee damage

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
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Arcane Research

- **Shared or class-only:** class-only
- **Trained by:** enchanting items and combining enchanted books
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Arcana
- **Tier II bonus:** +1 Arcana
- **Tier III bonus:** +10% Flux regen
- **Tier IV bonus:** +1 Arcana
- **Tier V bonus:** +2 Arcana

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| arcanist_start | Arcanist | start | - | - | - | +5% experience gained |
| arcanist_theory | Theory | small | 1 | 1 | arcanist_start | +6% magic damage |
| arcanist_practice | Practice | small | 1 | 1 | arcanist_start | +10% experience gained |
| arcanist_swift_arcana | Swift Arcana | notable | 2 | 12 | arcanist_theory | abilities cost 10% less Flux |
| arcanist_deep_reserves | Deep Reserves | notable | 2 | 15 | arcanist_practice | +20% max Mystic Flux |
| arcanist_unbound_mind | Unbound Mind | keystone | 3 | 30 | arcanist_swift_arcana, arcanist_deep_reserves | +30% max Mystic Flux, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Arcana Mastery | start | - | - | - | - |
| formulae | Formulae | small | 1 | 1 | start | +2 Arcana |
| recycling | Recycling | small | 1 | 5 | start | Reclaim chance +5% |
| swift_arcana | Swift Arcana | notable | 2 | 15 | formulae | abilities have 15% shorter cooldowns |
| reclaim_arcana | Reclaim Arcana | notable | 2 | 20 | recycling | Reclaim refunds go to 150% |
| archmage_s_insight | Archmage's Insight | keystone | 3 | 35 | swift_arcana, reclaim_arcana | every 3rd ability is free; -10% Flux regen |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| arcanist_stat_start | Arcanist | start | - | - | - | - |
| arcanist_brilliant | Brilliant | small | 2 | 1 | arcanist_stat_start | +3 Intelligence |
| arcanist_calm | Calm | small | 2 | 5 | arcanist_brilliant | +2 Wisdom, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Arcane Bolt** (basic magic missile every caster can be balanced against, 10 Flux).
- Book talents: Arcana Mastery (swap castings), Reclaim Arcana, Swift Arcana. Tradition: Arcana.
