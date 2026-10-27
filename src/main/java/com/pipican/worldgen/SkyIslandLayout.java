package com.pipican.worldgen;

import java.util.List;

/**
 * Authoritative, deterministic floating-island sites in the Aetheris reserve.
 * The sky city is the only castle; six separate islands carry its villages.
 * All elevations are in real world blocks, not author-map coordinates.
 */
public final class SkyIslandLayout {
    public static final int FOUNDATION_Y=319;
    public record Island(String id,int x,int z,int radius,boolean citadel) {}
    public static final List<Island> ISLANDS=List.of(
        new Island("aetheris_citadel",5700,1600,100,true),
        new Island("aetheris_village_n",5700,1180,78,false),
        new Island("aetheris_village_s",5700,2020,78,false),
        new Island("aetheris_village_w",5280,1600,78,false),
        new Island("aetheris_village_e",6120,1600,78,false),
        new Island("aetheris_village_nw",5385,1285,78,false),
        new Island("aetheris_village_se",6015,1915,78,false)
    );
    private SkyIslandLayout() {}

    /** Solid natural rock below the 319-height structure foundations.
     * Density fades to zero at the outside rim and in the underside tip.
     * Keeping y >= FOUNDATION_Y empty avoids burying authored buildings.
     */
    public static double density(int x,int y,int z) {
        int depth=FOUNDATION_Y-1-y;
        if(depth<0 || depth>=58)return 0;
        double best=0;
        for(Island island:ISLANDS){
            long dx=(long)x-island.x(),dz=(long)z-island.z();
            double radial=Math.hypot(dx,dz);
            double taper=Math.pow(Math.max(0,1-depth/58.0),0.8);
            double radius=island.radius()*taper;
            if(radial>=radius || radius<=1)continue;
            double edge=1-Math.pow(radial/radius,4);
            double tip=Math.min(1.0,(58.0-depth)/7.0);
            best=Math.max(best,12.0*edge*tip);
        }
        return best;
    }

    public static boolean themedSkyAt(int x,int y,int z){
        if(y<245)return false;
        for(Island island:ISLANDS){
            long dx=(long)x-island.x(),dz=(long)z-island.z();
            long reach=island.radius()+100L;
            if(dx*dx+dz*dz<reach*reach)return true;
        }
        return false;
    }
}
