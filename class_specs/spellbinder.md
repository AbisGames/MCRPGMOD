# Class: Spellbinder

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Spellbinder' (p. 72). -->

## 1. Basics

- **Id:** spellbinder
- **Display name:** Spellbinder
- **Description:** A warrior-mage who binds magic into one weapon - it can't be lost, burns with eldritch fire and hits like a spell.
- **Icon item:** amethyst_shard
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1.05 |
| Strength     | 4 | x1.05 |
| Dexterity    | 3 | x1 |
| Agility      | 2 | x1 |
| Intelligence | 4 | x1.15 |
| Wisdom       | 2 | x0.95 |
| Endurance    | 2 | x0.95 |
| Luck         | 0 | x0.85 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Binding
- **Description:** The strength of the bond between you and your weapon.
- **What each point gives:** +0.6% damage with your bound weapon
- **Max points:** 100

### Resource bar

- **Name:** Eldritch Charge
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** eldritch cyan (#3FE0E0)

## 4. Starting kit

- 1 × iron_sword
- 1 × leather_chestplate
- 4 × lapis_lazuli
- 6 × bread

## 5. Passives

- Spellbound Weapon: the weapon in hotbar slot 1 is your bound weapon: +10% damage with it, it never drops on death, and it loses durability 50% slower
- Your bound weapon counts as a magic focus: its hits deal 15% of their damage as magic damage (ignores armor)
- Invest Power: while Eldritch Charge is full, your bound weapon glows and lights the area around you

### Special passives

- Bound weapon durability loss -1% per 2 Binding
- Magic Weapon: per 10 Binding, +2% bound weapon critical damage

## 6. Weakness

- **What players read:** A spellbinder's magic only lives in one blade.
- **What it actually does:** -20% damage with any weapon that isn't your bound weapon

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Weaponbond

- **Shared or class-only:** class-only
- **Trained by:** killing mobs with your bound weapon
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Binding
- **Tier II bonus:** +1 Binding
- **Tier III bonus:** +10% Eldritch Charge regen
- **Tier IV bonus:** +1 Binding
- **Tier V bonus:** +2 Binding

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** left side of the web (War quarter), ring 2, leaning toward the Power quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| spellbinder_start | Spellbinder | start | - | - | - | +3% bound weapon damage |
| spellbinder_rune_edge | Rune Edge | small | 1 | 1 | spellbinder_start | +5% magic damage portion |
| spellbinder_warded | Warded | small | 1 | 1 | spellbinder_start | -8% magic damage taken |
| spellbinder_eldritch | Eldritch Flames | notable | 2 | 10 | spellbinder_rune_edge | bound weapon hits set mobs on fire for 2s while Charge is above half |
| spellbinder_battle_mage | Battle Mage | notable | 2 | 12 | spellbinder_warded | +1 armor, +5% attack speed |
| spellbinder_soulblade | Soulblade | keystone | 3 | 25 | spellbinder_eldritch, spellbinder_battle_mage | bound weapon damage +20%, but you can't hold any other weapon in your main hand without -50% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Spellbound Weapon | start | - | - | - | - |
| recall | Recall | small | 1 | 1 | start | +2 Binding; unlocks Recall Weapon ability |
| mend | Mend | small | 1 | 5 | start | bound weapon slowly repairs 1 durability every 10s |
| invest_power | Invest Power | notable | 2 | 15 | recall | unlocks Invest Power ability |
| sacrifice | Sacrifice | notable | 2 | 20 | mend | unlocks Sacrifice ability |
| magic_weapon | Magic Weapon | keystone | 3 | 35 | invest_power, sacrifice | the magic damage portion of your hits rises to 35%; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| spellbinder_stat_start | Spellbinder | start | - | - | - | - |
| spellbinder_arcane_arm | Arcane Arm | small | 2 | 1 | spellbinder_stat_start | +2 Strength, +1 Intelligence |
| spellbinder_focused | Focused | small | 2 | 5 | spellbinder_arcane_arm | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Recall Weapon** (teleport your bound weapon back to your hand, 10 Charge), **Invest Power** (+30% bound damage for 20s, 20 Charge), **Sacrifice** (spend all Charge: next hit deals +1 damage per 3 Charge).
- Book talents: spellbound weapon spell (implement, boon, recall, repair, sacrifice), Invest Power, Magic Weapon.
- **Co-design question:** Binding + Eldritch Charge. 'Bound weapon = hotbar slot 1' is a simple rule - would you prefer a ritual that binds a specific item instead?
