package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Fixed, once-per-save placement for unique regional landmarks. NPCs are
 * deliberately excluded. Vanilla auto-spawning is not used for these sites.
 */
@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class SpecialLandmarkPlacementEvents {
    public record Site(String id,int x,int z,int elevation,String template) {}
    // Y=-1 means surface anchoring; Aetheris is fixed at sky island altitude.
    private static final List<Site> SITES=List.of(
        new Site("blackspire",-4400,21100,-1,"pillager_blackspire"),
        new Site("umbral_city",4600,-500, -14,"umbral_ancient_city"),
        new Site("aetheris_citadel",5700,1600,319,"aetheris_sky_citadel")
    );
    private SpecialLandmarkPlacementEvents() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event){
        for(ServerLevel level:event.getServer().getAllLevels()){
            if(!(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource))continue;
            if(level.getGameTime()%80!=0)continue;
            var progress=CapitalPlacementSavedData.get(level);
            for(var player:level.players()){
                for(Site site:SITES){
                    long dx=(long)player.blockPosition().getX()-site.x();
                    long dz=(long)player.blockPosition().getZ()-site.z();
                    if(dx*dx+dz*dz>180L*180L)continue;
                    String key="special_"+site.id();
                    if(progress.progress(key)!=null)continue;
                    if(tryPlace(level,site)){
                        progress.advance(key,1,site.elevation());
                        Pipican.LOGGER.info("Placed unique RPG structure {}",site.id());
                        return;
                    }
                }
            }
        }
    }

    private static boolean tryPlace(ServerLevel level,Site site){
        var template=level.getServer().getStructureManager().get(
            Identifier.parse("pipican:special/"+site.template()));
        if(template.isEmpty())return false;
        int y=site.elevation()<0
            ?level.getHeight(Heightmap.Types.WORLD_SURFACE,site.x(),site.z())
            :site.elevation();
        var size=template.get().getSize();
        var origin=new BlockPos(site.x()-size.getX()/2,y,site.z()-size.getZ()/2);
        var settings=new StructurePlaceSettings().setIgnoreEntities(true);
        BoundingBox box=template.get().getBoundingBox(settings,origin);
        if(box.minY()<level.getMinY()||box.maxY()>=level.getMaxY())return false;
        if(CapitalStructureExclusion.intersects(box,200))return false;
        // Do not load ungenerated neighbours as a side effect of placing.
        for(int x=box.minX();x<=box.maxX();x+=16)
            for(int z=box.minZ();z<=box.maxZ();z+=16)
                if(!level.hasChunkAt(new BlockPos(x,y,z)))return false;
        return template.get().placeInWorld(level,origin,origin,settings,level.getRandom(),2);
    }
}
