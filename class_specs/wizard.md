# Class: Wizard

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Wizard' (p. 76). -->

## 1. Basics

- **Id:** wizard
- **Display name:** Wizard
- **Description:** A scholar of magic who keeps their power in a grimoire. Unmatched knowledge, deep Mana reserves.
- **Icon item:** writable_book
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x1 |
| Strength     | 0 | x0.6 |
| Dexterity    | 1 | x1.2 |
| Agility      | 1 | x0.8 |
| Intelligence | 9 | x1.4 |
| Wisdom       | 4 | x1.2 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Lore
- **Description:** Accumulated arcane knowledge. Deepens the Aether pool and sharpens enchanting.
- **What each point gives:** +0.5% magic damage and +0.5% max Aether
- **Max points:** 100

### Resource bar

- **Name:** Aether
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 5
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** deep blue (#2E4BD8)

## 4. Starting kit

- 1 × writable_book
- 1 × enchanted_book (random level-1 enchant)
- 4 × lapis_lazuli
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Grimoire: while a book, writable book or enchanted book is in your off-hand, +15% magic damage and +20% Aether regen
- +15% experience from all sources
- Enchanting costs 1 fewer level

### Special passives

- +1 bookshelf counted around your enchanting table per 20 Lore
- Spell Expertise: +1% Aether regen per 2 Lore

## 6. Weakness

- **What players read:** Without the grimoire, a wizard is just a nervous scholar.
- **What it actually does:** with no book anywhere in your inventory: -30% Aether regen and -10% magic damage

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
| Specific items / exceptions: | book/enchanted_book in off-hand: counts as proficient off-hand item |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Scholarship

- **Shared or class-only:** class-only
- **Trained by:** enchanting items and using enchanted books on an anvil
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Lore
- **Tier II bonus:** +1 Lore
- **Tier III bonus:** +10% Aether max
- **Tier IV bonus:** +1 Lore
- **Tier V bonus:** +2 Lore

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** top of the web (Power quarter), ring 2, outer edge

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| wizard_start | Wizard | start | - | - | - | +5% experience gained |
| wizard_study | Study | small | 1 | 1 | wizard_start | +5% magic damage |
| wizard_annotations | Annotations | small | 1 | 1 | wizard_start | +10% experience gained |
| wizard_library | Library | notable | 2 | 10 | wizard_annotations | +1 counted bookshelf, -1 enchanting cost |
| wizard_arcane_ward | Arcane Ward | notable | 2 | 12 | wizard_study | -12% magic damage taken |
| wizard_archmage | Path of the Archmage | keystone | 3 | 25 | wizard_library, wizard_arcane_ward | +25% max Aether and +10% magic damage, but -15% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Grimoire | start | - | - | - | - |
| grimoire_casting | Grimoire Casting | small | 1 | 1 | start | +2 Lore |
| margins | Notes in the Margins | small | 1 | 5 | start | +15% max Aether |
| spell_expertise | Spell Expertise | notable | 2 | 15 | grimoire_casting | +20% Aether regen |
| second_tome | Second Tome | notable | 2 | 20 | margins | Grimoire bonus also works with a book in any hotbar slot |
| spell_mastery | Spell Mastery | keystone | 3 | 35 | spell_expertise, second_tome | 6 'spell points' per rest that pay for abilities instead of Aether; -10% Aether regen |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| wizard_stat_start | Wizard | start | - | - | - | - |
| wizard_learned | Learned | small | 2 | 1 | wizard_stat_start | +3 Intelligence |
| wizard_patient | Patient | small | 2 | 5 | wizard_learned | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Spells are not built yet; Aether is the wizard's spell fuel. Book talents for later: **Grimoire Casting** (cast spells written into a grimoire), **Spell Mastery** (spell points).
- Idea: a craftable 'Grimoire' item (book + lapis + leather) could replace 'any book' later.
- **Decided:** Lore + Aether. Every caster keeps its own bar name.
