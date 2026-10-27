package com.pipican.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Immutable protection volumes around the 22 authored capital sites.
 * Reject candidate footprints in X/Z regardless of their height: underground
 * structures also cannot tunnel through castle foundations.
 */
public final class CapitalStructureExclusion {
    public record Zone(String id,int x,int z,int radius) {
        public boolean intersects(int minX,int minZ,int maxX,int maxZ,int margin) {
            long px=Math.max(minX,Math.min((long)x,maxX));
            long pz=Math.max(minZ,Math.min((long)z,maxZ));
            long dx=px-x,dz=pz-z,r=(long)radius+Math.max(0,margin);
            return dx*dx+dz*dz<=r*r;
        }
    }
    private static final List<Zone> ZONES=load();
    private CapitalStructureExclusion() {}
    private static List<Zone> load(){
        JsonObject json=WorldgenJson.read("fixed_capital_locations.json");
        List<Zone> zones=new ArrayList<>();
        for(JsonElement entry:json.getAsJsonArray("capitals")){
            JsonObject c=entry.getAsJsonObject();
            zones.add(new Zone(c.get("id").getAsString(),c.get("x").getAsInt(),
                c.get("z").getAsInt(),c.get("preserve_radius").getAsInt()));
        }
        if(zones.size()!=22)throw new IllegalStateException("Expected 22 capital protection zones");
        return List.copyOf(zones);
    }
    public static List<Zone> all(){return ZONES;}
    public static boolean contains(int x,int z,int margin){
        return intersects(x,z,x,z,margin);
    }
    public static boolean intersects(BoundingBox box,int margin){
        return intersects(box.minX(),box.minZ(),box.maxX(),box.maxZ(),margin);
    }
    public static boolean intersects(int minX,int minZ,int maxX,int maxZ,int margin){
        for(Zone zone:ZONES)if(zone.intersects(minX,minZ,maxX,maxZ,margin))return true;
        return false;
    }
}
