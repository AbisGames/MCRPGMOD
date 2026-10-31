# Class: Destroyer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Destroyer' (p. 84). -->

## 1. Basics

- **Id:** destroyer
- **Display name:** Destroyer
- **Description:** A master of Destruction magic who channels its self-harming energy outward in devastating bursts.
- **Icon item:** tnt
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 1 |
| Dexterity    | 1 |
| Agility      | 1 |
| Intelligence | 8 |
| Wisdom       | 6 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Ruin
- **Description:** Destructive energy pent up in your body.
- **What each point gives:** +1.5% explosion and magic damage
- **Max points:** 100

### Resource bar

- **Name:** Devastation
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** molten orange (#FF5A1F)

## 4. Starting kit

- 4 × tnt
- 1 × flint_and_steel
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Destruction Released: whenever you spend Devastation, a shockwave hits all mobs within 3 blocks for 2 damage
- Immune to your own explosions; -50% damage from all other explosions
- Utter Destruction: your TNT and explosive damage rolls high (+25% explosion damage)

### Special passives

- Shockwave radius +1 per 20 Ruin
- Shockwave damage +0.1 per Ruin point

## 6. Weakness

- **What players read:** Destruction tears at the caster too.
- **What it actually does:** every time you spend Devastation, take 1 damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | proficient |
| Spears | neutral |
| Maces | proficient |
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
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Demolition

- **Shared or class-only:** class-only
- **Trained by:** damaging mobs with explosions
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Ruin
- **Tier II bonus:** +1 Ruin
- **Tier III bonus:** +10% Devastation regen
- **Tier IV bonus:** +1 Ruin
- **Tier V bonus:** +2 Ruin

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Sorcerer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| destroyer_start | Destroyer | start | - | - | - | -10% explosion damage taken |
| destroyer_volatile | Volatile | small | 1 | 1 | destroyer_start | +8% explosion damage |
| destroyer_blast_shield | Blast Shield | small | 1 | 1 | destroyer_start | -15% explosion damage taken |
| destroyer_shrapnel | Shrapnel | notable | 2 | 12 | destroyer_volatile | explosions you cause give Slowness I |
| destroyer_bigger_booms | Bigger Booms | notable | 2 | 15 | destroyer_blast_shield | your TNT has +25% radius |
| destroyer_annihilator | Annihilator | keystone | 3 | 30 | destroyer_shrapnel, destroyer_bigger_booms | +30% explosion and magic damage, but explosions you make damage blocks more |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Destruction Released | start | - | - | - | - |
| fuse | Fuse | small | 1 | 1 | start | +2 Ruin |
| shockwave | Shockwave | small | 1 | 5 | start | shockwave knocks mobs back |
| controlled_burn | Controlled Burn | notable | 2 | 15 | fuse | no self-damage when spending Devastation |
| utter_destruction | Utter Destruction | notable | 2 | 20 | shockwave | +15% explosion damage |
| cataclysm | Cataclysm | keystone | 3 | 35 | controlled_burn, utter_destruction | unlocks Cataclysm ability; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| destroyer_stat_start | Destroyer | start | - | - | - | - |
| destroyer_destructive_mind | Destructive Mind | small | 2 | 1 | destroyer_stat_start | +2 Intelligence, +1 Endurance |
| destroyer_steady_will | Steady Will | small | 2 | 5 | destroyer_destructive_mind | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Cataclysm** (big explosion centred on you that doesn't hurt you, 60 Devastation).
- Book talents: Destruction Released, Utter Destruction. Tradition: Destruction.
