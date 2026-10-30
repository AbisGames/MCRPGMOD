package com.pipican.gametest;

import com.pipican.resource.*;
import com.pipican.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;

/** Observes naturally decorated chunks; no manual ore or feature placement in this test. */
final class RareGenerationTests {
    private RareGenerationTests(){}
    static void verify(GameTestHelper helper){
        var rpg=WorldgenIntegrationTests.openResourceFixture(helper);
        try {
            for(var deposit:RareResources.DEPOSITS) if(!deposit.region().equals("end")) scan(helper,rpg,deposit);
            var unrelated=rpg.getChunk(-5500>>4,-5500>>4);
            var cursor=new BlockPos.MutableBlockPos();
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=rpg.getMinY();y<rpg.getMaxY();y++){
                cursor.set(unrelated.getPos().getMinBlockX()+x,y,unrelated.getPos().getMinBlockZ()+z);
                for(var ore:RareResources.ORES.values()) helper.assertTrue(!unrelated.getBlockState(cursor).is(ore.get()),"rare deposit leaked into Heartlands");
            }
        }finally{ WorldgenIntegrationTests.closeResourceFixture(helper,rpg); }
        var end=helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end!=null,"End test dimension unavailable"); PlayerWorldgenProtection.get(end);
        for(var deposit:RareResources.DEPOSITS) if(deposit.region().equals("end")) scan(helper,end,deposit);
    }
    private static BlockPos anchor(GameTestHelper helper,ServerLevel level,RareResources.Deposit deposit){
        if(deposit.region().equals("end")) return new BlockPos(1200,40,1200);
        String parent=deposit.region().equals("umbral_chasm") || deposit.region().equals("aetheris_expanse") ? "greyspine_mountains"
            : deposit.region().equals("deep_ocean") ? "western_ocean" : deposit.region();
        var region=WorldLayout.REGIONS.stream().filter(r->r.id().equals(parent)).findFirst().orElseThrow();
        int y=(deposit.minY()+deposit.maxY())/2;
        for(int x=region.minX()+512;x<region.maxX()-512;x+=128) for(int z=region.minZ()+512;z<region.maxZ()-512;z+=128){
            var pos=new BlockPos(x+8,y,z+8);
            if(matches(level,deposit,pos) && !WorldStructureReservations.intersects(x,z,x,z,8)) return pos;
        }
        throw new IllegalStateException("No native biome anchor for "+deposit.id());
    }
    private static boolean matches(ServerLevel level,RareResources.Deposit deposit,BlockPos pos){
        var biome=level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()),QuartPos.fromBlock(pos.getY()),QuartPos.fromBlock(pos.getZ()),level.getChunkSource().randomState().sampler());
        if(deposit.region().equals("end") && !biome.is(Biomes.END_HIGHLANDS) && !biome.is(Biomes.END_MIDLANDS)) return false;
        return deposit.matchesBiome(biome);
    }
    private static void scan(GameTestHelper helper,ServerLevel level,RareResources.Deposit deposit){
        var anchor=anchor(helper,level,deposit); int generated=0,found=0;
        int limit=deposit.id().equals("primordial") ? 192 : 64;
        outer: for(int dx=0;dx<32;dx++) for(int dz=0;dz<32;dz++){
            var sample=anchor.offset(dx*16,0,dz*16);
            if(!matches(level,deposit,sample)) continue;
            WorldgenTestProgress.touch("rare "+deposit.id()+" native chunk "+sample+" sample "+generated);
            var chunk=level.getChunk(sample.getX()>>4,sample.getZ()>>4); generated++;
            var cursor=new BlockPos.MutableBlockPos(); int count=0;
            for(int x=0;x<16;x++) for(int z=0;z<16;z++) for(int y=deposit.minY();y<=Math.min(deposit.maxY(),level.getMaxY()-1);y++){
                cursor.set(chunk.getPos().getMinBlockX()+x,y,chunk.getPos().getMinBlockZ()+z);
                if(chunk.getBlockState(cursor).is(RareResources.ORES.get(deposit.id()).get())){
                    helper.assertTrue(deposit.allows(level,cursor),"ore generated outside its actual biome/Y/dimension: "+deposit.id()+" "+cursor);
                    count++; found++;
                }
            }
            helper.assertTrue(count<=deposit.size(),"rare vein exceeded configured chunk-local size");
            if(found>0 || generated>=limit) break outer;
        }
        helper.assertTrue(found>0,"native decoration produced no "+deposit.id()+" in "+generated+" eligible chunks");
        com.pipican.Pipican.LOGGER.info("RARE VERIFIED id={} blocks={} eligibleChunks={} dimension={}",deposit.id(),found,generated,level.dimension().identifier());
    }
}
