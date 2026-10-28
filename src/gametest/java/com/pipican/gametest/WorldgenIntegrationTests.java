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
    private static String suite;
    private static final List<String> SUITES=List.of("sky","terrain","capital","protection","unique","expeditions");
    private static final java.util.Map<ResourceKey<Level>,net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess>
        STORAGE=new java.util.HashMap<>();
    private static final java.util.Map<ResourceKey<Level>,java.nio.file.Path> WORLD_ROOTS=new java.util.HashMap<>();

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registrar -> SUITES.forEach(name ->
            registrar.register(Pipican.id("rpg_generated_"+name), helper -> generatedChunks(helper,name))));
    }

    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Pipican.id("rpg_integration_env"),
            new TestEnvironmentDefinition.AllOf(List.of()));
        for(String name:SUITES){
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(
                Registries.TEST_FUNCTION, Pipican.id("rpg_generated_"+name));
            event.registerTest(Pipican.id("rpg_generated_"+name), new FunctionGameTestInstance(fn,
                new TestData<>(env, net.minecraft.resources.Identifier.withDefaultNamespace("empty"), 2400, 0, true)));
        }
    }

    private static void check(GameTestHelper helper, ServerLevel level, BlockPos pos, boolean ok, String expected) {
        helper.assertTrue(ok, "seed=" + generationSeed + " dimension=" + level.dimension().identifier()
            + " pos=" + pos + " expected=" + expected + " actual=" + level.getBlockState(pos));
    }

    private static void generatedChunks(GameTestHelper helper,String name) {
        suite=name;
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
        for(long seed:new long[]{Long.getLong("pipican.worldgenTestSeed",0L)}){
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
        var RPG=ResourceKey.create(Registries.DIMENSION,Pipican.id("rpg_integration_"+suite+"_"+seed));
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
                var root=WORLD_ROOTS.get(RPG);
                if(root==null){
                    var parent=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("rpg-integration");
                    java.nio.file.Files.createDirectories(parent);
                    root=java.nio.file.Files.createTempDirectory(parent,suite+"_seed_"+seed+"_");
                    WORLD_ROOTS.put(RPG,root);
                }
                var source=net.minecraft.world.level.storage.LevelStorageSource.createDefault(root);
                var storage=source.validateAndCreateAccess("world");
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
        if(suite.equals("expeditions"))RegionalExpeditionTests.verify(helper,level,generationSeed,
            ()->createLevel(helper,generationSeed),key->saveAndClose(helper,key));
        if(suite.equals("unique")){
            for(var site:SpecialLandmarkPlacementEvents.sites()){
                if(site.id().startsWith("aetheris_"))continue;
                var template=level.getServer().getStructureManager().get(Pipican.id(site.template())).orElseThrow();
                var size=template.getSize();
                int minX=site.x()-size.getX()/2,minZ=site.z()-size.getZ()/2;
                for(int cx=Math.floorDiv(minX,16);cx<=Math.floorDiv(minX+size.getX()-1,16);cx++)
                    for(int cz=Math.floorDiv(minZ,16);cz<=Math.floorDiv(minZ+size.getZ()-1,16);cz++)level.getChunk(cx,cz);
                helper.assertTrue(SpecialLandmarkPlacementEvents.tryPlace(level,site),
                    "seed="+generationSeed+" unique landmark placement failed: "+site.id());
                if(site.id().equals("umbral_city")){
                    helper.assertTrue(site.elevation()<0&&site.elevation()+size.getY()<64,
                        "Umbral city escaped its underground elevation");
                }
            }
        }
        if(suite.equals("protection")){
            var built=new BlockPos(-2500,400,-3333);
            var node=built.east(2);
            level.getChunk(built.getX()>>4,built.getZ()>>4);
            var snapshot=net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,built);
            level.setBlock(built,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                new net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent(snapshot,
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),helper.makeMockServerPlayerInLevel()));
            helper.assertTrue(PlayerWorldgenProtection.get(level).contains(built),"real place event did not protect natural stone");
            long ready=level.getGameTime()+20;
            RenewableOreSavedData.get(level).record(node,"frostcrystal",ready);
            var unloaded=new BlockPos(23000,400,23000);
            level.getChunk(unloaded.getX()>>4,unloaded.getZ()>>4);
            RenewableOreSavedData.get(level).record(unloaded,"glacierite",ready);
            var key=level.dimension();
            saveAndClose(helper,key);
            level=createLevel(helper,generationSeed);
            level.getChunk(built.getX()>>4,built.getZ()>>4);
            helper.assertTrue(!level.hasChunkAt(unloaded),"distant queued node remained loaded after reopening the world");
            helper.assertTrue(PlayerWorldgenProtection.get(level).contains(built),"player provenance lost after disk reload");
            helper.assertTrue(level.getBlockState(built).is(net.minecraft.world.level.block.Blocks.STONE),"player stone lost after disk reload");
            helper.assertTrue(RegionalMineEvents.restoreDue(level,ready-1,8)==0,"ore restored before cooldown");
            helper.assertTrue(RegionalMineEvents.restoreDue(level,ready,8)==1,"queued ore did not survive disk reload");
            helper.assertTrue(level.getBlockState(node).is(RegionalOreRegistries.NODES.get("frostcrystal").get()),"wrong renewed ore");
            helper.assertTrue(!level.hasChunkAt(unloaded),"renewal force-loaded an unloaded chunk");
            helper.assertTrue(RenewableOreSavedData.get(level).contains(unloaded),"unloaded node discarded");
        }
        helper.assertTrue(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource,
            "integration test must use the real RPG generator");
        if(suite.equals("sky")){
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
                // Use vanilla eye insertion, rather than setting portal blocks
                // directly: wrong frame orientation must fail this test.
                for(int x=54;x<=58;x++)for(int z=54;z<=58;z++){
                    var pos=new BlockPos(minX+x,site.elevation()+45,minZ+z);
                    if(!level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME))continue;
                    var eye=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE);
                    var hit=new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
                    var context=new net.minecraft.world.item.context.UseOnContext(level,null,
                        net.minecraft.world.InteractionHand.MAIN_HAND,eye,hit){};
                    net.minecraft.world.item.Items.ENDER_EYE.useOn(context);
                }
                for(int x=55;x<=57;x++)for(int z=55;z<=57;z++){
                    var pos=new BlockPos(minX+x,site.elevation()+45,minZ+z);
                    check(helper,level,pos,level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.END_PORTAL),
                        "vanilla-activated End portal");
                }
            }
        }
        }
        if(suite.equals("terrain")){
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
        Pipican.LOGGER.info("RPG generated chunk checks passed for seed {}: 22 capital cores",
            generationSeed);
        }
        if(suite.equals("capital")){
            var cities=new LandmarkCatalog().all().stream().filter(c->c.tier().equals("capital")).toList();
            int shard=generationSeed==123L?1:generationSeed==987654321L?2:0;
            var failures=new java.util.ArrayList<String>();
            for(int i=shard;i<cities.size();i+=3){
                var city=cities.get(i);
                try{
                    verifyCapital(helper,level,city);
                    Pipican.LOGGER.info("Verified 63 placed capital pieces, foundations and disk reload: {} seed {}",city.id(),generationSeed);
                }catch(RuntimeException error){
                    failures.add(city.id()+": "+error.getMessage());
                    Pipican.LOGGER.error("Capital integration failure {} seed {}",city.id(),generationSeed,error);
                }finally{
                    var layout=WorldgenJson.read("capital_assemblies/"+city.id()+".json");
                    for(String collection:List.of("relative_positions","connectors"))
                        for(var row:layout.getAsJsonArray(collection))
                            level.getServer().getStructureManager().remove(net.minecraft.resources.Identifier.parse(
                                row.getAsJsonObject().get("template").getAsString()));
                    saveAndClose(helper,level.dimension());
                    level=createLevel(helper,generationSeed);
                }
            }
            helper.assertTrue(failures.isEmpty(),"seed="+generationSeed+" capital failures: "+failures);
        }
    }

    private static void verifyCapital(GameTestHelper helper,ServerLevel level,LandmarkCatalog.Landmark city){
        var progress=CapitalPlacementSavedData.get(level);
        for(int i=0;i<63;i++){
            var preparation=CapitalChunkLoader.prepare(level,city,progress);
            int before=progress.progress(city.id())==null?0:progress.progress(city.id()).nextTile();
            helper.assertTrue(before==i,"chunk preparation advanced placement progress");
            // Synchronously finish the fixture's chunks; production ticks wait
            // for preparation.ready() and never block on generation.
            try{
                helper.assertTrue(CapitalPlacementEvents.buildOneTile(level,city,progress),
                    "seed="+generationSeed+" capital="+city.id()+" tile="+i+" placement deferred");
            }finally{preparation.release(level);}
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
        if(city.id().equals("morvath")){
            int ox=city.x()-46,oz=city.z()-46,base=progress.progress(city.id()).foundationY();
            for(int x=28;x<=38;x++)for(int y=2;y<=13;y++){
                var pos=new BlockPos(ox+x,base+y,oz+51);
                boolean frame=x==28||x==38||y==2||y==13;
                check(helper,level,pos,frame?level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.OBSIDIAN):
                    level.getBlockState(pos).isAir(),"complete unlit Morvath Nether portal");
            }
        }
        var layout=WorldgenJson.read("capital_assemblies/"+city.id()+".json");
        for(String collection:List.of("relative_positions","connectors"))
            for(var element:layout.getAsJsonArray(collection)){
                var row=element.getAsJsonObject();
                var template=level.getServer().getStructureManager().get(
                    net.minecraft.resources.Identifier.parse(row.get("template").getAsString())).orElseThrow();
                var size=template.getSize();
                int x=CapitalPlacementEvents.tileOrigin(city.x(),true,size.getX(),row.get("x").getAsInt());
                int z=CapitalPlacementEvents.tileOrigin(city.z(),true,size.getZ(),row.get("z").getAsInt());
                int y=progress.progress(city.id()).foundationY()-1;
                for(int dx:new int[]{0,size.getX()/2,size.getX()-1})
                    for(int dz:new int[]{0,size.getZ()/2,size.getZ()-1}){
                        var pos=new BlockPos(x+dx,y,z+dz);
                        check(helper,level,pos,level.getBlockState(pos).isSolid(),"supported capital foundation "+city.id());
                    }
            }
        verifyRoadConnectivity(helper,level,city,progress.progress(city.id()).foundationY(),layout);
    }

    private static boolean walkable(ServerLevel level,BlockPos feet){
        var floor=feet.below();
        return level.getBlockState(floor).isCollisionShapeFullBlock(level,floor)
            &&level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()
            &&level.getBlockState(feet.above()).getCollisionShape(level,feet.above()).isEmpty();
    }

    private static void verifyRoadConnectivity(GameTestHelper helper,ServerLevel level,
            LandmarkCatalog.Landmark city,int base,com.google.gson.JsonObject layout){
        var targets=new java.util.HashSet<BlockPos>();
        for(var element:layout.getAsJsonArray("relative_positions")){
            var row=element.getAsJsonObject();
            var size=row.getAsJsonArray("size");
            int x=city.x()-46+row.get("x").getAsInt()+size.get(0).getAsInt()/2;
            int z=city.z()-46+row.get("z").getAsInt()+2;
            BlockPos target=new BlockPos(x,base+1,z);
            helper.assertTrue(walkable(level,target),"blocked district entrance "+city.id()+" "+target);
            targets.add(target);
        }
        var start=new BlockPos(city.x()+10,base+1,city.z()-44);
        helper.assertTrue(walkable(level,start),"blocked central keep approach "+city.id());
        var queue=new java.util.ArrayDeque<BlockPos>();
        var visited=new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        queue.add(start);visited.add(start.asLong());
        while(!queue.isEmpty()&&!targets.isEmpty()){
            var pos=queue.removeFirst();
            targets.remove(pos);
            for(var direction:List.of(net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.SOUTH,
                    net.minecraft.core.Direction.EAST,net.minecraft.core.Direction.WEST)){
                var adjacent=pos.relative(direction);
                if(Math.abs(adjacent.getX()-city.x())>274||Math.abs(adjacent.getZ()-city.z())>274)continue;
                for(int dy=1;dy>=-2;dy--){
                    var next=adjacent.above(dy);
                    if(next.getY()<base-2||next.getY()>base+3||!walkable(level,next))continue;
                    if(visited.add(next.asLong()))queue.add(next);
                    break;
                }
            }
        }
        helper.assertTrue(targets.isEmpty(),"disconnected capital district entrances "+city.id()+": "+targets);
    }
}
