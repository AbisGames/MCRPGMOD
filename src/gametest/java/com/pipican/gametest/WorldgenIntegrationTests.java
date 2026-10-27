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
        for(long seed:new long[]{0L,123L,987654321L}){
            generationSeed=seed;
            verifyGenerated(helper,createLevel(helper,seed));
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
                level=new ServerLevel(server,java.util.concurrent.ForkJoinPool.commonPool(),storage,
                    new net.minecraft.world.level.storage.DerivedLevelData(server.getWorldData(),
                        server.getWorldData().overworldData()),RPG,stem,false,seed,List.of(),false);
                server.forgeGetWorldMap().put(RPG,level);
                server.markWorldsDirty();
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        return level;
    }

    private static void verifyGenerated(GameTestHelper helper,ServerLevel level) {
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
        var progress=new CapitalPlacementSavedData();
        for(int i=0;i<63;i++){
            helper.assertTrue(CapitalPlacementEvents.buildOneTile(level,city,progress),
                "seed="+generationSeed+" capital="+city.id()+" tile="+i+" placement deferred");
            if(i==10){
                var saved=CapitalPlacementSavedData.CODEC.encodeStart(
                    com.mojang.serialization.JsonOps.INSTANCE,progress).getOrThrow();
                progress=CapitalPlacementSavedData.CODEC.parse(
                    com.mojang.serialization.JsonOps.INSTANCE,saved).getOrThrow();
            }
        }
        helper.assertTrue(progress.progress(city.id()).nextTile()==63,"capital not completed");
        helper.assertTrue(!CapitalPlacementEvents.buildOneTile(level,city,progress),"completed capital rebuilt");
    }
}
