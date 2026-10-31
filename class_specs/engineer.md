# Class: Engineer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Engineer' (p. 86). -->

## 1. Basics

- **Id:** engineer
- **Display name:** Engineer
- **Description:** Builder of the eidolon: a giant mechanical servant that fights at their side - and eventually carries them inside.
- **Icon item:** iron_block
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.1 |
| Strength     | 3 | x0.9 |
| Dexterity    | 4 | x1.1 |
| Agility      | 1 | x0.9 |
| Intelligence | 8 | x1.25 |
| Wisdom       | 2 | x1 |
| Endurance    | 3 | x1 |
| Luck         | 1 | x0.75 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Engineering
- **Description:** Mastery over your great machine.
- **What each point gives:** +1% eidolon damage and max health
- **Max points:** 100

### Resource bar

- **Name:** Fuel
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** brass (#C9A13A)

## 4. Starting kit

- 1 × iron_block
- 4 × redstone
- 1 × crossbow
- 8 × arrow
- 1 × chainmail_chestplate
- 4 × bread

## 5. Passives

- Eidolon: you own a personal custom construct mob, the Eidolon (50 HP, follows you, fights your targets - see section 12 for its mob spec). Rebuild after death with 4 iron blocks + 8 hours
- Spare Parts: right-clicking the eidolon with an iron ingot heals it 10 HP
- +10% crossbow damage

### Special passives

- Eidolon armor +1 per 10 Engineering
- Per 20 Engineering, eidolon +5% movement speed

## 6. Weakness

- **What players read:** Without the machine, an engineer is a nerd with a crossbow.
- **What it actually does:** -20% damage and -2 armor while your eidolon is destroyed

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
| Shields | neutral |
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
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Smithing

- Shared skill - full definition in `artificer.md`

### Skill: Mechanics

- **Shared or class-only:** class-only
- **Trained by:** your eidolon dealing damage and repairing it
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Engineering
- **Tier II bonus:** +1 Engineering
- **Tier III bonus:** +10% Fuel regen
- **Tier IV bonus:** +1 Engineering
- **Tier V bonus:** +2 Engineering

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Artificer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| engineer_start | Engineer | start | - | - | - | eidolon +10 max health |
| engineer_reinforced_frame | Reinforced Frame | small | 1 | 1 | engineer_start | eidolon +2 armor |
| engineer_pistons | Pistons | small | 1 | 1 | engineer_start | eidolon +10% damage |
| engineer_mighty_eidolon | Mighty Eidolon | notable | 2 | 12 | engineer_reinforced_frame | eidolon +25 max health |
| engineer_servo_speed | Servo Speed | notable | 2 | 15 | engineer_pistons | eidolon +15% movement speed |
| engineer_iron_titan | Iron Titan | keystone | 3 | 30 | engineer_mighty_eidolon, engineer_servo_speed | eidolon becomes Size 2 (bigger, +50% stats) but it moves 20% slower |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Eidolon | start | - | - | - | - |
| schematics | Schematics | small | 1 | 1 | start | +2 Engineering |
| field_repairs | Field Repairs | small | 1 | 5 | start | Spare Parts heal 15 HP |
| cockpit | Cockpit | notable | 2 | 15 | schematics | unlocks Cockpit ability |
| overdrive | Overdrive | notable | 2 | 20 | field_repairs | unlocks Overdrive ability |
| mecha | Mecha | keystone | 3 | 35 | cockpit, overdrive | while in the cockpit, eidolon damage +25%; you can't use your own weapons outside it for 10s after leaving |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| engineer_stat_start | Engineer | start | - | - | - | - |
| engineer_technical | Technical | small | 2 | 1 | engineer_stat_start | +2 Intelligence, +1 Dexterity |
| engineer_workshop_strong | Workshop Strong | small | 2 | 5 | engineer_technical | +2 Strength, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Cockpit** (climb into your eidolon and pilot it, book level 10), **Overdrive** (eidolon +50% speed/damage for 10s, 40 Fuel).
- Book talents: Eidolon, Spare Parts, Cockpit, Mighty Eidolon.
- **Custom mob spec - `eidolon`:** humanoid construct ~3.5 blocks tall (book: 12 ft, 2,000 lb, Size 2). 50 max health, 10 armor, 12 melee damage (appendage slam, knockback), movement speed 0.25 (slow), knockback resistance 100%, can't swim (sinks, doesn't drown). Immune to poison, wither, fire-spread, fall damage under 10 blocks, and all mind effects (no Nausea/Blindness/Darkness). Never despawns, has an owner (UUID), follows within 12 blocks, sits on sneak+right-click. Becomes an inert 'broken eidolon' block/item when killed instead of dropping loot. Book stats: Defense 11, Strength 16, Agility 8, Speed 6.
- Eidolon upgrades from the trees (Reinforced Frame, Pistons, Mighty Eidolon, Iron Titan) and the Cockpit ability modify this mob, so it needs attributes the class can change at runtime.
- **Decided:** The Eidolon is its own custom mob, not a re-skinned iron golem.
