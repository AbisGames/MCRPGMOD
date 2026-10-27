# Class: Zealot

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Zealot' (p. 98). -->

## 1. Basics

- **Id:** zealot
- **Display name:** Zealot
- **Description:** A wandering fanatic who trades sanity for second chances - failure is just a reason to try again.
- **Icon item:** flint
- **Selectable by players:** yes
- **Base health:** 36

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 7 |
| Strength     | 6 |
| Dexterity    | 2 |
| Agility      | 2 |
| Intelligence | 0 |
| Wisdom       | 4 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Fervor
- **Description:** Burning, self-destructive faith.
- **What each point gives:** +0.5% damage and +0.3% knockback resistance
- **Max points:** 100

### Resource bar

- **Name:** Madness
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** -0.3
- **Extra regen per second per special-stat point:** 0
- **Starts full when the class is chosen:** no (fills when you use Zeal, drains 0.3/s; at max you go Violently Mad - see weakness)
- **Colour:** fever red (#D93A2B)

## 4. Starting kit

- 1 × iron_axe
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Zeal: when a hit would bring you below 2 hearts, 30% chance to shrug it off entirely (+10 Madness)
- After Zeal triggers, immune to Slowness, Weakness and Nausea for 5s
- Divine Might: hits within 5s of a Zeal trigger deal +20% damage

### Special passives

- Zeal chance +0.3% per Fervor
- Per 10 Fervor, Divine Might +5%

## 6. Weakness

- **What players read:** Zeal cracks the mind.
- **What it actually does:** when Madness is full: Violent Madness - for 10s you attack anything near you (including allies), +30% damage, then Madness resets

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | mastery |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | clumsy |
| Shields | neutral |
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

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Mace Training

- Shared skill - full definition in `priest.md`

### Skill: Self-Denial

- **Shared or class-only:** class-only
- **Trained by:** surviving hits with Zeal
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Fervor
- **Tier II bonus:** +1 Fervor
- **Tier III bonus:** +5 max Madness
- **Tier IV bonus:** +1 Fervor
- **Tier V bonus:** +2 Fervor

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Oracle

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| zealot_start | Zealot | start | - | - | - | +3 max health |
| zealot_ascetic | Ascetic | small | 1 | 1 | zealot_start | -15% hunger drain |
| zealot_flagellant | Flagellant | small | 1 | 1 | zealot_start | +5% damage per missing heart (max +25%) |
| zealot_unbroken | Unbroken | notable | 2 | 12 | zealot_ascetic | Zeal chance +10% |
| zealot_fanatic | Fanatic | notable | 2 | 15 | zealot_flagellant | immune to Nausea and Darkness always |
| zealot_martyrdom | Martyrdom | keystone | 3 | 30 | zealot_unbroken, zealot_fanatic | Zeal chance doubled, but you never naturally regenerate above half health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Zeal | start | - | - | - | - |
| burning_faith | Burning Faith | small | 1 | 1 | start | +2 Fervor |
| iron_conviction | Iron Conviction | small | 1 | 5 | start | post-Zeal immunity lasts 3s longer |
| divine_might | Divine Might | notable | 2 | 15 | burning_faith | +10% more Divine Might damage |
| violent_madness | Violent Madness | notable | 2 | 20 | iron_conviction | Violent Madness no longer targets players |
| prophet_of_ruin | Prophet of Ruin | keystone | 3 | 35 | divine_might, violent_madness | Zeal triggers at 3 hearts; Madness fills twice as fast |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| zealot_stat_start | Zealot | start | - | - | - | - |
| zealot_fanatical | Fanatical | small | 2 | 1 | zealot_stat_start | +2 Strength, +1 Wisdom |
| zealot_wasted_frame | Wasted Frame | small | 2 | 5 | zealot_fanatical | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Madness is a reversed bar (fills up, drains), like Sorcerer Strain and Berserker Rage.
- Book talents: Zeal, Violent Madness, Divine Might.
