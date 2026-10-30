package com.pipican.resource;

import com.pipican.worldgen.PlayerWorldgenProtection;
import com.pipican.worldgen.WorldStructureReservations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Small veins, restricted to the decorating chunk. Never touch inventory/player provenance. */
public final class RareOreFeature extends Feature<NoneFeatureConfiguration> {
    private final RareResources.Deposit deposit;
    public RareOreFeature(RareResources.Deposit deposit){ super(NoneFeatureConfiguration.CODEC); this.deposit=deposit; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var world=context.level(); var origin=context.origin(); var random=context.random();
        var protection=PlayerWorldgenProtection.cached(world.getLevel());
        if(protection==null) return false; // Workers must never load SavedData themselves.
        var chunk=world.getChunk(origin);
        if(!chunk.getAllReferences().isEmpty()) return false;
        if(!deposit.region().equals("end") && WorldStructureReservations.intersects(origin.getX(),origin.getZ(),origin.getX(),origin.getZ(),4)) return false;
        int low=Math.max(deposit.minY(),world.getMinY()), high=Math.min(deposit.maxY(),world.getMaxY()-1);
        if(low>high) return false;
        int start=random.nextInt(high-low+1); BlockPos anchor=null;
        for(int offset=0;offset<=high-low;offset++){
            var candidate=new BlockPos(origin.getX(),low+(start+offset)%(high-low+1),origin.getZ());
            if(deposit.allows(world,candidate) && natural(world.getBlockState(candidate)) && !protection.contains(candidate)) { anchor=candidate; break; }
        }
        if(anchor==null) return false;
        int placed=0; var position=anchor;
        for(int attempt=0;attempt<deposit.size();attempt++){
            if((position.getX()>>4)==(origin.getX()>>4) && (position.getZ()>>4)==(origin.getZ()>>4)
                && world.ensureCanWrite(position) && deposit.allows(world,position) && !protection.contains(position)
                && natural(world.getBlockState(position)) && world.getBlockEntity(position)==null){
                world.setBlock(position,RareResources.ORES.get(deposit.id()).get().defaultBlockState(),2); placed++;
            }
            position=position.offset(random.nextInt(3)-1,random.nextInt(3)-1,random.nextInt(3)-1);
        }
        return placed>0;
    }
    private static boolean natural(net.minecraft.world.level.block.state.BlockState state){
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF) || state.is(Blocks.END_STONE);
    }
}
