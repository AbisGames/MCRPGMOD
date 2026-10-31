# Class: Bard

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Bard' (p. 81). -->

## 1. Basics

- **Id:** bard
- **Display name:** Bard
- **Description:** A performer whose Song magic inspires allies, demoralises foes and charms almost anyone.
- **Icon item:** note_block
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x0.85 |
| Strength     | 1 | x0.85 |
| Dexterity    | 4 | x1 |
| Agility      | 3 | x1.15 |
| Intelligence | 5 | x1.15 |
| Wisdom       | 6 | x1.25 |
| Endurance    | 1 | x0.75 |
| Luck         | 3 | x1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Resonance
- **Description:** The magic woven into your voice.
- **What each point gives:** +1% strength of your Song buffs
- **Max points:** 100

### Resource bar

- **Name:** Melody
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** rose (#F06BA8)

## 4. Starting kit

- 1 × goat_horn
- 1 × note_block
- 1 × stone_sword
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Inspiring Song: players within 10 blocks get +5% damage and +5% movement speed
- Esoteric Knowledge: +20% experience from all sources
- Villager trades cost 15% less (bards are welcome everywhere)
- Playing a goat horn or note block refills 10 Melody (30s cooldown)

### Special passives

- Inspiring Song range +1 block per 10 Resonance
- Disarming Charm: per 10 Resonance, +3% chance a hostile mob you hit forgets you

## 6. Weakness

- **What players read:** A bard's power is in an audience. Alone, the song falls flat.
- **What it actually does:** -50% Melody regen and Inspiring Song is disabled when no other player is within 16 blocks

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | neutral |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | none |

## 8. Trinket slots

| Slot | Count |
|---|---|
| Amulet | 1 |
| Ring | 2 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 1 |
| Charm | 1 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 3
- **Free skill points at start (special tree):** 1

## 10. Skills (mastery)

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Performance

- **Shared or class-only:** shared
- **Trained by:** using goat horns, note blocks and jukeboxes near other players
- **Tier points go to:** general tree
- **How fast it levels:** fast
- **Tier I bonus:** +1 block Song range
- **Tier II bonus:** +5% Song strength
- **Tier III bonus:** +1 block Song range
- **Tier IV bonus:** +5% Song strength
- **Tier V bonus:** +10% Song strength

### Skill: Repertoire

- **Shared or class-only:** class-only
- **Trained by:** allies killing mobs inside your Song
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Resonance
- **Tier II bonus:** +1 Resonance
- **Tier III bonus:** +10% Melody regen
- **Tier IV bonus:** +1 Resonance
- **Tier V bonus:** +2 Resonance

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Trickery quarter, just beyond the Witch

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| bard_start | Bard | start | - | - | - | +5% experience gained |
| bard_rousing_verse | Rousing Verse | small | 1 | 1 | bard_start | +3% Song damage bonus |
| bard_silver_tongue | Silver Tongue | small | 1 | 1 | bard_start | -5% villager prices |
| bard_ballad_of_heroes | Ballad of Heroes | notable | 2 | 12 | bard_rousing_verse | Song also gives +1 armor |
| bard_lullaby | Lullaby | notable | 2 | 15 | bard_silver_tongue | mobs within 6 blocks of you move 10% slower |
| bard_virtuoso | Virtuoso | keystone | 3 | 30 | bard_ballad_of_heroes, bard_lullaby | Song bonuses doubled, but you deal -20% damage yourself |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Song Magic | start | - | - | - | - |
| encore | Encore | small | 1 | 1 | start | +2 Resonance |
| swift_song | Swift Song | small | 1 | 5 | start | Inspiring Song refreshes twice as fast |
| disarming_charm | Disarming Charm | notable | 2 | 15 | encore | charmed mobs also get Weakness I |
| dirge | Dirge | notable | 2 | 20 | swift_song | unlocks Dirge ability |
| legend | Legend | keystone | 3 | 35 | disarming_charm, dirge | everyone in your Song gets Regeneration I; your max health -10% |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| bard_stat_start | Bard | start | - | - | - | - |
| bard_charismatic | Charismatic | small | 2 | 1 | bard_stat_start | +2 Wisdom, +1 Luck |
| bard_nimble_fingers | Nimble Fingers | small | 2 | 5 | bard_charismatic | +2 Dexterity, +1 Intelligence |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Dirge** (mobs within 10 blocks get Weakness + Slowness, 30 Melody), **Charm** (one mob stops attacking you for 10s, 20 Melody).
- Book talents: Esoteric Knowledge, Disarming Charm, Swift Song. Tradition: Song.
