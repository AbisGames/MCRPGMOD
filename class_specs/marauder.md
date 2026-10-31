# Class: Marauder

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Marauder' (p. 92). -->

## 1. Basics

- **Id:** marauder
- **Display name:** Marauder
- **Description:** A barbarian raider who charges headlong into battle, unafraid of death and fuelled by pain.
- **Icon item:** iron_axe
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 7 |
| Dexterity    | 3 |
| Agility      | 4 |
| Intelligence | 0 |
| Wisdom       | 0 |
| Endurance    | 5 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Savagery
- **Description:** The wildness of the raid.
- **What each point gives:** +0.5% damage while charging (sprinting)
- **Max points:** 100

### Resource bar

- **Name:** Warcry
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** war paint blue (#2B5CA8)

## 4. Starting kit

- 1 × iron_axe
- 1 × leather_chestplate
- 6 × cooked_mutton

## 5. Passives

- Powerful Charge: sprint-hits deal +25% damage and knock mobs back
- Bloodthirst: when you kill a mob, gain Speed II for 3s
- Strength from Pain: below half health, +20% melee damage

### Special passives

- Sprint-hit bonus +0.5% per Savagery
- Per 10 Savagery, +5% speed

## 6. Weakness

- **What players read:** Marauders fight without regard for their safety.
- **What it actually does:** -2 armor at all times and +10% damage taken while sprinting

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | mastery |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
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

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Raiding

- **Shared or class-only:** class-only
- **Trained by:** killing mobs with sprint-hits
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Savagery
- **Tier II bonus:** +1 Savagery
- **Tier III bonus:** +10% Warcry regen
- **Tier IV bonus:** +1 Savagery
- **Tier V bonus:** +2 Savagery

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Berserker

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| marauder_start | Marauder | start | - | - | - | +5% movement speed |
| marauder_bloodthirst | Bloodthirst | small | 1 | 1 | marauder_start | Bloodthirst lasts 2s longer |
| marauder_howl | Howl | small | 1 | 1 | marauder_start | +6% damage while sprinting |
| marauder_rampage | Rampage | notable | 2 | 12 | marauder_bloodthirst | consecutive kills within 5s add +5% damage (stacks 5) |
| marauder_scarred | Scarred | notable | 2 | 15 | marauder_howl | +3 max health |
| marauder_valhalla_bound | Valhalla Bound | keystone | 3 | 30 | marauder_rampage, marauder_scarred | +30% damage below half health, but you can't eat while in combat |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Powerful Charge | start | - | - | - | - |
| reaver | Reaver | small | 1 | 1 | start | +2 Savagery |
| trample | Trample | small | 1 | 5 | start | sprint-hits stun for 0.5s |
| strength_from_pain | Strength from Pain | notable | 2 | 15 | reaver | +10% more damage below half health |
| warcry | Warcry | notable | 2 | 20 | trample | unlocks Warcry ability |
| glorious_death | Glorious Death | keystone | 3 | 35 | strength_from_pain, warcry | when you'd die, you keep fighting for 5s at 1 HP, then die; once per day |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| marauder_stat_start | Marauder | start | - | - | - | - |
| marauder_wild | Wild | small | 2 | 1 | marauder_stat_start | +2 Strength, +1 Agility |
| marauder_battle_scarred | Battle-Scarred | small | 2 | 5 | marauder_wild | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Warcry** (mobs within 8 blocks get Weakness, allies get Speed, 30 Warcry).
- Book talents: Powerful Charge, Bloodthirst, Strength from Pain.
