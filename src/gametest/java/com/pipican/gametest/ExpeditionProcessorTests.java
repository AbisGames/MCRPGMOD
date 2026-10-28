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
        var processor=StructureProcessorType.SINGLE_CODEC.parse(JsonOps.INSTANCE,
            JsonParser.parseString("{\"processor_type\":\"pipican:expedition_access\"}")).getOrThrow();
        var origin=helper.absolutePos(new BlockPos(-40,25,0));
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
            try{
                level.setBlock(blocked,obstruction,2);
                helper.assertTrue(processor.finalizeProcessing(level,shifted,shifted,List.of(local),shiftedInput,clip).equals(shiftedInput),
                    "access processor replaced an inventory or artificial block: "+obstruction);
                helper.assertTrue(level.getBlockState(blocked).equals(obstruction),"preflight mutated the world");
            }finally{level.setBlock(blocked,previous,2);}
        }
        helper.succeed();
    }
}
