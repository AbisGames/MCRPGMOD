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
    private static final List<Site> SITES=allSites();
    private static List<Site> allSites(){
        var sites=new java.util.ArrayList<Site>();
        sites.add(new Site("blackspire",3500,21300,-1,"special/pillager_blackspire"));
        // The Umbral city's position is clear of Durnhold's protection radius.
        sites.add(new Site("umbral_city",4900,0,-14,"special/umbral_ancient_city"));
        for(var island:SkyIslandLayout.ISLANDS)
            sites.add(new Site(island.id(),island.x(),island.z(),
                SkyIslandLayout.FOUNDATION_Y,
                island.citadel()?"special/aetheris_sky_citadel":
                    "regional/aetheris_expanse/village"));
        return List.copyOf(sites);
    }
    private SpecialLandmarkPlacementEvents() {}
    /** -1 alone means surface. Negative underground Y values are literal. */
    public static int resolveY(Site site,int terrainY) {
        return site.elevation()==-1?terrainY:site.elevation();
    }
    public static List<Site> sites(){return SITES;}


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
            Identifier.parse("pipican:"+site.template()));
        if(template.isEmpty())return false;
        int terrainY=site.elevation()==-1
            ?level.getHeight(Heightmap.Types.WORLD_SURFACE,site.x(),site.z())
            :0;
        int y=resolveY(site,terrainY);
        var size=template.get().getSize();
        var origin=new BlockPos(site.x()-size.getX()/2,y,site.z()-size.getZ()/2);
        var settings=new StructurePlaceSettings().setIgnoreEntities(true);
        BoundingBox box=template.get().getBoundingBox(settings,origin);
        if(box.minY()<level.getMinY()||box.maxY()>=level.getMaxY())return false;
        if(CapitalStructureExclusion.intersects(box,200))return false;
        for(var existing:new LandmarkCatalog().all()){
            if(StructurePlanner.collides(site.x(),site.z(),
                    Math.max(size.getX(),size.getZ())/2+120,
                    existing.x(),existing.z(),existing.radius()))return false;
        }
        String expected=site.id().equals("blackspire")
            ?"redrock_badlands":"greyspine_mountains";
        if(!WorldLayout.authoredRegionAt(site.x(),site.z())
                .map(r->r.id().equals(expected)).orElse(false))return false;
        // Every potentially touched chunk must already exist in memory.
        for(int cx=Math.floorDiv(box.minX(),16);cx<=Math.floorDiv(box.maxX(),16);cx++)
            for(int cz=Math.floorDiv(box.minZ(),16);cz<=Math.floorDiv(box.maxZ(),16);cz++)
                if(!level.hasChunkAt(new BlockPos(cx*16,y,cz*16)))return false;
        for(int x=box.minX();x<=box.maxX();x+=4)
            for(int z=box.minZ();z<=box.maxZ();z+=4){
                if(site.elevation()>=0){
                    for(int yy=box.minY();yy<=box.maxY();yy+=4){
                        var p=new BlockPos(x,yy,z);
                        if(!level.getBlockState(p).isAir())return false;
                    }
                } else if(site.elevation()==-1){
                    int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
                    if(Math.abs(surface-y)>8)return false;
                } else {
                    // Underground city: do not replace existing inventories.
                    for(int yy=box.minY();yy<=box.maxY();yy+=4){
                        var p=new BlockPos(x,yy,z);
                        if(level.getBlockState(p).hasBlockEntity())return false;
                    }
                }
            }
        return template.get().placeInWorld(level,origin,origin,settings,level.getRandom(),2);
    }
}
