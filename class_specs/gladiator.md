# Class: Gladiator

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Gladiator' (p. 88). -->

## 1. Basics

- **Id:** gladiator
- **Display name:** Gladiator
- **Description:** An arena survivor who fights dirty - sand in the eyes, a kick to the knee - and finishes off anyone who's hurting.
- **Icon item:** trident
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 6 |
| Dexterity    | 4 |
| Agility      | 3 |
| Intelligence | 1 |
| Wisdom       | 0 |
| Endurance    | 4 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Showmanship
- **Description:** The crowd-pleasing ruthlessness of the pits.
- **What each point gives:** +0.5% damage against mobs with a harmful effect
- **Max points:** 100

### Resource bar

- **Name:** Bloodlust
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** no
- **Colour:** arena sand (#D8B46A)

## 4. Starting kit

- 1 × trident
- 1 × shield
- 1 × leather_chestplate
- 1 × splash_potion of weakness
- 4 × bread

## 5. Passives

- Dirty Fighting: 15% chance on hit to give Blindness or Slowness for 2s
- Win at Any Cost: +20% damage to mobs that have any harmful effect
- Finishing Blow: +15% critical chance against slowed, blinded or poisoned mobs

### Special passives

- Dirty Fighting chance +0.3% per Showmanship
- Per 10 Showmanship, +3% attack speed

## 6. Weakness

- **What players read:** Arena-born, a gladiator plays to the crowd - and loses when no one's watching.
- **What it actually does:** Bloodlust only fills while another player is within 32 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | mastery |
| Bows | neutral |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
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

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Arena

- **Shared or class-only:** class-only
- **Trained by:** killing mobs that have a harmful effect
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Showmanship
- **Tier II bonus:** +1 Showmanship
- **Tier III bonus:** +10% Bloodlust regen
- **Tier IV bonus:** +1 Showmanship
- **Tier V bonus:** +2 Showmanship

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Berserker

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| gladiator_start | Gladiator | start | - | - | - | +3% damage |
| gladiator_sand_in_the_eyes | Sand in the Eyes | small | 1 | 1 | gladiator_start | +5% Dirty Fighting chance |
| gladiator_net_and_trident | Net and Trident | small | 1 | 1 | gladiator_start | +10% trident damage |
| gladiator_crowd_pleaser | Crowd Pleaser | notable | 2 | 12 | gladiator_sand_in_the_eyes | kills give Speed I for 3s |
| gladiator_hamstring | Hamstring | notable | 2 | 15 | gladiator_net_and_trident | Dirty Fighting slow is Slowness II |
| gladiator_death_or_glory | Death or Glory | keystone | 3 | 30 | gladiator_crowd_pleaser, gladiator_hamstring | +25% damage, but you can't use shields |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Dirty Fighting | start | - | - | - | - |
| ruthless | Ruthless | small | 1 | 1 | start | +2 Showmanship |
| low_blow | Low Blow | small | 1 | 5 | start | Dirty Fighting can also apply Weakness |
| win_at_any_cost | Win at Any Cost | notable | 2 | 15 | ruthless | +10% more damage to affected mobs |
| finishing_blow | Finishing Blow | notable | 2 | 20 | low_blow | unlocks Coup de Grace ability |
| champion_of_the_pits | Champion of the Pits | keystone | 3 | 35 | win_at_any_cost, finishing_blow | every kill refills 10% health; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| gladiator_stat_start | Gladiator | start | - | - | - | - |
| gladiator_brawler | Brawler | small | 2 | 1 | gladiator_stat_start | +2 Strength, +1 Vitality |
| gladiator_pit_fighter | Pit-Fighter | small | 2 | 5 | gladiator_brawler | +2 Dexterity, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Coup de Grace** (heavy finisher vs affected mobs, 30 Bloodlust).
- Book talents: Dirty Fighting, Win at Any Cost, Finishing Blow.
