# Class: Berserker

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Berserker' (p. 63). -->

## 1. Basics

- **Id:** berserker
- **Display name:** Berserker
- **Description:** A warrior whose rage turns them into a wild, unstoppable killer - hard to control, harder to kill.
- **Icon item:** netherite_axe
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.2 |
| Strength     | 6 | x1.35 |
| Dexterity    | 1 | x1 |
| Agility      | 2 | x1 |
| Intelligence | 0 | x0.8 |
| Wisdom       | 0 | x0.65 |
| Endurance    | 5 | x1.2 |
| Luck         | 1 | x0.8 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Fury
- **Description:** The rage that is always rattling its cage.
- **What each point gives:** +0.5% melee damage
- **Max points:** 100

### Resource bar

- **Name:** Rage
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 2
- **Regen per second:** -0.5
- **Extra regen per second per special-stat point:** 0
- **Starts full when the class is chosen:** no (starts empty; fills by 2 per hit taken and 1 per hit dealt, drains 0.5/s out of combat)
- **Colour:** blood red (#B3171E)

## 4. Starting kit

- 1 × stone_axe
- 1 × leather_chestplate
- 6 × cooked_beef

## 5. Passives

- Iron Hide: +3 armor while wearing no medium or heavy armor
- Berserk: while Rage is above 75%: +6 max health, +25% melee damage, immune to Slowness and Weakness, but -20% damage with ranged weapons
- Frightful Wrath: when you go Berserk, mobs within 6 blocks get Slowness I for 3s

### Special passives

- Berserk melee bonus +1% per 2 Fury
- Ferocious Wrath: +0.2% movement speed per Fury while Berserk

## 6. Weakness

- **What players read:** When the rage leaves, it takes everything with it.
- **What it actually does:** when Berserk ends: Weakness I and Slowness I for 8s (fatigued); can't go Berserk again for 15s

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
| Cloak | 0 |
| Shoulderpad | 1 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Athletics

- Shared skill - full definition in `scout.md`

### Skill: Bloodrage

- **Shared or class-only:** class-only
- **Trained by:** taking damage while Berserk
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Fury
- **Tier II bonus:** +1 Fury
- **Tier III bonus:** +5 max Rage
- **Tier IV bonus:** +1 Fury
- **Tier V bonus:** +2 Fury

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** left side of the web (War quarter), ring 2, leaning toward the Trickery quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| berserker_start | Berserker | start | - | - | - | +2 max health |
| berserker_thick_skull | Thick Skull | small | 1 | 1 | berserker_start | +3 max health |
| berserker_savage | Savage | small | 1 | 1 | berserker_start | +6% axe damage |
| berserker_bloodlust | Bloodlust | notable | 2 | 10 | berserker_savage | heal 1 HP on each kill |
| berserker_unchained | Unchained | notable | 2 | 12 | berserker_thick_skull | Rage fills 25% faster |
| berserker_reckless | Reckless Strike | keystone | 3 | 25 | berserker_bloodlust, berserker_unchained | +20% melee damage at all times, but you take +15% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Berserk | start | - | - | - | - |
| iron_hide | Iron Hide | small | 1 | 1 | start | +2 Fury; Iron Hide +1 armor |
| frightful | Frightful Wrath | small | 1 | 5 | start | Frightful Wrath range +3 blocks |
| ferocious | Ferocious Wrath | notable | 2 | 15 | iron_hide | +15% movement speed while Berserk |
| fury_unleashed | Fury Unleashed | notable | 2 | 20 | frightful | once per rest, when you would die, go Berserk instead with 4 HP |
| reckless | Reckless Strike | keystone | 3 | 35 | ferocious, fury_unleashed | Berserk triggers at 50% Rage instead of 75%; fatigue lasts twice as long |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| berserker_stat_start | Berserker | start | - | - | - | - |
| berserker_brawn | Brawn | small | 2 | 1 | berserker_stat_start | +2 Strength, +1 Vitality |
| berserker_stamina | Unbreakable | small | 2 | 5 | berserker_brawn | +2 Endurance, +1 Vitality |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Rage is an inverted bar like the Sorcerer's Strain: it fills from combat and drains outside it.
- Book talents: Berserk, Fury Unleashed, Iron Hide, Ferocious Wrath, Frightful Wrath, Reckless Strike.
- **Decided:** Fury + Rage. Berserk triggers automatically at 75% Rage.
