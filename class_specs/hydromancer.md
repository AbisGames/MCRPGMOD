# Class: Hydromancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Hydromancer' (p. 89). -->

## 1. Basics

- **Id:** hydromancer
- **Display name:** Hydromancer
- **Description:** Bonded to a water genie, the hydromancer flows out of grapples, swims like a dolphin and becomes water itself.
- **Icon item:** heart_of_the_sea
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 1 |
| Dexterity    | 2 |
| Agility      | 5 |
| Intelligence | 6 |
| Wisdom       | 6 |
| Endurance    | 1 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Tide
- **Description:** The pull of the deep inside you.
- **What each point gives:** +1% swim speed and +0.5% damage while wet
- **Max points:** 100

### Resource bar

- **Name:** Current
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** ocean blue (#1F6FD1)

## 4. Starting kit

- 1 × trident
- 1 × turtle_helmet
- 1 × water_bucket
- 6 × cooked_salmon

## 5. Passives

- Swimmer: breathe underwater, swim 40% faster, no mining slowdown underwater
- Flow: while wet (rain or water), you can't be slowed or caught by cobwebs/powder snow and +10% damage
- Watery Form: in water, take 20% less damage from weapons and projectiles

### Special passives

- Watery Form reduction +0.2% per Tide
- Per 10 Tide, +1 Current regen in rain

## 6. Weakness

- **What players read:** Hydromancers dry out.
- **What it actually does:** -20% damage and Current regen in the Nether and deserts, and +25% fire damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | neutral |
| Tridents | mastery |
| Bows | proficient |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | proficient |
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

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Swimming

- **Shared or class-only:** shared
- **Trained by:** swimming distance and fighting underwater
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +5% swim speed
- **Tier II bonus:** +10% oxygen
- **Tier III bonus:** +5% swim speed
- **Tier IV bonus:** +5% underwater damage
- **Tier V bonus:** +10% swim speed

### Skill: Tidecalling

- **Shared or class-only:** class-only
- **Trained by:** killing mobs while wet
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Tide
- **Tier II bonus:** +1 Tide
- **Tier III bonus:** +10% Current regen
- **Tier IV bonus:** +1 Tide
- **Tier V bonus:** +2 Tide

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Druid

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| hydromancer_start | Hydromancer | start | - | - | - | +10% swim speed |
| hydromancer_gills | Gills | small | 1 | 1 | hydromancer_start | +5% underwater damage |
| hydromancer_riptide | Riptide | small | 1 | 1 | hydromancer_start | tridents with Riptide work without rain while Current is above half |
| hydromancer_undertow | Undertow | notable | 2 | 12 | hydromancer_gills | mobs you hit in water get Slowness II |
| hydromancer_mist | Mist | notable | 2 | 15 | hydromancer_riptide | +10% dodge in rain |
| hydromancer_child_of_the_sea | Child of the Sea | keystone | 3 | 30 | hydromancer_undertow, hydromancer_mist | +30% damage in water, but -20% damage on dry land |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Flow | start | - | - | - | - |
| wave | Wave | small | 1 | 1 | start | +2 Tide |
| slippery | Slippery | small | 1 | 5 | start | immune to being held (guardians' beam slow too) |
| watery_form | Watery Form | notable | 2 | 15 | wave | unlocks Watery Form ability |
| tsunami | Tsunami | notable | 2 | 20 | slippery | unlocks Wave ability |
| ocean_s_heart | Ocean's Heart | keystone | 3 | 35 | watery_form, tsunami | drowned and guardians are neutral to you; -15% max health on land |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| hydromancer_stat_start | Hydromancer | start | - | - | - | - |
| hydromancer_fluid | Fluid | small | 2 | 1 | hydromancer_stat_start | +2 Agility, +1 Wisdom |
| hydromancer_deep_mind | Deep Mind | small | 2 | 5 | hydromancer_fluid | +2 Intelligence, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Watery Form** (become water: half weapon damage, slip through 1-block gaps, invisible underwater, 40 Current), **Wave** (push mobs away, 25 Current).
- Book talents: Flow, Swimmer, Watery Form. Tradition: Water.
