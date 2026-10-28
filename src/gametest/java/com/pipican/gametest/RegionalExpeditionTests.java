package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.worldgen.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Controlled native-start fixtures: validate every themed model through real
 * chunk decoration. Start selection is forced for coverage, so these checks do
 * not claim to measure the natural structure-set frequency.
 */
final class RegionalExpeditionTests {
    static void verify(GameTestHelper helper,ServerLevel initial,long seed,
            Supplier<ServerLevel> reopen,Consumer<ResourceKey<Level>> close){
        ServerLevel level=initial;
        var failures=new ArrayList<String>();
        int shard=seed==123L?1:seed==987654321L?2:0;
        for(int oreIndex=shard;oreIndex<RegionalOreCatalog.ORES.size();oreIndex+=3){
        var ore=RegionalOreCatalog.ORES.get(oreIndex);
        for(String kind:List.of("mine","dungeon")){
            String id="regional/"+ore.region()+"/"+kind;
            try{
                var selected=level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
                    .get(ResourceKey.create(Registries.STRUCTURE,Pipican.id(id))).orElseThrow();
                var definition=WorldgenJson.read("structure/"+id+".json");
                int fixedY=definition.getAsJsonObject("start_height").get("absolute").getAsInt();
                String parent=WorldLayout.EXTRA_BIOMES.stream().filter(e->e.id().equals(ore.region()))
                    .map(WorldLayout.ExtraBiome::parent).findFirst().orElse(ore.region());
                var region=WorldLayout.REGIONS.stream().filter(r->r.id().equals(parent)).findFirst().orElseThrow();
                StructureStart start=null;
                search:
                for(int x=region.minX()+512;x<region.maxX()-512;x+=512)
                    for(int z=region.minZ()+512;z<region.maxZ()-512;z+=512){
                        if(WorldStructureReservations.intersects(x-112,z-112,x+112,z+112,100))continue;
                        int y=definition.has("project_start_to_heightmap")?100:fixedY;
                        var biome=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(
                            x>>2,y>>2,z>>2,level.getChunkSource().randomState().sampler());
                        if(!selected.value().biomes().contains(biome))continue;
                        var pos=new ChunkPos(x>>4,z>>4);
                        var candidate=selected.value().generate(selected,level.dimension(),level.registryAccess(),
                            level.getChunkSource().getGenerator(),level.getChunkSource().getGenerator().getBiomeSource(),
                            level.getChunkSource().randomState(),level.getServer().getStructureManager(),seed,pos,0,
                            level,selected.value().biomes()::contains);
                        if(!candidate.isValid())continue;
                        var box=candidate.getBoundingBox();
                        if(WorldStructureReservations.intersects(box.minX(),box.minZ(),box.maxX(),box.maxZ(),100))continue;
                        var chunk=level.getChunk(pos.x(),pos.z(),ChunkStatus.STRUCTURE_STARTS);
                        if(chunk.getPersistedStatus().isOrAfter(ChunkStatus.FEATURES)
                                ||chunk.getAllStarts().values().stream().anyMatch(StructureStart::isValid))continue;
                        level.structureManager().setStartForStructure(SectionPos.bottomOf(chunk),selected.value(),candidate,chunk);
                        start=candidate;
                        break search;
                    }
                helper.assertTrue(start!=null,"seed="+seed+" no valid themed native start for "+id);
                var box=start.getBoundingBox();
                for(int cx=Math.floorDiv(box.minX(),16);cx<=Math.floorDiv(box.maxX(),16);cx++)
                    for(int cz=Math.floorDiv(box.minZ(),16);cz<=Math.floorDiv(box.maxZ(),16);cz++)level.getChunk(cx,cz);
                BlockPos node=null;
                int spawners=0,chests=0;
                var regionalBlocks=new HashMap<net.minecraft.world.level.block.Block,String>();
                RegionalOreRegistries.NODES.forEach((name,block)->regionalBlocks.put(block.get(),name));
                for(int x=box.minX();x<=box.maxX();x++)for(int z=box.minZ();z<=box.maxZ();z++)
                    for(int y=box.minY();y<=box.maxY();y++){
                        var pos=new BlockPos(x,y,z);
                        var state=level.getBlockState(pos);
                        String found=regionalBlocks.get(state.getBlock());
                        if(found!=null){
                            helper.assertTrue(found.equals(ore.id()),"foreign regional ore in "+id+" at "+pos+": "+found);
                            if(node==null)node=pos;
                        }
                        if(state.is(net.minecraft.world.level.block.Blocks.TRIAL_SPAWNER))spawners++;
                        if(state.is(net.minecraft.world.level.block.Blocks.CHEST))chests++;
                    }
                if(kind.equals("mine")){
                    helper.assertTrue(node!=null,"native mine decoration produced no ore: "+id);
                    var data=RenewableOreSavedData.get(level);
                    var player=helper.makeMockServerPlayerInLevel();
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                        new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,node,level.getBlockState(node),player));
                    helper.assertTrue(data.contains(node),"natural mine ore lacked renewable structure provenance: "+id);
                    var queued=data.due(Long.MAX_VALUE,1).getFirst();
                    level.setBlock(node,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    helper.assertTrue(RegionalMineEvents.restoreDue(level,queued.restoreAt()-1,8)==0,"premature ore renewal: "+id);
                    helper.assertTrue(RegionalMineEvents.restoreDue(level,queued.restoreAt(),8)==1,"ore did not renew: "+id);
                    level.setBlock(node,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
                    var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,node);
                    level.setBlock(node,RegionalOreRegistries.NODES.get(ore.id()).get().defaultBlockState(),3);
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                        new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(snapshot,
                            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),player));
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                        new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level,node,level.getBlockState(node),player));
                    helper.assertTrue(!data.contains(node),"player-placed ore exploited a real native mine: "+id);
                }else{
                    helper.assertTrue(spawners==1&&chests==3,"native dungeon blocks missing: "+id+
                        " spawners="+spawners+" chests="+chests);
                }
                Pipican.LOGGER.info("Verified native chunk decoration seed {} structure {} bounds {}",seed,id,box);
            }catch(RuntimeException error){
                failures.add(id+": "+error.getMessage());
                Pipican.LOGGER.error("Native expedition integration failure seed {} structure {}",seed,id,error);
            }finally{
                close.accept(level.dimension());
                level.getServer().getStructureManager().remove(Pipican.id(id));
                level=reopen.get();
            }
        }
        }
        helper.assertTrue(failures.isEmpty(),"seed="+seed+" expedition failures: "+failures);
    }
}
