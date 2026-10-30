package com.pipican.worldgen;

import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Native jigsaw foundation columns, clipped to the chunk being decorated.
 * Beard noise smooths terrain but cannot guarantee support under a wide floor.
 * Only empty space, fluid and replaceable vegetation are filled; existing
 * solid blocks, inventories and built blocks end the column without replacement.
 */
public final class ExpeditionFoundationProcessor extends StructureProcessor {
    private static final ExpeditionFoundationProcessor INSTANCE=new ExpeditionFoundationProcessor();
    private static final MapCodec<ExpeditionFoundationProcessor> CODEC=MapCodec.unit(INSTANCE);
    private static final DeferredRegister<StructureProcessorType<?>> TYPES=
        DeferredRegister.create(Registries.STRUCTURE_PROCESSOR,Pipican.MOD_ID);
    private static final java.util.function.Supplier<StructureProcessorType<?>> TYPE=
        TYPES.register("expedition_foundation",()->(StructureProcessorType<ExpeditionFoundationProcessor>)()->CODEC);
    public static void register(IEventBus bus){TYPES.register(bus);}
    private ExpeditionFoundationProcessor(){}
    @Override protected StructureProcessorType<?> getType(){return TYPE.get();}

    @Override public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
            ServerLevelAccessor level,BlockPos origin,BlockPos reference,
            List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed,StructurePlaceSettings settings){
        int floor=processed.stream().mapToInt(b->b.pos().getY()).min().orElse(Integer.MIN_VALUE);
        var bounds=settings.getBoundingBox();
        var result=new ArrayList<>(processed);
        for(var block:processed){
            if(block.pos().getY()!=floor||!block.state().isSolid()||block.state().hasBlockEntity()
                    ||(bounds!=null&&!bounds.isInside(block.pos())))continue;
            for(int y=floor-1;y>=level.getMinY();y--){
                var pos=new BlockPos(block.pos().getX(),y,block.pos().getZ());
                if(bounds!=null&&!bounds.isInside(pos))break;
                var current=level.getBlockState(pos);
                boolean fillable=current.isAir()||current.canBeReplaced()
                    ||current.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock
                    ||current.is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN);
                if(current.hasBlockEntity()||!fillable)break;
                result.add(new StructureTemplate.StructureBlockInfo(pos,block.state(),null));
            }
        }
        return result;
    }
}
