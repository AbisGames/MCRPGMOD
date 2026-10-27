# Class: Duelist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Duelist' (p. 86). -->

## 1. Basics

- **Id:** duelist
- **Display name:** Duelist
- **Description:** A one-on-one specialist who challenges a single foe, pins them down and ripostes every mistake.
- **Icon item:** iron_sword
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 4 |
| Dexterity    | 7 |
| Agility      | 5 |
| Intelligence | 2 |
| Wisdom       | 1 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Finesse
- **Description:** Single-combat precision.
- **What each point gives:** +0.5% damage against your challenged foe
- **Max points:** 100

### Resource bar

- **Name:** Panache
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** fencing white (#F2F2F2)

## 4. Starting kit

- 1 × iron_sword
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Challenge: the first mob you hit becomes your challenged foe for 30s; it deals 20% less damage to anyone but you
- Riposte: when your challenged foe attacks you and misses or is blocked, you counterattack instantly
- Duel Mastery: +20% damage against your challenged foe

### Special passives

- Riposte chance on any hit from the foe: +0.3% per Finesse
- Per 10 Finesse, +5% attack speed against the foe

## 6. Weakness

- **What players read:** Duelists fight one enemy at a time. Crowds overwhelm them.
- **What it actually does:** -5% damage and -1 armor for each hostile mob beyond the first within 6 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | neutral |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
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

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Single Combat

- **Shared or class-only:** class-only
- **Trained by:** killing challenged foes
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Finesse
- **Tier II bonus:** +1 Finesse
- **Tier III bonus:** +10% Panache regen
- **Tier IV bonus:** +1 Finesse
- **Tier V bonus:** +2 Finesse

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| duelist_start | Duelist | start | - | - | - | +3% sword damage |
| duelist_en_garde | En Garde | small | 1 | 1 | duelist_start | +5% damage to challenged foe |
| duelist_footwork | Footwork | small | 1 | 1 | duelist_start | +5% movement speed |
| duelist_bind | Bind | notable | 2 | 12 | duelist_en_garde | challenged foe gets Slowness I |
| duelist_parry | Parry | notable | 2 | 15 | duelist_footwork | 10% chance to parry melee hits |
| duelist_lone_wolf | Lone Wolf | keystone | 3 | 30 | duelist_bind, duelist_parry | Duel bonuses doubled, but you can't damage non-challenged mobs for 3s after challenging |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Challenge | start | - | - | - | - |
| salute | Salute | small | 1 | 1 | start | +2 Finesse |
| lunge | Lunge | small | 1 | 5 | start | +1 block reach against challenged foe |
| riposte | Riposte | notable | 2 | 15 | salute | riposte hits are always critical |
| duel_mastery | Duel Mastery | notable | 2 | 20 | lunge | +10% more damage to challenged foe |
| flawless | Flawless | keystone | 3 | 35 | riposte, duel_mastery | while the foe hasn't hit you, +25% damage; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| duelist_stat_start | Duelist | start | - | - | - | - |
| duelist_quick_blade | Quick Blade | small | 2 | 1 | duelist_stat_start | +2 Dexterity, +1 Agility |
| duelist_cool_head | Cool Head | small | 2 | 5 | duelist_quick_blade | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Challenge, Riposte, Duel Mastery.
