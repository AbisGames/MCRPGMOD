# Class: Artificer

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Artificer' (p. 61). -->

## 1. Basics

- **Id:** artificer
- **Display name:** Artificer
- **Description:** A tinkerer who fuses magic and machinery, keeping gear in top shape and building mechanical servants.
- **Icon item:** crafter
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x1 |
| Strength     | 2 | x0.9 |
| Dexterity    | 4 | x1.1 |
| Agility      | 1 | x0.9 |
| Intelligence | 6 | x1.2 |
| Wisdom       | 1 | x0.8 |
| Endurance    | 3 | x1.1 |
| Luck         | 1 | x1 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Ingenuity
- **Description:** Clever hands and a head full of schematics. Feeds Spare Parts and durability bonuses.
- **What each point gives:** +0.5% chance that a durability point is not used
- **Max points:** 100

### Resource bar

- **Name:** Spare Parts
- **Max at 0 points:** 20
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.1
- **Extra regen per second per special-stat point:** 0.01
- **Starts full when the class is chosen:** yes
- **Colour:** copper (#C46A3B)

## 4. Starting kit

- 1 × crossbow
- 16 × arrow
- 1 × chainmail_chestplate
- 8 × copper_ingot
- 1 × crafting_table
- 6 × bread

## 5. Passives

- Anvil repairs and renames cost 1 fewer level
- Gear you wear and hold loses durability 15% slower
- +10% crossbow reload speed
- Spare Parts refill completely when you sleep

### Special passives

- +0.25% chance per Ingenuity point to get an extra output item when crafting arrows, rails or redstone components
- +1 armor while wearing any copper armor piece, per 20 Ingenuity

## 6. Weakness

- **What players read:** Metal and magic attract the storm.
- **What it actually does:** takes 25% more lightning damage, and a lightning strike drains all Spare Parts

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | neutral |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | mastery |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | mastery |
| Heavy armor (iron, diamond, netherite) | proficient |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 2 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Crossbowmanship

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with crossbow bolts
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% crossbow damage
- **Tier II bonus:** +5% reload speed
- **Tier III bonus:** +3% crossbow damage
- **Tier IV bonus:** +5% reload speed
- **Tier V bonus:** +6% crossbow damage

### Skill: Smithing

- **Shared or class-only:** shared
- **Trained by:** using anvils, grindstones and smithing tables
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** -5% anvil cost
- **Tier II bonus:** +5% durability on crafted tools
- **Tier III bonus:** -5% anvil cost
- **Tier IV bonus:** +5% durability on crafted tools
- **Tier V bonus:** anvils never break when you use them

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Tinkering

- **Shared or class-only:** class-only
- **Trained by:** crafting redstone components, rails and mechanisms
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Ingenuity
- **Tier II bonus:** +1 Ingenuity
- **Tier III bonus:** +10% Spare Parts max
- **Tier IV bonus:** +1 Ingenuity
- **Tier V bonus:** +2 Ingenuity

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** top of the web (Power quarter), ring 2, leaning toward the War quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| artificer_start | Artificer | start | - | - | - | +10% durability on everything you wear |
| artificer_rivets | Rivets | small | 1 | 1 | artificer_start | +1 armor |
| artificer_gears | Gears | small | 1 | 1 | artificer_start | +8% crossbow reload speed |
| artificer_alloy | Alloy Plating | notable | 2 | 10 | artificer_rivets | +1 armor toughness, +10% knockback resistance |
| artificer_repeater | Repeater | notable | 2 | 12 | artificer_gears | +10% crossbow damage |
| artificer_overclock | Overclock | keystone | 3 | 25 | artificer_alloy, artificer_repeater | +20% attack speed and reload speed, but your gear loses durability 25% faster |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Artificer's Bag | start | - | - | - | - |
| bigger_bag | Bigger Bag | small | 1 | 1 | start | +2 Ingenuity |
| quick_assembly | Quick Assembly | small | 1 | 5 | start | +25% Spare Parts regen |
| store_spell | Store Spell | notable | 2 | 15 | bigger_bag | potions you brew can be loaded into crafted gear (see section 12) |
| copper_heart | Copper Heart | notable | 2 | 20 | quick_assembly | copper golems near you move and work 25% faster |
| mechanical_servants | Mechanical Servants | keystone | 3 | 35 | store_spell, copper_heart | unlocks the Mechanical Servants ability; -15% max Spare Parts |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| artificer_stat_start | Artificer | start | - | - | - | - |
| artificer_inventive | Inventive | small | 2 | 1 | artificer_stat_start | +2 Intelligence, +1 Dexterity |
| artificer_sturdy | Workshop Hands | small | 2 | 5 | artificer_inventive | +2 Endurance, +1 Strength |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Assemble** (spend Spare Parts to make a temporary weapon/armor piece that vanishes when you sleep), **Store Spell** (load a potion effect into gear), **Mechanical Servants** (summon small temporary copper golems).
- Book talents: Artificer's Bag, Store Spell, Mechanical Servants.
- **Co-design question:** Ingenuity + Spare Parts (a slow-refilling bar that refills on sleep). Keep, or would you rather Spare Parts be a real item in the inventory?
