# Class: Transmuter

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Transmuter' (p. 97). -->

## 1. Basics

- **Id:** transmuter
- **Display name:** Transmuter
- **Description:** A changeable master of Alteration who reshapes their own body each day and eventually improves everything.
- **Icon item:** brewing_stand
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.1 |
| Strength     | 3 | x1 |
| Dexterity    | 3 | x1 |
| Agility      | 3 | x0.9 |
| Intelligence | 4 | x1.1 |
| Wisdom       | 4 | x1.15 |
| Endurance    | 3 | x0.9 |
| Luck         | 2 | x0.85 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Flux
- **Description:** How freely your body and mind can be rewritten.
- **What each point gives:** +0.2 to every stat per 10 points (rounded down)
- **Max points:** 100

### Resource bar

- **Name:** Alteration
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** shifting teal-gold (#55B8A0)

## 4. Starting kit

- 1 × brewing_stand
- 2 × glass_bottle
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Optimization: each time you sleep, choose one stat to lower by 3 and another to raise by 3 until you sleep again
- Potions you drink last 20% longer
- All your stats +1 (book level-10 'increase each attribute by 1')

### Special passives

- Optimization amount +1 per 20 Flux
- Potion duration +0.5% per Flux

## 6. Weakness

- **What players read:** Never quite the same twice.
- **What it actually does:** your Optimization resets if you die, and the stat you lower drops by 6 instead of 3 (you still only gain 3)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | clumsy |
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

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Alterations

- **Shared or class-only:** class-only
- **Trained by:** drinking potions and using Optimization
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Flux
- **Tier II bonus:** +1 Flux
- **Tier III bonus:** +10% Alteration regen
- **Tier IV bonus:** +1 Flux
- **Tier V bonus:** +2 Flux

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| transmuter_start | Transmuter | start | - | - | - | +1 to every stat |
| transmuter_malleable | Malleable | small | 1 | 1 | transmuter_start | +2 Optimization amount |
| transmuter_stable_form | Stable Form | small | 1 | 1 | transmuter_start | Optimization no longer resets on death |
| transmuter_iron_gut | Iron Gut | notable | 2 | 12 | transmuter_malleable | harmful potions last 30% shorter |
| transmuter_rapid_shift | Rapid Shift | notable | 2 | 15 | transmuter_stable_form | you can re-Optimize once per day without sleeping |
| transmuter_perfected | Perfected | keystone | 3 | 30 | transmuter_iron_gut, transmuter_rapid_shift | +2 to every stat, but potions last 30% shorter |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Optimization | start | - | - | - | - |
| transmute | Transmute | small | 1 | 1 | start | +2 Flux |
| alchemical_body | Alchemical Body | small | 1 | 5 | start | +10% potion duration |
| alter_self | Alter Self | notable | 2 | 15 | transmute | unlocks Alter Self ability |
| transmute_matter | Transmute Matter | notable | 2 | 20 | alchemical_body | unlocks Transmute ability |
| philosopher | Philosopher | keystone | 3 | 35 | alter_self, transmute_matter | Optimization amount doubled; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| transmuter_stat_start | Transmuter | start | - | - | - | - |
| transmuter_changeable | Changeable | small | 2 | 1 | transmuter_stat_start | +1 Agility, +1 Intelligence, +1 Wisdom |
| transmuter_balanced | Balanced | small | 2 | 5 | transmuter_changeable | +1 Vitality, +1 Strength, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Alter Self** (re-Optimize instantly, 50 Alteration), **Transmute** (turn a stack of one material into a similar one - e.g. cobblestone to stone, 30 Alteration).
- Book talents: Optimization, +1 to each attribute at level 10. Tradition: Alteration.
