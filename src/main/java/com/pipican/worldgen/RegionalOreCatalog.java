package com.pipican.worldgen;

import java.util.*;
/**
 * Regional resource plan. Ores belong to authored regions/biomes and renewable
 * fixed mine instances; this catalog deliberately does NOT add random overworld
 * ore features. Real block/item registrations and loot are a separate integration.
 */
public final class RegionalOreCatalog {
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC }
    public record Ore(String id,String region,String mineTheme,Rarity rarity,
                      int regenerationMinutes, String dungeonUpgradeCatalyst) {
        public Ore {
            if(regenerationMinutes<=0)throw new IllegalArgumentException("regeneration time");
        }
    }
    public static final List<Ore> ORES = List.of(
        new Ore("frostcrystal","whitecrown_icecap","quarry",Rarity.COMMON,45,"whitecrown_icecap_boss_core"),
        new Ore("glacierite","northern_ice_sea","vault",Rarity.UNCOMMON,90,"northern_ice_sea_boss_core"),
        new Ore("wintersteel","pinewall_taiga","open_pit",Rarity.RARE,180,"pinewall_taiga_boss_core"),
        new Ore("stormsilver","fjordmark","cliff_mine",Rarity.COMMON,45,"fjordmark_boss_core"),
        new Ore("thunderbasalt","stormcoast","sanctum",Rarity.UNCOMMON,90,"stormcoast_boss_core"),
        new Ore("skyiron","khagan_steppe","quarry",Rarity.RARE,180,"khagan_steppe_boss_core"),
        new Ore("tideglass","western_ocean","vault",Rarity.COMMON,45,"western_ocean_boss_core"),
        new Ore("mooncrystal","elderwood","open_pit",Rarity.UNCOMMON,90,"elderwood_boss_core"),
        new Ore("royal_silver","heartlands","cliff_mine",Rarity.RARE,180,"heartlands_boss_core"),
        new Ore("runic_iron","greyspine_mountains","sanctum",Rarity.COMMON,45,"greyspine_mountains_boss_core"),
        new Ore("jade_essence","jade_empire","quarry",Rarity.UNCOMMON,90,"jade_empire_boss_core"),
        new Ore("mirestone","marshfen","vault",Rarity.RARE,180,"marshfen_boss_core"),
        new Ore("pearlsilver","inner_sea","open_pit",Rarity.COMMON,45,"inner_sea_boss_core"),
        new Ore("cinderite","ashlands","cliff_mine",Rarity.UNCOMMON,90,"ashlands_boss_core"),
        new Ore("lotus_opal","lotus_delta","sanctum",Rarity.RARE,180,"lotus_delta_boss_core"),
        new Ore("sunglass","sunscorch_desert","quarry",Rarity.COMMON,45,"sunscorch_desert_boss_core"),
        new Ore("sunstone","riverlands","vault",Rarity.UNCOMMON,90,"riverlands_boss_core"),
        new Ore("saffron_gold","spice_coast","open_pit",Rarity.RARE,180,"spice_coast_boss_core"),
        new Ore("verdantite","emerald_jungle","cliff_mine",Rarity.COMMON,45,"emerald_jungle_boss_core"),
        new Ore("tidalite","coral_isles","sanctum",Rarity.UNCOMMON,90,"coral_isles_boss_core"),
        new Ore("dawnbronze","goldgrass_savanna","quarry",Rarity.RARE,180,"goldgrass_savanna_boss_core"),
        new Ore("redrock_iron","redrock_badlands","vault",Rarity.COMMON,45,"redrock_badlands_boss_core"),
        new Ore("skycrystal","aetheris_expanse","sky_island",Rarity.EPIC,240,"aetheris_expanse_boss_core"),
        new Ore("lifebloom_crystal","verdant_depths","ancient_grove",Rarity.EPIC,240,"verdant_depths_boss_core"),
        new Ore("umbrite","umbral_chasm","abyssal_cavern",Rarity.EPIC,240,"umbral_chasm_boss_core"),
        new Ore("bloodstone","crimson_wilds","corrupted_pit",Rarity.EPIC,240,"crimson_wilds_boss_core"),
        new Ore("abyssal_shard","shimmering_reefs","reef_grotto",Rarity.EPIC,240,"shimmering_reefs_boss_core")
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
