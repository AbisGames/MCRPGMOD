# Class: Thaumaturge

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Thaumaturge' (p. 97). -->

## 1. Basics

- **Id:** thaumaturge
- **Display name:** Thaumaturge
- **Description:** A slightly unhinged master of Chaos magic who rolls the dice on every spell - and usually wins.
- **Icon item:** end_crystal
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 0 |
| Dexterity    | 2 |
| Agility      | 3 |
| Intelligence | 7 |
| Wisdom       | 5 |
| Endurance    | 1 |
| Luck         | 5 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Entropy
- **Description:** How much chaos you can ride without falling off.
- **What each point gives:** +0.5% to all random effects in your favour (chaos, crits, loot)
- **Max points:** 100

### Resource bar

- **Name:** Chaos
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** chaotic rainbow (#FF3DD5)

## 4. Starting kit

- 1 × stone_sword
- 3 × random potion
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Seize Chaos: 10% of your hits trigger a random effect - double damage, ignite, Levitation on the target, a heal for you, or (rarely) a small explosion on yourself
- Fluid Magic: potions you drink have 20% chance to also give a random positive effect
- +2 Luck

### Special passives

- Seize Chaos chance +0.2% per Entropy
- Per 10 Entropy, bad chaos outcomes are 5% rarer

## 6. Weakness

- **What players read:** Chaos is never fully tamed.
- **What it actually does:** every 60s in combat, a random harmful effect hits you for 3s

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
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
| Belt | 0 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Alchemy

- Shared skill - full definition in `magician.md`

### Skill: Wild Surges

- **Shared or class-only:** class-only
- **Trained by:** triggering Seize Chaos
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Entropy
- **Tier II bonus:** +1 Entropy
- **Tier III bonus:** +10% Chaos regen
- **Tier IV bonus:** +1 Entropy
- **Tier V bonus:** +2 Entropy

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Sorcerer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| thaumaturge_start | Thaumaturge | start | - | - | - | +1 Luck |
| thaumaturge_lucky_streak | Lucky Streak | small | 1 | 1 | thaumaturge_start | +1 Luck |
| thaumaturge_wild_magic | Wild Magic | small | 1 | 1 | thaumaturge_start | +5% Seize Chaos chance |
| thaumaturge_probability_bend | Probability Bend | notable | 2 | 12 | thaumaturge_lucky_streak | bad outcomes 20% rarer |
| thaumaturge_chaos_shield | Chaos Shield | notable | 2 | 15 | thaumaturge_wild_magic | 10% chance an incoming hit is redirected to a random nearby mob |
| thaumaturge_agent_of_chaos | Agent of Chaos | keystone | 3 | 30 | thaumaturge_probability_bend, thaumaturge_chaos_shield | Seize Chaos chance doubled, but outcomes are fully random |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Seize Chaos | start | - | - | - | - |
| dice | Dice | small | 1 | 1 | start | +2 Entropy |
| reroll | Reroll | small | 1 | 5 | start | unlocks Reroll ability |
| fluid_magic | Fluid Magic | notable | 2 | 15 | dice | random potion effect chance 35% |
| pandemonium | Pandemonium | notable | 2 | 20 | reroll | chaos outcomes can hit 2 mobs |
| lord_of_misrule | Lord of Misrule | keystone | 3 | 35 | fluid_magic, pandemonium | you're immune to your own chaos backfires; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| thaumaturge_stat_start | Thaumaturge | start | - | - | - | - |
| thaumaturge_unpredictable | Unpredictable | small | 2 | 1 | thaumaturge_stat_start | +2 Luck, +1 Intelligence |
| thaumaturge_quick_wit | Quick Wit | small | 2 | 5 | thaumaturge_unpredictable | +2 Intelligence, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Reroll** (cancel the last harmful effect on you and roll a new random effect, 25 Chaos).
- Book talents: Seize Chaos, Fluid Magic. Tradition: Chaos.
