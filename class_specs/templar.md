# Class: Templar

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Templar' (p. 96). -->

## 1. Basics

- **Id:** templar
- **Display name:** Templar
- **Description:** A holy guardian of sacred places who consecrates ground and strikes down anything that steps into it.
- **Icon item:** lodestone
- **Selectable by players:** yes
- **Base health:** 32

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 6 |
| Dexterity    | 2 |
| Agility      | 1 |
| Intelligence | 1 |
| Wisdom       | 5 |
| Endurance    | 4 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Sanctum
- **Description:** Your bond with holy ground.
- **What each point gives:** +0.5% damage inside your Temple of Faith
- **Max points:** 100

### Resource bar

- **Name:** Consecration
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** temple marble (#E4DCC8)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × chainmail_chestplate
- 1 × lodestone
- 4 × bread

## 5. Passives

- Temple of Faith: an area within 10 blocks of a lodestone, beacon or bed you've set becomes your temple - inside it, +15% damage and +1 armor
- When a hostile mob enters your temple, you get Speed II for 2s
- Temple Guardian: hostile mobs hit inside your temple get Slowness II for 2s

### Special passives

- Temple radius +0.2 blocks per Sanctum
- Per 10 Sanctum, temple armor bonus +1

## 6. Weakness

- **What players read:** Templars are bound to their ground.
- **What it actually does:** -15% damage and -10% max health when more than 100 blocks from your temple

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | mastery |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | mastery |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Warding the Shrine

- **Shared or class-only:** class-only
- **Trained by:** killing mobs inside your temple
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Sanctum
- **Tier II bonus:** +1 Sanctum
- **Tier III bonus:** +10% Consecration regen
- **Tier IV bonus:** +1 Sanctum
- **Tier V bonus:** +2 Sanctum

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Paladin

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| templar_start | Templar | start | - | - | - | +1 armor |
| templar_hallowed | Hallowed | small | 1 | 1 | templar_start | +5% temple damage |
| templar_guardian_s_oath | Guardian's Oath | small | 1 | 1 | templar_start | +3 max health |
| templar_sacred_ground | Sacred Ground | notable | 2 | 12 | templar_hallowed | players in your temple regenerate 20% faster |
| templar_holy_bulwark | Holy Bulwark | notable | 2 | 15 | templar_guardian_s_oath | +1 more temple armor |
| templar_fortress_temple | Fortress Temple | keystone | 3 | 30 | templar_sacred_ground, templar_holy_bulwark | temple bonuses doubled, but outside it you lose all class passives |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Temple of Faith | start | - | - | - | - |
| consecrate | Consecrate | small | 1 | 1 | start | +2 Sanctum |
| wide_sanctum | Wide Sanctum | small | 1 | 5 | start | +5 temple radius |
| temple_guardian | Temple Guardian | notable | 2 | 15 | consecrate | Slowness becomes immobilise for 1s |
| portable_shrine | Portable Shrine | notable | 2 | 20 | wide_sanctum | unlocks Consecrate Ground ability |
| living_temple | Living Temple | keystone | 3 | 35 | temple_guardian, portable_shrine | your temple follows you (radius 6); -15% temple bonuses |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| templar_stat_start | Templar | start | - | - | - | - |
| templar_devout_warrior | Devout Warrior | small | 2 | 1 | templar_stat_start | +2 Strength, +1 Wisdom |
| templar_guardian | Guardian | small | 2 | 5 | templar_devout_warrior | +2 Vitality, +1 Endurance |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Consecrate Ground** (create a temporary temple where you stand for 60s, 50 Consecration).
- Book talents: Temple of Faith spell, Temple Guardian.
