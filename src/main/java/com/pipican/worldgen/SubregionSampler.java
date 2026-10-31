package com.pipican.worldgen;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Loads the exact 120 authored sub-region definitions at runtime.
 * Sampling is independent of chunk order and uses world seed + Voronoi cells.
 * The nominal 800-block diameter is deliberately NOT scaled from the original
 * 30-million-block world.
 */
public final class SubregionSampler {
    public record Subregion(String id,String parent,String climate,String culture,String settlementVariant) {}
    private record Weighted(Subregion region,int weight) {}
    private final Map<String,List<Weighted>> perParent;
    public SubregionSampler() {
        JsonObject root=WorldgenJson.read("region_map.json");
        Map<String,List<Weighted>> m=new HashMap<>();
        for(JsonElement regionElement:root.getAsJsonArray("macro_regions")) {
            JsonObject parent=regionElement.getAsJsonObject();
            String id=parent.get("id").getAsString();
            String climate=parent.get("climate").getAsString();
            String culture=parent.get("culture").getAsString();
            List<Weighted> entries=new ArrayList<>();
            for(JsonElement childElement:parent.getAsJsonArray("subregions")) {
                JsonObject child=childElement.getAsJsonObject();
                String variant=child.has("settlement_variant")&&!child.get("settlement_variant").isJsonNull()
                    ? child.get("settlement_variant").getAsString() : "";
                entries.add(new Weighted(new Subregion(child.get("id").getAsString(),id,climate,culture,variant),
                    child.get("weight").getAsInt()));
            }
            m.put(id,List.copyOf(entries));
        }
        this.perParent=Map.copyOf(m);
    }
    /** Biome variation: jittered Voronoi cells rather than rigid squares. */
    public Optional<Subregion> at(long seed,int x,int z) {
        var parent=WorldLayout.regionAt(seed,x,z);
        if(parent.isEmpty())return Optional.empty();
        int size=850;
        int cx=Math.floorDiv(x,size),cz=Math.floorDiv(z,size);
        long bestDistance=Long.MAX_VALUE,bestKey=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
            long cellKey=mix(seed,cx+dx,cz+dz);
            int px=(cx+dx)*size+size/4+(int)Math.floorMod(cellKey,size/2);
            int pz=(cz+dz)*size+size/4+(int)Math.floorMod(cellKey>>>21,size/2);
            long dd=(long)(x-px)*(x-px)+(long)(z-pz)*(z-pz);
            if(dd<bestDistance){bestDistance=dd;bestKey=cellKey;}
        }
        List<Weighted> entries=perParent.get(parent.get().id());
        if(entries==null||entries.isEmpty())return Optional.empty();
        int sum=entries.stream().mapToInt(Weighted::weight).sum();
        int pick=(int)Math.floorMod(bestKey>>>35,sum);
        for(Weighted w:entries){pick-=w.weight();if(pick<0)return Optional.of(w.region());}
        return Optional.of(entries.getLast().region());
    }
    public Set<String> subregionIds() {
        Set<String> ids=new HashSet<>();
        for(var list:perParent.values())for(var w:list)ids.add(w.region().id());
        return Set.copyOf(ids);
    }
    private static long mix(long seed,long x,long z){
        long h=seed ^ (x*0x9E3779B97F4A7C15L) ^ (z*0xC2B2AE3D27D4EB4FL);
        h^=h>>>30;h*=0xBF58476D1CE4E5B9L;h^=h>>>27;
        h*=0x94D049BB133111EBL;return h^(h>>>31);
    }
}
