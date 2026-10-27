# Class: Thief

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Thief' (p. 73). -->

## 1. Basics

- **Id:** thief
- **Display name:** Thief
- **Description:** A light-fingered burglar who gets more out of every chest and mob, climbs anything and dodges trouble.
- **Icon item:** chest
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 2 |
| Strength     | 1 |
| Dexterity    | 5 |
| Agility      | 5 |
| Intelligence | 2 |
| Wisdom       | 0 |
| Endurance    | 1 |
| Luck         | 4 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Finesse
- **Description:** Quick fingers and a light touch.
- **What each point gives:** +0.3% chance for one extra item from mob loot
- **Max points:** 100

### Resource bar

- **Name:** Nerve
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** coin gold (#E0B23A)

## 4. Starting kit

- 1 × stone_sword
- 1 × bow
- 16 × arrow
- 1 × leather_chestplate
- 1 × bundle
- 6 × bread

## 5. Passives

- Pick Pockets: +1 Looting level on all kills
- Open Locks: the first time you open a naturally generated loot chest, 15% chance of one extra roll of loot
- Trap Sense: you never trigger tripwires or pressure plates, and take -50% damage from TNT traps
- Dodge: 8% chance to completely avoid a melee hit

### Special passives

- +1% Dodge chance per 5 Finesse
- Open Locks chance +0.5% per Finesse point

## 6. Weakness

- **What players read:** Word gets around about thieves.
- **What it actually does:** villager trades cost 20% more, and iron golems turn hostile if they see you open a chest inside a village

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
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 2 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Backstab

- Shared skill - full definition in `rogue.md`

### Skill: Thievery

- **Shared or class-only:** class-only
- **Trained by:** opening naturally generated loot chests for the first time and killing mobs with Looting
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Finesse
- **Tier II bonus:** +1 Finesse
- **Tier III bonus:** +10% Nerve regen
- **Tier IV bonus:** +1 Finesse
- **Tier V bonus:** +2 Finesse

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom of the web (Trickery quarter), ring 2, outer edge

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| thief_start | Thief | start | - | - | - | +1 Luck |
| thief_sticky | Sticky Fingers | small | 1 | 1 | thief_start | +5% extra mob loot chance |
| thief_quiet | Quiet Steps | small | 1 | 1 | thief_start | +10% sneaking speed |
| thief_climber | Scale Walls | notable | 2 | 10 | thief_quiet | you can climb any wall like a ladder while sneaking and holding jump |
| thief_fence | Fence | notable | 2 | 12 | thief_sticky | cancels the villager price penalty from your weakness |
| thief_master | Master Thief | keystone | 3 | 25 | thief_climber, thief_fence | +1 more Looting level, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Thievery | start | - | - | - | - |
| escape_artist | Escape Artist | small | 1 | 1 | start | +2 Finesse; cobwebs don't slow you |
| hide_in_shadows | Hide in Shadows | small | 1 | 5 | start | -30% mob detection range in light level 7 or lower |
| keen_senses | Keen Senses | notable | 2 | 15 | escape_artist | chests with loot within 8 blocks sparkle while you sneak |
| dodge | Dodge | notable | 2 | 20 | hide_in_shadows | +6% Dodge chance |
| opportunist | Opportunist | keystone | 3 | 35 | keen_senses, dodge | when a mob within 3 blocks takes damage from another player, your next hit on it is a guaranteed critical; -10% max Nerve |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| thief_stat_start | Thief | start | - | - | - | - |
| thief_deft | Deft | small | 2 | 1 | thief_stat_start | +2 Dexterity, +1 Agility |
| thief_fortune | Fortune | small | 2 | 5 | thief_deft | +2 Luck, +1 Agility |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Pickpocket** (steal one item from a mob's hand/armor, 20 Nerve), **Escape** (break free of slowness/cobweb/being held, 10 Nerve).
- Book thievery talents mapped: Escape Artist, Hide in Shadows, Keen Senses, Move Silently (Stealth skill), Open Locks, Pick Pockets, Scale Walls, Trap Sense.
- **Decided:** Finesse + Nerve. The villager/iron golem weakness stays.
