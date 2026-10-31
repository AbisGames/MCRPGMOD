# Class: Fighter

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Fighter' (p. 66). -->

## 1. Basics

- **Id:** fighter
- **Display name:** Fighter
- **Description:** A master of arms who can fight with anything, and whose chosen fighting style makes them deadly.
- **Icon item:** diamond_sword
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 5 |
| Dexterity    | 3 |
| Agility      | 1 |
| Intelligence | 0 |
| Wisdom       | 1 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Discipline
- **Description:** Years of drilling. Every point makes your style sharper.
- **What each point gives:** +0.4% weapon damage and +0.2% attack speed
- **Max points:** 100

### Resource bar

- **Name:** Momentum
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** no
- **Colour:** steel (#9AA7B4)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 1 × bow
- 16 × arrow
- 6 × bread

## 5. Passives

- Fight with Anything: weapons you're clumsy or forbidden with count as neutral; any weapon/tool does at least 4 damage
- +1 attack damage with any weapon
- Durable: natural regeneration is 40% faster
- Weapon Mastery: 15% chance to reroll a 'glancing' (non-critical) hit into a critical hit

### Special passives

- +1% Weapon Mastery chance per 4 Discipline
- Fighter's Resolve: -0.25% damage taken per Discipline point (max -15%)

## 6. Weakness

- **What players read:** A Fighter's whole life is the weapon. Nothing else gets practice.
- **What it actually does:** -25% potion effect duration on you, and enchanting costs +1 level

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | proficient |
| Bows | proficient |
| Crossbows | proficient |
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
| Cloak | 0 |
| Shoulderpad | 1 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Drill

- **Shared or class-only:** class-only
- **Trained by:** killing mobs with 3 or more different weapon types in the same day
- **Tier points go to:** special tree
- **How fast it levels:** slow
- **Tier I bonus:** +1 Discipline
- **Tier II bonus:** +1 Discipline
- **Tier III bonus:** +1 attack damage
- **Tier IV bonus:** +1 Discipline
- **Tier V bonus:** +2 Discipline

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** left side of the web (War quarter), ring 2, centre

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| fighter_start | Fighter | start | - | - | - | +1 attack damage |
| fighter_conditioning | Conditioning | small | 1 | 1 | fighter_start | +3 max health |
| fighter_technique | Technique | small | 1 | 1 | fighter_start | +5% weapon damage |
| fighter_battle_rhythm | Battle Rhythm | notable | 2 | 10 | fighter_technique | +8% attack speed |
| fighter_iron_will | Fighter's Resolve | notable | 2 | 12 | fighter_conditioning | -8% damage taken |
| fighter_champion | Living Weapon | keystone | 3 | 25 | fighter_battle_rhythm, fighter_iron_will | +15% weapon damage and attack speed, but -30% magic damage and potion strength |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Fighting Style | start | - | - | - | - |
| two_weapons | Fight with Two Weapons | small | 1 | 1 | start | holding a weapon in your off-hand: +10% attack speed |
| haft_attack | Haft Attack | small | 1 | 1 | start | spears/tridents: every 3rd hit strikes twice |
| powerful_attack | Powerful Attack | small | 1 | 1 | start | axes/maces: fully charged hits +15% damage |
| precise_attack | Precise Attack | small | 1 | 1 | start | swords: your hits ignore 15% armor |
| shield_bash | Shield Bash | notable | 2 | 15 | start | blocking an attack makes your next hit within 3s deal +25% damage |
| swift_reload | Swift Reload | notable | 2 | 15 | start | +25% crossbow reload speed |
| swift_shot | Swift Shot | notable | 2 | 15 | start | +20% bow draw speed |
| weapon_mastery | Weapon Mastery | keystone | 3 | 35 | precise_attack, powerful_attack | Weapon Mastery chance +15%; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| fighter_stat_start | Fighter | start | - | - | - | - |
| fighter_drilled | Drilled | small | 2 | 1 | fighter_stat_start | +2 Strength, +1 Dexterity |
| fighter_seasoned | Seasoned | small | 2 | 5 | fighter_drilled | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- The book's 'Fighter Talents' (choose one at L3 and L6) are the special-tree style nodes - all link straight to the start, so players pick a style.
- Momentum: fills when you land hits, decays out of combat. Fuel for future abilities like **Cleave** or **Disarm**.
- Book talents: Fight with Anything, Durable, Fighter's Resolve, Weapon Mastery + all 7 fighter talents.
- **Decided:** Discipline + Momentum. Fighters can buy as many style nodes as they like.
