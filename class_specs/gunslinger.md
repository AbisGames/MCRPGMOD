# Class: Gunslinger

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Gunslinger' (p. 89). -->

## 1. Basics

- **Id:** gunslinger
- **Display name:** Gunslinger
- **Description:** A firearms expert and gunsmith who modifies pistols into six-shooters and never misses at close range.
- **Icon item:** crossbow
- **Selectable by players:** yes
- **Base health:** 30

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 2 |
| Dexterity    | 9 |
| Agility      | 5 |
| Intelligence | 3 |
| Wisdom       | 0 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Trigger Finger
- **Description:** Speed and accuracy with firearms.
- **What each point gives:** +0.5% firearm damage and +0.5% reload speed
- **Max points:** 100

### Resource bar

- **Name:** Grit
- **Max at 0 points:** 40
- **Extra max per special-stat point:** 3
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.06
- **Starts full when the class is chosen:** yes
- **Colour:** gunmetal (#4B5058)

## 4. Starting kit

- 1 × pistol (planned gun item) - until guns exist: 1 × crossbow
- 24 × bullets (planned) - until then: 24 × arrow
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Six-Shooter: a pistol you modify at a smithing table holds 6 shots before reloading and never misfires
- Deadeye Shot: +20% firearm damage and +15% accuracy (less spread)
- Speed Loader: reloading is 40% faster

### Special passives

- Firearm critical chance +0.3% per Trigger Finger
- Per 10 Trigger Finger, +1 shot per cylinder

## 6. Weakness

- **What players read:** Guns are loud and need reloading.
- **What it actually does:** every shot alerts hostile mobs within 24 blocks, and -20% melee damage

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | clumsy |
| Spears | neutral |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | proficient |
| Shields | clumsy |
| Light armor (leather, turtle) | mastery |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | clumsy |
| Specific items / exceptions: | Firearms (planned): mastery - pistols, six-shooters, rifles. Until guns exist, crossbows are the stand-in at proficient. |

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

### Skill: Gunplay

- **Shared or class-only:** shared
- **Trained by:** hitting mobs with firearms
- **Tier points go to:** general tree
- **How fast it levels:** normal
- **Tier I bonus:** +3% firearm damage
- **Tier II bonus:** +5% reload speed
- **Tier III bonus:** +3% firearm damage
- **Tier IV bonus:** +5% reload speed
- **Tier V bonus:** +6% firearm damage

### Skill: Crossbowmanship

- Shared skill - full definition in `artificer.md`

### Skill: Gunsmithing

- **Shared or class-only:** class-only
- **Trained by:** modifying and repairing firearms
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Trigger Finger
- **Tier II bonus:** +1 Trigger Finger
- **Tier III bonus:** +10% Grit regen
- **Tier IV bonus:** +1 Trigger Finger
- **Tier V bonus:** +2 Trigger Finger

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), War quarter, just beyond the Ranger

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| gunslinger_start | Gunslinger | start | - | - | - | +5% firearm damage |
| gunslinger_quick_draw | Quick Draw | small | 1 | 1 | gunslinger_start | first shot after swapping to a gun deals +25% |
| gunslinger_steady_hand | Steady Hand | small | 1 | 1 | gunslinger_start | +5% accuracy |
| gunslinger_fan_the_hammer | Fan the Hammer | notable | 2 | 12 | gunslinger_quick_draw | +15% fire rate |
| gunslinger_hollow_point | Hollow Point | notable | 2 | 15 | gunslinger_steady_hand | +10% damage to unarmored mobs |
| gunslinger_showdown | Showdown | keystone | 3 | 30 | gunslinger_fan_the_hammer, gunslinger_hollow_point | +30% firearm damage, but you can't wear heavy armor without -50% accuracy |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Six-Shooter | start | - | - | - | - |
| custom_grip | Custom Grip | small | 1 | 1 | start | +2 Trigger Finger |
| speed_loader | Speed Loader | small | 1 | 5 | start | +20% reload speed |
| deadeye_shot | Deadeye Shot | notable | 2 | 15 | custom_grip | unlocks Deadeye ability |
| ricochet | Ricochet | notable | 2 | 20 | speed_loader | bullets bounce to a second mob within 4 blocks for 50% |
| last_gunslinger | Last Gunslinger | keystone | 3 | 35 | deadeye_shot, ricochet | 6th shot in a cylinder always crits; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| gunslinger_stat_start | Gunslinger | start | - | - | - | - |
| gunslinger_steady | Steady | small | 2 | 1 | gunslinger_stat_start | +2 Dexterity, +1 Agility |
| gunslinger_tinkerer | Tinkerer | small | 2 | 5 | gunslinger_steady | +2 Intelligence, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- **Waits for the planned gun system.** Everything here assumes a 'Firearms' gear category (pistol, six-shooter, rifle). Until then, crossbows stand in.
- Active ability for later: **Deadeye** (slow time for you for 2s, every shot crits, 30 Grit).
- Book talents: Six-Shooter, Deadeye Shot, Speed Loader.
