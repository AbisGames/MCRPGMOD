# Ores

160 ores in nine tiers, from the design document "Fantastik Cevherler — Kademe, Nadirlik ve Setler" plus the
worldgen branch's 27 regional ores (sorted into the same tiers: common → T1, uncommon → T2, rare → T3, epic → T5;
Bloodstone and Sunstone were in both and are one ore each). The Mistborn base metals that have no vanilla ore
(tin, zinc, cadmium, chromium, nickel, aluminum) are ores too.

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

Counts are per chunk *in the ore's biomes* (see `PLACES` in the catalog). Rarer ores also hide from the air more
often, like diamonds. Overworld ores come in stone and deepslate; Nether ores in netherrack; End ores in end stone.

Each ore has: ore blocks, a raw item (metals, smelts and blasts into an ingot) or the gem itself, a storage block
(and a raw block for metals), 9 ↔ 1 recipes, Fortune and Silk Touch drops, and the `c:` tags other mods use
(`c:ores/<id>`, `c:ingots/<id>`, `c:gems/<id>`, ...). `pipican:ores/tier_N` and `pipican:materials/tier_N` group
them by tier. Tooltips show the tier and what the ore is for. Names are in English and Turkish.

## Phases

1. **Ores, worldgen, materials** — done.
2. Armor, tools and weapons for every set ore (and the T8/T9 and alloy sets), with tier stats and set bonuses.
3. Special uses (fuel, currency, potion boosts, lightning cores, element ammo, class tokens ...).
4. Alloys (Manyullyn, Arcanite, Terrasteel, Daedric, Malatium), Titanite upgrades, world events (meteor craters,
   growing Chlorophyte, spreading Tiberium, unstable Quantanium) and the limited T8/T9 sources.
5. Mistborn Allomancy.
6. Tamashigane.

**Fixed mines** (worldgen branch): they keep only T1 ores. That branch isn't on main yet; its mine catalog gets
updated to the T1 regional ores (stormsilver, runic iron, redrock iron, pearlsilver, frostcrystal, tideglass,
sunglass, verdantite) when it is merged.
