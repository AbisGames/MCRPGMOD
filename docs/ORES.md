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

## Special uses (phase 3)

`tools/ore_builder/specials.py` - what the special-use ores do (Java hooks in `com.pipican.ore.SpecialOreEvents`):

- **Fuel**: Vespene, Energon, Warpstone, Ion Cube, Naquadah, Elerium, Promethium (and their blocks, nine times over).
- **Brewing**: awkward potion + Frostcrystal (slowness), Tideglass / Abyssal Shard (water breathing), Mooncrystal
  (night vision), Jade Essence (healing), Lifebloom (regeneration), Skycrystal (slow falling), Mirestone (poison).
  **Amber** makes any potion long, like redstone; **Mercury** (smelted from Cinnabar) makes it strong, like glowstone.
- **Use up** (right-click): Eco, Jade Essence, Lifebloom, Mirestone (also cures poison), Skycrystal, Abyssal Shard,
  Mooncrystal; Lacrima refills 30% of your class resource; Evolution Stone +5 and Mega Stone +10 class levels.
- **Anvil coatings** (one per item): on weapons - Bloodstone (life steal 15%), Thunderstone (10% lightning),
  Silversheen (+30% vs undead and spirits), Dragonglass (+50% vs undead and ice creatures, wears the weapon), Kairoseki
  (+40% vs spellcasters, strips a buff), Dust (burn or slow), Prismatic Shard (burn, slow, 5% lightning), Materia
  (+15% magic); Kyanite on a helmet (water breathing); Starmetal on armor (+2 special stat per piece); Cortosis on a
  shield (blocking halves magic damage).
- **Off-hand focus**: Aquamarine, Khaydarin, Hextech (magic damage and more), Sunglass (day), Night Jade (night).
- **Blocks**: Sunstone (undead don't target players within 8 blocks), Sekkiseki (no teleporting within 8), Kairoseki
  (no class abilities within 6), Katchin (unbreakable).
- **Other**: Kryptonite in your inventory weakens bosses and large creatures within 8 blocks - and you; Octiron points
  to the nearest T4+ ore within 48 blocks; Energon / Sakuradite heal and empower an iron golem; Verdantite is strong
  bone meal; recipes: Nitre → gunpowder, Frostcrystal / Dragonglass arrows, Tideglass → glass, Sunglass → spyglass and
  lanterns, Thunderbasalt → lightning rods, Verdantite → bone meal.
- **Trade goods**: wandering traders (and one profession each) buy Morkite, Night Jade, Enor Pearl, Saffron Gold,
  Lotus Opal and Certus Quartz for emeralds.

Allomancy metals come in phase 5, Tamashigane in phase 6.

## Alloys, upgrades, world mechanics, limited ores (phase 4)

- **Alloys** (crafted, never mined): Cobalt + Ardite → Manyullyn; Thorium + Lacrima → Arcanite; Manasteel + Elementium
  → Terrasteel; 2 Ebony + Infernium (a boss material, the "demon heart") → Daedric; Atium + gold → Malatium.
- **Valyrian Steel** is found, never made: 1-3 ingots in 12% of ancient city, bastion, end city, stronghold library and
  mansion chests (and rare trail-ruin finds); they reforge into a sword, axe or spear.
- **Blacksmith**: Titanite stands in for any region's material, Raritanium for a boss core, Eridium for a rare trophy,
  and Pale Ore for any of them - at every region's blacksmith (`BlacksmithUpgrades.substitutes`).
- **Meteors**: on overworld nights a meteor now and then strikes near a player: a blast, then a ball of blackstone,
  magma and Meteoric Iron, with a chance of Raritanium, Luna Titanium, Element Zero, Kryptonite, Elerium, Nth Metal,
  Vibranium, and rarely Phazon or Uru.
- **Growing ores** (`SpreadingOreBlock`): Chlorophyte slowly grows into nearby stone and mud; Tiberium spreads faster,
  into stone and soil, and poisons whatever stands on it or near it.
- **Quantanium** is unstable: raw, it explodes 5 minutes after you pick it up. Refine it in a blast furnace; refined, it
  makes six TNT.
- **Warpstone**: carry it and hostile mobs near you mutate (half again the health, a quarter bigger, stronger).
- **Item uses**: Element Zero - a gravity well; Dilithium - travel to your respawn point; SAM - your ender chest
  anywhere; Zonaite - fused onto a tool, +25% mining speed.
- **Limited ores** (`LimitedOres`, counts saved with the world): one Unobtanium vein of exactly 24 ore (one full armor
  set) under the world's centre; 6 Pale Ore, 7 Chaos Emeralds, 12 Trellium deep down; one Lerasium (2000+ blocks out)
  and one Philosopher's Stone (3000+). Fortune doesn't multiply them. From bosses: Adamantium (regional bosses 15%,
  the Warden 50%), Luminite (the Wither), Draconium (the Ender Dragon), Mega Stone (regional bosses 25%, the Wither 20%).
- **Philosopher's Stone**: right-click to transmute the ore material in your off hand into one a tier higher; it costs
  2 max health each time, for good (kept through death).
- **Chaos Emeralds**: with all seven, right-click for a 30-second super form (flight, Strength III, Resistance III,
  Speed II; 10-minute cooldown).

## Phases

1. **Ores, worldgen, materials** — done.
2. **Armor, tools and weapons, set bonuses** — done.
3. **Special uses** — done.
4. **Alloys, upgrades, world mechanics, limited ores** — done.
5. Mistborn Allomancy.
6. Tamashigane.

## Regional mines

Each of the RPG world's 27 regions has a renewable fixed mine; mines hold **only T1 ores** (`mineOre` in
`RegionalOreCatalog`, `ORE` in `tools/worldgen/build_full_structures.py`): Frostcrystal, Stormsilver, Tideglass,
Pearlsilver, Electrum, Corundum, Verdantite, Redrock Iron, Runic Iron, Sunglass, Cold Iron, Amber, Tin, Nitre,
Truesilver or Morkite, by region. The region's own signature ore (`id`) is still what its boss drops and its
blacksmith uses - as an ingot (metals) or the gem - and it spawns by tier in the open world like every other ore.
Hand-made textures in `assets/pipican/textures/{block,item}/<name>.png` replace the tinted ones automatically.
