# Class: Champion

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Champion' (p. 82). -->

## 1. Basics

- **Id:** champion
- **Display name:** Champion
- **Description:** A warrior who fights for a cause. Plants their feet, takes a stance and refuses to fall.
- **Icon item:** bell
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 6 |
| Dexterity    | 3 |
| Agility      | 1 |
| Intelligence | 1 |
| Wisdom       | 3 |
| Endurance    | 6 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Resolve
- **Description:** Your commitment to the cause you fight for.
- **What each point gives:** +0.3% damage and -0.2% damage taken while in Battle Stance
- **Max points:** 100

### Resource bar

- **Name:** Valor
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** banner red (#D22F2F)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × white_banner
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Battle Stance: while sneaking and not moving, +20% damage and mobs deal 20% less damage to you
- Champion's Resolve: once per 5 minutes, when you'd drop below 2 hearts, heal 6 HP and gain +20% damage for 5s
- +10% knockback resistance

### Special passives

- Battle Stance bonuses +1% per 4 Resolve
- Champion's Resolve cooldown -1% per Resolve point

## 6. Weakness

- **What players read:** Champions plant their feet. Moving breaks their rhythm.
- **What it actually does:** -10% damage while sprinting

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | mastery |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | mastery |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Cause

- **Shared or class-only:** class-only
- **Trained by:** fighting in Battle Stance
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Resolve
- **Tier II bonus:** +1 Resolve
- **Tier III bonus:** +10% Valor regen
- **Tier IV bonus:** +1 Resolve
- **Tier V bonus:** +2 Resolve

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Paladin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| champion_start | Champion | start | - | - | - | +1 armor |
| champion_stalwart | Stalwart | small | 1 | 1 | champion_start | +3 max health |
| champion_sure_strike | Sure Strike | small | 1 | 1 | champion_start | +5% damage |
| champion_banner_bearer | Banner Bearer | notable | 2 | 12 | champion_stalwart | a banner in your off-hand gives players within 8 blocks +1 armor |
| champion_hold_the_line | Hold the Line | notable | 2 | 15 | champion_sure_strike | Battle Stance also gives knockback immunity |
| champion_last_stand | Last Stand | keystone | 3 | 30 | champion_banner_bearer, champion_hold_the_line | Battle Stance bonuses doubled, but you take +10% damage while moving |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Battle Stance | start | - | - | - | - |
| firm_footing | Firm Footing | small | 1 | 1 | start | +2 Resolve |
| rally | Rally | small | 1 | 5 | start | Champion's Resolve also heals players within 6 blocks by 4 |
| unbowed | Unbowed | notable | 2 | 15 | firm_footing | -10% damage taken below half health |
| champion_s_resolve | Champion's Resolve | notable | 2 | 20 | rally | Resolve trigger heals 10 HP instead |
| living_legend | Living Legend | keystone | 3 | 35 | unbowed, champion_s_resolve | Battle Stance can be held while walking slowly; -10% movement speed |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| champion_stat_start | Champion | start | - | - | - | - |
| champion_brave | Brave | small | 2 | 1 | champion_stat_start | +2 Strength, +1 Wisdom |
| champion_enduring | Enduring | small | 2 | 5 | champion_brave | +2 Endurance, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Battle Stance, Champion's Resolve.
