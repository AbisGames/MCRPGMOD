# Class: Exorcist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Exorcist' (p. 87). -->

## 1. Basics

- **Id:** exorcist
- **Display name:** Exorcist
- **Description:** A hunter of demons, wicked spirits and the undead who drives possessing horrors out and burns them down.
- **Icon item:** totem_of_undying
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.15 |
| Strength     | 3 | x1 |
| Dexterity    | 2 | x0.85 |
| Agility      | 1 | x0.75 |
| Intelligence | 4 | x1.15 |
| Wisdom       | 8 | x1.25 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.85 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Purity
- **Description:** The holy power you wield against the unclean.
- **What each point gives:** +1% damage against undead, Nether mobs, vexes and phantoms
- **Max points:** 100

### Resource bar

- **Name:** Sanctification
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** holy silver (#E6EEF2)

## 4. Starting kit

- 1 × iron_sword
- 1 × book
- 1 × chainmail_chestplate
- 1 × golden_apple
- 4 × bread

## 5. Passives

- Purge the Unclean: +25% damage to undead, Nether mobs, vexes, phantoms, endermen and the Wither
- Will of Iron: immune to Nausea, Darkness, Blindness and Bad Omen
- Exorcism: hitting a zombie villager has 10% chance to cure it instantly

### Special passives

- Purge bonus +0.5% per Purity
- Per 10 Purity, unclean mobs deal 3% less damage to you

## 6. Weakness

- **What players read:** Exorcists are trained against the unclean - not ordinary beasts.
- **What it actually does:** -15% damage against animals, illagers and other ordinary mobs

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | neutral |
| Spears | neutral |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
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

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Purging

- **Shared or class-only:** class-only
- **Trained by:** killing unclean mobs (undead, Nether, End, vexes, phantoms)
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Purity
- **Tier II bonus:** +1 Purity
- **Tier III bonus:** +10% Sanctification regen
- **Tier IV bonus:** +1 Purity
- **Tier V bonus:** +2 Purity

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Cleric

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| exorcist_start | Exorcist | start | - | - | - | +5% damage to unclean mobs |
| exorcist_holy_water | Holy Water | small | 1 | 1 | exorcist_start | splash potions you throw deal +2 damage to undead |
| exorcist_litany | Litany | small | 1 | 1 | exorcist_start | +1 armor |
| exorcist_banishing | Banishing | notable | 2 | 12 | exorcist_holy_water | unclean mobs you hit are knocked back 50% farther |
| exorcist_iron_will | Iron Will | notable | 2 | 15 | exorcist_litany | -20% damage from vexes, phantoms and endermen |
| exorcist_inquisitor_s_fire | Inquisitor's Fire | keystone | 3 | 30 | exorcist_banishing, exorcist_iron_will | +30% damage to unclean, but -30% to everything else |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Exorcism | start | - | - | - | - |
| rite | Rite | small | 1 | 1 | start | +2 Purity |
| cleanse | Cleanse | small | 1 | 5 | start | unlocks Exorcise ability |
| purge_the_unclean | Purge the Unclean | notable | 2 | 15 | rite | unclean mobs you hit can't teleport for 5s (endermen, shulkers) |
| will_of_iron | Will of Iron | notable | 2 | 20 | cleanse | players within 6 blocks also immune to Bad Omen |
| hammer_of_the_gods | Hammer of the Gods | keystone | 3 | 35 | purge_the_unclean, will_of_iron | unclean mobs below 20% health die from your hits; Sanctification regen -25% |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| exorcist_stat_start | Exorcist | start | - | - | - | - |
| exorcist_pious | Pious | small | 2 | 1 | exorcist_stat_start | +2 Wisdom, +1 Vitality |
| exorcist_disciplined | Disciplined | small | 2 | 5 | exorcist_pious | +2 Intelligence, +1 Wisdom |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Exorcise** (cleanse charm/fear/curse from a player, or stun an unclean mob for 2s, 25 Sanctification).
- Book talents: Exorcism spell, Purge the Unclean, Will of Iron.
