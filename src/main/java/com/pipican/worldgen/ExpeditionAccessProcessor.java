package com.pipican.worldgen;

import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** A native-generation escape ladder at the cave's sparse front boundary.
 * Every write remains inside the current decoration chunk. The shaft uses
 * only the template's X/Z footprint, so all of its chunks receive processing.
 */
public final class ExpeditionAccessProcessor extends StructureProcessor {
    private static final ExpeditionAccessProcessor INSTANCE=new ExpeditionAccessProcessor();
    private static final MapCodec<ExpeditionAccessProcessor> CODEC=MapCodec.unit(INSTANCE);
    private static final DeferredRegister<StructureProcessorType<?>> TYPES=
        DeferredRegister.create(Registries.STRUCTURE_PROCESSOR,Pipican.MOD_ID);
    private static final java.util.function.Supplier<StructureProcessorType<?>> TYPE=
        TYPES.register("expedition_access",()->(StructureProcessorType<ExpeditionAccessProcessor>)()->CODEC);
    public static void register(IEventBus bus){TYPES.register(bus);}
    private ExpeditionAccessProcessor(){}
    @Override protected StructureProcessorType<?> getType(){return TYPE.get();}

    @Override public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,BlockPos origin,BlockPos reference,
            List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed,StructurePlaceSettings settings){
        if(original.isEmpty())return processed; // SinglePoolElement also processes data markers.
        var ladder=world(origin,settings,40,0);
        var casing=world(origin,settings,39,0);
        var bounds=settings.getBoundingBox();
        if(bounds!=null&&!bounds.isInside(ladder)&&!bounds.isInside(casing))return processed;
        // Original positions precede foundation extension and are template-local.
        int floor=origin.getY()+original.stream().mapToInt(b->b.pos().getY()).min().orElse(0);
        var outside=world(origin,settings,40,-1);
        int surface=level.getHeight(level instanceof net.minecraft.server.level.ServerLevel?
            Heightmap.Types.WORLD_SURFACE:Heightmap.Types.WORLD_SURFACE_WG,outside.getX(),outside.getZ());
        while(surface>level.getMinY()+1&&foliage(level.getBlockState(new BlockPos(outside.getX(),surface-1,outside.getZ()))))surface--;
        Pipican.LOGGER.debug("Native expedition access origin {} floor {} outside {} surface {} bounds {}",origin,floor,outside,surface,bounds);
        if(Math.abs(surface-(floor+1))<=4)return processed;
        int bottom=Math.max(level.getMinY()+1,Math.min(surface,floor+1));
        int top=Math.min(level.getMaxY()-3,Math.max(surface,floor+1));
        BlockState support=processed.stream().filter(b->b.pos().getY()==floor&&b.state().isSolid()
            &&!b.state().hasBlockEntity()).map(StructureTemplate.StructureBlockInfo::state)
            .findFirst().orElse(Blocks.STONE.defaultBlockState());
        BlockState climbing=Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.EAST);
        // StructureTemplate rotates states after finalizeProcessing.
        var edits=new LinkedHashMap<BlockPos,BlockState>();
        for(int y=bottom;y<=top;y++){
            edits.put(new BlockPos(casing.getX(),y,casing.getZ()),support);
            edits.put(new BlockPos(ladder.getX(),y,ladder.getZ()),climbing);
        }
        for(int y=top+1;y<=top+2;y++)edits.put(new BlockPos(ladder.getX(),y,ladder.getZ()),Blocks.AIR.defaultBlockState());
        var protection=PlayerWorldgenProtection.cached(level.getLevel());
        if(protection==null)return processed; // Fail closed until server-thread initialization.
        // Preflight the entire local plan before returning any replacement.
        for(var entry:edits.entrySet()){
            var pos=entry.getKey();
            if(bounds!=null&&!bounds.isInside(pos))continue;
            var state=level.getBlockState(pos);
            if(protection.contains(pos)||state.hasBlockEntity()||(!state.isAir()&&!CapitalPlacementEvents.isNatural(state))){
                Pipican.LOGGER.debug("Expedition access deferred at {} by {} protected {}",pos,state,protection.contains(pos));
                return processed;
            }
        }
        var result=new ArrayList<>(processed);
        for(var entry:edits.entrySet())if(bounds==null||bounds.isInside(entry.getKey()))
            result.add(new StructureTemplate.StructureBlockInfo(entry.getKey(),entry.getValue(),null));
        return result;
    }
    private static BlockPos world(BlockPos origin,StructurePlaceSettings settings,int x,int z){
        return StructureTemplate.transform(new BlockPos(x,0,z),settings.getMirror(),settings.getRotation(),settings.getRotationPivot()).offset(origin);
    }
    private static boolean foliage(BlockState state){
        if(state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock
                ||state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||state.is(Blocks.SNOW))return true;
        for(var palette:RegionalNatureBlocks.PALETTES.values())
            if(state.is(palette.trunk().get())||state.is(palette.canopy().get())||state.is(palette.bloom().get()))return true;
        return false;
    }
}
