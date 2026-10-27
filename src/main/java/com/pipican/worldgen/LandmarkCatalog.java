package com.pipican.worldgen;

import com.google.gson.*;
import java.util.*;

/**
 * Keeps hand-authored cities/fortresses at their world-map locations without
 * scaling their physical radii. Spawn-cluster records are already authored in
 * actual player-scale blocks, unlike the original continental landmarks.
 */
public final class LandmarkCatalog {
    public record Landmark(String id,String tier,String region,String assetPool,
                           int x,int z,int radius,Set<String> tags) {}
    private final List<Landmark> landmarks;
    public LandmarkCatalog(){
        var root=WorldgenJson.read("settlements.json");
        List<Landmark> built=new ArrayList<>();
        for(JsonElement element:root.getAsJsonArray("settlements")){
            JsonObject e=element.getAsJsonObject();
            String id=e.get("id").getAsString();
            Set<String> tags=new HashSet<>();
            for(JsonElement tag:e.getAsJsonArray("tags"))tags.add(tag.getAsString());
            boolean spawnCluster=tags.contains("spawn");
            int x=e.get("x").getAsInt(),z=e.get("z").getAsInt();
            if(!spawnCluster){
                x=WorldLayout.scaleCoordinate(x);
                z=WorldLayout.scaleCoordinate(z);
            }
            // The authored Millbrook village clips Aldermere after scaling.
            // Offset within the Heartlands without reducing either landmark.
            if(id.equals("millbrook")){x=-4100;z=-2900;}
            JsonArray bounds=e.getAsJsonArray("radius");
            int radius=bounds.get(bounds.size()-1).getAsInt();
            built.add(new Landmark(id,e.get("tier").getAsString(),
                e.get("region").getAsString(),e.get("asset_pool").getAsString(),
                x,z,radius,Set.copyOf(tags)));
        }
        this.landmarks=List.copyOf(built);
        for(int i=0;i<landmarks.size();i++)for(int j=i+1;j<landmarks.size();j++) {
            Landmark p=landmarks.get(i),q=landmarks.get(j);
            if(StructurePlanner.collides(p.x(),p.z(),p.radius(),q.x(),q.z(),q.radius()))
                throw new IllegalStateException("Overlapping landmarks: "+p.id()+", "+q.id());
        }
    }
    public List<Landmark> all(){return landmarks;}
    /** Protected circle includes an additional margin for terrain blending. */
    public boolean inProtectedZone(int x,int z,int margin){
        if(CapitalStructureExclusion.contains(x,z,margin))return true;
        for(Landmark l:landmarks)
            if(StructurePlanner.collides(x,z,0,l.x(),l.z(),l.radius()+margin))
                return true;
        return false;
    }
}
