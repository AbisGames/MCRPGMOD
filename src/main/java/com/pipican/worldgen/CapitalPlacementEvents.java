package com.pipican.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pipican.Pipican;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Incremental near-player placement of each authored capital's 25-piece city.
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
    private static final List<LandmarkCatalog.Landmark> CAPITALS =
        new LandmarkCatalog().all().stream()
          .filter(p->p.tier().equals("capital")).toList();

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
                for(LandmarkCatalog.Landmark city:CAPITALS){
                    long dx=(long)Math.floor(player.getX())-city.x();
                    long dz=(long)Math.floor(player.getZ())-city.z();
                    if(dx*dx+dz*dz>PLAYER_TRIGGER_RADIUS_SQUARED)continue;
                    CapitalPlacementSavedData.Progress done=progress.progress(city.id());
                    if(done!=null&&done.nextTile()>=63)continue;
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
        if(index>=63)return false;
        // The initial height is based on terrain in this save, not a
        // globally fixed sea-level or arbitrary Y constant.
        var center=new BlockPos(city.x(),64,city.z());
        if(!level.hasChunkAt(center))return false;
        int foundationY=state==null?
            Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE,city.x(),city.z()),
                level.getMaxY()-75) : state.foundationY();
        JsonObject layout=WorldgenJson.read("capital_assemblies/"+city.id()+".json");
        JsonElement row=index<25
            ? layout.getAsJsonArray("relative_positions").get(index)
            : layout.getAsJsonArray("connectors").get(index-25);
        var tile=row.getAsJsonObject();
        String location=tile.get("template").getAsString();
        var id=net.minecraft.resources.Identifier.parse(location);
        var manager=level.getServer().getStructureManager();
        var template=manager.get(id);
        if(template.isEmpty()){
            Pipican.LOGGER.error("Missing RPG city structure {} for {}",id,city.id());
            return false;
        }
        // City plots are centred around the authored landmark location;
        // the +/-224 tile array is offset by -46 to centre the whole 5x5 area.
        int ox=city.x()-46+tile.get("x").getAsInt();
        int oz=city.z()-46+tile.get("z").getAsInt();
        BlockPos origin=new BlockPos(ox,foundationY,oz);
        StructurePlaceSettings options=new StructurePlaceSettings().setIgnoreEntities(true);
        if(!template.get().placeInWorld(level,origin,origin,options,level.getRandom(),2)){
            Pipican.LOGGER.warn("Failed to place tile {} of {}",index,city.id());
            return false;
        }
        progress.advance(city.id(),index+1,foundationY);
        if(index==62)Pipican.LOGGER.info("Completed RPG capital {} (25 city tiles and 38 bridges)",city.id());
        return true;
    }
}
