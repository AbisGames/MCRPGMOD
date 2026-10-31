# Class: Conqueror

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Conqueror' (p. 83). -->

## 1. Basics

- **Id:** conqueror
- **Display name:** Conqueror
- **Description:** A battlefield commander who hangs back, reads the fight and drives allies to strike where it hurts.
- **Icon item:** goat_horn
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.15 |
| Strength     | 5 | x1.1 |
| Dexterity    | 3 | x0.9 |
| Agility      | 2 | x1 |
| Intelligence | 3 | x0.9 |
| Wisdom       | 4 | x1.1 |
| Endurance    | 3 | x1 |
| Luck         | 1 | x0.85 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Command
- **Description:** Tactical authority over a battlefield.
- **What each point gives:** +0.5% damage for players near you
- **Max points:** 100

### Resource bar

- **Name:** Orders
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** imperial purple (#5B2A86)

## 4. Starting kit

- 1 × iron_sword
- 1 × goat_horn
- 1 × chainmail_chestplate
- 1 × white_banner
- 6 × bread

## 5. Passives

- Battlefield Leadership: when you hit a mob, players within 10 blocks deal +10% damage to it for 4s
- Direct the Troops: players within 10 blocks get +5% movement speed in combat
- Attack Command: blowing a goat horn gives players within 16 blocks Strength I for 6s (60s cooldown)

### Special passives

- Leadership bonus +0.5% per Command point
- Per 10 Command, +1 block to all your aura ranges

## 6. Weakness

- **What players read:** A commander leads from behind - and is caught out up front.
- **What it actually does:** -10% damage and -10% armor while no other player is within 16 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | proficient |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Generalship

- **Shared or class-only:** class-only
- **Trained by:** allies killing mobs you've hit in the last 4s
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Command
- **Tier II bonus:** +1 Command
- **Tier III bonus:** +10% Orders regen
- **Tier IV bonus:** +1 Command
- **Tier V bonus:** +2 Command

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Fighter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| conqueror_start | Conqueror | start | - | - | - | +3% damage |
| conqueror_drillmaster | Drillmaster | small | 1 | 1 | conqueror_start | allies near you +2% attack speed |
| conqueror_war_banner | War Banner | small | 1 | 1 | conqueror_start | banner in off-hand: allies +1 armor |
| conqueror_flanking_orders | Flanking Orders | notable | 2 | 12 | conqueror_drillmaster | +10% damage to mobs targeting someone else |
| conqueror_logistics | Logistics | notable | 2 | 15 | conqueror_war_banner | allies near you lose hunger 15% slower |
| conqueror_grand_strategy | Grand Strategy | keystone | 3 | 30 | conqueror_flanking_orders, conqueror_logistics | aura bonuses doubled, but your own damage -20% |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Battlefield Leadership | start | - | - | - | - |
| tactician | Tactician | small | 1 | 1 | start | +2 Command |
| forward | Forward! | small | 1 | 5 | start | Direct the Troops gives +5% more speed |
| attack_command | Attack Command | notable | 2 | 15 | tactician | goat horn cooldown -20s |
| hold_formation | Hold Formation | notable | 2 | 20 | forward | allies near you +10% knockback resistance |
| warlord | Warlord | keystone | 3 | 35 | attack_command, hold_formation | Attack Command gives Strength II; you can't sprint |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| conqueror_stat_start | Conqueror | start | - | - | - | - |
| conqueror_commanding | Commanding | small | 2 | 1 | conqueror_stat_start | +2 Wisdom, +1 Strength |
| conqueror_strategic | Strategic | small | 2 | 5 | conqueror_commanding | +2 Intelligence, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Attack Command** (target ally gets a free empowered hit), **Direct the Troops** (allies dash toward a point).
- Book talents: Attack Command, Direct the Troops, Battlefield Leadership.
