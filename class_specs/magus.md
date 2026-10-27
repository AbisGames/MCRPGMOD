# Class: Magus

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Magus' (p. 92). -->

## 1. Basics

- **Id:** magus
- **Display name:** Magus
- **Description:** A member of a secret cabal whose magic flows through a staff they built themselves.
- **Icon item:** blaze_rod
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 3 |
| Strength     | 2 |
| Dexterity    | 2 |
| Agility      | 2 |
| Intelligence | 8 |
| Wisdom       | 6 |
| Endurance    | 2 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Staffcraft
- **Description:** Your bond with the staff.
- **What each point gives:** +1% damage with your Staff of Magic
- **Max points:** 100

### Resource bar

- **Name:** Staff Charge
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** staff oak (#A5763E)

## 4. Starting kit

- 1 × blaze_rod (your Staff of Magic)
- 1 × leather_chestplate
- 6 × bread

## 5. Passives

- Staff of Magic: a blaze rod or stick held in the main hand counts as your staff - +6 attack damage, +20% of it as magic damage
- Staff of Power: while holding your staff, +2 armor and -15% magic damage taken
- Lost staffs can be remade at a crafting table (blaze rod + amethyst shard)

### Special passives

- Staff damage +1% per Staffcraft
- Per 10 Staffcraft, staff hits give Slowness I for 1s

## 6. Weakness

- **What players read:** A magus without a staff is half a mage.
- **What it actually does:** -25% damage and no Staff Charge regen when not holding your staff

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | clumsy |
| Axes | clumsy |
| Spears | mastery |
| Maces | clumsy |
| Tridents | neutral |
| Bows | neutral |
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

### Skill: Spear Training

- Shared skill - full definition in `druid.md`

### Skill: Enchanting

- Shared skill - full definition in `magician.md`

### Skill: Cabal Lore

- **Shared or class-only:** class-only
- **Trained by:** fighting with your staff
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Staffcraft
- **Tier II bonus:** +1 Staffcraft
- **Tier III bonus:** +10% Staff Charge regen
- **Tier IV bonus:** +1 Staffcraft
- **Tier V bonus:** +2 Staffcraft

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Power quarter, just beyond the Wizard

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| magus_start | Magus | start | - | - | - | +3% staff damage |
| magus_ironwood | Ironwood | small | 1 | 1 | magus_start | +5% staff damage |
| magus_focus_crystal | Focus Crystal | small | 1 | 1 | magus_start | +5% magic damage |
| magus_sweeping_staff | Sweeping Staff | notable | 2 | 12 | magus_ironwood | staff hits sweep 1 extra mob |
| magus_spellguard_staff | Spellguard Staff | notable | 2 | 15 | magus_focus_crystal | +1 armor with staff |
| magus_staff_of_the_archmagus | Staff of the Archmagus | keystone | 3 | 30 | magus_sweeping_staff, magus_spellguard_staff | +25% staff damage, but you can't use any other weapon at better than clumsy |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Staff of Magic | start | - | - | - | - |
| carved_runes | Carved Runes | small | 1 | 1 | start | +2 Staffcraft |
| long_reach | Long Reach | small | 1 | 5 | start | +0.5 reach with staff |
| staff_of_power | Staff of Power | notable | 2 | 15 | carved_runes | +1 more armor with staff |
| staff_strike | Staff Strike | notable | 2 | 20 | long_reach | unlocks Staff Bolt ability |
| living_staff | Living Staff | keystone | 3 | 35 | staff_of_power, staff_strike | your staff returns to you if dropped and can't be destroyed; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| magus_stat_start | Magus | start | - | - | - | - |
| magus_scholarly | Scholarly | small | 2 | 1 | magus_stat_start | +2 Intelligence, +1 Wisdom |
| magus_staff_fighter | Staff-Fighter | small | 2 | 5 | magus_scholarly | +2 Strength, +1 Dexterity |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Staff Bolt** (ranged magic bolt from the staff, 15 Staff Charge).
- Book talents: Staff of Magic, Staff of Power. Mastery: implements.
