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
                        boolean morvath=name.equals("morvath_ashen_king");
                        float shell=morvath && (id.endsWith("_arm") || id.endsWith("_leg")) && cubeIndex>0 ? 0.035f : 0;
                        // The original hips overlap by 0.825 px and share the torso's front plane.
                        // Keep the source assets intact while separating the visible armor surfaces.
                        if(morvath && id.equals("left_leg")) origin[0]-=0.125f;
                        if(morvath && id.equals("right_leg")) origin[0]+=0.95f;
                        if(morvath && id.equals("body") && cubeIndex==0) shell=0.06f;
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
                    .texOffs(160,160).addBox(-1.5f,-4,-1.5f,3,9,3)
                    .texOffs(174,160).addBox(-2,-6,-2,4,2,4)
                    .texOffs(180,160).addBox(-6,5,-2.5f,12,2,5)
                    .texOffs(160,180).addBox(-3.5f,7,-1.5f,7,19,3)
                    .texOffs(184,180).addBox(-2.5f,26,-1.5f,5,5,3)
                    .texOffs(204,180).addBox(-1.5f,31,-1.0f,3,4,2)
                    .texOffs(220,180).addBox(-0.5f,35,-0.5f,1,2,1),PartPose.offsetAndRotation(2.5f,19.5f,0,-1.25f,0,0));
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
