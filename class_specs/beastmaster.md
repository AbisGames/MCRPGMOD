# Class: Beastmaster

<!-- Source: Shadow of the Demon Lord core rulebook, master path 'Beastmaster' (p. 81). -->

## 1. Basics

- **Id:** beastmaster
- **Display name:** Beastmaster
- **Description:** A Primal mage bonded to animal companions who fight harder, live longer and share the beastmaster's senses.
- **Icon item:** lead
- **Selectable by players:** yes
- **Base health:** 28

## 2. Starting stats

| Stat         | Points |
|--------------|--------|
| Vitality     | 4 |
| Strength     | 3 |
| Dexterity    | 2 |
| Agility      | 3 |
| Intelligence | 3 |
| Wisdom       | 7 |
| Endurance    | 3 |
| Luck         | 1 |
| Special stat (section 3) | 10 |

## 3. Special stat

- **Name:** Primal Bond
- **Description:** How deeply your soul is knotted to your animals.
- **What each point gives:** +1% damage and +1% max health for your tamed animals
- **Max points:** 100

### Resource bar

- **Name:** Wild Spirit
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 4
- **Regen per second:** 0.8
- **Extra regen per second per special-stat point:** 0.08
- **Starts full when the class is chosen:** yes
- **Colour:** fur brown (#9A6A3A)

## 4. Starting kit

- 4 × bone
- 2 × lead
- 1 × leather_chestplate
- 4 × cooked_beef
- 4 × bread

## 5. Passives

- Tamed wolves, cats, horses and parrots get +4 max health and +10% damage
- Primal Bond: your tamed animals within 32 blocks share your Night Vision and you see them through walls
- Primal Beast: +10% unarmed and +10% axe damage
- Taming always succeeds on the first try

### Special passives

- Primal Power: per 10 Primal Bond, tamed animals get +1 armor
- +1 tamed-animal regen per 20 Primal Bond

## 6. Weakness

- **What players read:** When the beast suffers, so do you.
- **What it actually does:** when one of your tamed animals dies, you get Weakness I and Slowness I for 30s

## 7. Equipment (affinities)

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | proficient |
| Spears | proficient |
| Maces | neutral |
| Tridents | neutral |
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

### Skill: Beast Bond

- Shared skill - full definition in `druid.md`

### Skill: Axemanship

- Shared skill - full definition in `warrior.md`

### Skill: Pack Leader

- **Shared or class-only:** class-only
- **Trained by:** your tamed animals killing mobs
- **Tier points go to:** special tree
- **How fast it levels:** normal
- **Tier I bonus:** +1 Primal Bond
- **Tier II bonus:** +1 Primal Bond
- **Tier III bonus:** +10% Wild Spirit regen
- **Tier IV bonus:** +1 Primal Bond
- **Tier V bonus:** +2 Primal Bond

## 11. Trees

### 11a. General tree (shared by all classes)

- **Where this class starts:** ring 3 (outer), Faith quarter, just beyond the Druid

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| beastmaster_start | Beastmaster | start | - | - | - | tamed animals +2 max health |
| beastmaster_alpha | Alpha | small | 1 | 1 | beastmaster_start | tamed animals +5% damage |
| beastmaster_hide_like_leather | Hide Like Leather | small | 1 | 1 | beastmaster_start | +1 armor |
| beastmaster_pack_tactics | Pack Tactics | notable | 2 | 12 | beastmaster_alpha | +10% damage to mobs your animals are attacking |
| beastmaster_loyal_beasts | Loyal Beasts | notable | 2 | 15 | beastmaster_hide_like_leather | tamed animals take -15% damage |
| beastmaster_one_with_the_pack | One With the Pack | keystone | 3 | 30 | beastmaster_pack_tactics, beastmaster_loyal_beasts | your animals get +30% damage, but you deal -20% damage |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | Primal Bond | start | - | - | - | - |
| kinship | Kinship | small | 1 | 1 | start | +2 Primal Bond |
| primal_beast | Primal Beast | small | 1 | 5 | start | +10% more unarmed/axe damage |
| shared_spell | Shared Spell | notable | 2 | 15 | kinship | potion effects you drink also apply to your nearest tamed animal |
| call_of_the_wild | Call of the Wild | notable | 2 | 20 | primal_beast | unlocks Call of the Wild ability |
| primal_power | Primal Power | keystone | 3 | 35 | shared_spell, call_of_the_wild | tamed animals +1 armor and +10% damage; you can have at most 3 tamed wolves |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** shared

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| beastmaster_stat_start | Beastmaster | start | - | - | - | - |
| beastmaster_wild_blood | Wild Blood | small | 2 | 1 | beastmaster_stat_start | +2 Wisdom, +1 Vitality |
| beastmaster_feral | Feral | small | 2 | 5 | beastmaster_wild_blood | +2 Strength, +1 Agility |

## 12. Anything else

- **Power tier: Master.** Book levels 7 and 10. Balanced above an expert: 26 stat points, 10 special points, 5 trinket slots, 3+1 free skill points. Selectable from the start; if you later want a gate, suggested requirement is any class at level 35.
- Active abilities for later: **Call of the Wild** (summon a temporary wolf pack of 2 for 30s, 40 Wild Spirit), **Beast Within** (gain claws: +unarmed damage and speed, 20 Wild Spirit).
- Book talents: Primal Beast, Primal Bond, Primal Power. Tradition: Primal.
