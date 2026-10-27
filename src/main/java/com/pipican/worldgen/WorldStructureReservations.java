package com.pipican.worldgen;

import java.util.List;

/** Shared horizontal exclusion mask used by candidate planning and placement.
 * Native jigsaw exclusion is enforced separately via protected biome variants.
 */
public final class WorldStructureReservations {
    private static final List<LandmarkCatalog.Landmark> SETTLEMENTS=new LandmarkCatalog().all();

    private WorldStructureReservations(){}

    private static boolean meetsCircle(int x1,int z1,int x2,int z2,
            int cx,int cz,int radius) {
        long closestX=Math.max(x1,Math.min((long)cx,x2));
        long closestZ=Math.max(z1,Math.min((long)cz,z2));
        long dx=closestX-cx,dz=closestZ-cz;
        return dx*dx+dz*dz<=(long)radius*radius;
    }

    public static boolean intersects(int minX,int minZ,int maxX,int maxZ,int margin) {
        if(minX>maxX||minZ>maxZ)throw new IllegalArgumentException("Invalid footprint");
        if(CapitalStructureExclusion.intersects(minX,minZ,maxX,maxZ,margin))return true;
        for(var site:SETTLEMENTS)
            if(meetsCircle(minX,minZ,maxX,maxZ,site.x(),site.z(),
                    site.radius()+Math.max(0,margin)))return true;
        for(var site:SpecialLandmarkPlacementEvents.sites()) {
            int radius=site.id().equals("aetheris_citadel")?105:
                site.id().startsWith("aetheris_village")?85:
                site.id().equals("umbral_city")?90:80;
            if(meetsCircle(minX,minZ,maxX,maxZ,site.x(),site.z(),
                    radius+Math.max(0,margin)))return true;
        }
        return false;
    }
}
