package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Expiring, non-persistent loading tickets prepare one pending city tile.
 * The player tick never waits for a chunk-generation future to finish.
 */
public final class CapitalChunkLoader {
    private static final DeferredRegister<TicketType> TYPES=DeferredRegister.create(Registries.TICKET_TYPE,Pipican.MOD_ID);
    private static final DeferredHolder<TicketType,TicketType> CITY=TYPES.register("capital_placement",
        ()->new TicketType(200L,TicketType.FLAG_LOADING));
    public static void register(IEventBus bus){TYPES.register(bus);}
    public record Lease(ChunkPos centre,int radius){}
    public record Preparation(boolean ready,List<Lease> leases){
        public void release(ServerLevel level){
            for(var lease:leases)level.getChunkSource().removeTicketWithRadius(CITY.get(),lease.centre(),lease.radius());
        }
    }
    private CapitalChunkLoader(){}

    public static Preparation prepare(ServerLevel level,LandmarkCatalog.Landmark city,CapitalPlacementSavedData progress){
        var state=progress.progress(city.id());
        int index=state==null?0:state.nextTile();
        boolean capital=city.tier().equals("capital");
        if(index>=(capital?63:1))return new Preparation(false,List.of());
        int dx=0,dz=0;
        String location="pipican:authored/"+city.assetPool();
        if(capital){
            var layout=WorldgenJson.read("capital_assemblies/"+city.id()+".json");
            boolean centreFirst=state==null||progress.progress("centre_first_"+city.id())!=null;
            var row=(index<25?layout.getAsJsonArray("relative_positions")
                .get(CapitalPlacementEvents.cityTileIndex(index,centreFirst)):
                layout.getAsJsonArray("connectors").get(index-25)).getAsJsonObject();
            dx=row.get("x").getAsInt();dz=row.get("z").getAsInt();location=row.get("template").getAsString();
        }
        var template=level.getServer().getStructureManager().get(net.minecraft.resources.Identifier.parse(location));
        if(template.isEmpty()){
            Pipican.LOGGER.error("Missing city template {} for {}",location,city.id());
            return new Preparation(false,List.of());
        }
        var size=template.get().getSize();
        int x=CapitalPlacementEvents.tileOrigin(city.x(),capital,size.getX(),dx);
        int z=CapitalPlacementEvents.tileOrigin(city.z(),capital,size.getZ(),dz);
        int minX=Math.floorDiv(x,16),maxX=Math.floorDiv(x+size.getX()-1,16);
        int minZ=Math.floorDiv(z,16),maxZ=Math.floorDiv(z+size.getZ()-1,16);
        var centre=new ChunkPos(minX+(maxX-minX)/2,minZ+(maxZ-minZ)/2);
        int radius=Math.max(Math.max(centre.x()-minX,maxX-centre.x()),Math.max(centre.z()-minZ,maxZ-centre.z()));
        var anchor=new ChunkPos(Math.floorDiv(city.x(),16),Math.floorDiv(city.z(),16));
        var leases=List.of(new Lease(centre,radius),new Lease(anchor,0));
        var source=level.getChunkSource();
        boolean ready=source.getChunkNow(anchor.x(),anchor.z())!=null;
        for(int cx=minX;cx<=maxX;cx++)for(int cz=minZ;cz<=maxZ;cz++)
            ready &= source.getChunkNow(cx,cz)!=null;
        if(!ready)for(var lease:leases)
            source.addTicketAndLoadWithRadius(CITY.get(),lease.centre(),lease.radius());
        return new Preparation(ready,leases);
    }
}
