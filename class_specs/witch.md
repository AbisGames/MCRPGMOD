# Class: Witch

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Witch' (p. 75). -->

## 1. Basics

- **Id:** witch
- **Display name:** Witch
- **Description:** Keeper of the old faerie magic. Guides allies, brews potent potions and flies on an enchanted broom.
- **Icon item:** cauldron
- **Selectable by players:** yes
- **Base health:** 22

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 2 | x0.85 |
| Strength     | 0 | x0.7 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x1 |
| Intelligence | 5 | x1.15 |
| Wisdom       | 6 | x1.3 |
| Endurance    | 2 | x0.85 |
| Luck         | 1 | x1.15 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Craft
- **Description:** Mastery of the old ways - herbs, hexes and green fire.
- **What each point gives:** +1% duration of potions you brew and drink
- **Max points:** 100

### Resource bar

- **Name:** Witchery
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** witch green (#5BBF3A)

## 4. Starting kit

- 1 × cauldron
- 1 × brewing_stand
- 3 × glass_bottle
- 2 × nether_wart
- 1 × leather_helmet
- 6 × bread

## 5. Passives

- Guidance: other players within 8 blocks get +1 Luck
- You take no damage from witch potions thrown by witches
- Potions you brew last 20% longer
- Your Instant Health/Damage splash potions are 15% stronger

### Special passives

- +1% chance per 2 Craft to brew one extra potion per brewing cycle
- Guidance range +1 block per 10 Craft

## 6. Weakness

- **What players read:** Old stories end with a witch on a pyre.
- **What it actually does:** takes 25% more fire damage and burns for 50% longer

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | neutral |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | forbidden |
| Specific items / exceptions: | stick / blaze_rod: staff at proficient |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 2 |
| Ring | 1 |
| Cloak | 1 |
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

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Herbalism

- **Shared or class-only:** shared
- **Trained by:** harvesting fully grown crops, berries, flowers and mushrooms
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +5% chance of double crop drops
- **Tier II bonus:** +5% bone meal effect
- **Tier III bonus:** +5% chance of double crop drops
- **Tier IV bonus:** eating berries/crops restores +1 hunger
- **Tier V bonus:** +10% chance of double crop drops

### Skill: Coven Lore

- **Shared or class-only:** class-only
- **Trained by:** brewing potions and throwing them at mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Craft
- **Tier II bonus:** +1 Craft
- **Tier III bonus:** +10% Witchery regen
- **Tier IV bonus:** +1 Craft
- **Tier V bonus:** +2 Craft

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** top of the web (Power quarter), ring 2, leaning toward the Faith quarter

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| witch_start | Witch | start | - | - | - | +5% potion duration |
| witch_hedge | Hedge Lore | small | 1 | 1 | witch_start | +8% potion duration |
| witch_familiar | Familiar | small | 1 | 1 | witch_start | tamed cats near you give +1 armor |
| witch_hex | Hex | notable | 2 | 10 | witch_hedge | harmful potion effects you apply last 20% longer |
| witch_coven | Coven | notable | 2 | 12 | witch_familiar, witch_hex | +10% max Witchery, Guidance range +2 |
| witch_crone | Crone's Bargain | keystone | 3 | 25 | witch_coven | all potions you drink last 50% longer, but -15% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Guidance | start | - | - | - | - |
| witch_fire | Witch Fire | small | 1 | 1 | start | +2 Craft |
| green_flame | Green Flame | small | 1 | 5 | start | your fire damage to mobs is +15% |
| flying_broom | Flying Broom | notable | 2 | 15 | witch_fire | unlocks the Flying Broom ability |
| brewmistress | Brewmistress | notable | 2 | 20 | green_flame | brewing takes 25% less time |
| lasting_bond | Lasting Bond | keystone | 3 | 35 | flying_broom, brewmistress | bond with one player: you each heal 50% of what the other heals while within 32 blocks; Guidance range -2 |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| witch_stat_start | Witch | start | - | - | - | - |
| witch_wise | Wise Woman | small | 2 | 1 | witch_stat_start | +2 Wisdom, +1 Intelligence |
| witch_wild | Wildwood | small | 2 | 5 | witch_wise | +2 Agility, +1 Luck |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active abilities for later: **Witch Fire** (become invisible, steer a green fireball that burns mobs, 15 Witchery/s), **Flying Broom** (hold a broom-like item - e.g. a renamed stick - and fly, drains Witchery), **Lasting Bond** (see keystone).
- Book talents: Guidance, Witch Fire, Flying Broom, Lasting Bond.
- **Co-design question:** Craft + Witchery. Should the Flying Broom need a custom broom item?
