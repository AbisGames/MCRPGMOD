# Class: Technomancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Technomancer' (p. 96). -->

## 1. Basics

- **Id:** technomancer
- **Display name:** Technomancer
- **Description:** A mage-engineer who fuses Technomancy with redstone - quick gadgets, and later objects that come to life.
- **Icon item:** observer
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 2 |
| Dexterity    | 4 |
| Agility      | 1 |
| Intelligence | 9 |
| Wisdom       | 4 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Circuitry
- **Description:** How deeply magic and mechanism mesh in your hands.
- **What each point gives:** +0.5% chance a crafted redstone item is doubled
- **Max points:** 100

### Resource bar

- **Name:** Voltage
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** redstone red (#E0241B)

## 4. Starting kit

- 8 × redstone
- 2 × repeater
- 1 × crossbow
- 8 × arrow
- 1 × leather_chestplate
- 4 × bread

## 5. Passives

- Invention: each time you spend Voltage, you get a 'gadget' charge - your next attack or check (dodge, crit) is improved (+20%)
- Redstone you place powers 2 blocks farther
- +10% crossbow damage and reload speed

### Special passives

- Gadget bonus +0.5% per Circuitry
- Per 20 Circuitry, +1 gadget charge stored

## 6. Weakness

- **What players read:** Gadgets break in harsh environments.
- **What it actually does:** Voltage regen stops while wet and gadgets fizzle in the Nether

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | neutral |
| Maces | proficient |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | mastery |
| Shields | neutral |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
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
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Smithing

- Shared skill - full definition in `artificer.md`

### Skill: Tinkerwork

- **Shared or class-only:** class-only
- **Trained by:** crafting and placing redstone components
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Circuitry
- **Tier II bonus:** +1 Circuitry
- **Tier III bonus:** +10% Voltage regen
- **Tier IV bonus:** +1 Circuitry
- **Tier V bonus:** +2 Circuitry

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Artificer

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| technomancer_start | Technomancer | start | - | - | - | +5% crossbow damage |
| technomancer_capacitor | Capacitor | small | 1 | 1 | technomancer_start | +20% max Voltage |
| technomancer_overclocked | Overclocked | small | 1 | 1 | technomancer_start | +10% attack speed |
| technomancer_spring_loaded | Spring-Loaded | notable | 2 | 12 | technomancer_capacitor | +15% crossbow reload speed |
| technomancer_gyro_stabilizer | Gyro Stabilizer | notable | 2 | 15 | technomancer_overclocked | +10% knockback resistance |
| technomancer_mad_inventor | Mad Inventor | keystone | 3 | 30 | technomancer_spring_loaded, technomancer_gyro_stabilizer | gadget bonus doubled, but 10% chance gadgets misfire and damage you |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Invention | start | - | - | - | - |
| wiring | Wiring | small | 1 | 1 | start | +2 Circuitry |
| pocket_gadget | Pocket Gadget | small | 1 | 5 | start | +1 gadget charge |
| animate_object | Animate Object | notable | 2 | 15 | wiring | unlocks Animate Object ability |
| tesla_coil | Tesla Coil | notable | 2 | 20 | pocket_gadget | mobs that hit you take 2 lightning damage |
| clockwork_legion | Clockwork Legion | keystone | 3 | 35 | animate_object, tesla_coil | Animate Object can keep 3 at once; -15% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| technomancer_stat_start | Technomancer | start | - | - | - | - |
| technomancer_technical | Technical | small | 2 | 1 | technomancer_stat_start | +2 Intelligence, +1 Dexterity |
| technomancer_steady_fingers | Steady Fingers | small | 2 | 5 | technomancer_technical | +2 Dexterity, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Animate Object** (turn a placed block - e.g. an anvil or cauldron - into a temporary friendly construct for 60s, 50 Voltage).
- Book talents: Invention, Animate Object. Tradition: Technomancy.
