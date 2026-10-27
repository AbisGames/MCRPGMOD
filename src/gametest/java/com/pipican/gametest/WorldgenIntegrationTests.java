package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.worldgen.*;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Integration checks use the actual RPG noise generator in a test-only dimension. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class WorldgenIntegrationTests {
    private static long generationSeed;
    private static final java.util.Map<ResourceKey<Level>,net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess>
        STORAGE=new java.util.HashMap<>();

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registrar ->
            registrar.register(Pipican.id("rpg_generated_chunks"), WorldgenIntegrationTests::generatedChunks));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Pipican.id("rpg_integration_env"),
            new TestEnvironmentDefinition.AllOf(List.of()));
        ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(
            Registries.TEST_FUNCTION, Pipican.id("rpg_generated_chunks"));
        event.registerTest(Pipican.id("rpg_generated_chunks"), new FunctionGameTestInstance(fn,
            new TestData<>(env, net.minecraft.resources.Identifier.withDefaultNamespace("empty"), 2400, 0, true)));
    }

    private static void check(GameTestHelper helper, ServerLevel level, BlockPos pos, boolean ok, String expected) {
        helper.assertTrue(ok, "seed=" + generationSeed + " dimension=" + level.dimension().identifier()
            + " pos=" + pos + " expected=" + expected + " actual=" + level.getBlockState(pos));
    }

    private static void generatedChunks(GameTestHelper helper) {
        var server=helper.getLevel().getServer();
        var original=server.getWorldGenSettings();
        // GameTestServer hardcodes WORLD_OPTIONS=(0,false,false). Configure
        // its private final instance field only in this test harness, restoring
        // it before any other test batch runs. Passing ServerLevel's zoom seed
        // alone does not change noise or enable native structure generation.
        java.lang.reflect.Field options;
        try{
            options=net.minecraft.server.MinecraftServer.class.getDeclaredField("worldGenSettings");
            options.setAccessible(true);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("GameTest world options unavailable",e);}
        for(long seed:new long[]{0L,123L,987654321L}){
            generationSeed=seed;
            ServerLevel level=null;
            try{
                options.set(server,new net.minecraft.world.level.levelgen.WorldGenSettings(
                    new net.minecraft.world.level.levelgen.WorldOptions(seed,true,false),original.dimensions()));
                level=createLevel(helper,seed);
                verifyGenerated(helper,level);
            }catch(IllegalAccessException e){throw new IllegalStateException("Cannot set independent test seed",e);}
            finally{
                try{if(level!=null)saveAndClose(helper,level.dimension());}
                finally{
                    try{options.set(server,original);}
                    catch(IllegalAccessException e){throw new IllegalStateException("Cannot restore GameTest world options",e);}
                }
            }
        }
        helper.succeed();
    }

    private static ServerLevel createLevel(GameTestHelper helper,long seed) {
        var RPG=ResourceKey.create(Registries.DIMENSION,Pipican.id("rpg_integration_"+seed));
        ServerLevel level = helper.getLevel().getServer().getLevel(RPG);
        // GameTestServer intentionally creates only vanilla dimensions. Build
        // a real ServerLevel from the RPG preset instead of falling back to its
        // flat overworld (which would make terrain assertions meaningless).
        if(level==null){
            var server=helper.getLevel().getServer();
            var json=WorldgenJson.read("world_preset/rpg_world.json")
                .getAsJsonObject("dimensions").get("minecraft:overworld");
            var ops=net.minecraft.resources.RegistryOps.create(
                com.mojang.serialization.JsonOps.INSTANCE,server.registryAccess());
            var stem=net.minecraft.world.level.dimension.LevelStem.CODEC.parse(ops,json).getOrThrow();
            try{
                var source=net.minecraft.world.level.storage.LevelStorageSource.createDefault(
                    server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("rpg-integration"));
                var storage=source.validateAndCreateAccess("seed_"+seed);
                STORAGE.put(RPG,storage);
                level=new ServerLevel(server,java.util.concurrent.ForkJoinPool.commonPool(),storage,
                    new net.minecraft.world.level.storage.DerivedLevelData(server.getWorldData(),
                        server.getWorldData().overworldData()),RPG,stem,false,
                    net.minecraft.world.level.biome.BiomeManager.obfuscateSeed(seed),List.of(),false);
                server.forgeGetWorldMap().put(RPG,level);
                server.markWorldsDirty();
            }catch(Exception e){throw new IllegalStateException("Unable to open temporary RPG test world for seed "+seed,e);}
        }
        return level;
    }

    private static void saveAndClose(GameTestHelper helper,ResourceKey<Level> key){
        var server=helper.getLevel().getServer();
        var level=server.getLevel(key);
        if(level==null)return;
        try{
            level.save(null,true,false);
            server.forgeGetWorldMap().remove(key);
            server.markWorldsDirty();
            level.close();
            var storage=STORAGE.remove(key);
            if(storage!=null)storage.close();
        }catch(Exception e){throw new IllegalStateException("Saving temporary RPG integration world "+key,e);}
    }

    private static void verifyGenerated(GameTestHelper helper,ServerLevel level) {
        helper.assertTrue(level.getSeed()==generationSeed,"test world did not receive its independent seed");
        helper.assertTrue(level.structureManager().shouldGenerateStructures(),"native structures disabled in test world");
        helper.assertTrue(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource,
            "integration test must use the real RPG generator");
        for (var island : SkyIslandLayout.ISLANDS) {
            // Centre plus four corners within the authored structure support area.
            for (int[] offset : new int[][] {{0,0},{-30,-30},{-30,30},{30,-30},{30,30}}) {
                var floor = new BlockPos(island.x()+offset[0], 318, island.z()+offset[1]);
                level.getChunk(floor.getX() >> 4, floor.getZ() >> 4);
                check(helper,level,floor,level.getBlockState(floor).isSolid(), "solid island support " + island.id());
                var clearance = floor.above(4);
                check(helper,level,clearance,level.getBlockState(clearance).isAir(), "clear building space " + island.id());
            }
        }
        for(var site:SpecialLandmarkPlacementEvents.sites()){
            if(!site.id().startsWith("aetheris_"))continue;
            var template=level.getServer().getStructureManager().get(
                Pipican.id(site.template())).orElseThrow();
            var size=template.getSize();
            int minX=site.x()-size.getX()/2,minZ=site.z()-size.getZ()/2;
            for(int cx=Math.floorDiv(minX,16);cx<=Math.floorDiv(minX+size.getX()-1,16);cx++)
                for(int cz=Math.floorDiv(minZ,16);cz<=Math.floorDiv(minZ+size.getZ()-1,16);cz++)
                    level.getChunk(cx,cz);
            helper.assertTrue(SpecialLandmarkPlacementEvents.tryPlace(level,site),
                "seed="+generationSeed+" failed sky structure placement: "+site.id());
            if(site.id().equals("aetheris_citadel")){
                int frames=0;
                for(int x=54;x<=58;x++)for(int z=54;z<=58;z++){
                    var pos=new BlockPos(minX+x,site.elevation()+45,minZ+z);
                    var block=level.getBlockState(pos);
                    if(block.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)){
                        frames++;
                        var expected=x==54?net.minecraft.core.Direction.EAST:
                            x==58?net.minecraft.core.Direction.WEST:
                            z==54?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH;
                        check(helper,level,pos,block.getValue(net.minecraft.world.level.block.EndPortalFrameBlock.FACING)==expected,
                            "inward-facing End portal frame");
                    }
                }
                helper.assertTrue(frames==12,"Sky Citadel must contain twelve End portal frames");
            }
        }
        // A physically generated capital core in every region, not a density calculation.
        for (var city : new LandmarkCatalog().all()) {
            if (!city.tier().equals("capital")) continue;
            level.getChunk(city.x() >> 4,city.z() >> 4);
            int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,
                city.x(),city.z()) - 1;
            var floor = new BlockPos(city.x(),y,city.z());
            check(helper,level,floor,y > level.getMinY() && level.getBlockState(floor).isSolid(),
                "solid capital terrain " + city.id());
        }
        Pipican.LOGGER.info("RPG generated chunk checks passed for seed {}: seven sky islands and 22 capital cores",
            generationSeed);
        var city=new LandmarkCatalog().all().stream()
            .filter(c->c.tier().equals("capital")&&c.region().equals("heartlands")).findFirst().orElseThrow();
        var progress=CapitalPlacementSavedData.get(level);
        for(int i=0;i<63;i++){
            helper.assertTrue(CapitalPlacementEvents.buildOneTile(level,city,progress),
                "seed="+generationSeed+" capital="+city.id()+" tile="+i+" placement deferred");
            if(i==10){
                var key=level.dimension();
                saveAndClose(helper,key);
                level=createLevel(helper,generationSeed);
                progress=CapitalPlacementSavedData.get(level);
                helper.assertTrue(progress.progress(city.id())!=null&&progress.progress(city.id()).nextTile()==11,
                    "partially built capital progress lost after actual disk save/reload");
            }
        }
        helper.assertTrue(progress.progress(city.id()).nextTile()==63,"capital not completed");
        helper.assertTrue(!CapitalPlacementEvents.buildOneTile(level,city,progress),"completed capital rebuilt");
    }
}
