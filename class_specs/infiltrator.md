# Class: Infiltrator

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Infiltrator' (p. 90). -->

## 1. Basics

- **Id:** infiltrator
- **Display name:** Infiltrator
- **Description:** A master spy who can hide in plain sight, be forgotten moments later and strike from nowhere.
- **Icon item:** leather_helmet
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.9 |
| Strength     | 2 | x1 |
| Dexterity    | 6 | x1.1 |
| Agility      | 6 | x1.25 |
| Intelligence | 4 | x1.1 |
| Wisdom       | 1 | x0.9 |
| Endurance    | 1 | x0.75 |
| Luck         | 3 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Subtlety
- **Description:** The art of not being there.
- **What each point gives:** -0.4% mob detection range
- **Max points:** 100

### Resource bar

- **Name:** Cover
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** spy charcoal (#33363B)

## 4. Starting kit

- 1 × stone_sword
- 1 × leather_helmet
- 1 × leather_chestplate
- 1 × potion of invisibility
- 4 × bread

## 5. Passives

- Faceless: while sneaking and not attacking, hostile mobs beyond 4 blocks can't see you at all
- Treacherous Strike: +40% damage to mobs that weren't targeting you
- Vanish: breaking line of sight with a mob makes it forget you after 2s
- Your nameplate is hidden from other players while sneaking or invisible

### Special passives

- Faceless distance -0.05 blocks per Subtlety
- Per 10 Subtlety, +3% Treacherous Strike

## 6. Weakness

- **What players read:** Infiltrators avoid notice - and avoid armor that clanks.
- **What it actually does:** while wearing any medium or heavy armor, Faceless doesn't work

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
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

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Backstab

- Shared skill - full definition in `rogue.md`

### Skill: Espionage

- **Shared or class-only:** class-only
- **Trained by:** sneaking past hostile mobs within 4 blocks unnoticed
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Subtlety
- **Tier II bonus:** +1 Subtlety
- **Tier III bonus:** +10% Cover regen
- **Tier IV bonus:** +1 Subtlety
- **Tier V bonus:** +2 Subtlety

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Scout

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| infiltrator_start | Infiltrator | start | - | - | - | -5% detection range |
| infiltrator_grey_man | Grey Man | small | 1 | 1 | infiltrator_start | -10% detection range |
| infiltrator_shadow_step | Shadow Step | small | 1 | 1 | infiltrator_start | +10% sneaking speed |
| infiltrator_ambush | Ambush | notable | 2 | 12 | infiltrator_grey_man | first round of combat: +20% damage and speed |
| infiltrator_forgettable | Forgettable | notable | 2 | 15 | infiltrator_shadow_step | mobs forget you 1s faster |
| infiltrator_no_one | No One | keystone | 3 | 30 | infiltrator_ambush, infiltrator_forgettable | Faceless works within 2 blocks, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Faceless | start | - | - | - | - |
| disguise | Disguise | small | 1 | 1 | start | +2 Subtlety |
| quiet | Quiet | small | 1 | 5 | start | no footstep sounds |
| treacherous_strike | Treacherous Strike | notable | 2 | 15 | disguise | +15% more Treacherous Strike damage |
| vanish | Vanish | notable | 2 | 20 | quiet | unlocks Vanish ability |
| ghost_agent | Ghost Agent | keystone | 3 | 35 | treacherous_strike, vanish | after a stealth kill, you are invisible for 3s; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| infiltrator_stat_start | Infiltrator | start | - | - | - | - |
| infiltrator_unremarkable | Unremarkable | small | 2 | 1 | infiltrator_stat_start | +2 Agility, +1 Luck |
| infiltrator_sharp_mind | Sharp Mind | small | 2 | 5 | infiltrator_unremarkable | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Vanish** (instantly hide even while seen, 30 Cover).
- Book talents: Faceless, Treacherous Strike, Vanish.
