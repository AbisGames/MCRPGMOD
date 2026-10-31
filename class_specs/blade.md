# Class: Blade

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Blade' (p. 82). -->

## 1. Basics

- **Id:** blade
- **Display name:** Blade
- **Description:** A master knife-fighter whose quick, precise strikes leave bleeding wounds.
- **Icon item:** iron_sword
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1 |
| Strength     | 3 | x1 |
| Dexterity    | 8 | x1.3 |
| Agility      | 6 | x1.15 |
| Intelligence | 1 | x0.85 |
| Wisdom       | 0 | x0.7 |
| Endurance    | 2 | x0.85 |
| Luck         | 2 | x1.15 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Edge
- **Description:** Speed and precision with short blades.
- **What each point gives:** +0.5% sword attack speed
- **Max points:** 100

### Resource bar

- **Name:** Bloodletting
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** dark red (#7E0F15)

## 4. Starting kit

- 1 × iron_sword
- 1 × leather_chestplate
- 1 × leather_boots
- 6 × bread

## 5. Passives

- Bleed: critical sword hits cause bleeding - the mob takes 1 damage per second for 4s
- +10% sword attack speed
- Swift Blade: holding a sword in your off-hand lets you hit with it too (off-hand swing every 2nd attack)

### Special passives

- +0.1s bleed duration per 2 Edge
- Bleeding mobs take +0.25% more damage from you per Edge

## 6. Weakness

- **What players read:** Knives are poor against armor and size.
- **What it actually does:** -20% damage against mobs with natural armor (iron golems, ravagers, wardens, the Wither) and with any weapon that isn't a sword

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
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
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Backstab

- Shared skill - full definition in `rogue.md`

### Skill: Knifework

- **Shared or class-only:** class-only
- **Trained by:** making mobs bleed
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Edge
- **Tier II bonus:** +1 Edge
- **Tier III bonus:** +1s bleed duration
- **Tier IV bonus:** +1 Edge
- **Tier V bonus:** +2 Edge

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Assassin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| blade_start | Blade | start | - | - | - | +3% sword damage |
| blade_quick_cuts | Quick Cuts | small | 1 | 1 | blade_start | +6% sword attack speed |
| blade_serrated | Serrated | small | 1 | 1 | blade_start | +1 bleed damage per tick on criticals |
| blade_arterial_strike | Arterial Strike | notable | 2 | 12 | blade_quick_cuts | bleeding mobs are Slowed |
| blade_close_quarters | Close Quarters | notable | 2 | 15 | blade_serrated | +10% damage to mobs within 2 blocks |
| blade_thousand_cuts | Thousand Cuts | keystone | 3 | 30 | blade_arterial_strike, blade_close_quarters | every hit (not just crits) causes bleeding, but -20% sword damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Bleed | start | - | - | - | - |
| keen | Keen | small | 1 | 1 | start | +2 Edge |
| twin_fangs | Twin Fangs | small | 1 | 5 | start | off-hand swing every attack instead of every 2nd |
| hemorrhage | Hemorrhage | notable | 2 | 15 | keen | bleed stacks up to 3 times |
| swift_blade | Swift Blade | notable | 2 | 20 | twin_fangs | +10% sword attack speed |
| red_mist | Red Mist | keystone | 3 | 35 | hemorrhage, swift_blade | killing a bleeding mob refills 20 Bloodletting; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| blade_stat_start | Blade | start | - | - | - | - |
| blade_quick | Quick | small | 2 | 1 | blade_stat_start | +2 Dexterity, +1 Agility |
| blade_steady_hand | Steady Hand | small | 2 | 5 | blade_quick | +2 Dexterity, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- The book's knives/daggers are mapped to swords (Minecraft has no dagger).
- Book talents: Bleed, Swift Blade.
