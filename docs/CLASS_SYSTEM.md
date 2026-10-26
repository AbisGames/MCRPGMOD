# Pipican Class System

How classes work, how to add one, and where everything lives in the code.

> **Short version:** a class is one JSON file. Stats, skill trees and trinket slot types are JSON files too.
> You never write Java to add a class. Copy `docs/templates/pipican_template_pack/`, fill in the TODOs, done.

---

## 1. The big picture

```
                 ┌──────────────── JSON (datapacks) ────────────────┐
                 │  stat/*.json   trinket_slot/*.json               │
                 │  skill_tree/*.json   class/*.json  (the template)│
                 └───────────────────────┬──────────────────────────┘
                                         │ loaded at world start, synced to clients
                                         ▼
  player choices (saved on the player)   +   class data   ──►  ClassEffectApplier.refresh()
  - chosen class, class level & XP                              │
  - unlocked skill nodes (+ rank)                               ▼
  - stat points bought with vanilla XP           attribute modifiers, potion effects,
  - items in trinket slots                       damage multipliers, resource bar
```

* **Only choices are saved** on the player. Every number (total stats, bonuses, penalties) is recomputed
  from the JSON whenever something changes. Edit a class JSON and existing players get the new numbers.
* **Everything goes through one function**, `ClassEffectApplier.refresh(player)`: it removes what the
  class system applied before and re-applies it from scratch.

### Two kinds of XP

| | Earned by | Spent on |
|---|---|---|
| **Class XP** (separate from vanilla) | killing mobs (more for tougher mobs), `/pipican xp add` | nothing, it raises your **class level**. Each level-up gives skill points. |
| **Vanilla XP levels** | normal Minecraft | buying **stat points** (`/pipican stat raise`, later a UI) |

---

## 2. Data files

All files go in a datapack (or in the mod's own `src/main/resources/data/`).
The id of an entry is `<namespace>:<file name>`; e.g. `data/mypack/pipican/class/knight.json` is the class `mypack:knight`.

| What | Path | Java record |
|---|---|---|
| Stat | `data/<ns>/pipican/stat/<name>.json` | `stat/StatDefinition` |
| Trinket slot type | `data/<ns>/pipican/trinket_slot/<name>.json` | `trinket/TrinketSlotType` |
| Skill tree | `data/<ns>/pipican/skill_tree/<name>.json` | `skill/SkillTree`, `skill/SkillNode` |
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
| `max` | 100 | Highest value players can **buy** up to (bonuses from skills/passives can exceed it). |
| `per_point` | none | Attribute modifiers applied once per point. `operation`: `add_value` (default), `add_multiplied_base`, `add_multiplied_total`. Any vanilla or modded attribute works. |
| `exclusive` | false | If true, only classes whose **special stat** is this stat can buy points in it. Use it for signature stats like "Faith". |
| `color` | white | ARGB int for UIs (`-1` = white). |

A player's total in a stat = class `base_stats` + points bought with vanilla XP + `pipican:stat` effects (passives, skill nodes).

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

### 2.3 Skill trees — `skill_tree/<name>.json`

Every class has two trees, both in this format:

* **General tree** (`skill_tree`) – the class's own version of common upgrades. Paid with *skill points*.
* **Special tree** (`special_skill_tree`) – the signature tree, usually built around the special stat. Paid with *special skill points*.

```json
{
  "name": "Knight Arts",
  "nodes": [
    {
      "id": "iron_skin",
      "name": "Iron Skin",
      "description": "+1 armor per rank.",
      "icon": "minecraft:iron_chestplate",
      "x": 0, "y": 0,
      "cost": 1,
      "max_rank": 3,
      "required_level": 1,
      "requires": [],
      "effects": [ { "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 1 } ],
      "grants": []
    }
  ]
}
```

| Field | Default | Meaning |
|---|---|---|
| `id` | required | Unique inside the tree. Saved on players — **don't rename after release** (a renamed node is simply lost/refunded). |
| `name`, `description`, `icon` | – / empty / book | For the (upcoming) tree screen. |
| `x`, `y` | 0 | Position in the (upcoming) tree screen. |
| `cost` | 1 | Points per rank. |
| `max_rank` | 1 | How many times it can be bought. |
| `required_level` | 1 | Minimum class level. |
| `requires` | none | Node ids that need at least 1 rank first (all of them). Typos are caught at load time. |
| `effects` | none | [Effects](#3-effects) applied **once per rank** (3 ranks of +1 armor = +3). |
| `grants` | none | Ids of active abilities this node unlocks. Stored now; the ability system is a later task. |

Points: each level-up gives `skill_points_per_level` / `special_skill_points_per_level` (see class `progression`).
Spent points are derived from unlocked nodes, so a reset refunds everything automatically.

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
| `skill_tree`, `special_skill_tree` | none | Ids of skill trees. |
| `progression` | 50 / 1 / 1 | `{ "max_level", "skill_points_per_level", "special_skill_points_per_level" }`. |
| `passives` | none | Always-on [effects](#3-effects). |
| `special_passives` | none | Always-on signature effects; usually scale with `special_stat`. |
| `weakness` | none | `{ "description": "...", "effects": [ ... ] }` – text for players plus the effects that implement it. |
| `affinities` | all neutral | Equipment rules, see [section 4](#4-equipment-rules-affinities). |
| `trinket_slots` | none | `{ "<slot type id>": count }`, e.g. a mage `{ "pipican:ring": 2, "pipican:amulet": 1 }`, a fighter `{ "pipican:shoulderpad": 1 }`. |

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

The special stat is a normal stat (it gives its `per_point` bonuses and can be bought with vanilla XP even if
`exclusive`), and it is the `special_stat` [scaling source](#scaling). `resource` is optional: when present the
class gets a resource bar (mana, rage, energy...) next to the hotbar:
max = `base_max + max_per_point × points`, regen/s = `regen_per_second + regen_per_point × points`.
Spending the resource is up to the upcoming active-skill system.

---

## 3. Effects

Passives, special passives, weaknesses and skill nodes are all lists of the same **effect** objects,
picked by `"type"`:

| Type | Fields | Does |
|---|---|---|
| `pipican:attribute` | `attribute`, `amount`, `operation` (=`add_value`), `scaling` | Modifies any attribute. |
| `pipican:stat` | `stat`, `amount` | Adds points to a stat (applied before everything else). |
| `pipican:mob_effect` | `effect`, `amplifier` (=0) | Permanent potion effect (re-applied if milk removes it). |
| `pipican:damage_taken` | `damage_types` (tag, optional), `multiplier`, `scaling` | Multiplies incoming damage. >1 = weakness, <1 = resistance. |
| `pipican:damage_dealt` | `damage_types` (tag, optional), `multiplier`, `scaling` | Multiplies outgoing damage. |

```json
{ "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 }
{ "type": "pipican:mob_effect", "effect": "minecraft:night_vision" }
{ "type": "pipican:stat", "stat": "mypack:strength", "amount": 2 }
```

**Ranks** (skill nodes): attribute/stat amounts are multiplied by the rank; damage bonuses stack their
bonus part (1.1 at rank 3 = 1.3); potion amplifier goes up by rank − 1.

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

| Tier | Damage | Attack speed | Armor value | Move speed per piece | Trinket strength |
|---|---|---|---|---|---|
| `mastery` | ×1.25 | ×1.10 | ×1.15 | ×1.00 | ×1.25 |
| `proficient` | ×1.10 | ×1.05 | ×1.05 | ×1.00 | ×1.10 |
| `neutral` | ×1 | ×1 | ×1 | ×1 | ×1 |
| `clumsy` | ×0.75 | ×0.85 | ×0.75 | ×0.97 | ×0.75 |
| `forbidden` | ×0.35 | ×0.60 | ×0.40 | ×0.90 | ×0.25 |

The numbers are defaults from the **server config** (`<world>/serverconfig/pipican-server.toml`, section
`[affinity]`) so they can be balanced without code. Where they apply:

* **Damage** – any hit whose weapon is the item (melee and arrows/tridents fired from it).
* **Attack speed** – while the item is in the main hand. Keep `default` neutral, otherwise holding a torch counts too.
* **Armor / move speed** – per worn armor piece.
* **Trinket strength** – scales the trinket's attribute modifiers.

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
3. **Playing** – mob kills give class XP (`max health × leveling.kill_xp_per_max_health`). XP to go from level L to L+1 is
   `xp_base × L^xp_exponent` (config). Each level-up gives skill points.
4. **Trinkets** – press **G**. The screen shows exactly the slots the class has. Trinkets drop on death unless `keepInventory`.
   If the class changes and loses a slot, the item goes back to the inventory.
5. **Death** – class, levels, skills and stats are kept.

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
| `/pipican skill reset <players> [general\|special]` | admin | Refunds points. |
| `/pipican stat reset <players>` | admin | Refunds the vanilla levels spent. |
| `/pipican class info [player]` | everyone | Class sheet: level, points, stats, resource. |
| `/pipican class choose` | everyone | Reopen selection if you have no class. |
| `/pipican skill unlock <general\|special> <node>` | everyone | Stand-in until the tree screen exists. |
| `/pipican stat raise <stat>` | everyone | Costs vanilla levels: `stats.cost_base_levels`, +1 every `stats.cost_increase_every` points bought. Free in creative. |
| `/pipican trinkets` | everyone | Same as **G**. |

---

## 7. Code map

```
com.pipican
├── Pipican.java              mod entry: registers everything
├── core/
│   ├── PipicanRegistries     the 4 datapack registries
│   ├── PipicanConfig         server config (XP curve, stat cost, affinity tiers)
│   └── ClassLookup           helpers to read the registries (server & client)
├── rpgclass/                 the class template: RpgClass, SpecialStat, Progression, Weakness,
│                             Affinity (tiers), AffinityRules
├── stat/StatDefinition
├── skill/                    SkillTree, SkillNode, TreeKind (general/special)
├── trinket/                  TrinketSlotType, TrinketLayout, TrinketMenu, PipicanMenus
├── effect/                   ClassEffect (interface), ClassEffectTypes (type registry),
│   └── type/                 the built-in effect types
├── player/
│   ├── PlayerClassData       what is saved on the player (+ PipicanAttachments)
│   ├── ClassProgress         level, XP, unlocked nodes for one class
│   ├── ClassService          ALL mutations (choose, xp, unlock, raise stat, reset...)
│   ├── ClassEffectApplier    recomputes and applies all bonuses  ← the heart of it
│   └── ClassState            computed result, read by event handlers
├── event/ClassEvents         login/respawn/equipment/damage/kill/tick hooks
├── command/PipicanCommands
├── network/                  packets (class data itself syncs via the attachment)
└── client/                   selection screen, trinket screen, resource HUD, keys, tooltips
```

**Rules of thumb for contributors**

* Mutate player data only through `ClassService`. It calls `changed()` → `refresh()` + sync.
* Never store derived numbers on the player; compute them in `ClassEffectApplier`.
* Client code stays in `client/` (it is only loaded on the client).
* Keep JSON fields `snake_case` and optional with sensible defaults.

---

## 8. Testing

* `gradlew runGameTestServer` runs the GameTests in `src/gametest` (choosing a class, skill trees, affinities,
  damage multipliers, stats/levels, trinkets). They use the hidden test class `pipican_test:tester` from
  `src/gametest/resources`, which never ships in the mod jar. In a dev world, `/test run pipican:*` runs them too.
* `gradlew runClient` + a datapack in `run/saves/<world>/datapacks/` to try classes by hand. The template pack in
  `docs/templates/` is a working starting point.

Note: Minecraft 26.1 needs **Java 25**; Gradle downloads it automatically the first time.
