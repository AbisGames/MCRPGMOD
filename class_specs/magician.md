# Class: Magician

<!-- Source: Shadow of the Demon Lord core rulebook, novice path 'Magician' (p. 55). -->

## 1. Basics

- **Id:** magician
- **Display name:** Magician
- **Description:** A student of raw magic. Fragile in a brawl, but with unmatched magical potential.
- **Icon item:** enchanted_book
- **Selectable by players:** yes
- **Base health:** 18

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 1 | x0.75 |
| Strength     | 0 | x0.55 |
| Dexterity    | 1 | x1 |
| Agility      | 1 | x1.25 |
| Intelligence | 6 | x1.45 |
| Wisdom       | 4 | x1.25 |
| Endurance    | 0 | x0.75 |
| Luck         | 1 | x1 |
| Special stat (section 3) | 5 | |

## 3. Special stat

- **Name:** Power
- **Description:** How much magical energy you can safely channel. The heart of every Magician.
- **What each point gives:** +1% magic damage (potions, future spells)
- **Max points:** 100

### Resource bar

- **Name:** Mana
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 5
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** light blue (#3F7FFF)

## 4. Starting kit

- 1 × wooden_sword
- 1 × book
- 2 × lapis_lazuli
- 6 × bread

## 5. Passives

- Sense Magic: enchanted items and mobs with potion effects within 12 blocks glow for you while sneaking
- Counterspell: -15% magic damage taken
- Thrown splash/lingering potions last 15% longer

### Special passives

- Enchanting costs 1 fewer level for every 25 Power points
- +0.5% chance per Power point to keep the lapis when enchanting

## 6. Weakness

- **What players read:** A frail scholar, not a fighter.
- **What it actually does:** takes 20% more damage from melee attacks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | blaze_rod and stick held in main hand: count as a staff (spear) at proficient |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 1
- **Free skill points at start (special tree):** 0

## 10. Skills (mastery)

### Skill: Enchanting

- **Shared or class-only:** shared
- **Trained by:** enchanting items at an enchanting table
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +5% chance of a better enchantment roll
- **Tier II bonus:** -1 lapis cost on level-3 enchants
- **Tier III bonus:** +5% chance of a better enchantment roll
- **Tier IV bonus:** +1 bookshelf counted around your table
- **Tier V bonus:** +10% chance of a better enchantment roll

### Skill: Alchemy

- **Shared or class-only:** shared
- **Trained by:** brewing potions
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +5% potion duration
- **Tier II bonus:** +5% potion duration
- **Tier III bonus:** brewing is 10% faster
- **Tier IV bonus:** +5% potion duration
- **Tier V bonus:** +10% potion duration

### Skill: Arcane Study

- **Shared or class-only:** class-only
- **Trained by:** gaining experience levels
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Power
- **Tier II bonus:** +1 Power
- **Tier III bonus:** +10% Mana regen
- **Tier IV bonus:** +1 Power
- **Tier V bonus:** +2 Power

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** top of the web (the Power quarter), ring 1

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| magician_start | Magician | start | - | - | - | +5% magic damage |
| magician_focus | Focus | small | 1 | 1 | magician_start | +5% magic damage |
| magician_ward | Minor Ward | small | 1 | 1 | magician_start | -8% magic damage taken |
| magician_scholar | Scholar | small | 1 | 5 | magician_focus | +10% experience gained |
| magician_arcane_mind | Arcane Mind | notable | 2 | 10 | magician_scholar, magician_ward | +10% magic damage, +5% potion duration |
| magician_glass_cannon | Glass Cannon | keystone | 3 | 25 | magician_arcane_mind | +25% magic damage, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Tradition | start | - | - | - | - |
| cantrip | Cantrip | small | 1 | 1 | start | +2 Power |
| counterspell | Counterspell | small | 1 | 5 | start | -10% more magic damage taken |
| spell_recovery | Spell Recovery | notable | 2 | 15 | cantrip | +25% Mana regen while below half Mana |
| deep_well | Deep Well | notable | 2 | 20 | cantrip, counterspell | +20% max Mana |
| improved_recovery | Improved Spell Recovery | keystone | 3 | 35 | spell_recovery, deep_well | once per rest, refill 50% Mana instantly when you drop below 10%; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| magician_stat_start | Magician | start | - | - | - | - |
| magician_keen | Keen Mind | small | 2 | 1 | magician_stat_start | +2 Intelligence, +1 Wisdom |
| magician_lucky_star | Lucky Star | small | 2 | 5 | magician_keen | +1 Luck, +1 Intelligence |

## 12. Anything else

- **Power tier: Novice.** Book levels 1, 2, 5, 8. Baseline power: 14 stat points, 5 special points, 3 trinket slots. Selectable from the start.
- Spells are not built yet. Mana is the shared fuel for future spells. Book talents for later: **Spell Recovery** (refill Mana + heal), **Sense Magic** as a toggle.
- The book lets magicians pick traditions freely (Fire, Air, Life...). Later the master magic classes (Pyromancer, Healer...) each get one tradition.
- **Co-design question:** Power + Mana are my proposal. Keep, or rename/rework?
