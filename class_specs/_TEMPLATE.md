# Class: TODO Name

<!--
HOW TO USE
1. Copy this file to class_specs/<class_name>.md (e.g. class_specs/knight.md). One file per class.
2. Fill in every section. Write in plain words - no JSON needed. Delete the hint lines in < > if you like.
3. Leave a line as "none" or "-" if the class doesn't have it. Write "?" if you want Claude to suggest something.
4. Commit/upload the files and ask Claude to build the classes from class_specs/.
See class_specs/README.md for what each thing does and the list of what is possible.
-->

## 1. Basics

- **Id:** <lowercase, no spaces, e.g. knight. Saved on players - don't change it after release>
- **Display name:**
- **Description:** <1-2 sentences shown on the class-selection screen>
- **Icon item:** <a Minecraft item, e.g. iron_sword>
- **Selectable by players:** yes <or "no" = only admins can give it>
- **Base health:** 20 <20 = 10 hearts, the vanilla default>

## 2. Starting stats

Points the class starts with in each stat. Leave 0 for none.

Optionally add a third column, **Growth**, for how much this class gets out of each point of a stat: "x1.5" means
every point counts half again (a mage's Intelligence), "x0.5" half. Leave it out (or "x1") for normal.

What a point gives: Vitality +1 max health, Strength +0.1 attack damage, Dexterity +0.2% attack speed, Agility +0.5%
movement speed, Intelligence +0.5% magic damage, Wisdom +0.5% healing you do, Endurance +1% natural health regen,
Luck +0.2% critical hit chance.

| Stat         | Points |
|--------------|--------|
| Vitality     | 0 |
| Strength     | 0 |
| Dexterity    | 0 |
| Agility      | 0 |
| Intelligence | 0 |
| Wisdom       | 0 |
| Endurance    | 0 |
| Luck         | 0 |
| Special stat (section 3) | 0 |

## 3. Special stat

The class's signature stat (like "Faith" or "Fury"). Other classes can't raise it.

- **Name:**
- **Description:**
- **What each point gives:** <e.g. "+0.5 armor", or "nothing by itself, only drives the resource and passives">
- **Max points:** 100

### Resource bar (optional - delete if the class has none)

- **Name:** <Mana, Rage, Energy...>
- **Max at 0 points:** 50
- **Extra max per special-stat point:** 5
- **Regen per second:** 1
- **Extra regen per second per special-stat point:** 0.1
- **Starts full when the class is chosen:** yes
- **Colour:** <e.g. "light blue" or a hex code like #3F7FFF>

## 4. Starting kit

Items given once when the class is chosen. One per line, with a count.

- 1 × wooden_sword
- 8 × bread

## 5. Passives

Always-on bonuses. One per line, in plain words. Can grow with level or stats
(e.g. "+1 armor, +0.25 more per special-stat point").

- 

### Special passives (signature bonuses, usually grow with the special stat)

- 

## 6. Weakness

- **What players read:** <e.g. "Heavy armor slows you down.">
- **What it actually does:** <e.g. "takes 50% more fire damage">

## 7. Equipment (affinities)

How good the class is with each kind of gear. Tiers: **mastery** (best), **proficient**, **neutral**, **clumsy**, **forbidden** (worst).
Nothing is ever blocked - bad gear just works much worse. Anything not listed is **neutral**.

| Gear | Tier |
|---|---|
| Swords | neutral |
| Axes | neutral |
| Spears | neutral |
| Maces | neutral |
| Tridents | neutral |
| Bows | neutral |
| Crossbows | neutral |
| Shields | neutral |
| Light armor (leather, turtle) | neutral |
| Medium armor (chainmail, copper, gold) | neutral |
| Heavy armor (iron, diamond, netherite) | neutral |
| Specific items / exceptions: | <e.g. "trident: mastery even though other melee is clumsy"> |

## 8. Trinket slots

How many of each slot the class has (0 = none).

| Slot | Count |
|---|---|
| Amulet | 0 |
| Ring | 0 |
| Cloak | 0 |
| Shoulderpad | 0 |
| Belt | 0 |
| Charm | 0 |

## 9. Levels

- **Max class level:** 50
- **Free skill points at start (general tree):** 0
- **Free skill points at start (special tree):** 0

## 10. Skills (mastery)

Skills level up by *doing* them (5 tiers each). Every tier gives a bonus and 1 skill point.
List the skills this class trains. Shared skills (e.g. Swordsmanship) only need to be written once, in any class file -
after that just write their name.

### Skill: TODO name

- **Shared or class-only:** <shared = every class can train it; class-only = only this class>
- **Trained by:** <e.g. "hitting mobs with swords", "sprinting", "blocking with a shield", "killing mobs with bows">
- **Tier points go to:** general tree <or special tree>
- **How fast it levels:** normal <slow / normal / fast, or exact numbers for tiers I-V like 100, 400, 1200, 3000, 7000>
- **Tier I bonus:**
- **Tier II bonus:**
- **Tier III bonus:**
- **Tier IV bonus:**
- **Tier V bonus:**

<copy the block above for each extra skill>

## 11. Trees

All three trees are webs: each node links to one or more others, and players can only buy a node next to one
they already own, starting from the class's start node.
**Cost** is in skill points (general/special trees) or stat points (stat tree). Default 1.
**Size:** small (minor), notable (strong), keystone (build-defining, with a downside).
**Min level:** class level needed (default 1).

### 11a. General tree (shared by all classes)

- **Where this class starts:** <e.g. "top-left, next to the Warrior", or "?">

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| knight_start | Knight | start | - | - | - | <optional bonus for the start node> |
|  |  | small | 1 | 1 | knight_start |  |

### 11b. Special tree (this class only)

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | TODO | start | - | - | - |  |
|  |  | small | 1 | 1 | start |  |

### 11c. Stat tree (paid with stat points)

- **Shared with other classes, or its own?** <shared / own>

| Node id | Name | Size | Cost | Min level | Links to | What it gives |
|---|---|---|---|---|---|---|
| start | TODO | start | - | - | - |  |
|  |  | small | 1 | 1 | start |  |

<!-- Tip: instead of filling the tables you can attach a sketch/screenshot of the tree layout and describe the nodes in a list. -->

## 12. Anything else

- Active abilities: **TODO Name** (what it does, cost in your resource), **TODO Name 2** (...)

<Ideas, balance notes, questions. Things that aren't possible yet go here too. A tree node can unlock an
ability: write "unlocks TODO Name ability" in its "What it gives".>
