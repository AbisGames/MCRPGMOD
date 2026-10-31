# Class: Sorcerer

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Sorcerer' (p. 71). -->

## 1. Basics

- **Id:** sorcerer
- **Display name:** Sorcerer
- **Description:** Seizes far more magic than is safe. Hits incredibly hard, but the leftover energy can explode out of them.
- **Icon item:** fire_charge
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 2 |
| Strength     | 0 |
| Dexterity    | 1 |
| Agility      | 2 |
| Intelligence | 6 |
| Wisdom       | 6 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Potency
- **Description:** Raw force behind your magic. Makes everything hit harder - including your own outbursts.
- **What each point gives:** +1.5% magic damage
- **Max points:** 100

### Resource bar

- **Name:** Strain
- **Max at 0 points:** 10
- **Extra max per special-stat point:** 0.2
- **Regen per second:** -0.2
- **Extra regen per second per special-stat point:** 0
- **Starts full when the class is chosen:** no (starts empty; it FILLS as you use magic and slowly drains - see section 12)
- **Colour:** violent magenta (#D0219B)

## 4. Starting kit

- 1 × wooden_sword
- 2 × fire_charge
- 1 × book
- 6 × bread

## 5. Passives

- +15% magic damage
- Sorcery: magic damage you deal ignores 20% of the target's armor
- Every magic hit you land adds 1 Strain

### Special passives

- Sorcerous Outburst explosion damage +0.5 per Potency point
- +1% magic damage per 4 Potency while Strain is above half

## 6. Weakness

- **What players read:** Bottled power always wants out.
- **What it actually does:** when Strain reaches max it detonates: an explosion around you (power 2 + 1 per 25 Potency) that damages everything nearby except you, breaks no blocks, and resets Strain to 0. Allies are not safe.

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Overchannel

- **Shared or class-only:** class-only
- **Trained by:** triggering Sorcerous Outbursts and landing magic hits while Strain is above half
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Potency
- **Tier II bonus:** +1 Potency
- **Tier III bonus:** +1 max Strain
- **Tier IV bonus:** +1 Potency
- **Tier V bonus:** +2 Potency

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** top of the web (Power quarter), ring 2, centre

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sorcerer_start | Sorcerer | start | - | - | - | +5% magic damage |
| sorcerer_surge | Surge | small | 1 | 1 | sorcerer_start | +6% magic damage |
| sorcerer_insulated | Insulated | small | 1 | 1 | sorcerer_start | -10% explosion damage taken |
| sorcerer_far | Far Reach | notable | 2 | 10 | sorcerer_surge | +15% projectile speed for thrown potions and fire charges |
| sorcerer_empowered | Empowered | notable | 2 | 12 | sorcerer_insulated, sorcerer_far | +12% magic damage |
| sorcerer_living_bomb | Living Bomb | keystone | 3 | 25 | sorcerer_empowered | Outbursts deal double damage, but you take 25% of the outburst damage yourself |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Sorcery | start | - | - | - | - |
| enlarged | Enlarged Spell | small | 1 | 1 | start | Outburst radius +1 block |
| potent | Potent Spell | small | 1 | 5 | start | +2 Potency |
| lasting | Lasting Spell | notable | 2 | 15 | enlarged | potion effects you apply last 30% longer |
| greater_sorcery | Greater Sorcery | notable | 2 | 20 | potent | Strain drains 50% faster |
| sorcerous_blast | Sorcerous Blast | keystone | 3 | 35 | lasting, greater_sorcery | unlocks Sorcerous Blast (spend Strain as a ranged bolt); max Strain -2 |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sorcerer_stat_start | Sorcerer | start | - | - | - | - |
| sorcerer_will | Iron Will | small | 2 | 1 | sorcerer_stat_start | +2 Wisdom, +1 Endurance |
| sorcerer_spark | Spark | small | 2 | 5 | sorcerer_will | +2 Intelligence, +1 Luck |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Strain is an inverted bar: it fills from use and drains at 0.2/s ('Regen per second: -0.2').
- Active ability for later: **Sorcerous Blast** (spend 1 Strain: magic bolt, 6 damage).
- Book talents: Sorcery, Sorcerous Outburst, Greater Sorcery (Empowered/Enlarged/Far/Lasting/Potent), Sorcerous Blast.
- **Decided:** Potency + Strain. Strain stays a reversed danger meter that fills and drains.
