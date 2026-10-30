package com.pipican.client.boss;

import com.google.gson.*;
import com.pipican.Pipican;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/** Converts the preserved Bedrock box-UV geometry to native Minecraft model parts. */
final class BossGeometry {
    private BossGeometry(){}
    static ModelPart load(String name){
        try(var reader=Minecraft.getInstance().getResourceManager().getResourceOrThrow(Pipican.id("geo/bosses/"+name+".geo.json")).openAsReader()){
            var geometry=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            var description=geometry.getAsJsonObject("description");
            var mesh=new MeshDefinition();
            var pending=new ArrayList<JsonObject>();
            geometry.getAsJsonArray("bones").forEach(b->pending.add(b.getAsJsonObject()));
            Map<String,PartDefinition> parts=new HashMap<>();
            Map<String,float[]> pivots=new HashMap<>();
            while(!pending.isEmpty()){
                int before=pending.size();
                for(var iterator=pending.iterator();iterator.hasNext();){
                    var bone=iterator.next(); String id=bone.get("name").getAsString();
                    String parent=bone.has("parent") ? bone.get("parent").getAsString() : null;
                    if(parent!=null && !parts.containsKey(parent)) continue;
                    float[] pivot=vector(bone.getAsJsonArray("pivot"));
                    float[] base=parent==null ? new float[]{0,0,0} : pivots.get(parent);
                    var cubes=CubeListBuilder.create();
                    if(bone.has("cubes")) for(var entry:bone.getAsJsonArray("cubes")){
                        var cube=entry.getAsJsonObject(); float[] origin=vector(cube.getAsJsonArray("origin")), size=vector(cube.getAsJsonArray("size"));
                        var uv=cube.getAsJsonArray("uv");
                        cubes.texOffs(uv.get(0).getAsInt(),uv.get(1).getAsInt()).addBox(origin[0]-pivot[0],-origin[1]-size[1]+pivot[1],origin[2]-pivot[2],size[0],size[1],size[2]);
                    }
                    var target=parent==null ? mesh.getRoot() : parts.get(parent);
                    parts.put(id,target.addOrReplaceChild(id,cubes,PartPose.offset(pivot[0]-base[0],-pivot[1]+base[1]+(parent==null ? 24 : 0),pivot[2]-base[2])));
                    pivots.put(id,pivot); iterator.remove();
                }
                if(before==pending.size()) throw new IllegalArgumentException("Cyclic or missing boss bone parent: "+name);
            }
            return LayerDefinition.create(mesh,description.get("texture_width").getAsInt(),description.get("texture_height").getAsInt()).bakeRoot();
        }catch(IOException exception){ throw new IllegalStateException("Cannot load boss geometry "+name,exception); }
    }
    private static float[] vector(JsonArray array){ return new float[]{array.get(0).getAsFloat(),array.get(1).getAsFloat(),array.get(2).getAsFloat()}; }
}
