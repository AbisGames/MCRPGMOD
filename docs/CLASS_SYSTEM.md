# Pipican Class System

How classes work, how to add one, and where everything lives in the code.

> **Short version:** a class is one JSON file. Stats, passive trees, masterable skills and trinket slot types are JSON files too.
> You never write Java to add a class. Copy `docs/templates/pipican_template_pack/`, fill in the TODOs, done.

---

## 1. The big picture

```
                 ┌──────────────── JSON (datapacks) ────────────────┐
                 │  stat/*.json   trinket_slot/*.json  skill/*.json │
                 │  skill_tree/*.json   class/*.json  (the template)│
                 └───────────────────────┬──────────────────────────┘
                                         │ loaded at world start, synced to clients
                                         ▼
  player choices (saved on the player)   +   class data   ──►  ClassEffectApplier.refresh()
  - chosen class, class level & XP                              │
  - allocated passive-tree nodes, skill mastery                 ▼
  - stats raised, lifetime kill XP               attribute modifiers, potion effects,
  - items in trinket slots                       damage multipliers, resource bar
```

* **Only choices are saved** on the player. Every number (total stats, bonuses, penalties) is recomputed
  from the JSON whenever something changes. Edit a class JSON and existing players get the new numbers.
* **Everything goes through one function**, `ClassEffectApplier.refresh(player)`: it removes what the
  class system applied before and re-applies it from scratch.

### Three kinds of progress

| | Earned by | Gives |
|---|---|---|
| **Skill mastery** | *using* skills: hitting with swords, sprinting, blocking... (see [2.5](#25-skills--mastery--skillnamejson)) | 5 tiers per skill. Each tier = a bonus **and one skill point** for the general or special tree. |
| **Kill XP** (separate from vanilla) | killing mobs (more for tougher mobs), `/pipican xp add` | two things at once: **class level** (gates tree nodes with `required_level`, can scale effects) and **stat points** (raise a stat with `/pipican stat raise`, or buy nodes in the stat tree). |
| **Vanilla XP levels** | normal Minecraft | only paying for tree refunds |

---

## 2. Data files

All files go in a datapack (or in the mod's own `src/main/resources/data/`).
The id of an entry is `<namespace>:<file name>`; e.g. `data/mypack/pipican/class/knight.json` is the class `mypack:knight`.

| What | Path | Java record |
|---|---|---|
| Stat | `data/<ns>/pipican/stat/<name>.json` | `stat/StatDefinition` |
| Trinket slot type | `data/<ns>/pipican/trinket_slot/<name>.json` | `trinket/TrinketSlotType` |
| Passive tree (one or more files per tree) | `data/<ns>/pipican/skill_tree/<name>.json` | `skill/SkillTreeFile`, `skill/SkillNode` |
| Masterable skill | `data/<ns>/pipican/skill/<name>.json` | `mastery/Skill` |
| Active ability | `data/<ns>/pipican/ability/<name>.json` | `ability/Ability` |
| Class | `data/<ns>/pipican/class/<name>.json` | `rpgclass/RpgClass` |

These are **datapack registries**: they load when the world loads (not on `/reload`). Item tags *do* reload with `/reload`.
A broken file stops the world from loading and the log says which file and field are wrong.

Text fields (`name`, `description`) accept a plain string (`"Knight"`) or a text component
(`{ "translate": "class.mypack.knight" }`) if you want translations.

### 2.1 Stats — `stat/<name>.json`

The number of stats is not fixed: each file is one stat.

```json
{
  "name": "Strength",
  "description": "Raw melee power.",
  "max": 100,
  "exclusive": false,
  "color": -1,
  "per_point": [
    { "attribute": "minecraft:attack_damage", "amount": 0.25, "operation": "add_value" }
  ]
}
```

| Field | Default | Meaning |
|---|---|---|
| `name` | required | |
| `description` | empty | |
| `max` | 100 | Highest value players can **raise** it to with stat points (bonuses from skills/passives can exceed it). |
| `per_point` | none | Attribute modifiers applied once per point. `operation`: `add_value` (default), `add_multiplied_base`, `add_multiplied_total`. Any vanilla or modded attribute works. |
| `exclusive` | false | If true, only classes whose **special stat** is this stat can buy points in it. Use it for signature stats like "Faith". |
| `color` | white | ARGB int for UIs (`-1` = white). |

A player's total in a stat = class `base_stats` + stat points put into it + `pipican:stat` effects (passives, tree nodes).

**Stat points** come from lifetime kill XP (the same XP that levels the class). The Nth point needs
`stats.first_point_xp + (N - 1) × stats.xp_step` more XP than the previous one; the defaults (100, 50) give points
at 100, 250, 450, 700... XP. One pool of stat points pays for both raising stats and [stat-tree](#23-passive-trees--skill_treenamejson-path-of-exile-style) nodes.

**Built-in stats.** The mod ships eight stats in the `pipican` namespace: `vitality` (+1 max health per point),
`strength` (+0.1 attack damage), `dexterity` (+0.2% attack speed), `agility` (+0.5% movement speed),
`intelligence`, `wisdom`, `endurance` and `luck`. The last four have no `per_point` bonuses yet: magic damage,
healing power, health regen and crit chance have no vanilla attribute to hook into. Override any of them with a
datapack file at the same path to change the numbers.

### 2.2 Trinket slot types — `trinket_slot/<name>.json`

Slot **types** are global. **How many** of each slot a player has comes from their class.

```json
{ "name": "Ring", "order": 20, "accepts": "pipican:trinkets/ring" }
```

| Field | Default | Meaning |
|---|---|---|
| `name` | required | Label in the trinket screen. |
| `order` | 0 | Sort order in the trinket screen (lower = higher). |
| `accepts` | `<ns>:trinkets/<name>` | Item tag of items that fit in this slot. |

The mod ships `amulet`, `ring`, `cloak`, `shoulderpad`, `belt`, `charm` with **empty** item tags
(`data/pipican/tags/item/trinkets/*.json`). To make an item a ring, add it to `pipican:trinkets/ring`.

A trinket's bonuses are the item's normal **attribute modifiers** (the `minecraft:attribute_modifiers` component),
scaled by the class's affinity for that item. In a trinket slot every modifier counts, whatever slot group it
declares — so give trinket modifiers the slot group `"body"` (players never wear body armor), otherwise vanilla
would also apply them while the item is simply held in a hand.

### 2.3 Passive trees — `skill_tree/<name>.json` (Path of Exile style)

A tree is a web of **nodes** joined by **links**. Rules, enforced on the server and shown in the screen:

* Each class has a **start node** in the tree (set in the class JSON). It is owned for free.
* You can only allocate a node that **touches** the start node or a node you own.
* A refund is allowed only if every other owned node **stays connected** to the start. It costs vanilla XP levels
  (`skill_tree.refund_cost_levels`, default 1; free in creative) and gives the point back.
* Other classes' start nodes can't be bought.

Every class has up to three trees:

* **General tree** (`skill_tree`) – normally **one big tree shared by all classes**, each starting in its own area.
* **Special tree** (`special_skill_tree`) – a small class-only tree (like PoE's Ascendancy), built around the special stat.
* **Stat tree** (`stat_tree`) – paid with **stat points** instead of skill points, so players choose between
  raising a stat directly or buying a node. Can be shared like the general tree.

All three use this format. General and special points come from skill mastery: each skill sends its tier-up
points to one of those two trees (`points_to` can't be `stat`). Stat-tree points are the player's stat points.

```json
{
  "tree": "pipican:main",
  "name": "Passive Tree",
  "nodes": [
    { "id": "warrior_start", "name": "Warrior", "type": "start", "x": 0, "y": 0 },
    {
      "id": "iron_skin",
      "name": "Iron Skin",
      "description": "+2 armor.",
      "type": "small",
      "icon": "minecraft:iron_chestplate",
      "x": 60, "y": 0,
      "links": [ "warrior_start" ],
      "effects": [ { "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 2 } ]
    }
  ]
}
```

**Splitting a big tree:** every file with the same `"tree"` id is merged into one tree, so the shared tree can be
split into regions (`main_warrior.json`, `main_mage.json`, ...) that different people edit. Links can cross files.
Without `"tree"`, a file is a tree of its own whose id is the file's id. Only one file per tree needs `"name"`.

| Node field | Default | Meaning |
|---|---|---|
| `id` | required | Unique in the whole tree. Saved on players — **don't rename after release** (a renamed node is lost and its point comes back). |
| `name`, `description` | – / empty | Shown in the tooltip. Write the effect in `description`; it is what players read. |
| `type` | `small` | `small` (minor bonus), `notable` (strong bonus), `keystone` (build-defining, with a downside), `start` (a class's starting point). Only changes the look and buyability. |
| `icon` | book | Item drawn in the node. |
| `x`, `y` | 0 | Position in the screen (0,0 = centre, +y = down, ~60 apart reads well). |
| `links` | none | Connected node ids. Links are two-way, declare them on either side. |
| `cost` | 1 | Points to allocate. |
| `required_level` | 1 | Minimum class level. |
| `effects` | none | [Effects](#3-effects) while the node is owned. A start node's effects apply too. |
| `grants` | none | Ids of [active abilities](#26-active-abilities--abilitynamejson) this node unlocks while owned. |

Data mistakes (duplicate ids, links to missing nodes) are logged as errors and skipped, they don't crash the world.

**Screen:** the **Passives** button above the inventory, or `/pipican tree`. Drag to pan, scroll to zoom,
left-click to allocate, right-click to refund, search box highlights matching nodes. Tabs: General, Special, Mastery.

### 2.4 The class template — `class/<name>.json`

Copy `docs/templates/pipican_template_pack/data/mypack/pipican/class/example_class.json`. Every field except `name` is optional.

| Field | Default | Meaning |
|---|---|---|
| `name`, `description` | – / empty | Shown on the selection screen. |
| `icon` | player head | Item shown as the class icon. |
| `selectable` | true | false = hidden from the selection screen (admins can still `/pipican class set` it). |
| `base_health` | 20 | Starting max health (20 = 10 hearts). Does **not** grow with level; stats/skills can still add health. |
| `base_stats` | none | `{ "<stat id>": points }` starting points, including the special stat. |
| `special_stat` | none | See below. |
| `starting_kit` | none | Items given once when the class is chosen: `"minecraft:bread"` or `{ "id": "minecraft:bread", "count": 8, "components": { ... } }`. |
| `skill_tree`, `special_skill_tree`, `stat_tree` | none | `{ "tree": "<tree id>", "start": "<start node id>" }`. Several classes can share a tree with different starts. |
| `progression` | 50 / 0 / 0 | `{ "max_level", "starting_skill_points", "starting_special_skill_points" }`. |
| `passives` | none | Always-on [effects](#3-effects). |
| `special_passives` | none | Always-on signature effects; usually scale with `special_stat`. |
| `weakness` | none | `{ "description": "...", "effects": [ ... ] }` – text for players plus the effects that implement it. |
| `affinities` | all neutral | Equipment rules, see [section 4](#4-equipment-rules-affinities). |
| `trinket_slots` | none | `{ "<slot type id>": count }`, e.g. a mage `{ "pipican:ring": 2, "pipican:amulet": 1 }`, a fighter `{ "pipican:shoulderpad": 1 }`. |
| `abilities` | none | [Active abilities](#26-active-abilities--abilitynamejson) the class always has, e.g. `[ "mypack:haste" ]`. Tree nodes can `grant` more. |

**Special stat**

```json
"special_stat": {
  "stat": "mypack:faith",
  "resource": {
    "name": "Mana", "base_max": 50, "max_per_point": 5,
    "regen_per_second": 1, "regen_per_point": 0.1,
    "color": -12627969, "starts_full": true
  }
}
```

The special stat is a normal stat (it gives its `per_point` bonuses and can be raised with stat points even if
`exclusive`), and it is the `special_stat` [scaling source](#scaling). `resource` is optional: when present the
class gets a resource bar (mana, rage, energy...) next to the hotbar:
max = `base_max + max_per_point × points`, regen/s = `regen_per_second + regen_per_point × points`.
Spending the resource is up to the upcoming active-skill system.

### 2.5 Skills & mastery — `skill/<name>.json`

A **skill** is something the player gets better at by doing it. Each use adds mastery; there are always
**5 tiers**. Reaching a tier gives that tier's bonus (tiers stack) **and 1 point** in the tree named by `points_to`.

```json
{
  "name": "Swordsmanship",
  "description": "Hitting mobs with swords.",
  "icon": "minecraft:iron_sword",
  "trigger": { "type": "pipican:melee_hit", "items": "#pipican:weapons/swords" },
  "gain_per_use": 1,
  "cooldown_ticks": 10,
  "thresholds": [ 100, 400, 1200, 3000, 7000 ],
  "points_to": "general",
  "tiers": [
    { "description": "+3% sword damage",
      "effects": [ { "type": "pipican:damage_dealt", "weapons": "#pipican:weapons/swords", "multiplier": 1.03 } ] }
  ]
}
```

A tier can also unlock abilities while it is reached: `"grants": [ "pipican:knight/rally" ]`.

| Field | Default | Meaning |
|---|---|---|
| `trigger` | required | What counts as a use (table below). |
| `gain_per_use` | 1 | Mastery per use. |
| `cooldown_ticks` | 10 | Minimum ticks between counted uses (20 = 1 s), so spam-clicking doesn't count. Movement ignores it. |
| `thresholds` | 100 / 400 / 1200 / 3000 / 7000 | Total mastery for tiers I–V. Exactly 5, ascending. |
| `tiers` | none | Up to 5 `{ "description", "effects" }`, one per tier. Tiers stack (tier III has I + II + III). |
| `points_to` | `general` | `general` or `special`: which tree gets the tier-up points. |
| `classes` | all | If set, only these classes can train it and get its bonuses/points. |
| `affinity_scaled` | true | Gain × the class's affinity for the item used (config `affinity.<tier>.mastery`: mastery ×1.5 … forbidden ×0.5). |

| Trigger type | Fields | One use = |
|---|---|---|
| `pipican:melee_hit` | `items` (optional) | a melee hit on a mob with one of the items |
| `pipican:ranged_hit` | `items` (optional) | a projectile from one of the items (bow, crossbow...) hitting a mob |
| `pipican:kill` | `items` (optional) | killing a mob with one of the items |
| `pipican:block` | `items` (optional) | blocking an attack with a shield |
| `pipican:hurt_wearing` | `items`, `min_pieces` (=1) | taking damage while wearing at least `min_pieces` of the items |
| `pipican:move` | `mode`, `per_blocks` (=10) | every `per_blocks` blocks moved in `mode`: `walk`, `sprint`, `sneak`, `swim`, `climb`, `glide` |
| `pipican:jump` | – | a jump |
| `pipican:ability` | `ability` | using an active ability, e.g. a dodge roll. Nothing fires it yet: the ability system will. |

**Total points:** players get 5 points per skill they can train (plus the class's starting points), so the number
of skills decides how much of the trees a character can fill. Keep that in mind when sizing trees.

**Adding a trigger type (Java):** write a record implementing `mastery/SkillTrigger`, register it in
`mastery/SkillTriggerTypes`. If it reacts to a new kind of action, add a record to `mastery/SkillAction` and fire it from
`event/MasteryEvents` (or from your own code via `MasteryService.onAction`).

---

### 2.6 Active abilities — `ability/<name>.json`

Something the player presses a key to do. A player's abilities are the class's `abilities` plus those
`grant`ed by tree nodes they own. The first four (except `auto` ones) sit on the ability keys
**Z X C V** (rebindable under Controls → Pipican), shown next to the hotbar with their cooldowns.

```json
{
  "name": "Haste", "description": "Speed III + Haste II for 8s.", "icon": "minecraft:sugar",
  "cost": 25, "cooldown_ticks": 600,
  "actions": [ { "type": "pipican:effect", "target": "self",
                 "effects": [ { "effect": "minecraft:speed", "seconds": 8, "amplifier": 2 },
                              { "effect": "minecraft:haste", "seconds": 8, "amplifier": 1 } ] } ]
}
```

| Field | Default | Meaning |
|---|---|---|
| `cost` | 0 | Resource spent on use. |
| `cooldown_ticks` | 200 | Ticks until it can be used again (20 = 1 s). For a toggle it starts when the toggle ends. |
| `duration_ticks` | 0 | > 0 makes it a **timed state**: `effects` count for that long. |
| `toggle`, `drain_per_second` | false, 0 | A **toggle**: press to start/stop; drains the resource while on and stops when it runs out. |
| `actions` | none | Run when used (table below). |
| `end_actions` | none | Run when a state or toggle ends. |
| `effects` | none | [Effects](#3-effects) that count while a state/toggle runs. Other bonuses can also check it with the `pipican:ability_active` [condition](#conditions). |
| `requires` | none | A [condition](#conditions) that must hold to use it (e.g. holding a staff). |
| `auto` | none | Starts by itself whenever this condition holds (and it's off cooldown and affordable). Auto abilities don't take a key. |
| `end_when` | none | A running state/toggle stops early when this holds. |
| `spend_all` | false | Spends the whole bar instead of `cost`; `empower` can scale with what was spent. |
| `uses_per_rest` | 0 | > 0: only that many uses between sleeps (sleeping in a bed resets it). |
| `pulse_ticks`, `pulse_actions` | 0, none | A state/toggle runs `pulse_actions` every `pulse_ticks`. |
| `anchored` | false | Pulses stay where the ability was used instead of following the player. |

If an action needs something that isn't there (no mob in the crosshair, no waypoint, no book), the use is
**cancelled**: no cost, no cooldown, and the player is told why.

Cooldowns and running states are saved on the player, so relogging doesn't reset them; running
states end on death and when the class changes.

**Actions.** Most take the target fields `target`, `radius` (=6), `range` (=16) and `cone` (degrees, =360):
`self`, `allies` (you + players in radius), `players` (others in radius), `hostiles`, `mobs` (non-players in
radius), `look` (the mob in your crosshair within range), `look_ally` (the player in your crosshair, else you),
`victim` (in a `pipican:on` reaction: the mob hit/killed, or the attacker), `companions` (your summons, pets and
eidolon). `entities` (`"#minecraft:undead"`, a list...) keeps only those kinds of mob. `look` with nothing in
sight cancels the use unless `"required": false`.

| Action | Fields | Does |
|---|---|---|
| `pipican:effect` | targets, `effects`: `[ { "effect", "seconds", "amplifier" } ]` | Potion effects. A "stun" is Slowness with amplifier 9 for a few seconds. |
| `pipican:heal` | targets (=self), `amount`, `percent` (of max health) | Heals. |
| `pipican:cleanse` | targets (=self), `effects` (optional) | Removes harmful effects (or only the listed ones). |
| `pipican:damage` | targets (=hostiles), `amount`, `kind` (`magic`/`melee`/`fire`), `execute_below`, `fire_seconds` | Damages; class damage bonuses apply. `execute_below`: targets at or under that health die. |
| `pipican:push` | targets (=hostiles), `strength` | Knocks targets away from you. |
| `pipican:dash` | `strength`, `up` | Launches you (or your mount) where you look. |
| `pipican:blink` | `distance` | Teleports you where you look, stopping short of walls. |
| `pipican:lightning` | `range` | Lightning on the mob or block you look at. |
| `pipican:summon` | `entity`, `count`, `seconds` | Temporary helpers; tameable mobs are tamed to you, iron golems are player-made. They vanish (no drops) when time runs out or you leave. |
| `pipican:absorb` | targets (=self), `amount`, `seconds` | A damage shield (Absorption, in steps of 4). |
| `pipican:explode` | `power`, `fire` | An explosion on you that never hurts you or breaks blocks. |
| `pipican:weather` | `seconds`, `thunder` | Starts rain or a storm. |
| `pipican:resource` | `amount`, `percent` | Refills (or drains) your resource bar. |
| `pipican:mark` | targets (=look), `seconds`, `multiplier`, `shared`, `next_hit`, `projectile_only`, `glow` | Your damage to marked mobs is multiplied (`shared`: everyone's; `next_hit`: used up by one hit). |
| `pipican:empower` | targets (=self), `multiplier`, `bonus`, `bonus_per_spent`, `hits`, `seconds`, `against`, `effects` | "Your next hit…": bigger damage and/or potion effects. |
| `pipican:imbue` | `hits`, `seconds` | Uses up the off-hand potion: your next hits apply it. |
| `pipican:control` | targets (=look), `mode` (`flee`/`pacify`/`ally`), `seconds` | Frighten, charm, or turn mobs to your side. |
| `pipican:decoy` | `seconds`, `radius` | A fake of you that hostile mobs attack instead. |
| `pipican:negate_hit` | targets (=look_ally), `seconds`, `guard` | The next attack on them misses (`guard`: you take it instead). |
| `pipican:teleport_to` | targets (=look_ally) | Teleports you beside the target. |
| `pipican:recall` | – | Sneak + use saves a waypoint; use returns to it. |
| `pipican:recall_item` | `items`, `radius` | Pulls back the nearest item you dropped or threw (e.g. your weapon, a trident). |
| `pipican:locate` | `structures` (tag, optional) | Tells you the direction and distance to your bed / the nearest structure. |
| `pipican:scry` | `seconds`, `range` | See through another player's eyes. |
| `pipican:give` | `item`, `sneak_item`, `seconds` | A temporary item that vanishes after `seconds` or when you sleep. |
| `pipican:steal` | `range` | Takes the held item (or an armor piece) of the mob you look at. |
| `pipican:transmute` | `pairs`: `[ { "from", "to" } ]` | Turns the held stack into another item. |
| `pipican:transfer_effects` | targets (=look) | Moves your harmful effects onto the target. |
| `pipican:cast` | `spells` (ability ids) | Casts the spell whose name is on the first page of a book in your hand. |
| `pipican:eidolon` | `item`, `count`, `rebuild_ticks` | Builds your eidolon (or calls it to you). |
| `pipican:mount` | `range` | Rides (or leaves) your eidolon. |
| `pipican:bond` | `range` | Bonds you with the player you look at: each heals half of what the other heals within 32 blocks. |
| `pipican:optimize` | – | Offers the Optimization choice again. |

**The eidolon** (`pipican:eidolon` entity) is a big iron construct with an owner: it follows you, fights what
hurts you or what you hit, never attacks players, sits on sneak + right-click, is repaired with iron ingots, can
be ridden (`pipican:mount`) and is upgraded by your class through `pipican:eidolon` effects. When it is destroyed
the ability that built it goes on its `rebuild_ticks` cooldown.

The `pipican:flight` effect gives creative-style flight while it counts — put it in a toggle's `effects`.

Adding an action type (Java): implement `ability/AbilityAction` and register it in `ability/AbilityActionTypes`.

## 3. Effects

Passives, special passives, weaknesses, tree nodes and mastery tiers are all lists of the same **effect** objects,
picked by `"type"`:

| Type | Fields | Does |
|---|---|---|
| `pipican:attribute` | `attribute`, `amount`, `operation` (=`add_value`), `scaling` | Modifies any attribute. |
| `pipican:stat` | `stat`, `amount` | Adds points to a stat (applied before everything else). |
| `pipican:mob_effect` | `effect`, `amplifier` (=0) | Permanent potion effect (re-applied if milk removes it). |
| `pipican:damage_taken` | `damage_types` (tag), `weapons` (items/tag), `against` (entity types/tag), `multiplier`, `scaling` | Multiplies incoming damage. >1 = weakness, <1 = resistance. `against` limits it to hits from those mobs. |
| `pipican:damage_dealt` | `damage_types` (tag), `weapons` (items/tag), `against` (entity types/tag), `victim`, `multiplier`, `scaling` | Multiplies outgoing damage. `weapons` limits it to hits with those items (e.g. swords), `against` to those mobs (`#pipican:mobs/undead`), `victim` to mobs in a state (below). |
| `pipican:critical` | `damage`, `chance`, `weapons`, `scaling` (damage), `chance_scaling` | Melee crits: `damage` 0.1 = crits deal +10%; `chance` = the chance a normal hit becomes a crit. Both add up. |
| `pipican:dodge` | `chance`, `melee_only`, `scaling` | Chance to fully avoid a hit from a mob/player (not falls, fire...). Adds up, capped at 90%. |
| `pipican:immune` | `effects` (mob effects/tag) | Those potion effects can't be applied to you (and are removed if you have them). |
| `pipican:resource` | `target` (`max` / `regen`), `amount`, `operation` (=`add_value`), `scaling` | Changes the resource bar. `add_value`: flat (regen per second); `add_multiplied_base`: a fraction of the class's own numbers (`-1` = no regen). Percentages add up, then apply once. |
| `pipican:conditional` | `when` ([condition](#conditions)), `effects` | The inner effects only count while the condition holds. |
| `pipican:flight` | – | Creative-style flight while it counts (usually in an ability toggle). Never touches creative/spectator players. |
| `pipican:on` | `event` (`hit`/`melee_hit`/`ranged_hit`/`kill`/`hurt`/`block`), `chance`, `cooldown_ticks`, `weapons`, `victims`, `actions` | Runs [ability actions](#26-active-abilities--abilitynamejson) when it happens ("10% chance your hits set mobs on fire", "kills give Speed"). |
| `pipican:eidolon` | `attribute`, `amount`, `operation`, `scaling` | Changes an attribute of your eidolon. |
| `pipican:spell_points` | `amount` | Spell points that refill on every sleep and pay for abilities instead of the resource. |
| `pipican:optimization` | `amount`, `extra_loss`, `scaling` | After each sleep, pick a stat to raise by `amount` and one to lower by `amount + extra_loss` (clickable chat). |

`victim` on `damage_dealt`: `{ "health": { "max": 0.5 }, "harmful_effect": true, "not_targeting_you": true,
"marked": true, "on_fire": true }` (every field optional; all given ones must hold). `health` is a fraction of max health.

```json
{ "type": "pipican:damage_dealt", "against": "#pipican:mobs/undead", "multiplier": 1.2 }
{ "type": "pipican:damage_dealt", "victim": { "health": { "max": 0.3 } }, "multiplier": 1.25 }
{ "type": "pipican:immune", "effects": [ "minecraft:poison", "minecraft:wither" ] }
{ "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 }
{ "type": "pipican:mob_effect", "effect": "minecraft:night_vision" }
{ "type": "pipican:stat", "stat": "mypack:strength", "amount": 2 }
```

### Conditions

`pipican:conditional` effects switch on and off with the player's situation. Conditions are re-checked twice a
second, and a player's bonuses are rebuilt only when an answer changes.

```json
{ "type": "pipican:conditional",
  "when": { "type": "pipican:any_of", "conditions": [
    { "type": "pipican:not", "condition": { "type": "pipican:state", "state": "sees_sky" } },
    { "type": "pipican:state", "state": "underwater" } ] },
  "effects": [
    { "type": "pipican:damage_dealt", "multiplier": 0.8 },
    { "type": "pipican:resource", "target": "regen", "amount": -1, "operation": "add_multiplied_base" } ] }
```

| Condition | Fields | True when |
|---|---|---|
| `pipican:state` | `state` | `wet` (water or rain), `in_water`, `underwater`, `in_lava`, `swimming`, `sprinting`, `sneaking`, `blocking`, `mounted`, `on_fire`, `night`, `day`, `raining`, `thundering`, `sees_sky` (outdoors; `not` it for underground) |
| `pipican:health` | `min`, `max` (0–1) | Health ÷ max health is in range. `"max": 0.5` = below half health. |
| `pipican:resource` | `min`, `max` (0–1) | Resource bar ÷ its max is in range. `"min": 1` = full. |
| `pipican:light` | `min`, `max` (0–15) | Light level at the feet. `"max": 8` = level 7 or lower. |
| `pipican:dimension` | `dimensions` (list) | In one of the dimensions. |
| `pipican:biome` | `biomes` (id / list / `#tag`) | In one of the biomes. The mod ships `#pipican:natural` and `#pipican:cold`. |
| `pipican:standing_on` | `blocks` (id / list / `#tag`) | On the ground, on one of the blocks. |
| `pipican:holding` | `items`, `hand` (`main` / `off` / `either`=default) | Holding one of the items. |
| `pipican:wearing` | `items`, `min` (=1), `max` | That many worn armor pieces are among the items. `"max": 0` = none. |
| `pipican:nearby` | `entities` (`players` / `hostile_mobs`), `radius` (=8), `min` (=1), `max` | That many others are within the radius. `"max": 0` = alone. |
| `pipican:effect` | `effects` (id / list / `#tag`) | The player has one of the potion effects. |
| `pipican:ability_active` | `ability` | One of the player's ability states/toggles is running. |
| `pipican:has_eidolon` | – | The player's eidolon exists. |
| `pipican:not` | `condition` | The condition is false. |
| `pipican:all_of`, `pipican:any_of` | `conditions` (list) | All / at least one are true. |

`min` is inclusive and `max` exclusive for fractions and light; both are inclusive for counts (`wearing`, `nearby`).
Adding a condition type (Java): implement `condition/ClassCondition` and register it in `condition/ConditionTypes`.

### Scaling

`attribute` and `damage_*` effects can grow with the player:

```json
{ "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 1,
  "scaling": [ { "source": "special_stat", "per": 0.25 }, { "source": "class_level", "per": 0.1 } ] }
```

final amount = `amount + Σ(per × source)`. Sources: `class_level`, `special_stat`, or any stat id (`"mypack:strength"`).

### Adding a new effect type (Java, rare)

1. Write a record implementing `effect/ClassEffect` (see `effect/type/AttributeEffect` – ~30 lines).
2. Register it in `effect/ClassEffectTypes` with `register("my_type", MyEffect.CODEC)`.
3. Every class can now use `"type": "pipican:my_type"` in JSON.

If it needs something the `EffectContext` can't collect yet, add a method there and implement it in
`ClassEffectApplier.Collector`.

---

## 4. Equipment rules (affinities)

Every item resolves to one **affinity tier** for the player's class. Nothing is blocked: bad equipment
still works, just with heavy penalties.

```json
"affinities": {
  "default": "neutral",
  "rules": [
    { "items": "minecraft:trident",        "tier": "mastery" },
    { "items": "#pipican:weapons/swords",  "tier": "proficient" },
    { "items": "#pipican:armor/heavy",     "tier": "forbidden" },
    { "items": ["minecraft:bow", "minecraft:crossbow"], "tier": "clumsy" }
  ]
}
```

Rules are checked **top to bottom, first match wins** — put single items before broad tags.
`items` can be an item id, a list of ids, or a `#tag`.

| Tier | Damage | Attack speed | Armor value | Move speed per piece | Trinket strength | Mastery gain |
|---|---|---|---|---|---|---|
| `mastery` | ×1.25 | ×1.10 | ×1.15 | ×1.00 | ×1.25 | ×1.50 |
| `proficient` | ×1.10 | ×1.05 | ×1.05 | ×1.00 | ×1.10 | ×1.25 |
| `neutral` | ×1 | ×1 | ×1 | ×1 | ×1 | ×1 |
| `clumsy` | ×0.75 | ×0.85 | ×0.75 | ×0.97 | ×0.75 | ×0.75 |
| `forbidden` | ×0.35 | ×0.60 | ×0.40 | ×0.90 | ×0.25 | ×0.50 |

The numbers are defaults from the **server config** (`<world>/serverconfig/pipican-server.toml`, section
`[affinity]`) so they can be balanced without code. Where they apply:

* **Damage** – any hit whose weapon is the item (melee and arrows/tridents fired from it).
* **Attack speed** – while the item is in the main hand. Keep `default` neutral, otherwise holding a torch counts too.
* **Armor / move speed** – per worn armor piece.
* **Trinket strength** – scales the trinket's attribute modifiers.
* **Mastery gain** – how fast skills level when used with the item (see [2.5](#25-skills--mastery--skillnamejson)).

Players see "Class affinity: Clumsy" in item tooltips and get an action-bar warning when equipping bad gear.

### Shipped category tags

| Tag | Contains |
|---|---|
| `pipican:weapons/swords`, `axes`, `spears`, `maces`, `tridents`, `bows`, `crossbows`, `shields` | the matching vanilla / `c:` tags |
| `pipican:weapons/melee`, `pipican:weapons/ranged` | groups of the above |
| `pipican:armor/light` | leather, turtle shell |
| `pipican:armor/medium` | chainmail, copper, gold |
| `pipican:armor/heavy` | iron, diamond, netherite |
| `pipican:trinkets/<slot>` | empty, one per shipped slot type |

New weapons/armor (ours or other mods') only need to be added to these tags to be covered by every class.

---

## 5. Player flow

1. **First join** – if the player has no class, the selection screen opens (config `selection.open_on_join`).
   "Decide later" closes it; reopen with **J** or `/pipican class choose`.
2. **Choosing** – sets the class, gives the starting kit, heals to full, fills the resource. Players can only
   choose once; admins can change it (respec).
3. **Playing** – mob kills give kill XP (`max health × leveling.kill_xp_per_max_health`). It levels the class (XP to go
   from level L to L+1 is `xp_base × L^xp_exponent`) and earns stat points (see [2.1](#21-stats--statnamejson)).
   Using skills builds mastery; every tier-up gives a skill point. Spend points in the **Passives** screen
   (button above the inventory; tabs General, Special, Stats, Mastery).
4. **Trinkets** – press **G**. The screen shows exactly the slots the class has. Trinkets drop on death unless `keepInventory`.
   If the class changes and loses a slot, the item goes back to the inventory.
5. **Death** – class, levels, tree nodes, mastery and stats are kept.

Multiclassing: the save format already keeps progress **per class** (`PlayerClassData.progress`), so a future
multiclass system can add classes without migrating saves. How it should work is still to be decided.

---

## 6. Commands

| Command | Who | |
|---|---|---|
| `/pipican class set <players> <class> [giveKit]` | admin | Set/change class (respec). |
| `/pipican class reset <players>` | admin | Remove class + class progress, reopen selection. |
| `/pipican level set <players> <level>` | admin | |
| `/pipican xp add <players> <amount>` | admin | Class XP. |
| `/pipican tree reset <players> [general\|special\|stat]` | admin | Clears allocated nodes (points come back). |
| `/pipican mastery add <players> <skill> <amount>` | admin | |
| `/pipican mastery set <players> <skill> <tier>` | admin | Sets a skill to the start of tier 0–5. |
| `/pipican mastery reset <players>` | admin | Clears all mastery and the general and special trees (the stat tree is kept). |
| `/pipican stat reset <players>` | admin | Clears raised stats; the stat points come back. |
| `/pipican class info [player]` | everyone | Class sheet: level, points, stats, resource. |
| `/pipican class choose` | everyone | Reopen selection if you have no class. |
| `/pipican tree` | everyone | Opens the passive-tree screen. |
| `/pipican tree allocate\|refund <general\|special\|stat> <node>` | everyone | Same as clicking in the screen. |
| `/pipican mastery list [player]` | everyone | Skills with tier and progress. |
| `/pipican stat raise <stat>` | everyone | Costs one stat point (earned with kill XP). |
| `/pipican trinkets` | everyone | Same as **G**. |
| `/pipican ability use <ability>` | everyone | Same as pressing the ability's key. |
| `/pipican ability force <players> <ability>` | admin | Uses any ability for free, ignoring cooldowns (for testing). |
| `/pipican ability reset <players>` | admin | Clears ability cooldowns, per-rest uses and running abilities. |
| `/pipican optimize <raise> <lower>` | everyone | The Optimization choice (the chat buttons run it). |

---

## 7. Code map

```
com.pipican
├── Pipican.java              mod entry: registers everything
├── core/
│   ├── PipicanRegistries     the datapack registries
│   ├── PipicanConfig         server config (XP curve, stat cost, affinity tiers)
│   └── ClassLookup           helpers to read the registries (server & client)
├── rpgclass/                 the class template: RpgClass, SpecialStat, Progression, Weakness,
│                             Affinity (tiers), AffinityRules
├── stat/                     StatDefinition (JSON), StatPoints (stat points from kill XP)
├── skill/                    passive trees: SkillTreeFile (JSON) -> SkillTrees (merge) -> SkillTree
│                             (adjacency rules), SkillNode, NodeType, TreeRef, TreePoints, TreeKind
├── mastery/                  Skill (JSON), SkillTrigger + SkillTriggerTypes, SkillAction, MoveMode
│   └── trigger/              the built-in trigger types
├── trinket/                  TrinketSlotType, TrinketLayout, TrinketMenu, PipicanMenus
├── effect/                   ClassEffect (interface), ClassEffectTypes (type registry),
│   └── type/                 the built-in effect types
├── ability/                  Ability (JSON), AbilityService (use/tick), AbilitySlots (keys), Targets, Summons,
│                             MobControl, Marks, Empowers, HitShields, Reactions, Optimize, Bonds, Scry,
│   └── action/               AbilityAction + AbilityActionTypes: the built-in action types
├── entity/                   Eidolon (the Engineer's construct), PipicanEntities
├── condition/                ClassCondition (interface), ConditionTypes (type registry), Bounds
│   └── type/                 the built-in condition types (used by pipican:conditional)
├── player/
│   ├── PlayerClassData       what is saved on the player (+ PipicanAttachments)
│   ├── ClassProgress         level, XP, allocated nodes for one class
│   ├── ClassService          ALL class mutations (choose, xp, allocate/refund, raise stat, reset...)
│   ├── MasteryService        actions -> mastery -> tier-ups
│   ├── ClassEffectApplier    recomputes and applies all bonuses  ← the heart of it
│   └── ClassState            computed result, read by event handlers
├── event/ClassEvents         login/respawn/equipment/damage/kill/tick hooks
├── event/MasteryEvents       game events -> SkillActions
├── command/PipicanCommands
├── network/                  packets (class data itself syncs via the attachment)
└── client/                   selection, passive-tree, trinket screens, resource HUD, keys, tooltips
```

**Rules of thumb for contributors**

* Mutate player data only through `ClassService`. It calls `changed()` → `refresh()` + sync.
* Never store derived numbers on the player; compute them in `ClassEffectApplier`.
* Client code stays in `client/` (it is only loaded on the client).
* Keep JSON fields `snake_case` and optional with sensible defaults.

---

## 8. Testing

* `gradlew runGameTestServer` runs the GameTests in `src/gametest` (choosing a class, passive-tree rules, mastery,
  affinities, damage multipliers, stats/levels, trinkets). They use the hidden test class `pipican_test:tester` from
  `src/gametest/resources`, which never ships in the mod jar. In a dev world, `/test run pipican:*` runs them too.
* `gradlew runClient` + a datapack in `run/saves/<world>/datapacks/` to try classes by hand. The template pack in
  `docs/templates/` is a working starting point.

Note: Minecraft 26.1 needs **Java 25**; Gradle downloads it automatically the first time.
