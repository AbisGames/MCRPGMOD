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
 * chunk-generation structure feature. Placement can load neighbouring chunks
 * and the monumental terrain still needs an art-directed adaptation pass.
 */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class CapitalPlacementEvents {
    private static final int BUILD_TICK_INTERVAL=40;
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
                    if(buildOneTile(level,city,progress)) {
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
        var center=new BlockPos(city.x(),64,city.z());
        if(!level.hasChunkAt(center))return false;
        int foundationY=state==null?
            Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE,city.x(),city.z()),
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
        // A city's outer districts extend beyond the player's loaded view.
        // Waiting for those chunks forever also blocks all later districts.
        // Generate only the current piece; protection is checked before writing.
        for(int cx=Math.floorDiv(bounds.minX(),16);cx<=Math.floorDiv(bounds.maxX(),16);cx++)
            for(int cz=Math.floorDiv(bounds.minZ(),16);cz<=Math.floorDiv(bounds.maxZ(),16);cz++)
                level.getChunk(cx,cz);
        if(!safeFootprint(level,bounds,foundationY)) {
            Pipican.LOGGER.warn("Unsafe terrain or obstruction at tile {} of {}; placement deferred",index,city.id());
            return false;
        }
        if(!piece.placeInWorld(level,origin,origin,options,level.getRandom(),2)){
            Pipican.LOGGER.warn("Failed to place tile {} of {}",index,city.id());
            return false;
        }
        progress.advance(city.id(),index+1,foundationY);
        if(capital&&centreFirst&&index==0)
            progress.advance(orderMarker,1,foundationY);
        if((capital&&index==62)||(!capital&&index==0))
            Pipican.LOGGER.info("Completed RPG landmark {} ({} pieces)",city.id(),capital?63:1);
        return true;
    }
    // Full voxel check; do not mutate the world before a placement succeeds.
    // Terrain-fitted foundations must be generated separately.
    private static boolean safeFootprint(ServerLevel level,BoundingBox box,int foundationY) {
        if(box.minY()<level.getMinY() || box.maxY()>=level.getMaxY())return false;
        var protection=PlayerWorldgenProtection.get(level);
        for(int x=box.minX();x<=box.maxX();x+=8)for(int z=box.minZ();z<=box.maxZ();z+=8){
            if(!level.hasChunkAt(new BlockPos(x,foundationY,z)))return false;
            int top=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            if(top>foundationY+12 || top<foundationY-12)return false;
        }
        for(int x=box.minX();x<=box.maxX();x++)
            for(int z=box.minZ();z<=box.maxZ();z++)
                for(int y=box.minY();y<=box.maxY();y++) {
                    BlockPos position=new BlockPos(x,y,z);
                    if(protection.contains(position))return false;
                    BlockState state=level.getBlockState(position);
                    if(state.hasBlockEntity() || (!state.isAir() && !isNatural(state)))return false;
                }
        return true;
    }

    static boolean isNatural(BlockState state) {
        if(RegionalNatureBlocks.PALETTES.values().stream().anyMatch(p ->
                state.is(p.ground().get()) || state.is(p.strata().get()) || state.is(p.trunk().get())
                || state.is(p.canopy().get()) || state.is(p.bloom().get())))return true;
        return state.is(net.minecraft.tags.BlockTags.DIRT)
            || state.is(net.minecraft.tags.BlockTags.SAND)
            || state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
            || state.is(net.minecraft.tags.BlockTags.LEAVES)
            || state.is(net.minecraft.tags.BlockTags.LOGS)
            || state.is(net.minecraft.tags.BlockTags.FLOWERS)
            || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.TALL_GRASS) || state.is(Blocks.SNOW)
            || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.GRAVEL)
            || state.is(Blocks.CLAY) || state.is(Blocks.WATER)
            || state.is(Blocks.LAVA);
    }

}
