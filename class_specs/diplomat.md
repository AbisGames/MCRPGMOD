# Class: Diplomat

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Diplomat' (p. 85). -->

## 1. Basics

- **Id:** diplomat
- **Display name:** Diplomat
- **Description:** A silver-tongued negotiator who turns enemies into allies and talks their way out of death.
- **Icon item:** emerald
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 4 | x1.1 |
| Strength     | 1 | x0.75 |
| Dexterity    | 3 | x0.9 |
| Agility      | 3 | x1 |
| Intelligence | 6 | x1.25 |
| Wisdom       | 6 | x1.1 |
| Endurance    | 1 | x0.9 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Rapport
- **Description:** Your knack for making people listen.
- **What each point gives:** -0.3% villager trade prices
- **Max points:** 100

### Resource bar

- **Name:** Persuasion
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** emerald (#2CC768)

## 4. Starting kit

- 8 × emerald
- 1 × stone_sword
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Master of Diplomacy: villager trades cost 25% less and villagers restock twice as often for you
- Stay the Hand: 15% chance that a melee hit against you is cancelled and the attacker stops targeting you for 3s
- Piglins never attack you while you hold gold; illagers are 30% slower to notice you

### Special passives

- Stay the Hand +0.3% per Rapport
- Per 10 Rapport, wandering traders offer 1 extra trade

## 6. Weakness

- **What players read:** Diplomats win with words, not blades.
- **What it actually does:** -25% melee and ranged damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
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
| Ring | 2 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Bartering

- **Shared or class-only:** shared
- **Trained by:** trading with villagers, wandering traders and piglins
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** -2% trade prices
- **Tier II bonus:** +1 villager restock
- **Tier III bonus:** -3% trade prices
- **Tier IV bonus:** +5% piglin barter quality
- **Tier V bonus:** -5% trade prices

### Skill: Negotiation

- **Shared or class-only:** class-only
- **Trained by:** cancelling attacks with Stay the Hand and trading
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Rapport
- **Tier II bonus:** +1 Rapport
- **Tier III bonus:** +10% Persuasion regen
- **Tier IV bonus:** +1 Rapport
- **Tier V bonus:** +2 Rapport

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Thief

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| diplomat_start | Diplomat | start | - | - | - | -5% trade prices |
| diplomat_charming | Charming | small | 1 | 1 | diplomat_start | -5% trade prices |
| diplomat_calm_words | Calm Words | small | 1 | 1 | diplomat_start | +5% Stay the Hand |
| diplomat_unexpected_alliances | Unexpected Alliances | notable | 2 | 12 | diplomat_charming | hostile mobs you cancel with Stay the Hand fight other mobs for 5s |
| diplomat_envoy | Envoy | notable | 2 | 15 | diplomat_calm_words | Hero of the Village lasts twice as long |
| diplomat_peacemaker | Peacemaker | keystone | 3 | 30 | diplomat_unexpected_alliances, diplomat_envoy | Stay the Hand 30%, but -40% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Stay the Hand | start | - | - | - | - |
| parley | Parley | small | 1 | 1 | start | +2 Rapport |
| soothing_words | Soothing Words | small | 1 | 5 | start | unlocks Soothe ability |
| silver_tongue | Silver Tongue | notable | 2 | 15 | parley | first trade with a new villager is 50% off |
| unexpected_alliances | Unexpected Alliances | notable | 2 | 20 | soothing_words | unlocks Turncoat ability |
| grand_ambassador | Grand Ambassador | keystone | 3 | 35 | silver_tongue, unexpected_alliances | neutral mobs never turn hostile on you; you deal -10% more damage |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| diplomat_stat_start | Diplomat | start | - | - | - | - |
| diplomat_eloquent | Eloquent | small | 2 | 1 | diplomat_stat_start | +2 Wisdom, +1 Intelligence |
| diplomat_persuasive | Persuasive | small | 2 | 5 | diplomat_eloquent | +2 Intelligence, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Soothe** (remove Nausea/Blindness/Weakness from a player, 15 Persuasion), **Turncoat** (one mob fights for you for 15s, 50 Persuasion).
- Book talents: Master of Diplomacy, Stay the Hand, Soothing Words, Unexpected Alliances.
