package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.worldgen.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Dedicated-server tests for the registered worldgen catalogs and renewable sites. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class WorldgenTests {
    private static final Map<String,Consumer<GameTestHelper>> TESTS=Map.of(
        "worldgen_catalog", WorldgenTests::worldgenCatalog,
        "renewable_mine", WorldgenTests::renewableMine,
        "structure_nbt_load", WorldgenTests::structureNbtLoad
    );
    private WorldgenTests(){}

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,
            helper->TESTS.forEach((name,fn)->helper.register(Pipican.id(name),fn)));
    }
    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env=event.registerEnvironment(
            Pipican.id("worldgen_test"),new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name->{
            ResourceKey<Consumer<GameTestHelper>> fn=ResourceKey.create(
                Registries.TEST_FUNCTION,Pipican.id(name));
            event.registerTest(Pipican.id(name),new FunctionGameTestInstance(
                fn,new TestData<>(env,net.minecraft.resources.Identifier.withDefaultNamespace("empty"),100,0,true)));
        });
    }

    private static void worldgenCatalog(GameTestHelper helper) {
        helper.assertTrue(WorldLayout.REGIONS.size()==22,"expected 22 macroregions");
        SubregionSampler sampler=new SubregionSampler();
        helper.assertTrue(sampler.subregionIds().size()==120,"expected 120 subregions");
        helper.assertTrue(new LandmarkCatalog().all().size()==70,"expected 70 landmarks");
        helper.assertTrue(RegionalOreCatalog.ORES.size()==27,"expected 27 ores");

        var biomes=helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        for(WorldLayout.Region region:WorldLayout.REGIONS){
            Optional<SubregionSampler.Subregion> sampled=sampler.at(12345L,
                (region.minX()+region.maxX())/2,(region.minZ()+region.maxZ())/2);
            helper.assertTrue(sampled.isPresent(),"missing sampled biome for "+region.id());
        }
        helper.assertTrue(RegionalOreRegistries.NODES.size()==27,"missing registered ore nodes");
        helper.assertTrue(RegionalOreRegistries.MATERIALS.size()==27,"missing registered ore items");
        helper.assertTrue(RegionalOreRegistries.BOSS_CORES.size()==27,"missing regional boss cores");
        helper.succeed();
    }

    /** Loads real binary NBT through Minecraft's server template manager. */
    private static void structureNbtLoad(GameTestHelper helper) {
        String[] ids={
            "authored/landmarks/elderwood/sylvaran",
            "authored/landmarks/greyspine_mountains/durnhold",
            "regional/ashlands/dungeon",
            "regional/ashlands/mine"
        };
        int[] widths={112,112,80,80};
        for(int i=0;i<ids.length;i++){
            var template=helper.getLevel().getStructureManager().get(Pipican.id(ids[i]));
            helper.assertTrue(template.isPresent(),"missing NBT "+ids[i]);
            if(template.isPresent()) {
                helper.assertTrue(template.get().getSize().getX()==widths[i],
                    "unexpected structure width for "+ids[i]);
            }
        }
        helper.succeed();
    }

    private static void renewableMine(GameTestHelper helper) {
        RenewableSite site=new RenewableSite("test_mine",RenewableSite.Kind.MINE,1000L,0L);
        helper.assertTrue(site.ready(0),"site ready initially");
        helper.assertTrue(site.consume(0),"consume once");
        helper.assertTrue(!site.consume(1),"cannot harvest during cooldown");
        helper.assertTrue(!site.ready(999),"not yet renewed");
        site.setOccupied(true);
        helper.assertTrue(!site.ready(1000),"occupied site blocked");
        site.setOccupied(false);
        helper.assertTrue(site.ready(1000),"renewed site ready");
        helper.succeed();
    }
}
