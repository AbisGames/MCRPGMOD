# Class: Astromancer

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Astromancer' (p. 80). -->

## 1. Basics

- **Id:** astromancer
- **Display name:** Astromancer
- **Description:** A wielder of sun, moon and starlight who blazes like a beacon and blinds foes.
- **Icon item:** glowstone_dust
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1.15 |
| Strength     | 1 | x0.75 |
| Dexterity    | 2 | x1 |
| Agility      | 2 | x0.85 |
| Intelligence | 7 | x1.25 |
| Wisdom       | 7 | x1.15 |
| Endurance    | 2 | x0.85 |
| Luck         | 2 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Radiance
- **Description:** The celestial light you hold within.
- **What each point gives:** +1% magic damage while you are outdoors
- **Max points:** 100

### Resource bar

- **Name:** Starlight
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** star gold (#FFE27A)

## 4. Starting kit

- 1 × spyglass
- 4 × glowstone_dust
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Inner Radiance: you emit light (dynamic light level 12) while Starlight is above half
- Intense Light: +20% damage against undead and mobs that burn in daylight
- Starlight regenerates twice as fast under the open night sky and in daylight

### Special passives

- Blinding Corona: per 10 Radiance, +5% chance your hits give Blindness for 2s
- Light radius +1 per 20 Radiance

## 6. Weakness

- **What players read:** Starlight fades in deep places.
- **What it actually does:** no Starlight regen and -15% damage underground with no sky access, and in the Nether

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | proficient |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
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
| Ring | 1 |
| Cloak | 1 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Stargazing

- **Shared or class-only:** class-only
- **Trained by:** spending time outdoors at night and killing mobs while glowing
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Radiance
- **Tier II bonus:** +1 Radiance
- **Tier III bonus:** +10% Starlight regen
- **Tier IV bonus:** +1 Radiance
- **Tier V bonus:** +2 Radiance

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Oracle

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| astromancer_start | Astromancer | start | - | - | - | +5% damage outdoors |
| astromancer_sunlit | Sunlit | small | 1 | 1 | astromancer_start | +6% magic damage in daylight |
| astromancer_moonlit | Moonlit | small | 1 | 1 | astromancer_start | +6% magic damage at night |
| astromancer_beacon | Beacon | notable | 2 | 12 | astromancer_sunlit | players within 8 blocks get Night Vision |
| astromancer_solar_flare | Solar Flare | notable | 2 | 15 | astromancer_moonlit | mobs that hit you have 10% chance to be set on fire |
| astromancer_power_of_the_sun | Power of the Sun | keystone | 3 | 30 | astromancer_beacon, astromancer_solar_flare | +25% damage outdoors, but -25% damage underground |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Inner Radiance | start | - | - | - | - |
| starfire | Starfire | small | 1 | 1 | start | +2 Radiance |
| corona | Corona | small | 1 | 5 | start | Blindness chance +5% |
| intense_light | Intense Light | notable | 2 | 15 | starfire | +10% more damage vs undead |
| blinding_corona | Blinding Corona | notable | 2 | 20 | corona | unlocks Corona Burst ability |
| living_star | Living Star | keystone | 3 | 35 | intense_light, blinding_corona | you glow and give Glowing to every mob within 10 blocks; mobs notice you from 50% farther |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| astromancer_stat_start | Astromancer | start | - | - | - | - |
| astromancer_celestial_mind | Celestial Mind | small | 2 | 1 | astromancer_stat_start | +2 Wisdom, +1 Intelligence |
| astromancer_bright | Bright | small | 2 | 5 | astromancer_celestial_mind | +2 Intelligence, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Corona Burst** (blind all mobs within 3 blocks, 30 Starlight), **Sunbeam** (ray attack, 20 Starlight).
- Book talents: Inner Radiance, Intense Light, Blinding Corona, Power of the Sun. Tradition: Celestial.
