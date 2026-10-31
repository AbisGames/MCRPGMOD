# Class: Shapeshifter

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Shapeshifter' (p. 95). -->

## 1. Basics

- **Id:** shapeshifter
- **Display name:** Shapeshifter
- **Description:** A master of Transformation who slips between beast and person, gaining power in every form.
- **Icon item:** rabbit_foot
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 5 |
| Strength     | 4 |
| Dexterity    | 2 |
| Agility      | 4 |
| Intelligence | 3 |
| Wisdom       | 6 |
| Endurance    | 1 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Mutability
- **Description:** How freely your body changes.
- **What each point gives:** +1% bonus from your current form
- **Max points:** 100

### Resource bar

- **Name:** Change
- **Max at 0 points:** 60
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** shifting amber (#D98E3A)

## 4. Starting kit

- 1 × leather_chestplate
- 4 × rabbit_foot
- 6 × cooked_rabbit

## 5. Passives

- Superior Transformation: you can shift into one form at a time from your unlocked forms list (starting forms: Wolf, Bear, Cat, Fish; Bird unlocks in the special tree) - drains Change while active. See section 12 for the form system
- Swift Transformation: changing form is instant and cleanses Slowness
- In any animal form, +1 armor

### Special passives

- Form bonuses +1% per Mutability
- Per 10 Mutability, -5% Change drain

## 6. Weakness

- **What players read:** Animals can't use tools.
- **What it actually does:** while transformed you can't use weapons, bows, shields or items (only bare-handed attacks)

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
| Bows | neutral |
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

### Skill: Beast Bond

- Shared skill - full definition in `druid.md`

### Skill: Shifting

- **Shared or class-only:** class-only
- **Trained by:** time spent in animal forms
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Mutability
- **Tier II bonus:** +1 Mutability
- **Tier III bonus:** +10% Change regen
- **Tier IV bonus:** +1 Mutability
- **Tier V bonus:** +2 Mutability

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Druid

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| shapeshifter_start | Shapeshifter | start | - | - | - | +5% unarmed damage |
| shapeshifter_wolf_heart | Wolf Heart | small | 1 | 1 | shapeshifter_start | wolf form +10% damage |
| shapeshifter_bear_hide | Bear Hide | small | 1 | 1 | shapeshifter_start | bear form +4 max health |
| shapeshifter_cat_s_grace | Cat's Grace | notable | 2 | 12 | shapeshifter_wolf_heart | cat form +10% speed |
| shapeshifter_seal_skin | Seal Skin | notable | 2 | 15 | shapeshifter_bear_hide | fish form +20% swim speed |
| shapeshifter_feral_soul | Feral Soul | keystone | 3 | 30 | shapeshifter_cat_s_grace, shapeshifter_seal_skin | forms drain 50% slower, but in human form you deal -20% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Superior Transformation | start | - | - | - | - |
| fluid_flesh | Fluid Flesh | small | 1 | 1 | start | +2 Mutability |
| quick_change | Quick Change | small | 1 | 5 | start | no cooldown between forms |
| bird_form | Bird Form | notable | 2 | 15 | fluid_flesh | unlocks a bird form (slow-fall glide) |
| dire_form | Dire Form | notable | 2 | 20 | quick_change | forms also give +1 armor toughness |
| thousand_faces | Thousand Faces | keystone | 3 | 35 | bird_form, dire_form | you can disguise as a villager to be ignored by illagers; -10% max health |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| shapeshifter_stat_start | Shapeshifter | start | - | - | - | - |
| shapeshifter_primal | Primal | small | 2 | 1 | shapeshifter_stat_start | +2 Strength, +1 Agility |
| shapeshifter_adaptable | Adaptable | small | 2 | 5 | shapeshifter_primal | +2 Wisdom, +1 Vitality |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- **Form system (configurable, built to grow):** each form is one data entry, so adding a form = adding an entry, no new code. Fields per form: `id`, `display name`, `model/mob to look like`, `hitbox size`, `stat changes` (health, armor, damage, speed, etc.), `granted effects/abilities` (e.g. water breathing, slow fall, climbing, night vision), `restrictions` (no items / no armor / can't swim...), `Change drain per second`, `unlock` (start / special-tree node / class level / skill tier).
- Starting forms: **Wolf** (+20% damage, +10% speed, night vision), **Bear** (+6 max health, +2 armor, +20% knockback resistance, -10% speed), **Cat** (+20% speed, no fall damage, creepers flee), **Fish** (fast swim, water breathing, can't leave water for long), **Bird** (special-tree unlock: slow fall + glide, -50% max health).
- Ideas for later forms: spider (wall climb), horse (fast, carries a player), goat (ram knockback, jump), axolotl, bat, fox, ravager (late-game).
- If true model changes are hard, forms can start as visual particles + stat changes.
- Book talents: Superior Transformation (+Defense/+Speed/+Perception/+Health options = the forms), Swift Transformation. Tradition: Transformation.
- **Decided:** Wolf, bear, cat, fish and bird to start; the form system must be data-driven so more forms can be added later.
