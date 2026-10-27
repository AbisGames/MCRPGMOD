# Class: Theurge

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Theurge' (p. 97). -->

## 1. Basics

- **Id:** theurge
- **Display name:** Theurge
- **Description:** A holy caster with a direct line to their god, healing allies and stunning the wicked with pulses of divine power.
- **Icon item:** nether_star
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 2 |
| Dexterity    | 1 |
| Agility      | 1 |
| Intelligence | 5 |
| Wisdom       | 9 |
| Endurance    | 3 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Piety
- **Description:** The directness of your connection to the divine.
- **What each point gives:** +1% healing and +0.5% magic damage
- **Max points:** 100

### Resource bar

- **Name:** Divinity
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** heavenly gold (#FFE58A)

## 4. Starting kit

- 1 × book
- 1 × golden_apple
- 1 × chainmail_chestplate
- 6 × bread

## 5. Passives

- Pious Restoration: every time you spend Divinity, heal 2 HP
- Invocation: every 30s in combat, a holy pulse hits mobs within 4 blocks - Weakness I for 3s (Slowness too on undead)
- -15% magic damage taken

### Special passives

- Invocation radius +0.1 per Piety
- Per 10 Piety, Pious Restoration +1 HP

## 6. Weakness

- **What players read:** A theurge's power is borrowed - and the lender is watching.
- **What it actually does:** killing villagers, golems or tamed animals locks Divinity regen for 10 minutes

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | mastery |
| Tridents | neutral |
| Bows | clumsy |
| Crossbows | neutral |
| Shields | proficient |
| Light armor (leather, turtle) | proficient |
| Medium armor (chainmail, copper, gold) | proficient |
| Heavy armor (iron, diamond, netherite) | clumsy |
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

### Skill: Mending

- Shared skill - full definition in `priest.md`

### Skill: Undead Bane

- Shared skill - full definition in `priest.md`

### Skill: Theurgy

- **Shared or class-only:** class-only
- **Trained by:** healing with Pious Restoration and hitting mobs with Invocation
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Piety
- **Tier II bonus:** +1 Piety
- **Tier III bonus:** +10% Divinity regen
- **Tier IV bonus:** +1 Piety
- **Tier V bonus:** +2 Piety

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Cleric

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| theurge_start | Theurge | start | - | - | - | +5% healing |
| theurge_prayerful | Prayerful | small | 1 | 1 | theurge_start | +8% healing |
| theurge_halo | Halo | small | 1 | 1 | theurge_start | -10% damage from undead |
| theurge_pulse | Pulse | notable | 2 | 12 | theurge_prayerful | Invocation every 20s |
| theurge_divine_aegis | Divine Aegis | notable | 2 | 15 | theurge_halo | +1 armor |
| theurge_chosen | Chosen | keystone | 3 | 30 | theurge_pulse, theurge_divine_aegis | Invocation stuns frightened (weakened) mobs for 1s, but -20% max health |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Invocation | start | - | - | - | - |
| devotions | Devotions | small | 1 | 1 | start | +2 Piety |
| pious_restoration | Pious Restoration | small | 1 | 5 | start | heal 3 HP |
| invocation | Invocation | notable | 2 | 15 | devotions | Invocation radius +2 |
| holy_word | Holy Word | notable | 2 | 20 | pious_restoration | unlocks Holy Word ability |
| saint_of_the_new_god | Saint of the New God | keystone | 3 | 35 | invocation, holy_word | Invocation also heals players within its radius by 2; -15% Divinity regen |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| theurge_stat_start | Theurge | start | - | - | - | - |
| theurge_devoted | Devoted | small | 2 | 1 | theurge_stat_start | +2 Wisdom, +1 Vitality |
| theurge_learned_priest | Learned Priest | small | 2 | 5 | theurge_devoted | +2 Intelligence, +1 Wisdom |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active ability for later: **Holy Word** (pulse on demand: frighten/stun mobs, 35 Divinity).
- Book talents: Pious Restoration, Invocation. Tradition: Theurgy.
