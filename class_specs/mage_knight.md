# Class: Mage Knight

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Mage Knight' (p. 91). -->

## 1. Basics

- **Id:** mage_knight
- **Display name:** Mage Knight
- **Description:** A warrior who weaves Battle magic between sword strokes. Enemies never know if a blade or a blast is coming.
- **Icon item:** blaze_rod
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1 |
| Strength     | 6 | x1.25 |
| Dexterity    | 3 | x1 |
| Agility      | 2 | x0.85 |
| Intelligence | 5 | x1.15 |
| Wisdom       | 3 | x0.85 |
| Endurance    | 3 | x1.15 |
| Luck         | 0 | x0.75 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Battle Arcana
- **Description:** Magic trained for war.
- **What each point gives:** +0.5% weapon damage and +0.5% magic damage
- **Max points:** 100

### Resource bar

- **Name:** Warfire
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** battle orange (#E86A17)

## 4. Starting kit

- 1 × iron_sword
- 1 × chainmail_chestplate
- 1 × blaze_rod
- 6 × bread

## 5. Passives

- Escalating Violence: after spending Warfire, your next 3 melee hits deal +20% damage
- Mage Knight Tactics: your melee hits refill 2 Warfire
- Your weapon hits deal 10% of their damage as magic damage (ignores armor)

### Special passives

- Escalating Violence +0.5% per Battle Arcana
- Per 10 Battle Arcana, +1 escalating hit

## 6. Weakness

- **What players read:** Splitting focus between spell and steel.
- **What it actually does:** -10% max health and -10% potion duration

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 1 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: War Magic

- **Shared or class-only:** class-only
- **Trained by:** hitting mobs while Escalating Violence is active
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Battle Arcana
- **Tier II bonus:** +1 Battle Arcana
- **Tier III bonus:** +10% Warfire regen
- **Tier IV bonus:** +1 Battle Arcana
- **Tier V bonus:** +2 Battle Arcana

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Spellbinder

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| mage_knight_start | Mage Knight | start | - | - | - | +3% weapon damage |
| mage_knight_arcane_steel | Arcane Steel | small | 1 | 1 | mage_knight_start | +5% magic portion |
| mage_knight_drilled_caster | Drilled Caster | small | 1 | 1 | mage_knight_start | +1 armor |
| mage_knight_spell_strike | Spell Strike | notable | 2 | 12 | mage_knight_arcane_steel | Escalating Violence +10% |
| mage_knight_combat_casting | Combat Casting | notable | 2 | 15 | mage_knight_drilled_caster | taking damage doesn't drain Warfire |
| mage_knight_battlemage | Battlemage | keystone | 3 | 30 | mage_knight_spell_strike, mage_knight_combat_casting | +20% weapon and magic damage, but you can't use shields |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Escalating Violence | start | - | - | - | - |
| battle_hymn | Battle Hymn | small | 1 | 1 | start | +2 Battle Arcana |
| charged_blade | Charged Blade | small | 1 | 5 | start | first escalating hit also knocks back |
| mage_knight_tactics | Mage Knight Tactics | notable | 2 | 15 | battle_hymn | +2 more Warfire per hit |
| spellguard | Spellguard | notable | 2 | 20 | charged_blade | -15% magic damage taken |
| war_wizard | War Wizard | keystone | 3 | 35 | mage_knight_tactics, spellguard | Escalating Violence also gives Speed I; -10% max Warfire |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| mage_knight_stat_start | Mage Knight | start | - | - | - | - |
| mage_knight_martial_mind | Martial Mind | small | 2 | 1 | mage_knight_stat_start | +2 Strength, +1 Intelligence |
| mage_knight_spellsword | Spellsword | small | 2 | 5 | mage_knight_martial_mind | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Battle Spell** (a short-range blast that leads into Escalating Violence, 20 Warfire).
- Book talents: Escalating Violence, Mage Knight Tactics. Tradition: Battle.
