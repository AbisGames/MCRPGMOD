# Class: Death Dealer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Death Dealer' (p. 84). -->

## 1. Basics

- **Id:** death_dealer
- **Display name:** Death Dealer
- **Description:** A heavy-weapon reaper who carves through crowds - every kill feeds the next swing.
- **Icon item:** netherite_axe
- **Selectable by players:** yes
- **Base health:** 36

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 9 |
| Dexterity    | 2 |
| Agility      | 1 |
| Intelligence | 0 |
| Wisdom       | 0 |
| Endurance    | 6 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Carnage
- **Description:** Momentum of slaughter.
- **What each point gives:** +0.6% damage with axes and maces
- **Max points:** 100

### Resource bar

- **Name:** Bloodshed
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** -0.5
- **Extra regen per second per special-stat point:** 0
- **Starts full when the class is chosen:** no (fills by 8 per kill, drains 0.5/s)
- **Colour:** gore red (#8E1010)

## 4. Starting kit

- 1 × iron_axe
- 1 × chainmail_chestplate
- 6 × cooked_beef

## 5. Passives

- Brutal Swing: when you kill a mob with an axe or mace, your attack cooldown resets instantly
- +15% axe and mace damage
- Fully charged axe/mace hits damage all mobs within 2 blocks for 40% (sweep)

### Special passives

- Sweep damage +0.5% per Carnage point
- Make Mountains of the Dead: per 10 Carnage, +5% damage to mobs below 30% health

## 6. Weakness

- **What players read:** Heavy weapons need room and time.
- **What it actually does:** -20% damage with swords, bows and crossbows; -10% attack speed

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | mastery |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
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
| Shoulderpad | 1 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Reaping

- **Shared or class-only:** class-only
- **Trained by:** killing 3+ mobs within 10s
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Carnage
- **Tier II bonus:** +1 Carnage
- **Tier III bonus:** +10 max Bloodshed
- **Tier IV bonus:** +1 Carnage
- **Tier V bonus:** +2 Carnage

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Berserker

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| death_dealer_start | Death Dealer | start | - | - | - | +5% axe damage |
| death_dealer_cleaver | Cleaver | small | 1 | 1 | death_dealer_start | +6% axe damage |
| death_dealer_crusher | Crusher | small | 1 | 1 | death_dealer_start | +6% mace damage |
| death_dealer_wide_arc | Wide Arc | notable | 2 | 12 | death_dealer_cleaver | sweep radius +1 block |
| death_dealer_executioner_s_weight | Executioner's Weight | notable | 2 | 15 | death_dealer_crusher | +10% damage to mobs below half health |
| death_dealer_slaughterhouse | Slaughterhouse | keystone | 3 | 30 | death_dealer_wide_arc, death_dealer_executioner_s_weight | Brutal Swing also heals 2 HP, but -3 armor |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Brutal Swing | start | - | - | - | - |
| heft | Heft | small | 1 | 1 | start | +2 Carnage |
| follow_through | Follow-Through | small | 1 | 5 | start | sweep damage 60% |
| mountains_of_the_dead | Mountains of the Dead | notable | 2 | 15 | heft | unlocks Whirlwind ability |
| bloodsoaked | Bloodsoaked | notable | 2 | 20 | follow_through | full Bloodshed gives +15% damage |
| reaper | Reaper | keystone | 3 | 35 | mountains_of_the_dead, bloodsoaked | mobs below 15% health that you sweep die instantly; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| death_dealer_stat_start | Death Dealer | start | - | - | - | - |
| death_dealer_huge | Huge | small | 2 | 1 | death_dealer_stat_start | +2 Strength, +1 Vitality |
| death_dealer_tireless | Tireless | small | 2 | 5 | death_dealer_huge | +2 Endurance, +1 Strength |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Whirlwind** (Make Mountains of the Dead: spin attack; mobs within 2 blocks under 20 HP die, 40 Bloodshed).
- Book talents: Brutal Swing, Make Mountains of the Dead.
