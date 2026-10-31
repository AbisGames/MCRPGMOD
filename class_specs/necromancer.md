# Class: Necromancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Necromancer' (p. 93). -->

## 1. Basics

- **Id:** necromancer
- **Display name:** Necromancer
- **Description:** A master of death magic who commands the undead and refuses to die.
- **Icon item:** skeleton_skull
- **Selectable by players:** yes
- **Base health:** 26

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x1 |
| Strength     | 0 | x0.65 |
| Dexterity    | 1 | x0.85 |
| Agility      | 1 | x0.85 |
| Intelligence | 9 | x1.15 |
| Wisdom       | 8 | x1.35 |
| Endurance    | 4 | x1.15 |
| Luck         | 1 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Deathgrip
- **Description:** Your hold over the boundary of death.
- **What each point gives:** +1% damage and health for undead you command
- **Max points:** 100

### Resource bar

- **Name:** Necrosis
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** grave green (#3C6B4A)

## 4. Starting kit

- 1 × stone_sword
- 4 × bone
- 4 × rotten_flesh
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Command Undead: hitting a zombie or skeleton that isn't targeting you has a 20% chance to make it your servant for 60s (max 2)
- Inured to Death: immune to Poison, Hunger and Wither, and rotten flesh is normal food for you
- Undead mobs ignore you unless you attack them

### Special passives

- Command chance +0.3% per Deathgrip
- Per 20 Deathgrip, +1 servant

## 6. Weakness

- **What players read:** The living sense the grave on you.
- **What it actually does:** Regeneration and Instant Health are 50% weaker on you, and villagers won't trade with you

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
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Necromancy

- **Shared or class-only:** class-only
- **Trained by:** your undead servants killing mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Deathgrip
- **Tier II bonus:** +1 Deathgrip
- **Tier III bonus:** +10% Necrosis regen
- **Tier IV bonus:** +1 Deathgrip
- **Tier V bonus:** +2 Deathgrip

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Warlock

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| necromancer_start | Necromancer | start | - | - | - | +5% servant damage |
| necromancer_grave_dust | Grave Dust | small | 1 | 1 | necromancer_start | +5% command chance |
| necromancer_bone_armor | Bone Armor | small | 1 | 1 | necromancer_start | +2 armor |
| necromancer_master_of_undeath | Master of Undeath | notable | 2 | 12 | necromancer_grave_dust | servants +20% damage |
| necromancer_lich_touch | Lich Touch | notable | 2 | 15 | necromancer_bone_armor | your hits give Wither I for 2s |
| necromancer_lichdom | Lichdom | keystone | 3 | 30 | necromancer_master_of_undeath, necromancer_lich_touch | +1 servant and +20% servant damage, but sunlight burns you like a zombie |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Command Undead | start | - | - | - | - |
| death_s_whisper | Death's Whisper | small | 1 | 1 | start | +2 Deathgrip |
| long_leash | Long Leash | small | 1 | 5 | start | servants last 30s longer |
| master_of_undeath | Master of Undeath | notable | 2 | 15 | death_s_whisper | double servant cap |
| raise_dead | Raise Dead | notable | 2 | 20 | long_leash | unlocks Raise Dead ability |
| lord_of_bones | Lord of Bones | keystone | 3 | 35 | master_of_undeath, raise_dead | servants never expire until killed; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| necromancer_stat_start | Necromancer | start | - | - | - | - |
| necromancer_morbid_intellect | Morbid Intellect | small | 2 | 1 | necromancer_stat_start | +2 Intelligence, +1 Endurance |
| necromancer_cold_will | Cold Will | small | 2 | 5 | necromancer_morbid_intellect | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Raise Dead** (a mob you kill rises as a servant zombie/skeleton, 40 Necrosis).
- Book talents: Inured to Death, Command Undead, Master of Undeath. Tradition: Necromancy.
