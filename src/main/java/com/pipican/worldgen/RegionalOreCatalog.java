package com.pipican.worldgen;

import java.util.*;
/**
 * Regional resource plan: each region's signature material ({@code id}, dropped by its boss and used by its
 * blacksmith) and the ore its renewable fixed mine holds ({@code mineOre}). Mines hold only T1 (common) ores; the
 * ores themselves - and where they spawn in the open world - come from the tiered ore catalog
 * ({@link com.pipican.ore.PipicanOres}, tools/ore_builder/catalog.py).
 */
public final class RegionalOreCatalog {
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC }
    public record Ore(String id,String region,String mineTheme,Rarity rarity,
                      int regenerationMinutes, String dungeonUpgradeCatalyst, String mineOre) {
        public Ore {
            if(regenerationMinutes<=0)throw new IllegalArgumentException("regeneration time");
        }
    }
    public static final List<Ore> ORES = List.of(
        new Ore("frostcrystal","whitecrown_icecap","quarry",Rarity.COMMON,45,"whitecrown_icecap_boss_core","frostcrystal"),
        new Ore("glacierite","northern_ice_sea","vault",Rarity.COMMON,45,"northern_ice_sea_boss_core","frostcrystal"),
        new Ore("wintersteel","pinewall_taiga","open_pit",Rarity.COMMON,45,"pinewall_taiga_boss_core","cold_iron"),
        new Ore("stormsilver","fjordmark","cliff_mine",Rarity.COMMON,45,"fjordmark_boss_core","stormsilver"),
        new Ore("thunderbasalt","stormcoast","sanctum",Rarity.COMMON,45,"stormcoast_boss_core","stormsilver"),
        new Ore("skyiron","khagan_steppe","quarry",Rarity.COMMON,45,"khagan_steppe_boss_core","corundum"),
        new Ore("tideglass","western_ocean","vault",Rarity.COMMON,45,"western_ocean_boss_core","tideglass"),
        new Ore("mooncrystal","elderwood","open_pit",Rarity.COMMON,45,"elderwood_boss_core","amber"),
        new Ore("royal_silver","heartlands","cliff_mine",Rarity.COMMON,45,"heartlands_boss_core","electrum"),
        new Ore("runic_iron","greyspine_mountains","sanctum",Rarity.COMMON,45,"greyspine_mountains_boss_core","runic_iron"),
        new Ore("jade_essence","jade_empire","quarry",Rarity.COMMON,45,"jade_empire_boss_core","verdantite"),
        new Ore("mirestone","marshfen","vault",Rarity.COMMON,45,"marshfen_boss_core","tin"),
        new Ore("pearlsilver","inner_sea","open_pit",Rarity.COMMON,45,"inner_sea_boss_core","pearlsilver"),
        new Ore("cinderite","ashlands","cliff_mine",Rarity.COMMON,45,"ashlands_boss_core","nitre"),
        new Ore("lotus_opal","lotus_delta","sanctum",Rarity.COMMON,45,"lotus_delta_boss_core","pearlsilver"),
        new Ore("sunglass","sunscorch_desert","quarry",Rarity.COMMON,45,"sunscorch_desert_boss_core","sunglass"),
        new Ore("sunstone","riverlands","vault",Rarity.COMMON,45,"riverlands_boss_core","electrum"),
        new Ore("saffron_gold","spice_coast","open_pit",Rarity.COMMON,45,"spice_coast_boss_core","corundum"),
        new Ore("verdantite","emerald_jungle","cliff_mine",Rarity.COMMON,45,"emerald_jungle_boss_core","verdantite"),
        new Ore("tidalite","coral_isles","sanctum",Rarity.COMMON,45,"coral_isles_boss_core","tideglass"),
        new Ore("dawnbronze","goldgrass_savanna","quarry",Rarity.COMMON,45,"goldgrass_savanna_boss_core","corundum"),
        new Ore("redrock_iron","redrock_badlands","vault",Rarity.COMMON,45,"redrock_badlands_boss_core","redrock_iron"),
        new Ore("skycrystal","aetheris_expanse","sky_island",Rarity.COMMON,45,"aetheris_expanse_boss_core","truesilver"),
        new Ore("lifebloom_crystal","verdant_depths","ancient_grove",Rarity.COMMON,45,"verdant_depths_boss_core","verdantite"),
        new Ore("umbrite","umbral_chasm","abyssal_cavern",Rarity.COMMON,45,"umbral_chasm_boss_core","morkite"),
        new Ore("bloodstone","crimson_wilds","corrupted_pit",Rarity.COMMON,45,"crimson_wilds_boss_core","redrock_iron"),
        new Ore("abyssal_shard","shimmering_reefs","reef_grotto",Rarity.COMMON,45,"shimmering_reefs_boss_core","tideglass")
    );
    private static final Map<String,Ore> BY_ID;
    static {
        Map<String,Ore> m=new HashMap<>();
        for(Ore o:ORES)if(m.putIfAbsent(o.id(),o)!=null)throw new IllegalStateException("duplicate ore");
        BY_ID=Map.copyOf(m);
    }
    private RegionalOreCatalog(){}
    public static Optional<Ore> get(String id){return Optional.ofNullable(BY_ID.get(id));}
    public static List<Ore> forRegion(String region){
        return ORES.stream().filter(o->o.region().equals(region)).toList();
    }
    /** Dungeon gear cannot be upgraded using ore alone. */
    public static boolean canUpgrade(String oreId,String catalystId){
        return get(oreId).filter(o->o.dungeonUpgradeCatalyst().equals(catalystId)).isPresent();
    }
}
