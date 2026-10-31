package com.pipican.worldgen;

import java.util.*;
/**
 * Stable logical world map. Horizontal coordinates are scaled from the authored
 * 30,000,000-block map to a -25,000..25,000 play area. Actual castles, mines
 * and structure pieces are NOT scaled. Nothing below assumes a fixed surface Y.
 */
public final class WorldLayout {
    public static final int HALF_SIZE = 25_000;
    /** Authored extent is -15 million..15 million, yielding scale 1:600. */
    private static final double AUTHOR_SCALE = 1.0 / 600.0;
    public record Region(String id,int minX,int minZ,int maxX,int maxZ,String ore) {
        public Region {
            if(minX>=maxX||minZ>=maxZ)throw new IllegalArgumentException("region bounds");
        }
        public boolean contains(double x,double z){return x>=minX&&x<maxX&&z>=minZ&&z<maxZ;}
    }
    public record ExtraBiome(String id,String parent,String ore,int nominalDiameter) {}
    public static final List<Region> REGIONS = List.of(
        new Region("whitecrown_icecap",-25000,-25000,0,-16667,"frostcrystal"),
        new Region("northern_ice_sea",0,-25000,25000,-16667,"glacierite"),
        new Region("pinewall_taiga",-25000,-16667,-8333,-8333,"wintersteel"),
        new Region("fjordmark",-8333,-16667,0,-8333,"stormsilver"),
        new Region("stormcoast",0,-16667,8333,-8333,"thunderbasalt"),
        new Region("khagan_steppe",8333,-16667,25000,-8333,"skyiron"),
        new Region("western_ocean",-25000,-8333,-16667,8333,"tideglass"),
        new Region("elderwood",-16667,-8333,-8333,0,"mooncrystal"),
        new Region("heartlands",-8333,-8333,0,0,"royal_silver"),
        new Region("greyspine_mountains",0,-8333,8333,8333,"runic_iron"),
        new Region("jade_empire",8333,-8333,25000,0,"jade_essence"),
        new Region("marshfen",-16667,0,-8333,8333,"mirestone"),
        new Region("inner_sea",-8333,0,0,8333,"pearlsilver"),
        new Region("ashlands",8333,0,16667,8333,"cinderite"),
        new Region("lotus_delta",16667,0,25000,8333,"lotus_opal"),
        new Region("sunscorch_desert",-25000,8333,-8333,16667,"sunglass"),
        new Region("riverlands",-8333,8333,0,16667,"sunstone"),
        new Region("spice_coast",0,8333,8333,16667,"saffron_gold"),
        new Region("emerald_jungle",8333,8333,16667,25000,"verdantite"),
        new Region("coral_isles",16667,8333,25000,25000,"tidalite"),
        new Region("goldgrass_savanna",-25000,16667,-8333,25000,"dawnbronze"),
        new Region("redrock_badlands",-8333,16667,8333,25000,"redrock_iron")
    );
    public static final List<ExtraBiome> EXTRA_BIOMES = List.of(
        new ExtraBiome("aetheris_expanse","greyspine_mountains","skycrystal",1800),
        new ExtraBiome("verdant_depths","emerald_jungle","lifebloom_crystal",2200),
        new ExtraBiome("umbral_chasm","greyspine_mountains","umbrite",1700),
        new ExtraBiome("crimson_wilds","ashlands","bloodstone",1700),
        new ExtraBiome("shimmering_reefs","coral_isles","abyssal_shard",2400)
    );
    private WorldLayout() {}
    public static int scaleCoordinate(int authored) {
        return (int)Math.round(authored * AUTHOR_SCALE);
    }
    /** Fast exact macro lookup (before edge warping). */
    public static Optional<Region> authoredRegionAt(int x,int z) {
        return REGIONS.stream().filter(r->r.contains(x,z)).findFirst();
    }
    /**
     * Deterministic coordinate warp: deforms straight authored borders while
     * preserving the map's macro layout, regardless of sampling order.
     * The 180-block warp must be disabled near protected landmark centres.
     */
    public static Optional<Region> regionAt(long seed,int x,int z){
        if(Math.abs(x)>HALF_SIZE||Math.abs(z)>HALF_SIZE)return Optional.empty();
        double dx=fractal(seed,x,z,4100)*180;
        double dz=fractal(seed^0x6a09e667f3bcc909L,x,z,4100)*180;
        return authoredRegionAt((int)Math.floor(x+dx),(int)Math.floor(z+dz));
    }
    /** Smooth value noise; pure and repeatable across chunks. */
    public static double fractal(long seed,int x,int z,int period) {
        return 0.65*valueNoise(seed,x/(double)period,z/(double)period)
            +0.35*valueNoise(seed+17,x/(double)(period/2),z/(double)(period/2));
    }
    private static double valueNoise(long seed,double x,double z) {
        long ix=(long)Math.floor(x), iz=(long)Math.floor(z);
        double fx=x-ix,fz=z-iz;
        fx=fx*fx*(3-2*fx); fz=fz*fz*(3-2*fz);
        double a=mix(hash(seed,ix,iz),hash(seed,ix+1,iz),fx);
        double b=mix(hash(seed,ix,iz+1),hash(seed,ix+1,iz+1),fx);
        return mix(a,b,fz);
    }
    private static double mix(double a,double b,double t){return a+(b-a)*t;}
    private static double hash(long seed,long x,long z) {
        long h=seed ^ (x*0x9E3779B97F4A7C15L) ^ (z*0xC2B2AE3D27D4EB4FL);
        h^=h>>>30;h*=0xBF58476D1CE4E5B9L;
        h^=h>>>27;h*=0x94D049BB133111EBL;h^=h>>>31;
        return ((h>>>11)*0x1.0p-53)*2-1;
    }
}
