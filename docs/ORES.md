# Ores

169 ores in nine tiers, from the design document "Fantastik Cevherler — Kademe, Nadirlik ve Setler", the RPG world's
27 regional ores (sorted into the same tiers: common → T1, uncommon → T2, rare → T3, epic → T5; Bloodstone and
Sunstone were in both and are one ore each) and its 8 rare deposits (soulstone, dragonite, abyssium, runestone,
astralite, sunforged, moonshard, primordial). The Mistborn base metals that have no vanilla ore (tin, zinc, cadmium,
chromium, nickel, aluminum) are ores too. There is one ore system: the RPG world's code
(`RegionalOreRegistries`, `RareResources`) looks its ores up in this catalog.

## Where things live

- `tools/ore_builder/catalog.py` — **the source of truth**: every ore's tier, form (metal / gem), use, spawn place,
  colour and description. Edit this, then run the generator.
- `tools/ore_builder/build_ores.py` — writes everything else:
  `python3 tools/ore_builder/build_ores.py src/main/resources`
- `tools/ore_builder/textures.py` — draws the grayscale textures (our own art). The game tints them per ore, so
  one drawing serves every ore.
- `src/main/resources/pipican/ores.json` — generated; Java registers blocks and items from it at startup
  (`com.pipican.ore.PipicanOres`), so adding an ore needs no Java.

## Tiers

| Tier | Name | Spawns | Mined with |
|---|---|---|---|
| T1 | Common | 10 veins per chunk | stone pickaxe |
| T2 | Frequent | 6 per chunk | iron |
| T3 | Medium | 3 per chunk | iron |
| T4 | Rare | 0–2 per chunk | diamond |
| T5 | Very rare | 1 per 10 chunks | netherite |
| T6 | Epic | 1 per 50 chunks | a T5 tool of this mod (phase 2) |
| T7 | Legendary | 1 per 200 chunks | a T6 tool (phase 2) |
| T8 | Mythic | limited per world (phase 4) | a T7 tool |
| T9 | Unique | one source per world (phase 4) | a T8 tool |

Counts are per chunk *in the ore's biomes* (see `PLACES` in the catalog; in the RPG world preset, the regions listed
in `REGIONS` too). Rarer ores also hide from the air more
often, like diamonds. Overworld ores come in stone and deepslate; Nether ores in netherrack; End ores in end stone.

Each ore has: ore blocks, a raw item (metals, smelts and blasts into an ingot) or the gem itself, a storage block
(and a raw block for metals), 9 ↔ 1 recipes, Fortune and Silk Touch drops, and the `c:` tags other mods use
(`c:ores/<id>`, `c:ingots/<id>`, `c:gems/<id>`, ...). `pipican:ores/tier_N` and `pipican:materials/tier_N` group
them by tier. Tooltips show the tier and what the ore is for. Names are in English and Turkish.

## Gear sets (phase 2)

86 sets (`tools/ore_builder/sets.py`): every set ore of the document, the T8/T9 sets and the alloy sets. Each has a
helmet, chestplate, leggings, boots, sword, axe, spear, pickaxe, shovel and hoe (Valyrian steel: sword, axe and spear
only, and no recipe - it is found, not forged).

- **Numbers** come from the tier (T4 = diamond, T5 = netherite, higher tiers above) and the set's *style*: `fast`,
  `light`, `magic`, `ranged`, `crit`, `sustain`, `balanced`, `tank`, `heavy`. A faster set protects less, a tank
  set is tougher but its swing is slower - so sets of a tier are about equally strong overall.
- **Mining**: T1-T2 tools mine like iron (up to T3 ores), T3 like diamond (T4), T4 like netherite (T5), T5 and up one
  tier above their own. Adamantine and Unobtanium tools mine everything.
- **Set bonuses** (`data/pipican/pipican/gear_set/<set>.json`): class effects while wearing all four pieces, while
  holding one of the set's weapons or tools, and (Truesilver) while wearing the helmet. They're written in
  `sets.py` as class-spec lines ("+25% damage against undead") and translated by the class builder, or as raw
  effect JSON. They apply to players who have a class.
- **Item traits**: `unbreakable` (Darksteel, Adamantine, Adamantium, Unobtanium), `self_repair` (Manasteel, Goblin
  Silver, Void Metal), `autosmelt` (Dwemer), `area_mine` 3x3 (Iridium), `soulbound` kept on death (Beskar,
  Unobtanium), `mine_all`, `fragile` / `durable` (half / double durability).
- **Requirements**: tier gear needs Strength (weapons, armor) or Dexterity (spears): 2, 3, 5, 6, 9, 12, 15, 20, 25
  points for T1-T9 - one affinity tier worse below it.
- Textures: shared grayscale icons (wooden handle + tinted head) and three worn-armor weights (light, medium, heavy),
  tinted with the ore's colour.

## Phases

1. **Ores, worldgen, materials** — done.
2. **Armor, tools and weapons, set bonuses** — done.
3. Special uses (fuel, currency, potion boosts, lightning cores, element ammo, class tokens ...).
4. Alloys (Manyullyn, Arcanite, Terrasteel, Daedric, Malatium), Titanite upgrades, world events (meteor craters,
   growing Chlorophyte, spreading Tiberium, unstable Quantanium) and the limited T8/T9 sources.
5. Mistborn Allomancy.
6. Tamashigane.

## Regional mines

Each of the RPG world's 27 regions has a renewable fixed mine; mines hold **only T1 ores** (`mineOre` in
`RegionalOreCatalog`, `ORE` in `tools/worldgen/build_full_structures.py`): Frostcrystal, Stormsilver, Tideglass,
Pearlsilver, Electrum, Corundum, Verdantite, Redrock Iron, Runic Iron, Sunglass, Cold Iron, Amber, Tin, Nitre,
Truesilver or Morkite, by region. The region's own signature ore (`id`) is still what its boss drops and its
blacksmith uses - as an ingot (metals) or the gem - and it spawns by tier in the open world like every other ore.
Hand-made textures in `assets/pipican/textures/{block,item}/<name>.png` replace the tinted ones automatically.
