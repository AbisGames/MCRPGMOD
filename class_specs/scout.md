# Class: Scout

<!-- Source: Shadow of the Demon Lord core rulebook, expert path 'Scout' (p. 70). -->

## 1. Basics

- **Id:** scout
- **Display name:** Scout
- **Description:** A fast, watchful pathfinder who ranges ahead, is never caught off guard and exposes enemies' weak spots.
- **Icon item:** spyglass
- **Selectable by players:** yes
- **Base health:** 24

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.7 |
| Strength     | 1 | x1 |
| Dexterity    | 4 | x1.15 |
| Agility      | 6 | x1.3 |
| Intelligence | 2 | x1.15 |
| Wisdom       | 1 | x0.85 |
| Endurance    | 2 | x1 |
| Luck         | 1 | x0.85 |
| Special stat (section 3) | 8 | |

## 3. Special stat

- **Name:** Awareness
- **Description:** Sharp senses and good instincts.
- **What each point gives:** +0.3% movement speed
- **Max points:** 100

### Resource bar

- **Name:** Stride
- **Max at 0 points:** 30
- **Extra max per special-stat point:** 2
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.05
- **Starts full when the class is chosen:** yes
- **Colour:** sky blue (#6CC6F0)

## 4. Starting kit

- 1 × stone_sword
- 1 × bow
- 24 × arrow
- 1 × spyglass
- 1 × leather_boots
- 6 × bread

## 5. Passives

- +12% movement speed
- Alertness: hostile mobs within 10 blocks that are targeting you glow
- Trackless: your footsteps never trigger sculk sensors, shriekers or tripwires
- Forward Observer: +20% stealth (smaller mob detection range) when no other player is within 16 blocks

### Special passives

- Alertness range +1 block per 5 Awareness
- -1% fall damage per Awareness point

## 6. Weakness

- **What players read:** Scouts travel light - armor just gets in the way.
- **What it actually does:** -1 armor and -4% movement speed for every heavy armor piece worn

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | neutral |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | neutral |
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
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 2
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Acrobatics

- Shared skill - full definition in `rogue.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Athletics

- **Shared or class-only:** shared
- **Trained by:** sprinting
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +2% sprint speed
- **Tier II bonus:** -5% hunger from sprinting
- **Tier III bonus:** +2% sprint speed
- **Tier IV bonus:** -5% hunger from sprinting
- **Tier V bonus:** +4% sprint speed

### Skill: Pathfinding

- **Shared or class-only:** class-only
- **Trained by:** discovering new chunks / travelling long distances
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Awareness
- **Tier II bonus:** +1 Awareness
- **Tier III bonus:** +10% Stride regen
- **Tier IV bonus:** +1 Awareness
- **Tier V bonus:** +2 Awareness

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** bottom of the web (Trickery quarter), ring 2, centre

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| scout_start | Scout | start | - | - | - | +3% movement speed |
| scout_light_feet | Light Feet | small | 1 | 1 | scout_start | +4% movement speed |
| scout_keen_eyes | Keen Eyes | small | 1 | 1 | scout_start | +4 blocks Alertness range |
| scout_skirmisher | Skirmisher | notable | 2 | 10 | scout_light_feet | +10% bow damage while moving |
| scout_lookout | Lookout | notable | 2 | 12 | scout_keen_eyes | can't be hit by sneak-attack bonuses (you are never 'unaware') |
| scout_pathfinder | Pathfinder | keystone | 3 | 25 | scout_skirmisher, scout_lookout | +15% movement speed and no hunger from sprinting, but -15% melee damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Alertness | start | - | - | - | - |
| forward_observer | Forward Observer | small | 1 | 1 | start | +2 Awareness |
| quick_reflexes | Quick Reflexes | small | 1 | 5 | start | after taking damage, +20% speed for 2s |
| reveal_weakness | Reveal Weakness | notable | 2 | 15 | forward_observer | unlocks Reveal Weakness ability |
| trackless | Ghost Trail | notable | 2 | 20 | quick_reflexes | mobs lose track of you 30% faster |
| low_blow | Low Blow | keystone | 3 | 35 | reveal_weakness, trackless | when a revealed mob takes damage from an ally, you get a free 50% damage hit on it; -10% max Stride |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| scout_stat_start | Scout | start | - | - | - | - |
| scout_swift | Swift | small | 2 | 1 | scout_stat_start | +2 Agility, +1 Endurance |
| scout_hawkeye | Hawkeye | small | 2 | 5 | scout_swift | +2 Dexterity, +1 Luck |

## 12. Anything else

- **Power tier: Expert.** Book levels 3, 6, 9. Balanced ~30% above a novice: 20 stat points, 8 special points, 4 trinket slots, 2+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 20.
- Active ability for later: **Reveal Weakness** (mark a mob: all players deal +15% damage to it for 6s, 15 Stride), **Dash** (burst of speed, 10 Stride).
- Book talents: Alertness, Forward Observer, Quick Reflexes, Trackless, Reveal Weakness, Low Blow.
- **Co-design question:** Awareness + Stride. Should Stride be spent on a sprint burst, or only fuel future abilities?
