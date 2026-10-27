package com.pipican.worldgen;

import java.util.*;
/**
 * Pure structure candidate planner, safe to query out of chunk order.
 * Actual structure templates, biome checks, protected capital masks and
 * NeoForge placement registration must be implemented by the platform bridge.
 */
public final class StructurePlanner {
    public enum Type { VILLAGE, OUTPOST, DUNGEON, MINE }
    public record Candidate(Type type,int x,int z,String region) {}
    private StructurePlanner(){}
    public static Optional<Candidate> atCell(long seed,int cellX,int cellZ,int spacing,
                                              Type type) {
        if(spacing<=0)throw new IllegalArgumentException("spacing");
        int cx=cellX*spacing+spacing/2,cz=cellZ*spacing+spacing/2;
        long v=seed ^ ((long)cellX*0x9E3779B97F4A7C15L)
                    ^ ((long)cellZ*0xBF58476D1CE4E5B9L) ^ type.ordinal()*137L;
        v^=v>>>33;v*=0xff51afd7ed558ccdL;v^=v>>>33;
        int radius=spacing/3;
        int x=cx+(int)Math.floorMod(v,2L*radius+1)-radius;
        int z=cz+(int)Math.floorMod(v>>>16,2L*radius+1)-radius;
        if(CapitalStructureExclusion.contains(x,z,350))return Optional.empty();
        return WorldLayout.regionAt(seed,x,z)
            .map(r->new Candidate(type,x,z,r.id()));
    }
    /** Must be checked against each candidate's complete footprint, not its centre alone. */
    public static boolean canPlaceFootprint(int minX,int minZ,int maxX,int maxZ,int margin) {
        if(minX>maxX||minZ>maxZ)throw new IllegalArgumentException("Invalid footprint");
        return !CapitalStructureExclusion.intersects(minX,minZ,maxX,maxZ,margin);
    }
    public static boolean collides(int x1,int z1,int radius1,int x2,int z2,int radius2){
        long dx=(long)x1-x2,dz=(long)z1-z2,lim=(long)radius1+radius2;
        return dx*dx+dz*dz<lim*lim;
    }
}
