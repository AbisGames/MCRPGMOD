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
        "capital_protection", WorldgenTests::capitalProtection,
        "renewable_mine", WorldgenTests::renewableMine,
        "structure_nbt_load", WorldgenTests::structureNbtLoad,
        "registries_loaded", WorldgenTests::registriesLoaded,
        "nature_features_loaded", WorldgenTests::natureFeaturesLoaded
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

    private static void capitalProtection(GameTestHelper helper) {
        helper.assertTrue(CapitalStructureExclusion.all().size()==22,"capital zone count");
        for(var zone:CapitalStructureExclusion.all()) {
            helper.assertTrue(CapitalStructureExclusion.contains(zone.x(),zone.z(),0),
                "capital centre not reserved: "+zone.id());
            helper.assertTrue(!StructurePlanner.canPlaceFootprint(
                zone.x()-10,zone.z()-10,zone.x()+10,zone.z()+10,0),
                "candidate overlaps capital: "+zone.id());
            helper.assertTrue(StructurePlanner.canPlaceFootprint(
                zone.x()+zone.radius()+500,zone.z()+zone.radius()+500,
                zone.x()+zone.radius()+510,zone.z()+zone.radius()+510,0),
                "distant candidate rejected: "+zone.id());
        }
        helper.succeed();
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


    /**
     * Validates that the datapack entries needed to start an RPG save are
     * fully decoded and registered in a real running NeoForge server.
     * This is stronger than JSON syntax checks but not an FPS benchmark.
     */
    private static void registriesLoaded(GameTestHelper helper){
        var access=helper.getLevel().registryAccess();
        var biomeRegistry=access.lookupOrThrow(Registries.BIOME);
        var presetRegistry=access.lookupOrThrow(Registries.WORLD_PRESET);
        var noiseRegistry=access.lookupOrThrow(Registries.NOISE_SETTINGS);
        var dimensionRegistry=access.lookupOrThrow(Registries.DIMENSION_TYPE);
        var structureRegistry=access.lookupOrThrow(Registries.STRUCTURE);
        var poolRegistry=access.lookupOrThrow(Registries.TEMPLATE_POOL);

        helper.assertTrue(presetRegistry.get(ResourceKey.create(Registries.WORLD_PRESET,
            Pipican.id("rpg_world"))).isPresent(),"missing RPG world preset");
        helper.assertTrue(noiseRegistry.get(ResourceKey.create(Registries.NOISE_SETTINGS,
            Pipican.id("rpg_overworld"))).isPresent(),"missing 576-high noise settings");
        helper.assertTrue(dimensionRegistry.get(ResourceKey.create(Registries.DIMENSION_TYPE,
            Pipican.id("rpg_overworld"))).isPresent(),"missing 576-high dimension");

        int sampled=0;
        var regionMap=WorldgenJson.read("region_map.json");
        for(var region:regionMap.getAsJsonArray("macro_regions")){
            var r=region.getAsJsonObject();
            String parent=r.get("id").getAsString();
            for(var part:r.getAsJsonArray("subregions")){
                String id=parent+"/"+part.getAsJsonObject().get("id").getAsString();
                helper.assertTrue(biomeRegistry.get(ResourceKey.create(Registries.BIOME,
                    Pipican.id(id))).isPresent(),"missing biome "+id);
                sampled++;
            }
        }
        helper.assertTrue(sampled==120,"unexpected authored subregion count");
        for(var extra:WorldLayout.EXTRA_BIOMES){
            helper.assertTrue(biomeRegistry.get(ResourceKey.create(Registries.BIOME,
                Pipican.id(extra.id()))).isPresent(),"missing fantasy biome "+extra.id());
        }

        int structures=0;
        for(var ore:RegionalOreCatalog.ORES){
            String id=ore.region();
            for(String kind:List.of("dungeon","mine")){
                String path="regional/"+id+"/"+kind;
                helper.assertTrue(structureRegistry.get(ResourceKey.create(Registries.STRUCTURE,
                    Pipican.id(path))).isPresent(),"missing RPG structure "+path);
                structures++;
            }
        }
        helper.assertTrue(structures==54,"expected 54 regional structures");

        var manifest=WorldgenJson.read("structure_pools_manifest.json");
        int pools=0;
        for(var entry:manifest.getAsJsonArray("pools")){
            String pool=entry.getAsJsonObject().get("pool").getAsString();
            helper.assertTrue(poolRegistry.get(ResourceKey.create(Registries.TEMPLATE_POOL,
                Pipican.id(pool))).isPresent(),"missing structure pool "+pool);
            pools++;
        }
        helper.assertTrue(pools==331,"expected 331 original structure pools");
        helper.succeed();
    }

    /**
     * The server must decode all 27 tree, flora and geological definitions.
     * These checks use live datapack registries rather than JSON file paths.
     */
    private static void natureFeaturesLoaded(GameTestHelper helper) {
        var access=helper.getLevel().registryAccess();
        var configured=access.lookupOrThrow(Registries.CONFIGURED_FEATURE);
        var placed=access.lookupOrThrow(Registries.PLACED_FEATURE);
        var themes=WorldgenJson.read("biome_theme_catalog.json");
        int count=0;
        for(var element:themes.getAsJsonArray("biomes")) {
            String id=element.getAsJsonObject().get("id").getAsString();
            helper.assertTrue(RegionalNatureBlocks.PALETTES.containsKey(id),
                "no decorative block palette for "+id);
            for(String type:List.of("tree","bloom","outcrop")) {
                var key=Pipican.id(id+"_"+type);
                helper.assertTrue(configured.get(ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,key)).isPresent(),"no feature "+key);
                helper.assertTrue(placed.get(ResourceKey.create(
                    Registries.PLACED_FEATURE,key)).isPresent(),"no placement "+key);
                count++;
            }
        }
        helper.assertTrue(count==81,"expected 81 placed biome feature types");
        helper.assertTrue(RegionalNatureBlocks.PALETTES.size()==27,
            "expected 27 biome decor palettes");
        // Each authored subregion has its own flora placement, rather than
        // sharing one blanket density with the parent realm.
        var map=WorldgenJson.read("region_map.json");
        int checked=0;
        for(var element:map.getAsJsonArray("macro_regions")){
            var macro=element.getAsJsonObject();
            String prefix=macro.get("id").getAsString();
            for(var child:macro.getAsJsonArray("subregions")){
                String id=child.getAsJsonObject().get("id").getAsString();
                var key=Pipican.id("subregion/"+prefix+"/"+id+"_bloom");
                helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE,key))
                    .isPresent(),"missing subregion undergrowth "+key);
                checked++;
            }
        }
        helper.assertTrue(checked==120,"expected 120 distinct undergrowth placements");
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
