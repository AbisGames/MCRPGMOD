# Class: Runesmith

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Runesmith' (p. 94). -->

## 1. Basics

- **Id:** runesmith
- **Display name:** Runesmith
- **Description:** A rune-carver taught by the dwarfs who inscribes glowing sigils on weapons and armor.
- **Icon item:** coast_armor_trim_smithing_template
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.1 |
| Strength     | 4 | x1 |
| Dexterity    | 2 | x0.9 |
| Agility      | 1 | x0.9 |
| Intelligence | 7 | x1.2 |
| Wisdom       | 5 | x1.1 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Runecraft
- **Description:** Skill at carving and charging runes.
- **What each point gives:** +0.5% damage with sigil-marked weapons and +0.05 armor with sigil-marked armor
- **Max points:** 100

### Resource bar

- **Name:** Rune Power
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** rune blue (#3FA9F5)

## 4. Starting kit

- 1 × iron_axe
- 1 × chainmail_chestplate
- 1 × smithing_template (any trim)
- 4 × bread

## 5. Passives

- Sigils of Power: armor with a trim and weapons you've renamed at an anvil count as sigil-marked
- Sigil weapons: +10% damage; sigil armor: +1 armor per piece
- Mighty Sigils: while Rune Power is full, sigil weapons glow and deal +10% more

### Special passives

- Sigil weapon damage +0.5% per Runecraft
- Per 20 Runecraft, +1 armor toughness from sigil armor

## 6. Weakness

- **What players read:** Runes need marked gear. Plain gear is dead weight.
- **What it actually does:** -15% damage with unmarked weapons and -1 armor per unmarked armor piece

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | mastery |
| Spears | proficient |
| Maces | mastery |
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

### Skill: Smithing

- Shared skill - full definition in `artificer.md`

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Runecarving

- **Shared or class-only:** class-only
- **Trained by:** applying armor trims and renaming weapons at anvils
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Runecraft
- **Tier II bonus:** +1 Runecraft
- **Tier III bonus:** +10% Rune Power regen
- **Tier IV bonus:** +1 Runecraft
- **Tier V bonus:** +2 Runecraft

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Artificer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| runesmith_start | Runesmith | start | - | - | - | +1 armor |
| runesmith_deep_cuts | Deep Cuts | small | 1 | 1 | runesmith_start | +5% sigil weapon damage |
| runesmith_etched_plate | Etched Plate | small | 1 | 1 | runesmith_start | +1 armor per sigil piece |
| runesmith_runes_of_return | Runes of Return | notable | 2 | 12 | runesmith_deep_cuts | sigil weapons lose durability 30% slower |
| runesmith_ward_rune | Ward Rune | notable | 2 | 15 | runesmith_etched_plate | -10% magic damage with 4 sigil pieces |
| runesmith_runelord | Runelord | keystone | 3 | 30 | runesmith_runes_of_return, runesmith_ward_rune | sigil bonuses doubled, but unmarked gear is forbidden-tier |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Sigils of Power | start | - | - | - | - |
| chisel | Chisel | small | 1 | 1 | start | +2 Runecraft |
| fire_rune | Fire Rune | small | 1 | 5 | start | sigil weapon hits ignite for 2s |
| mighty_sigils | Mighty Sigils | notable | 2 | 15 | chisel | Mighty Sigils triggers at 75% Rune Power |
| thunder_rune | Thunder Rune | notable | 2 | 20 | fire_rune | unlocks Rune Blast ability |
| dwarfen_mastery | Dwarfen Mastery | keystone | 3 | 35 | mighty_sigils, thunder_rune | anvil costs halved for you; -10% movement speed |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| runesmith_stat_start | Runesmith | start | - | - | - | - |
| runesmith_steady_carver | Steady Carver | small | 2 | 1 | runesmith_stat_start | +2 Dexterity, +1 Intelligence |
| runesmith_forge_strong | Forge-Strong | small | 2 | 5 | runesmith_steady_carver | +2 Strength, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Rune Blast** (detonate a sigil for an AoE burst, 40 Rune Power).
- Book talents: Sigils of Power, Mighty Sigils. Tradition: Rune. Armor trims as 'sigils' is my Minecraft mapping.
