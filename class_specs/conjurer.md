# Class: Conjurer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Conjurer' (p. 83). -->

## 1. Basics

- **Id:** conjurer
- **Display name:** Conjurer
- **Description:** A summoner who pulls monsters out of nothing to fight on their behalf.
- **Icon item:** zombie_spawn_egg
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 0 | x0.75 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x0.9 |
| Intelligence | 8 | x1.25 |
| Wisdom       | 6 | x1.1 |
| Endurance    | 2 | x0.9 |
| Luck         | 3 | x1.1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Summoning
- **Description:** How much of the void you can drag into the world.
- **What each point gives:** +1% damage and health for your summoned creatures
- **Max points:** 100

### Resource bar

- **Name:** Ether
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** void teal (#1E8C8C)

## 4. Starting kit

- 1 × stone_sword
- 1 × leather_chestplate
- 4 × ender_pearl
- 6 × bread

## 5. Passives

- Conjure Tiny Monster: every 30s while in combat, a friendly silverfish-sized minion (vex model) appears to fight for 15s
- Frightening Monsters: mobs hit by your minions get Weakness I for 3s
- Mobs you kill have a 10% chance to drop 2 experience orbs extra

### Special passives

- Minion damage +1% per Summoning point
- Per 20 Summoning, +1 minion at a time

## 6. Weakness

- **What players read:** All power, no personal combat skill.
- **What it actually does:** -20% damage dealt yourself

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
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Conjuration

- **Shared or class-only:** class-only
- **Trained by:** your minions killing mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Summoning
- **Tier II bonus:** +1 Summoning
- **Tier III bonus:** +10% Ether regen
- **Tier IV bonus:** +1 Summoning
- **Tier V bonus:** +2 Summoning

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Artificer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| conjurer_start | Conjurer | start | - | - | - | +5% minion damage |
| conjurer_bigger_beasts | Bigger Beasts | small | 1 | 1 | conjurer_start | minions +4 max health |
| conjurer_sharper_teeth | Sharper Teeth | small | 1 | 1 | conjurer_start | minions +10% damage |
| conjurer_lingering_summons | Lingering Summons | notable | 2 | 12 | conjurer_bigger_beasts | minions last 10s longer |
| conjurer_swarm | Swarm | notable | 2 | 15 | conjurer_sharper_teeth | +1 minion at a time |
| conjurer_horde_master | Horde Master | keystone | 3 | 30 | conjurer_lingering_summons, conjurer_swarm | +2 minions at a time, but each is 30% weaker |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Conjure Tiny Monster | start | - | - | - | - |
| gate | Gate | small | 1 | 1 | start | +2 Summoning |
| fearsome | Fearsome | small | 1 | 5 | start | minions also apply Slowness |
| powerful_monsters | Powerful Monsters | notable | 2 | 15 | gate | minions +20% damage |
| steady_gate | Steady Gate | notable | 2 | 20 | fearsome | minion spawn every 20s |
| conjure_horror | Conjure Horror | keystone | 3 | 35 | powerful_monsters, steady_gate | unlocks Conjure Horror ability; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| conjurer_stat_start | Conjurer | start | - | - | - | - |
| conjurer_summoner_s_mind | Summoner's Mind | small | 2 | 1 | conjurer_stat_start | +2 Intelligence, +1 Luck |
| conjurer_command | Command | small | 2 | 5 | conjurer_summoner_s_mind | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Conjure Horror** (summon a large monster - ravager-sized - for 20s, 60 Ether).
- Book talents: Conjure Tiny Monster, Frightening Monsters, Powerful Monsters. Tradition: Conjuration.
