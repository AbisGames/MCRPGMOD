# Class: Sentinel

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Sentinel' (p. 95). -->

## 1. Basics

- **Id:** sentinel
- **Display name:** Sentinel
- **Description:** A guardian with supernatural senses - never surprised, sees the invisible and owns the ground they stand on.
- **Icon item:** spyglass
- **Selectable by players:** yes
- **Base health:** 34

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 6 |
| Strength     | 5 |
| Dexterity    | 5 |
| Agility      | 2 |
| Intelligence | 1 |
| Wisdom       | 3 |
| Endurance    | 3 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Vigilance
- **Description:** Sharpened awareness.
- **What each point gives:** +0.5 blocks hostile-mob detection range
- **Max points:** 100

### Resource bar

- **Name:** Watch
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.6
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** watch gold (#C9A43B)

## 4. Starting kit

- 1 × iron_sword
- 1 × shield
- 1 × spyglass
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Perfect Awareness: you can't be surprised - hostile mobs within 16 blocks and all invisible creatures glow for you
- Immune to Blindness and Darkness
- Stand Guard: after standing still for 3s, +20% damage and knockback immunity until you move

### Special passives

- Glow range +0.5 per Vigilance
- Per 10 Vigilance, Stand Guard +5% damage

## 6. Weakness

- **What players read:** Sentinels hold a post - roaming makes them uneasy.
- **What it actually does:** -10% damage while sprinting or riding

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | mastery |
| Axes | proficient |
| Spears | proficient |
| Maces | proficient |
| Tridents | neutral |
| Bows | proficient |
| Crossbows | proficient |
| Shields | mastery |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | proficient |
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

### Skill: Shield Wall

- Shared skill - full definition in `warrior.md`

### Skill: Swordsmanship

- Shared skill - full definition in `warrior.md`

### Skill: Archery

- Shared skill - full definition in `warrior.md`

### Skill: Watchkeeping

- **Shared or class-only:** class-only
- **Trained by:** killing mobs while Stand Guard is active
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Vigilance
- **Tier II bonus:** +1 Vigilance
- **Tier III bonus:** +10% Watch regen
- **Tier IV bonus:** +1 Vigilance
- **Tier V bonus:** +2 Vigilance

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Scout

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sentinel_start | Sentinel | start | - | - | - | +4 blocks detection range |
| sentinel_eagle_eyes | Eagle Eyes | small | 1 | 1 | sentinel_start | +8 blocks glow range |
| sentinel_unflinching | Unflinching | small | 1 | 1 | sentinel_start | +15% knockback resistance |
| sentinel_watchtower | Watchtower | notable | 2 | 12 | sentinel_eagle_eyes | Stand Guard +10% damage |
| sentinel_night_watch | Night Watch | notable | 2 | 15 | sentinel_unflinching | permanent Night Vision |
| sentinel_post_holder | Post Holder | keystone | 3 | 30 | sentinel_watchtower, sentinel_night_watch | Stand Guard bonuses doubled, but -15% damage while moving |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Perfect Awareness | start | - | - | - | - |
| keen | Keen | small | 1 | 1 | start | +2 Vigilance |
| no_hiding | No Hiding | small | 1 | 5 | start | mobs you see can't become invisible to anyone near you |
| stand_guard | Stand Guard | notable | 2 | 15 | keen | Stand Guard triggers after 1s |
| sentry | Sentry | notable | 2 | 20 | no_hiding | players within 8 blocks also see what glows for you |
| unsleeping | Unsleeping | keystone | 3 | 35 | stand_guard, sentry | you never need sleep to skip phantoms (no phantoms spawn for you); -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| sentinel_stat_start | Sentinel | start | - | - | - | - |
| sentinel_watchful | Watchful | small | 2 | 1 | sentinel_stat_start | +2 Dexterity, +1 Wisdom |
| sentinel_stout_guard | Stout Guard | small | 2 | 5 | sentinel_watchful | +2 Vitality, +1 Strength |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Book talents: Perfect Awareness, Stand Guard.
