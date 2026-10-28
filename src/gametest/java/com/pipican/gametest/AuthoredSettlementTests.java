package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.worldgen.*;
import java.util.ArrayList;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** The other 48 authored sites exercise the same near-player placement path. */
final class AuthoredSettlementTests {
    static void verify(GameTestHelper helper,ServerLevel initial,long seed,
            Supplier<ServerLevel> reopen,Consumer<ResourceKey<Level>> close){
        var sites=new LandmarkCatalog().all().stream().filter(s->!s.tier().equals("capital")).toList();
        helper.assertTrue(sites.size()==48,"expected 48 authored non-capital landmarks");
        var failures=new ArrayList<String>();
        ServerLevel level=initial;
        int shard=seed==123L?1:seed==987654321L?2:0;
        for(int index=shard;index<sites.size();index+=3){
            var site=sites.get(index);
            WorldgenTestProgress.touch("authored settlement "+site.id());
            try{
                var template=level.getServer().getStructureManager().get(Pipican.id("authored/"+site.assetPool())).orElseThrow();
                var size=template.getSize();
                var progress=CapitalPlacementSavedData.get(level);
                var preparation=CapitalChunkLoader.prepare(level,site,progress);
                try{
                    helper.assertTrue(progress.progress(site.id())==null,"new fixture already placed: "+site.id());
                    helper.assertTrue(CapitalPlacementEvents.buildOneTile(level,site,progress),
                        "authored settlement placement deferred: "+site.id());
                }finally{preparation.release(level);}
                var placed=progress.progress(site.id());
                helper.assertTrue(placed!=null&&placed.nextTile()==1,"missing settlement completion: "+site.id());
                var origin=new BlockPos(site.x()-size.getX()/2,placed.foundationY(),site.z()-size.getZ()/2);
                var chests=template.filterBlocks(origin,new StructurePlaceSettings(),Blocks.CHEST);
                helper.assertTrue(!chests.isEmpty(),"authored settlement lacks furnished storage: "+site.id());
                for(var chest:chests)helper.assertTrue(level.getBlockState(chest.pos()).is(Blocks.CHEST),
                    "missing placed storage: "+site.id()+" at "+chest.pos());
                for(int x:new int[]{0,size.getX()/2,size.getX()-1})
                    for(int z:new int[]{0,size.getZ()/2,size.getZ()-1}){
                        var support=origin.offset(x,-1,z);
                        helper.assertTrue(level.getBlockState(support).isSolid(),"unsupported authored site "+site.id()+" at "+support);
                    }
                close.accept(level.dimension());
                level=reopen.get();
                var reloaded=CapitalPlacementSavedData.get(level);
                helper.assertTrue(reloaded.progress(site.id())!=null&&reloaded.progress(site.id()).nextTile()==1,
                    "authored settlement progress lost after disk reload: "+site.id());
                helper.assertTrue(!CapitalPlacementEvents.buildOneTile(level,site,reloaded),
                    "completed authored settlement rebuilt: "+site.id());
                for(var chest:chests)helper.assertTrue(level.getBlockState(chest.pos()).is(Blocks.CHEST),
                    "placed storage lost after disk reload: "+site.id()+" at "+chest.pos());
                Pipican.LOGGER.info("Verified authored settlement seed {} dimension {} site {} origin {} size {}",
                    seed,level.dimension().identifier(),site.id(),origin,size);
            }catch(RuntimeException error){
                failures.add(site.id()+" at "+site.x()+","+site.z()+": "+error.getMessage());
                Pipican.LOGGER.error("Authored settlement integration failure seed {} dimension {} site {} at {},{}",
                    seed,level.dimension().identifier(),site.id(),site.x(),site.z(),error);
            }finally{
                close.accept(level.dimension());
                level=reopen.get();
            }
        }
        helper.assertTrue(failures.isEmpty(),"seed="+seed+" authored settlement failures: "+failures);
    }
}
