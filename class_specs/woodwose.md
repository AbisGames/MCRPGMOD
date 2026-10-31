# Class: Woodwose

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Woodwose' (p. 98). -->

## 1. Basics

- **Id:** woodwose
- **Display name:** Woodwose
- **Description:** A wild guardian of the land, slowly turning into a plant - bark skin, emerald eyes, leaves in the hair.
- **Icon item:** oak_leaves
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 5 | x1.15 |
| Strength     | 3 | x0.7 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x1.15 |
| Intelligence | 3 | x0.85 |
| Wisdom       | 7 | x1.3 |
| Endurance    | 3 | x1 |
| Luck         | 1 | x0.85 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Verdure
- **Description:** How much of the forest grows in you.
- **What each point gives:** +0.1 armor and +0.5% regeneration in natural biomes
- **Max points:** 100

### Resource bar

- **Name:** Sap
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** moss green (#4E8A2F)

## 4. Starting kit

- 1 × wooden_axe
- 8 × oak_sapling
- 4 × bone_meal
- 1 × leather_chestplate
- 6 × apple

## 5. Passives

- Forest Hide: in foliage (leaves, tall grass, ferns), sneaking makes you invisible
- Power of Nature: -50% fire damage while wearing leather armor; +2 armor in forests
- Nature's Resilience: bone-mealing a plant gives you +2 absorption hearts for 30s (60s cooldown)
- Nature Bond: you can rest in a forest (sneak still for 10s) to heal 1 HP every 2s

### Special passives

- Forest armor +1 per 20 Verdure
- Nature Bond heal speed +1% per Verdure

## 6. Weakness

- **What players read:** Woodwoses are part plant now. Fire is their worst fear.
- **What it actually does:** +25% fire damage while not wearing leather, and axes deal +20% damage to you

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | proficient |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | clumsy |
| Shields | neutral |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | forbidden |
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

### Skill: Herbalism

- Shared skill - full definition in `witch.md`

### Skill: Beast Bond

- Shared skill - full definition in `druid.md`

### Skill: Wildwood

- **Shared or class-only:** class-only
- **Trained by:** planting trees and fighting in forests
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Verdure
- **Tier II bonus:** +1 Verdure
- **Tier III bonus:** +10% Sap regen
- **Tier IV bonus:** +1 Verdure
- **Tier V bonus:** +2 Verdure

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Druid

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| woodwose_start | Woodwose | start | - | - | - | +1 armor in forests |
| woodwose_barkskin | Barkskin | small | 1 | 1 | woodwose_start | +2 armor |
| woodwose_thorns | Thorns | small | 1 | 1 | woodwose_start | mobs that melee you take 1 damage |
| woodwose_deep_roots | Deep Roots | notable | 2 | 12 | woodwose_barkskin | +20% knockback resistance on grass |
| woodwose_overgrowth | Overgrowth | notable | 2 | 15 | woodwose_thorns | crops grow 25% faster near you |
| woodwose_ent | Ent | keystone | 3 | 30 | woodwose_deep_roots, woodwose_overgrowth | +6 max health and +3 armor in forests, but -15% movement speed everywhere |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Forest Hide | start | - | - | - | - |
| green_blood | Green Blood | small | 1 | 1 | start | +2 Verdure |
| oak_hide | Oak Hide | small | 1 | 5 | start | unlocks Oak Hide ability |
| nature_s_resilience | Nature's Resilience | notable | 2 | 15 | green_blood | absorption +2 more hearts |
| nature_bond | Nature Bond | notable | 2 | 20 | oak_hide | unlocks Treeform ability |
| green_man | Green Man | keystone | 3 | 35 | nature_s_resilience, nature_bond | passive animals follow you and hostile spiders ignore you; -10% max health outside forests |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| woodwose_stat_start | Woodwose | start | - | - | - | - |
| woodwose_rooted | Rooted | small | 2 | 1 | woodwose_stat_start | +2 Vitality, +1 Wisdom |
| woodwose_wild | Wild | small | 2 | 5 | woodwose_rooted | +2 Wisdom, +1 Strength |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Oak Hide** (+4 armor for 30s, 30 Sap), **Treeform** (become a tree - fully hidden, heal all damage while resting, book level 10).
- Book talents: Power of Nature, Forest Hide, Nature Bond, Nature's Resilience. Tradition: Nature.
