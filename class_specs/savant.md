# Class: Savant

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Savant' (p. 94). -->

## 1. Basics

- **Id:** savant
- **Display name:** Savant
- **Description:** A broad scholar of magic who learns more of everything rather than more power in one thing.
- **Icon item:** bookshelf
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 0 |
| Dexterity    | 2 |
| Agility      | 2 |
| Intelligence | 9 |
| Wisdom       | 6 |
| Endurance    | 2 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Breadth
- **Description:** Range of magical knowledge.
- **What each point gives:** +0.5% magic damage and +0.5% potion duration
- **Max points:** 100

### Resource bar

- **Name:** Insight
- **Max at 0 points:** 70
- **Extra max per special-stat point:** 5
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** parchment (#E9D8A6)

## 4. Starting kit

- 2 × book
- 4 × lapis_lazuli
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Tradition Focus: pick two elemental focus schools when you choose the class - Fire, Water, Air, Earth, Storm, Shadow, Light (sun/celestial) or Life. Damage and effects of those elements are +20% (Fire = burning/fire charges, Water = drowned/underwater/trident, Air = wind charges/fall, Earth = explosions/blocks, Storm = lightning, Shadow = darkness/wither, Light = undead/smite, Life = healing)
- +20% experience gained
- Enchanting, brewing and anvil use all cost slightly less (-1 level / 10% faster)

### Special passives

- Focus bonus +0.5% per Breadth
- Per 10 Breadth, +5% experience

## 6. Weakness

- **What players read:** Breadth, not depth.
- **What it actually does:** -10% max Insight regen and your focus schools are the only ones above neutral

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

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Broad Study

- **Shared or class-only:** class-only
- **Trained by:** using magic of both focus schools in one fight
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Breadth
- **Tier II bonus:** +1 Breadth
- **Tier III bonus:** +10% Insight regen
- **Tier IV bonus:** +1 Breadth
- **Tier V bonus:** +2 Breadth

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| savant_start | Savant | start | - | - | - | +5% experience |
| savant_bookish | Bookish | small | 1 | 1 | savant_start | +10% experience |
| savant_cross_discipline | Cross-Discipline | small | 1 | 1 | savant_start | +5% magic damage |
| savant_library_mind | Library Mind | notable | 2 | 12 | savant_bookish | +20% max Insight |
| savant_fast_learner | Fast Learner | notable | 2 | 15 | savant_cross_discipline | skills train 15% faster |
| savant_omniscient | Omniscient | keystone | 3 | 30 | savant_library_mind, savant_fast_learner | +1 more focus school, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Tradition Focus | start | - | - | - | - |
| annotations | Annotations | small | 1 | 1 | start | +2 Breadth |
| second_focus | Second Focus | small | 1 | 5 | start | focus bonus +10% |
| generalist | Generalist | notable | 2 | 15 | annotations | non-focus schools get +10% |
| encyclopedic | Encyclopedic | notable | 2 | 20 | second_focus | +10% potion duration |
| grand_savant | Grand Savant | keystone | 3 | 35 | generalist, encyclopedic | focus bonus doubled; -10% Insight regen |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| savant_stat_start | Savant | start | - | - | - | - |
| savant_erudite | Erudite | small | 2 | 1 | savant_stat_start | +3 Intelligence |
| savant_composed | Composed | small | 2 | 5 | savant_erudite | +2 Wisdom, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- The book's Savant learns extra spells instead of more Power - mapped to choosing focus schools and generic magic bonuses.
- Book talents: Tradition Focus, extra spells.
- **Decided:** Focus schools are elements (Fire, Water, Air, Earth, Storm, Shadow, Light, Life). The list is meant to match the elemental magic classes, so new elements can be added later.
