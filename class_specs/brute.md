# Class: Brute

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Brute' (p. 82). -->

## 1. Basics

- **Id:** brute
- **Display name:** Brute
- **Description:** Pure muscle and toughness. Shrugs off blows that would drop anyone else and hits like a falling tree.
- **Icon item:** anvil
- **Selectable by players:** yes
- **Base health:** 40

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 7 |
| Strength     | 9 |
| Dexterity    | 1 |
| Agility      | 1 |
| Intelligence | 0 |
| Wisdom       | 0 |
| Endurance    | 7 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Brawn
- **Description:** Raw physical strength built over countless hours.
- **What each point gives:** +0.6% melee damage and +0.1 max health
- **Max points:** 100

### Resource bar

- **Name:** Might
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** rust (#B4532A)

## 4. Starting kit

- 1 × stone_axe
- 1 × chainmail_chestplate
- 8 × cooked_beef

## 5. Passives

- +20% melee damage
- +20% knockback resistance
- Mighty Thews: your melee hits knock mobs back 50% farther
- You can carry on fighting: hunger drains 20% slower

### Special passives

- +0.5% knockback resistance per Brawn point
- Per 10 Brawn, +1 max health

## 6. Weakness

- **What players read:** Big and slow.
- **What it actually does:** -10% movement speed and -10% attack speed

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | mastery |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
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

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Iron Body

- **Shared or class-only:** class-only
- **Trained by:** taking big hits (5+ damage) and surviving
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Brawn
- **Tier II bonus:** +1 Brawn
- **Tier III bonus:** +3 max health
- **Tier IV bonus:** +1 Brawn
- **Tier V bonus:** +2 Brawn

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Berserker

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| brute_start | Brute | start | - | - | - | +3 max health |
| brute_thick_neck | Thick Neck | small | 1 | 1 | brute_start | +4 max health |
| brute_heavy_hands | Heavy Hands | small | 1 | 1 | brute_start | +6% melee damage |
| brute_stonewall | Stonewall | notable | 2 | 12 | brute_thick_neck | +15% knockback resistance |
| brute_crusher | Crusher | notable | 2 | 15 | brute_heavy_hands | +10% damage against shields and armored mobs |
| brute_juggernaut | Juggernaut | keystone | 3 | 30 | brute_stonewall, brute_crusher | +30% max health, but -15% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Brawn | start | - | - | - | - |
| muscle | Muscle | small | 1 | 1 | start | +2 Brawn |
| shove | Shove | small | 1 | 5 | start | melee knockback +25% |
| mighty_thews | Mighty Thews | notable | 2 | 15 | muscle | +10% melee damage |
| bone_and_sinew | Bone and Sinew | notable | 2 | 20 | shove | +1 armor toughness |
| unstoppable | Unstoppable | keystone | 3 | 35 | mighty_thews, bone_and_sinew | immune to Slowness and knockback; -20% attack speed |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| brute_stat_start | Brute | start | - | - | - | - |
| brute_bulk | Bulk | small | 2 | 1 | brute_stat_start | +2 Vitality, +1 Strength |
| brute_iron_lungs | Iron Lungs | small | 2 | 5 | brute_bulk | +2 Endurance, +1 Strength |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Ground Slam** (stun mobs within 3 blocks, 30 Might).
- Book talents: Brawn, Mighty Thews.
