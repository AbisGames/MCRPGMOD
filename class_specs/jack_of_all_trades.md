# Class: Jack-of-All-Trades

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Jack-of-All-Trades' (p. 91). -->

## 1. Basics

- **Id:** jack_of_all_trades
- **Display name:** Jack-of-All-Trades
- **Description:** A dabbler in everything who finds a way to succeed when they should fail.
- **Icon item:** bundle
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 3 |
| Dexterity    | 4 |
| Agility      | 4 |
| Intelligence | 3 |
| Wisdom       | 3 |
| Endurance    | 3 |
| Luck         | 2 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Aptitude
- **Description:** A little bit of everything.
- **What each point gives:** +0.2% to all damage and +0.2% movement speed
- **Max points:** 100

### Resource bar

- **Name:** Knack
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** patchwork (#8FA35A)

## 4. Starting kit

- 1 × stone_sword
- 1 × bow
- 16 × arrow
- 1 × stone_pickaxe
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Aptitude: once every 60s, your next hit is a guaranteed critical with +20% damage
- Flexible Profession: every shared skill trains 25% faster for you
- Uncommon Ability: +1 Luck and no gear is worse than neutral for you
- Prodigy: each time you sleep, one random stat gets +2 until you sleep again

### Special passives

- Skill training speed +0.5% per Aptitude
- Per 10 Aptitude, Prodigy bonus +1

## 6. Weakness

- **What players read:** Master of none.
- **What it actually does:** you can never reach Tier V in any skill (max Tier IV)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | proficient |
| Bows | proficient |
| Crossbows | proficient |
| Shields | proficient |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | neutral |
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

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Herbalism

- Shared skill - full definition in `witch.md`

### Skill: Dabbling

- **Shared or class-only:** class-only
- **Trained by:** using 4+ different item types in combat in one day
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Aptitude
- **Tier II bonus:** +1 Aptitude
- **Tier III bonus:** +10% Knack regen
- **Tier IV bonus:** +1 Aptitude
- **Tier V bonus:** +2 Aptitude

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Thief

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| jack_of_all_trades_start | Jack-of-All-Trades | start | - | - | - | +1 Luck |
| jack_of_all_trades_handy | Handy | small | 1 | 1 | jack_of_all_trades_start | +3% all damage |
| jack_of_all_trades_quick_study | Quick Study | small | 1 | 1 | jack_of_all_trades_start | +10% experience |
| jack_of_all_trades_improviser | Improviser | notable | 2 | 12 | jack_of_all_trades_handy | +5% attack speed |
| jack_of_all_trades_polymath | Polymath | notable | 2 | 15 | jack_of_all_trades_quick_study | +1 to every stat |
| jack_of_all_trades_renaissance | Renaissance | keystone | 3 | 30 | jack_of_all_trades_improviser, jack_of_all_trades_polymath | +2 to every stat, but -10% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Aptitude | start | - | - | - | - |
| natural | Natural | small | 1 | 1 | start | +2 Aptitude |
| magical_epiphany | Magical Epiphany | small | 1 | 5 | start | brewing and enchanting cost slightly less (-1 level, 10% faster brew) |
| flexible_profession | Flexible Profession | notable | 2 | 15 | natural | skills train 15% faster |
| prodigy | Prodigy | notable | 2 | 20 | magical_epiphany | Prodigy picks the stat you most recently used |
| uncommon_ability | Uncommon Ability | keystone | 3 | 35 | flexible_profession, prodigy | Aptitude crit triggers every 30s; -10% max Knack |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| jack_of_all_trades_stat_start | Jack-of-All-Trades | start | - | - | - | - |
| jack_of_all_trades_versatile | Versatile | small | 2 | 1 | jack_of_all_trades_stat_start | +1 Strength, +1 Dexterity, +1 Intelligence |
| jack_of_all_trades_lucky_generalist | Lucky Generalist | small | 2 | 5 | jack_of_all_trades_versatile | +2 Luck, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Aptitude, Flexible Profession, Magical Epiphany, Prodigy, Uncommon Ability.
- This is the one class where every gear tier is proficient - a true generalist.
