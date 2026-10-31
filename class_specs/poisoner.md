# Class: Poisoner

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Poisoner' (p. 93). -->

## 1. Basics

- **Id:** poisoner
- **Display name:** Poisoner
- **Description:** A dealer in toxins who coats every blade and arrow, and carries a poisoned needle for emergencies.
- **Icon item:** pufferfish
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 2 |
| Dexterity    | 7 |
| Agility      | 5 |
| Intelligence | 5 |
| Wisdom       | 0 |
| Endurance    | 2 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Toxicity
- **Description:** Your mastery of venoms.
- **What each point gives:** +1% poison damage and duration you apply
- **Max points:** 100

### Resource bar

- **Name:** Venom
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** toxic lime (#9BE02F)

## 4. Starting kit

- 1 × stone_sword
- 1 × bow
- 16 × arrow
- 2 × splash_potion of poison
- 1 × pufferfish
- 1 × leather_chestplate

## 5. Passives

- Poison Mastery: your melee hits and arrows apply Poison I for 4s (Poison II if you hold a pufferfish or poison potion in the off-hand)
- Poison you apply also works on undead (as Wither instead) and deals +50% damage
- Immune to Poison; brewing poison potions is 50% faster

### Special passives

- Poison duration +0.1s per Toxicity
- Per 10 Toxicity, +5% damage to poisoned mobs

## 6. Weakness

- **What players read:** Poisoners rely on toxins - in a straight fight they're weak.
- **What it actually does:** -25% damage to mobs that aren't poisoned or withered

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
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

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Toxicology

- **Shared or class-only:** class-only
- **Trained by:** poisoning mobs and brewing poison
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Toxicity
- **Tier II bonus:** +1 Toxicity
- **Tier III bonus:** +10% Venom regen
- **Tier IV bonus:** +1 Toxicity
- **Tier V bonus:** +2 Toxicity

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Assassin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| poisoner_start | Poisoner | start | - | - | - | +5% poison duration |
| poisoner_virulent | Virulent | small | 1 | 1 | poisoner_start | +10% poison damage |
| poisoner_tipped_arrows | Tipped Arrows | small | 1 | 1 | poisoner_start | crafting tipped arrows gives double |
| poisoner_paralytic | Paralytic | notable | 2 | 12 | poisoner_virulent | poisoned mobs also get Slowness I |
| poisoner_neurotoxin | Neurotoxin | notable | 2 | 15 | poisoner_tipped_arrows | poisoned mobs also get Weakness I |
| poisoner_plague_doctor | Plague Doctor | keystone | 3 | 30 | poisoner_paralytic, poisoner_neurotoxin | poison spreads to mobs within 2 blocks on death, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Poison Mastery | start | - | - | - | - |
| distillation | Distillation | small | 1 | 1 | start | +2 Toxicity |
| antidote | Antidote | small | 1 | 5 | start | players within 6 blocks also immune to Poison |
| poisonous_touch | Poisonous Touch | notable | 2 | 15 | distillation | unlocks Poison Needle ability |
| lethal_dose | Lethal Dose | notable | 2 | 20 | antidote | Poison II always |
| toxic_blood | Toxic Blood | keystone | 3 | 35 | poisonous_touch, lethal_dose | mobs that hit you are poisoned; healing on you is 15% weaker |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| poisoner_stat_start | Poisoner | start | - | - | - | - |
| poisoner_precise_doser | Precise Doser | small | 2 | 1 | poisoner_stat_start | +2 Dexterity, +1 Intelligence |
| poisoner_chemist | Chemist | small | 2 | 5 | poisoner_precise_doser | +2 Intelligence, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Poison Needle** (Poisonous Touch: a quick jab that applies Poison III, Slowness and Nausea, 30 Venom).
- Book talents: Poison Mastery, Poisonous Touch.
