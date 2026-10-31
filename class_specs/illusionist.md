# Class: Illusionist

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Illusionist' (p. 90). -->

## 1. Basics

- **Id:** illusionist
- **Display name:** Illusionist
- **Description:** A weaver of lies made light. Decoys, false images and duplicates keep enemies swinging at nothing.
- **Icon item:** glass
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points | Growth |
|--------------|--------|--------|
| Vitality     | 3 | x1 |
| Strength     | 0 | x0.75 |
| Dexterity    | 3 | x1 |
| Agility      | 3 | x0.9 |
| Intelligence | 8 | x1.25 |
| Wisdom       | 6 | x1.1 |
| Endurance    | 1 | x0.9 |
| Luck         | 2 | x1.1 |
| Special stat (section 3) | 10 | |

## 3. Special stat

- **Name:** Phantasm
- **Description:** How real your illusions seem.
- **What each point gives:** +0.3% chance attacks against you hit an illusion instead
- **Max points:** 100

### Resource bar

- **Name:** Mirage
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** prism pink (#FF8BE8)

## 4. Starting kit

- 1 × potion of invisibility
- 4 × glass
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Illusory Duplicates: 12% chance an attack against you hits a decoy instead; you blink 4 blocks away
- Convincing Illusions: mobs that lose sight of you forget you 50% faster
- Invisibility potions last twice as long on you

### Special passives

- Decoy chance +0.3% per Phantasm
- Per 10 Phantasm, blink distance +1

## 6. Weakness

- **What players read:** Illusions shatter on the truly perceptive.
- **What it actually does:** decoys never fool wardens, endermen or the Wither, and you take 15% more damage from them

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | proficient |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | clumsy |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | clumsy |
| Heavy armor (iron, diamond, netherite) | forbidden |
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

### Skill: Stealth

- Shared skill - full definition in `rogue.md`

### Skill: Glamourcraft

- **Shared or class-only:** class-only
- **Trained by:** dodging hits with decoys
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Phantasm
- **Tier II bonus:** +1 Phantasm
- **Tier III bonus:** +10% Mirage regen
- **Tier IV bonus:** +1 Phantasm
- **Tier V bonus:** +2 Phantasm

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Warlock

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| illusionist_start | Illusionist | start | - | - | - | +3% decoy chance |
| illusionist_mirror_image | Mirror Image | small | 1 | 1 | illusionist_start | +5% decoy chance |
| illusionist_smoke_and_mirrors | Smoke and Mirrors | small | 1 | 1 | illusionist_start | +1 blink distance |
| illusionist_phantom_strike | Phantom Strike | notable | 2 | 12 | illusionist_mirror_image | first hit after a blink deals +25% |
| illusionist_veil | Veil | notable | 2 | 15 | illusionist_smoke_and_mirrors | mobs 25% slower to notice you |
| illusionist_hall_of_mirrors | Hall of Mirrors | keystone | 3 | 30 | illusionist_phantom_strike, illusionist_veil | decoy chance doubled, but -25% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Illusory Duplicates | start | - | - | - | - |
| glamour | Glamour | small | 1 | 1 | start | +2 Phantasm |
| blur | Blur | small | 1 | 5 | start | after a decoy, you're invisible for 1s |
| convincing_illusions | Convincing Illusions | notable | 2 | 15 | glamour | unlocks Decoy ability |
| phantasmal_killer | Phantasmal Killer | notable | 2 | 20 | blur | unlocks Nightmare ability |
| unreal | Unreal | keystone | 3 | 35 | convincing_illusions, phantasmal_killer | you can pass through mobs and they lose track every 10s; -10% damage |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| illusionist_stat_start | Illusionist | start | - | - | - | - |
| illusionist_deceptive | Deceptive | small | 2 | 1 | illusionist_stat_start | +2 Intelligence, +1 Dexterity |
| illusionist_dreamer | Dreamer | small | 2 | 5 | illusionist_deceptive | +2 Wisdom, +1 Luck |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Decoy** (spawn a fake you that mobs target for 6s, 25 Mirage), **Nightmare** (target mob is frightened and flees, 30 Mirage).
- Book talents: Convincing Illusions, Illusory Duplicates. Tradition: Illusion.
