package com.pipican.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pipican.Pipican;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Incremental near-player placement of all 70 authored settlements, including
 * each capital's 63-piece city (25 buildings and 38 connecting roads).
 *
 * <p>Triggered only in the RPG overworld, once per 40 server ticks, one tile
 * per pass. The foundation elevation is sampled once from real terrain, never
 * assigned a fixed Y=70. Completion survives save reloads. This avoids
 * spawning all 22 gigantic cities on the first world tick.
 *
 * <p>This is a singleplayer-oriented integration fallback, NOT a native
 * chunk-generation structure feature. Expiring tickets prepare neighbouring
 * chunks before a production tick writes one tile. Monumental terrain still
 * needs an art-directed adaptation pass.
 */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class CapitalPlacementEvents {
    private static final int BUILD_TICK_INTERVAL=40;
    private static final int MAX_NATURAL_CUT=24;
    private static final int MAX_FOUNDATION_FILL=12;
    private static final long PLAYER_TRIGGER_RADIUS_SQUARED=150L*150L;
    private static final List<LandmarkCatalog.Landmark> LANDMARKS =
        new LandmarkCatalog().all();

    /** Central keep first, then successive city rings. Existing partially
     * generated saves without the version marker keep the legacy tile order.
     */
    private static final int[] CENTRE_FIRST_ORDER = buildTileOrder();
    private static int[] buildTileOrder(){
        List<Integer> indices=new ArrayList<>();
        for(int i=0;i<25;i++)indices.add(i);
        indices.sort(Comparator.comparingInt((Integer i)->
            Math.max(Math.abs(i%5-2),Math.abs(i/5-2)))
            .thenComparingInt(i->Math.abs(i%5-2)+Math.abs(i/5-2))
            .thenComparingInt(i->i));
        return indices.stream().mapToInt(Integer::intValue).toArray();
    }
    public static int tileOrigin(int centre,boolean capital,int size,int offset){
        return centre-(capital?46:size/2)+offset;
    }
    public static int cityTileIndex(int serialIndex,boolean centreFirst){
        if(serialIndex<0||serialIndex>=25)throw new IllegalArgumentException();
        return centreFirst?CENTRE_FIRST_ORDER[serialIndex]:serialIndex;
    }
    private CapitalPlacementEvents(){}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for(ServerLevel level : event.getServer().getAllLevels()) {
            if(!(level.getChunkSource().getGenerator().getBiomeSource()
                    instanceof RegionalBiomeSource)) continue;
            if(level.getGameTime()%BUILD_TICK_INTERVAL!=0)continue;
            CapitalPlacementSavedData progress=CapitalPlacementSavedData.get(level);
            boolean placed=false;
            for(var player:level.players()) {
                if(placed)break;
                for(LandmarkCatalog.Landmark city:LANDMARKS){
                    long dx=(long)Math.floor(player.getX())-city.x();
                    long dz=(long)Math.floor(player.getZ())-city.z();
                    long trigger=city.tier().equals("capital")?380L*380L:PLAYER_TRIGGER_RADIUS_SQUARED;
                    if(dx*dx+dz*dz>trigger)continue;
                    CapitalPlacementSavedData.Progress done=progress.progress(city.id());
                    int total=city.tier().equals("capital")?63:1;
                    if(done!=null&&done.nextTile()>=total)continue;
                    var preparation=CapitalChunkLoader.prepare(level,city,progress);
                    if(!preparation.ready())continue;
                    boolean completed;
                    try{completed=buildOneTile(level,city,progress);}
                    finally{preparation.release(level);}
                    if(completed) {
                        placed=true;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Place only one 92x34 district or 112x72 palace per call.
     * Failing templates are never acknowledged as placed.
     */
    public static boolean buildOneTile(ServerLevel level,LandmarkCatalog.Landmark city,
                                       CapitalPlacementSavedData progress) {
        var state=progress.progress(city.id());
        int index=state==null?0:state.nextTile();
        boolean capital=city.tier().equals("capital");
        String orderMarker="centre_first_"+city.id();
        boolean centreFirst=capital&&(progress.progress(orderMarker)!=null||state==null);
        if(index>=(capital?63:1))return false;
        // The initial height is based on terrain in this save, not a
        // globally fixed sea-level or arbitrary Y constant.
        level.getChunk(Math.floorDiv(city.x(),16),Math.floorDiv(city.z(),16));
        int foundationY=state==null?
            Math.min(foundationHeight(level,city.x(),city.z()),
                level.getMaxY()-75) : state.foundationY();
        JsonObject tile;
        String location;
        if(capital){
            JsonObject layout=WorldgenJson.read("capital_assemblies/"+city.id()+".json");
            JsonElement row=index<25
                ? layout.getAsJsonArray("relative_positions").get(cityTileIndex(index,centreFirst))
                : layout.getAsJsonArray("connectors").get(index-25);
            tile=row.getAsJsonObject();
            location=tile.get("template").getAsString();
        }else{
            tile=new JsonObject();
            tile.addProperty("x",0);
            tile.addProperty("z",0);
            location="pipican:authored/"+city.assetPool();
        }
        var id=net.minecraft.resources.Identifier.parse(location);
        var manager=level.getServer().getStructureManager();
        var template=manager.get(id);
        if(template.isEmpty()){
            Pipican.LOGGER.error("Missing RPG city structure {} for {}",id,city.id());
            return false;
        }
        // City plots are centred around the authored landmark location;
        // the +/-224 tile array is offset by -46 to centre the whole 5x5 area.
        StructureTemplate piece=template.get();
        int ox=tileOrigin(city.x(),capital,piece.getSize().getX(),tile.get("x").getAsInt());
        int oz=tileOrigin(city.z(),capital,piece.getSize().getZ(),tile.get("z").getAsInt());
        BlockPos origin=new BlockPos(ox,foundationY,oz);
        StructurePlaceSettings options=new StructurePlaceSettings().setIgnoreEntities(true);
        BoundingBox bounds=piece.getBoundingBox(options,origin);
        // Low road templates also need headroom when grading a natural hill.
        // Preflight the entire volume that may be cleared, including player
        // edits above a short connector's own four-block template bounds.
        BoundingBox clearance=new BoundingBox(bounds.minX(),bounds.minY(),bounds.minZ(),
            bounds.maxX(),Math.max(bounds.maxY(),foundationY+MAX_NATURAL_CUT),bounds.maxZ());
        // A city's outer districts extend beyond the player's loaded view.
        // Waiting for those chunks forever also blocks all later districts.
        // Generate only the current piece; protection is checked before writing.
        for(int cx=Math.floorDiv(bounds.minX(),16);cx<=Math.floorDiv(bounds.maxX(),16);cx++)
            for(int cz=Math.floorDiv(bounds.minZ(),16);cz<=Math.floorDiv(bounds.maxZ(),16);cz++)
                level.getChunk(cx,cz);
        var graded=inspectFootprint(level,clearance,foundationY);
        if(graded==null) {
            Pipican.LOGGER.warn("Unsafe terrain or obstruction at tile {} of {}; placement deferred",index,city.id());
            return false;
        }
        if(!safeFoundation(level,bounds,foundationY))return false;
        for(var block:graded)level.setBlock(block.position(),Blocks.AIR.defaultBlockState(),2);
        if(!piece.placeInWorld(level,origin,origin,options,level.getRandom(),2)){
            // Vanilla returns false before writing an empty/invalid template.
            // Restore the natural terrain if such a datapack template is used.
            for(var block:graded)level.setBlock(block.position(),block.state(),2);
            Pipican.LOGGER.warn("Failed to place tile {} of {}",index,city.id());
            return false;
        }
        var palette=RegionalNatureBlocks.PALETTES.get(city.region());
        BlockState support=palette==null?Blocks.STONE.defaultBlockState():palette.strata().get().defaultBlockState();
        for(int x=bounds.minX();x<=bounds.maxX();x++)
            for(int z=bounds.minZ();z<=bounds.maxZ();z++)
                for(int y=foundationY-1;y>=foundationY-MAX_FOUNDATION_FILL;y--){
                    var p=new BlockPos(x,y,z);
                    if(level.getBlockState(p).isSolid())break;
                    level.setBlock(p,support,2);
                }
        if(capital)clearWalkways(level,piece,origin,tile.get("kind").getAsString());
        progress.advance(city.id(),index+1,foundationY);
        if(capital&&centreFirst&&index==0)
            progress.advance(orderMarker,1,foundationY);
        // These authored pieces are placed directly and never referenced by
        // native StructureStarts. Keeping every decoded city in the global
        // template cache retains tens of millions of BlockInfo objects.
        manager.remove(id);
        if((capital&&index==62)||(!capital&&index==0))
            Pipican.LOGGER.info("Completed RPG landmark {} ({} pieces)",city.id(),capital?63:1);
        return true;
    }
    // Full voxel preflight; collect the grading plan without changing blocks.
    // Foundation volume is checked separately before either phase writes.
    private static List<TerrainBlock> inspectFootprint(ServerLevel level,BoundingBox box,int foundationY) {
        if(box.minY()<level.getMinY() || box.maxY()>=level.getMaxY())return null;
        var protection=PlayerWorldgenProtection.get(level);
        for(int x=box.minX();x<=box.maxX();x+=8)for(int z=box.minZ();z<=box.maxZ();z+=8){
            if(!level.hasChunkAt(new BlockPos(x,foundationY,z)))return null;
            int top=terrainSurface(level,x,z);
            if(top>foundationY+MAX_NATURAL_CUT||top<foundationY-MAX_FOUNDATION_FILL)top=rawSurface(level,x,z);
            if(top>foundationY+MAX_NATURAL_CUT || top<foundationY-MAX_FOUNDATION_FILL){
                Pipican.LOGGER.warn("Capital terrain mismatch at {},{}: surface {}, foundation {}",x,z,top,foundationY);
                return null;
            }
        }
        var removed=new ArrayList<TerrainBlock>();
        for(int x=box.minX();x<=box.maxX();x++)
            for(int z=box.minZ();z<=box.maxZ();z++)
                for(int y=box.minY();y<=box.maxY();y++) {
                    BlockPos position=new BlockPos(x,y,z);
                    if(protection.contains(position))return null;
                    BlockState state=level.getBlockState(position);
                    if(state.hasBlockEntity() || (!state.isAir() && !isNatural(state))){
                        Pipican.LOGGER.warn("Capital placement obstructed at {} by {}",position,state);
                        return null;
                    }
                    if(y>foundationY&&!state.isAir())removed.add(new TerrainBlock(position,state));
                }
        return removed;
    }

    // The preflight collects this plan without changing the world. Bounded
    // natural excavation grades courtyards and roads to the saved elevation.
    private record TerrainBlock(BlockPos position,BlockState state){}

    static int terrainSurface(ServerLevel level,int x,int z){
        int y=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
        while(y>level.getMinY()){
            var state=level.getBlockState(new BlockPos(x,y-1,z));
            boolean foliage=state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock
                || state.is(net.minecraft.tags.BlockTags.LOGS) || state.is(net.minecraft.tags.BlockTags.LEAVES)
                || state.is(Blocks.SNOW);
            if(!foliage)for(var p:RegionalNatureBlocks.PALETTES.values())
                if(state.is(p.trunk().get())||state.is(p.canopy().get())||state.is(p.bloom().get())){foliage=true;break;}
            if(!foliage)break;
            y--;
        }
        return y;
    }

    static int rawSurface(ServerLevel level,int x,int z){
        return level.getChunkSource().getGenerator().getBaseHeight(x,z,
            Heightmap.Types.OCEAN_FLOOR_WG,level,level.getChunkSource().randomState());
    }

    private static int foundationHeight(ServerLevel level,int x,int z){
        // One decorative boulder at the anchor must not lift an entire city.
        // The median samples the real, terrain-generated flat pad.
        int[] samples=new int[25];
        int i=0;
        for(int dx=-24;dx<=24;dx+=12)
            for(int dz=-24;dz<=24;dz+=12)samples[i++]=rawSurface(level,x+dx,z+dz);
        Arrays.sort(samples);
        return samples[samples.length/2];
    }

    private static boolean safeFoundation(ServerLevel level,BoundingBox box,int foundationY){
        var protection=PlayerWorldgenProtection.get(level);
        for(int x=box.minX();x<=box.maxX();x++)
            for(int z=box.minZ();z<=box.maxZ();z++){
                boolean supported=false;
                for(int y=foundationY-1;y>=Math.max(level.getMinY(),foundationY-MAX_FOUNDATION_FILL);y--){
                    var pos=new BlockPos(x,y,z);
                    var state=level.getBlockState(pos);
                    if(protection.contains(pos)||state.hasBlockEntity()||(!state.isAir()&&!isNatural(state))){
                        Pipican.LOGGER.warn("Capital foundation obstructed at {} by {}",pos,state);
                        return false;
                    }
                    if(state.isSolid()){supported=true;break;}
                }
                if(!supported){
                    Pipican.LOGGER.warn("Capital lacks foundation support at {},{} below {} (terrain {})",
                        x,z,foundationY,terrainSurface(level,x,z));
                    return false;
                }
            }
        return true;
    }

    private static void clearWalkways(ServerLevel level,StructureTemplate piece,BlockPos origin,String kind){
        int width=piece.getSize().getX(),depth=piece.getSize().getZ();
        var protection=PlayerWorldgenProtection.get(level);
        for(int x=0;x<width;x++)for(int z=0;z<depth;z++){
            boolean path;
            if(kind.equals("central_keep"))
                path=((z<3||z>=depth-3)&&x>=42&&x<=60)
                    ||((x<3||x>=width-3)&&z>=42&&z<=60);
            else if(kind.equals("walkway_eastwest"))path=z>0&&z<depth-1;
            else if(kind.equals("walkway_northsouth"))path=x>0&&x<width-1;
            else path=Math.abs(x-width/2)<=3||Math.abs(z-depth/2)<=3;
            if(!path)continue;
            for(int y=1;y<=3;y++){
                var pos=origin.offset(x,y,z);
                var state=level.getBlockState(pos);
                if(!protection.contains(pos)&&!state.hasBlockEntity()&&!state.isAir()&&isNatural(state))
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            }
        }
    }

    private static final class NaturalPalette {
        static final Set<net.minecraft.world.level.block.Block> BLOCKS=load();
        private static Set<net.minecraft.world.level.block.Block> load(){
            var result=new HashSet<net.minecraft.world.level.block.Block>();
            for(var p:RegionalNatureBlocks.PALETTES.values()){
                result.add(p.ground().get());result.add(p.strata().get());
                result.add(p.trunk().get());result.add(p.canopy().get());result.add(p.bloom().get());
            }
            return Set.copyOf(result);
        }
    }
    static boolean isNatural(BlockState state) {
        // Modern vanilla biomes include bushes, ferns and other vegetation
        // outside FLOWERS/LOGS. Player provenance is checked before this
        // classification, so player-planted vegetation remains protected.
        if(state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock)return true;
        if(NaturalPalette.BLOCKS.contains(state.getBlock()))return true;
        return state.is(net.minecraft.tags.BlockTags.DIRT)
            || state.is(net.minecraft.tags.BlockTags.COAL_ORES)
            || state.is(net.minecraft.tags.BlockTags.IRON_ORES)
            || state.is(net.minecraft.tags.BlockTags.COPPER_ORES)
            || state.is(net.minecraft.tags.BlockTags.GOLD_ORES)
            || state.is(net.minecraft.tags.BlockTags.REDSTONE_ORES)
            || state.is(net.minecraft.tags.BlockTags.LAPIS_ORES)
            || state.is(net.minecraft.tags.BlockTags.DIAMOND_ORES)
            || state.is(net.minecraft.tags.BlockTags.EMERALD_ORES)
            || state.is(net.minecraft.tags.BlockTags.SAND)
            || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
            || state.is(net.minecraft.tags.BlockTags.LEAVES)
            || state.is(net.minecraft.tags.BlockTags.LOGS)
            || state.is(net.minecraft.tags.BlockTags.FLOWERS)
            || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.TALL_GRASS) || state.is(Blocks.SNOW)
            || state.is(Blocks.GLOW_LICHEN) || state.is(Blocks.VINE)
            || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON) || state.is(Blocks.COCOA)
            || state.is(Blocks.BAMBOO) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)
            || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT)
            || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS)
            || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)
            || state.is(Blocks.MUSHROOM_STEM)
            || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.GRAVEL)
            || state.is(Blocks.CLAY) || state.is(Blocks.WATER) || state.is(Blocks.BUBBLE_COLUMN)
            || state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.RAW_COPPER_BLOCK)
            || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.ICE)
            || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
            || state.is(Blocks.LAVA);
    }

}
