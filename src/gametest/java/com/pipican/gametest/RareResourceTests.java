package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.resource.RareResources;
import com.pipican.worldgen.RegionalOreCatalog;
import com.pipican.worldgen.RegionalOreRegistries;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Rare deposits and regional ores now come from the tiered ore catalog: the deposits spawn by tier in their regions
 * and are mined with a pickaxe of the right tier; regional mines hold only T1 ores.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class RareResourceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
            "rare_resource_catalog", RareResourceTests::catalog,
            "regional_mines_t1", RareResourceTests::mines);

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, fn) -> registry.register(Pipican.id(name), fn)));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Pipican.id("rare_resource_test"), new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name -> event.registerTest(Pipican.id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, Pipican.id(name)),
                new TestData<>(env, net.minecraft.resources.Identifier.withDefaultNamespace("empty"), 200, 0, true))));
    }

    private static void catalog(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(RareResources.ORES.size() == 8 && RareResources.MATERIALS.size() == 12, "8 deposits and 4 boss materials");
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        for (String deposit : RareResources.DEPOSITS) {
            Block ore = RareResources.ORES.get(deposit).get();
            var state = ore.defaultBlockState();
            helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, Pipican.id("ore_" + deposit))).isPresent(), "deposit spawns: " + deposit);
            // Mined by tier: a netherite pickaxe takes T4-T5 deposits; T6+ (astralite, primordial) need this mod's tools.
            boolean vanilla = new ItemStack(Items.NETHERITE_PICKAXE).isCorrectToolForDrops(state);
            boolean high = deposit.equals("astralite") || deposit.equals("primordial");
            helper.assertTrue(vanilla != high, "mining tier of " + deposit);
            helper.assertTrue(new ItemStack(BuiltInRegistries.ITEM.getValue(Pipican.id("adamantine_pickaxe"))).isCorrectToolForDrops(state), "adamantine mines " + deposit);
            var drops = Block.getDrops(state, level, helper.absolutePos(new BlockPos(1, 1, 1)), null);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(RareResources.MATERIALS.get(deposit).get()), "drops its material: " + deposit);
        }
        helper.succeed();
    }

    private static void mines(GameTestHelper helper) {
        helper.assertTrue(RegionalOreCatalog.ORES.size() == 27 && RegionalOreRegistries.NODES.size() == 27, "27 regions");
        for (var ore : RegionalOreCatalog.ORES) {
            var node = RegionalOreRegistries.NODES.get(ore.id()).get();
            var key = BuiltInRegistries.BLOCK.getKey(node).getPath();
            helper.assertTrue(com.pipican.ore.PipicanOres.of(key).map(o -> o.tier() == 1).orElse(false), "mine of " + ore.region() + " holds a T1 ore, not " + key);
            helper.assertTrue(RegionalOreRegistries.MATERIALS.get(ore.id()) != null, "regional material " + ore.id());
        }
        helper.succeed();
    }
}
