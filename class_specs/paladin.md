# Class: Paladin

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Paladin' (p. 68). -->

## 1. Basics

- **Id:** paladin
- **Display name:** Paladin
- **Description:** A holy knight who burns divine power into every strike and shields the innocent from darkness.
- **Icon item:** golden_sword
- **Selectable by players:** yes
- **Base health:** 26

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 5 |
| Dexterity    | 1 |
| Agility      | 0 |
| Intelligence | 1 |
| Wisdom       | 4 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 8 |

## 3. Special stat

- **Name:** Sanctity
- **Description:** The holy power stored in your oath. Fuels Smite and protects you.
- **What each point gives:** +0.05 armor
- **Max points:** 100

### Resource bar

- **Name:** Smite
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.5
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** radiant yellow (#FFD84A)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Divine Smite: your first melee hit every 5s deals +20% damage (+20% more against undead, Nether mobs, vexes and phantoms)
- Divine Cause: -10% magic damage taken
- Divine Vigor: immune to Poison and Hunger effects

### Special passives

- Smite bonus damage +1% per 2 Sanctity
- Faith Healing: +1% healing received per 2 Sanctity

## 6. Weakness

- **What players read:** A paladin's power flows from an oath, and oaths can break.
- **What it actually does:** killing a villager, iron golem or tamed animal empties Smite and gives Weakness I for 5 minutes

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | clumsy |
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
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Holy Oath

- **Shared or class-only:** class-only
- **Trained by:** landing Divine Smite hits
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Sanctity
- **Tier II bonus:** +1 Sanctity
- **Tier III bonus:** +10% Smite regen
- **Tier IV bonus:** +1 Sanctity
- **Tier V bonus:** +2 Sanctity

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** right side of the web (Faith quarter), ring 2, closest to the War quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| paladin_start | Paladin | start | - | - | - | +1 armor |
| paladin_righteous | Righteous | small | 1 | 1 | paladin_start | +5% melee damage |
| paladin_aegis | Aegis | small | 1 | 1 | paladin_start | +1 armor |
| paladin_sense | Sense Enemies | notable | 2 | 10 | paladin_aegis | hostile mobs within 12 blocks glow when you hold a shield |
| paladin_zeal | Holy Zeal | notable | 2 | 12 | paladin_righteous | +10% damage to undead and Nether mobs |
| paladin_beacon | Beacon of Hope | keystone | 3 | 25 | paladin_sense, paladin_zeal | other players within 8 blocks get +2 armor, but you get -10% movement speed |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Divine Smite | start | - | - | - | - |
| divine_cause | Divine Cause | small | 1 | 1 | start | +2 Sanctity |
| faith_healing | Faith Healing | small | 1 | 5 | start | unlocks Lay on Hands ability |
| divine_vigor | Divine Vigor | notable | 2 | 15 | divine_cause | also immune to Wither effect |
| sense_evil | Sense Evil | notable | 2 | 20 | faith_healing | +5% damage to mobs that glow for you |
| holy_radiance | Holy Radiance | keystone | 3 | 35 | divine_vigor, sense_evil | unlocks Holy Radiance; -15% max Smite |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| paladin_stat_start | Paladin | start | - | - | - | - |
| paladin_valiant | Valiant | small | 2 | 1 | paladin_stat_start | +2 Strength, +1 Wisdom |
| paladin_unbowed | Unbowed | small | 2 | 5 | paladin_valiant | +2 Endurance, +1 Vitality |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Lay on Hands** (heal a player 6 HP, 15 Smite), **Holy Radiance** (light aura: allies resist, undead slowed and weakened, 25 Smite).
- Book talents: Divine Cause, Divine Smite, Faith Healing, Divine Vigor, Sense Enemies, Holy Radiance.
- **Co-design question:** Sanctity + Smite. Is the oath-break weakness too harsh, or just right for the fantasy?
