# Class: Abjurer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Abjurer' (p. 79). -->

## 1. Basics

- **Id:** abjurer
- **Display name:** Abjurer
- **Description:** A paranoid master of Protection magic who wraps themself in wards. Very hard to hurt, slow to kill anything.
- **Icon item:** shield
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 1 | x1 |
| Dexterity    | 1 | x0.85 |
| Agility      | 1 | x0.85 |
| Intelligence | 7 | x1.3 |
| Wisdom       | 6 | x1.15 |
| Endurance    | 6 | x1.15 |
| Luck         | 1 | x0.7 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Warding
- **Description:** The layered protections you keep around yourself at all times.
- **What each point gives:** +0.1 armor and +0.05 armor toughness
- **Max points:** 100

### Resource bar

- **Name:** Ward
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** ice white (#DDEFFF)

## 4. Starting kit

- 1 × shield
- 1 × chainmail_chestplate
- 1 × splash_potion of resistance
- 6 × bread

## 5. Passives

- Guarded Casting: whenever you spend Ward, gain +3 armor for 10s
- Warded: mobs deal 10% less damage to you and your potion debuffs last 20% shorter
- -15% magic damage taken

### Special passives

- -0.2% damage taken per Warding point (max -20%)
- Guarded Casting armor +1 per 20 Warding

## 6. Weakness

- **What players read:** Abjurers trust nothing - not even their own blades.
- **What it actually does:** -20% damage with all weapons

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
| Shields | proficient |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Abjuration

- **Shared or class-only:** class-only
- **Trained by:** taking damage while Ward is above half
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Warding
- **Tier II bonus:** +1 Warding
- **Tier III bonus:** +10% Ward regen
- **Tier IV bonus:** +1 Warding
- **Tier V bonus:** +2 Warding

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| abjurer_start | Abjurer | start | - | - | - | -5% damage taken |
| abjurer_thick_wards | Thick Wards | small | 1 | 1 | abjurer_start | +2 armor |
| abjurer_suspicion | Suspicion | small | 1 | 1 | abjurer_start | +10% knockback resistance |
| abjurer_spellguard | Spellguard | notable | 2 | 12 | abjurer_thick_wards | -15% magic damage taken |
| abjurer_sanctum | Sanctum | notable | 2 | 15 | abjurer_suspicion | +20% regeneration while standing still |
| abjurer_fortress_of_one | Fortress of One | keystone | 3 | 30 | abjurer_spellguard, abjurer_sanctum | -25% damage taken, but -25% damage dealt |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Protection | start | - | - | - | - |
| layered_ward | Layered Ward | small | 1 | 1 | start | +2 Warding |
| quick_ward | Quick Ward | small | 1 | 5 | start | Guarded Casting lasts 5s longer |
| warded | Warded | notable | 2 | 15 | layered_ward | +5% less damage from mobs |
| ward_allies | Ward Allies | notable | 2 | 20 | quick_ward | Guarded Casting also gives +2 armor to players within 6 blocks |
| inviolable | Inviolable | keystone | 3 | 35 | warded, ward_allies | immune to knockback while Ward is above half; -10% movement speed |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| abjurer_stat_start | Abjurer | start | - | - | - | - |
| abjurer_watchful | Watchful | small | 2 | 1 | abjurer_stat_start | +2 Endurance, +1 Wisdom |
| abjurer_studious_guard | Studious Guard | small | 2 | 5 | abjurer_watchful | +2 Intelligence, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Ward** (absorb the next 8 damage, 20 Ward), **Sanctuary** (mobs can't enter a 3-block bubble for 5s, 40 Ward).
- Book talents: Guarded Casting, Warded. Tradition: Protection.
