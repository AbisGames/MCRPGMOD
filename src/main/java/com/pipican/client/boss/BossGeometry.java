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
                    int cubeIndex=0;
                    if(bone.has("cubes")) for(var entry:bone.getAsJsonArray("cubes")){
                        var cube=entry.getAsJsonObject(); float[] origin=vector(cube.getAsJsonArray("origin")), size=vector(cube.getAsJsonArray("size"));
                        var uv=cube.getAsJsonArray("uv");
                        // Prototype cuffs/boots share planes with their underlying limb cubes.
                        // Separate just those outer shells to avoid base/emissive z-fighting.
                        float shell=name.equals("morvath_ashen_king") && (id.endsWith("_arm") || id.endsWith("_leg")) && cubeIndex>0 ? 0.035f : 0;
                        cubes.texOffs(uv.get(0).getAsInt(),uv.get(1).getAsInt()).addBox(origin[0]-pivot[0],-origin[1]-size[1]+pivot[1],origin[2]-pivot[2],size[0],size[1],size[2],new CubeDeformation(shell));
                        cubeIndex++;
                    }
                    var target=parent==null ? mesh.getRoot() : parts.get(parent);
                    parts.put(id,target.addOrReplaceChild(id,cubes,PartPose.offset(pivot[0]-base[0],-pivot[1]+base[1]+(parent==null ? 24 : 0),pivot[2]-base[2])));
                    pivots.put(id,pivot); iterator.remove();
                }
                if(before==pending.size()) throw new IllegalArgumentException("Cyclic or missing boss bone parent: "+name);
            }
            if(name.equals("morvath_ashen_king")){
                // Native attachments preserve the original 27 geometry files.
                parts.get("right_arm").addOrReplaceChild("royal_blade",CubeListBuilder.create()
                    .texOffs(160,160).addBox(-2,0,-2,4,9,4)
                    .texOffs(180,160).addBox(-8,8,-3,16,3,6)
                    .texOffs(160,180).addBox(-4,11,-1.5f,8,30,3),PartPose.offsetAndRotation(3,19,-2,-1.5708f,0,0));
                parts.get("head").addOrReplaceChild("animated_eyes",CubeListBuilder.create().texOffs(230,230)
                    .addBox(-5,-6,-8.1f,3,1,1).addBox(2,-6,-8.1f,3,1,1),PartPose.ZERO);
                parts.get("left_arm").addOrReplaceChild("shoulder_plate",CubeListBuilder.create().texOffs(190,100).addBox(-9,-4,-7,14,9,14),PartPose.ZERO);
                parts.get("right_arm").addOrReplaceChild("shoulder_plate",CubeListBuilder.create().texOffs(190,100).addBox(-5,-4,-7,14,9,14),PartPose.ZERO);
            }
            return LayerDefinition.create(mesh,description.get("texture_width").getAsInt(),description.get("texture_height").getAsInt()).bakeRoot();
        }catch(IOException exception){ throw new IllegalStateException("Cannot load boss geometry "+name,exception); }
    }
    private static float[] vector(JsonArray array){ return new float[]{array.get(0).getAsFloat(),array.get(1).getAsFloat(),array.get(2).getAsFloat()}; }
}
