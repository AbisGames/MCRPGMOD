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
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Integration checks use the actual RPG noise generator in a test-only dimension. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class WorldgenIntegrationTests {
    private static final ResourceKey<Level> RPG = ResourceKey.create(
        Registries.DIMENSION, Pipican.id("rpg_integration"));

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
        helper.assertTrue(ok, "seed=" + level.getSeed() + " dimension=" + RPG.identifier()
            + " pos=" + pos + " expected=" + expected + " actual=" + level.getBlockState(pos));
    }

    private static void generatedChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel().getServer().getLevel(RPG);
        helper.assertTrue(level != null, "test RPG dimension was not loaded");
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
            level.getSeed());
        helper.succeed();
    }
}
