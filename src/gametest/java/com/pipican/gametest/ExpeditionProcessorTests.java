package com.pipican.gametest;

import java.util.List;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.pipican.worldgen.PlayerWorldgenProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Safety regressions for the native access edit plan, including protected air. */
final class ExpeditionProcessorTests {
    private ExpeditionProcessorTests(){}
    static void verify(GameTestHelper helper){
        var level=helper.getLevel();
        PlayerWorldgenProtection.get(level);
        var processor=StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"processor_type\":\"pipican:expedition_access\"}")).getOrThrow();
        var origin=helper.absolutePos(new BlockPos(-1040,25,-1024));
        var ladder=origin.offset(40,1,0);
        var local=new StructureTemplate.StructureBlockInfo(new BlockPos(40,0,1),Blocks.STONE.defaultBlockState(),null);
        var placed=new StructureTemplate.StructureBlockInfo(origin.offset(40,0,1),Blocks.STONE.defaultBlockState(),null);
        var input=List.of(placed);
        var bounds=new BoundingBox(ladder.getX()-1,level.getMinY(),ladder.getZ(),
            ladder.getX(),level.getMaxY()-1,ladder.getZ());
        var settings=new StructurePlaceSettings().setBoundingBox(bounds);
        var plan=processor.finalizeProcessing(level,origin,origin,List.of(local),input,settings);
        helper.assertTrue(plan.size()>input.size(),"access fixture did not create an edit plan");
        for(var edit:plan.subList(input.size(),plan.size()))
            helper.assertTrue(bounds.isInside(edit.pos()),"access processor wrote outside its decoration box");
        helper.assertTrue(processor.finalizeProcessing(level,origin,origin,List.of(),List.of(),settings).isEmpty(),
            "access processor treated data markers as a second template placement");
        // The original state is air: historical excavation must still be protected.
        PlayerWorldgenProtection.get(level).protect(ladder);
        helper.assertTrue(processor.finalizeProcessing(level,origin,origin,List.of(local),input,settings).equals(input),
            "access processor ignored player-protected air");
        for(var obstruction:List.of(Blocks.CHEST.defaultBlockState(),Blocks.GOLD_BLOCK.defaultBlockState())){
            var shifted=origin.offset(8,0,0);
            var blocked=shifted.offset(40,1,0);
            var previous=level.getBlockState(blocked);
            var shiftedInput=List.of(new StructureTemplate.StructureBlockInfo(shifted.offset(40,0,1),Blocks.STONE.defaultBlockState(),null));
            var clip=new StructurePlaceSettings().setBoundingBox(new BoundingBox(blocked.getX()-1,level.getMinY(),blocked.getZ(),
                blocked.getX(),level.getMaxY()-1,blocked.getZ()));
            helper.assertTrue(processor.finalizeProcessing(level,shifted,shifted,List.of(local),shiftedInput,clip).size()>shiftedInput.size(),
                "obstruction fixture was already blocked before inserting the tested block");
            try{
                level.setBlock(blocked,obstruction,2);
                helper.assertTrue(processor.finalizeProcessing(level,shifted,shifted,List.of(local),shiftedInput,clip).equals(shiftedInput),
                    "access processor replaced an inventory or artificial block: "+obstruction);
                helper.assertTrue(level.getBlockState(blocked).equals(obstruction),"preflight mutated the world");
            }finally{level.setBlock(blocked,previous,2);}
        }
        var naturalOrigin=origin.offset(16,0,0);
        var naturalPos=naturalOrigin.offset(40,1,0);
        var naturalInput=List.of(new StructureTemplate.StructureBlockInfo(naturalOrigin.offset(40,0,1),Blocks.STONE.defaultBlockState(),null));
        var naturalClip=new StructurePlaceSettings().setBoundingBox(new BoundingBox(naturalPos.getX()-1,level.getMinY(),naturalPos.getZ(),
            naturalPos.getX(),level.getMaxY()-1,naturalPos.getZ()));
        var supportPos=naturalPos.below();
        var previousSupport=level.getBlockState(supportPos);
        try{
            level.setBlock(supportPos,Blocks.MOSS_BLOCK.defaultBlockState(),2);
            for(var growth:List.of(Blocks.BIG_DRIPLEAF.defaultBlockState(),Blocks.MOSS_BLOCK.defaultBlockState(),Blocks.MOSS_CARPET.defaultBlockState(),
                    Blocks.SCULK.defaultBlockState(),Blocks.SCULK_VEIN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN,true))){
                var previous=level.getBlockState(naturalPos);
                try{
                    level.setBlock(naturalPos,growth,2);
                    helper.assertTrue(level.getBlockState(naturalPos).is(growth.getBlock()),"natural growth fixture did not survive placement");
                    helper.assertTrue(processor.finalizeProcessing(level,naturalOrigin,naturalOrigin,List.of(local),naturalInput,naturalClip).size()>naturalInput.size(),
                        "natural deep-dark terrain blocked access: "+growth);
                }finally{level.setBlock(naturalPos,previous,2);}
            }
        }finally{level.setBlock(supportPos,previousSupport,2);}
        var vineCeiling=naturalPos.above();
        var oldCeiling=level.getBlockState(vineCeiling);
        var oldVine=level.getBlockState(naturalPos);
        try{
            level.setBlock(vineCeiling,Blocks.STONE.defaultBlockState(),2);
            level.setBlock(naturalPos,Blocks.CAVE_VINES.defaultBlockState(),2);
            helper.assertTrue(level.getBlockState(naturalPos).is(Blocks.CAVE_VINES),"cave-vine fixture did not survive placement");
            helper.assertTrue(processor.finalizeProcessing(level,naturalOrigin,naturalOrigin,List.of(local),naturalInput,naturalClip).size()>naturalInput.size(),
                "natural cave vines blocked access");
        }finally{level.setBlock(naturalPos,oldVine,2);level.setBlock(vineCeiling,oldCeiling,2);}
        var canopyPos=origin.offset(64,10,0);
        level.getChunk(canopyPos.getX()>>4,canopyPos.getZ()>>4);
        double ground=ExpeditionAccessTests.exteriorSurface(level,canopyPos.getX(),canopyPos.getZ());
        var previousCanopy=level.getBlockState(canopyPos);
        try{
            level.setBlock(canopyPos,Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.PERSISTENT,true),2);
            helper.assertTrue(ExpeditionAccessTests.exteriorSurface(level,canopyPos.getX(),canopyPos.getZ())==ground,
                "foliage and air gaps incorrectly hid exterior ground");
            level.setBlock(canopyPos,Blocks.STONE.defaultBlockState(),2);
            helper.assertTrue(ExpeditionAccessTests.exteriorSurface(level,canopyPos.getX(),canopyPos.getZ())>canopyPos.getY(),
                "solid underground ceiling was incorrectly accepted as exterior ground");
        }finally{level.setBlock(canopyPos,previousCanopy,2);}
        var poolJson=com.pipican.worldgen.WorldgenJson.read("template_pool/regional/heartlands/mine.json")
            .getAsJsonArray("elements").get(0).getAsJsonObject().get("element");
        var ops=net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE,level.registryAccess());
        var pool=net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.CODEC.parse(ops,poolJson).getOrThrow();
        helper.assertTrue(pool instanceof com.pipican.worldgen.ExpeditionPoolElement,"native access pool codec was not registered");
        var encoded=net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.CODEC.encodeStart(ops,pool).getOrThrow();
        var restored=net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement.CODEC.parse(ops,encoded).getOrThrow();
        for(var rotation:net.minecraft.world.level.block.Rotation.values()){
            var box=pool.getBoundingBox(level.getServer().getStructureManager(),origin,rotation);
            var portal=new BlockPos(40,0,-com.pipican.worldgen.ExpeditionPoolElement.ACCESS_LENGTH).rotate(rotation).offset(origin);
            helper.assertTrue(box.isInside(portal),"native decoration bounds omitted rotated access portal: "+rotation);
            helper.assertTrue(box.equals(restored.getBoundingBox(level.getServer().getStructureManager(),origin,rotation)),
                "serialized native access bounds changed on reload: "+rotation);
        }
        helper.succeed();
    }
}
