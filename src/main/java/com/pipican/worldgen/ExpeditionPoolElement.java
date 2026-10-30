package com.pipican.worldgen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Reserves native decoration chunks for the cave's outside access corridor. */
public final class ExpeditionPoolElement extends SinglePoolElement {
    public static final int ACCESS_LENGTH=16;
    public static final MapCodec<ExpeditionPoolElement> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        ExpeditionPoolElement.<ExpeditionPoolElement>templateCodec(),
        ExpeditionPoolElement.<ExpeditionPoolElement>processorsCodec(),
        ExpeditionPoolElement.<ExpeditionPoolElement>projectionCodec(),
        ExpeditionPoolElement.<ExpeditionPoolElement>overrideLiquidSettingsCodec()).apply(i,ExpeditionPoolElement::new));
    private static final DeferredRegister<StructurePoolElementType<?>> TYPES=
        DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT,Pipican.MOD_ID);
    private static final java.util.function.Supplier<StructurePoolElementType<?>> TYPE=
        TYPES.register("expedition",()->(StructurePoolElementType<ExpeditionPoolElement>)()->CODEC);
    public static void register(IEventBus bus){TYPES.register(bus);}
    private ExpeditionPoolElement(Either<Identifier,StructureTemplate> template,Holder<StructureProcessorList> processors,
            StructureTemplatePool.Projection projection,Optional<LiquidSettings> liquids){
        super(template,processors,projection,liquids);
    }
    @Override public StructurePoolElementType<?> getType(){return TYPE.get();}
    @Override public BoundingBox getBoundingBox(StructureTemplateManager manager,BlockPos origin,Rotation rotation){
        var building=super.getBoundingBox(manager,origin,rotation);
        var near=new BlockPos(36,0,-ACCESS_LENGTH).rotate(rotation).offset(origin);
        var far=new BlockPos(44,0,0).rotate(rotation).offset(origin);
        return building.encapsulate(BoundingBox.fromCorners(near,far));
    }
    @Override public Vec3i getSize(StructureTemplateManager manager,Rotation rotation){
        var box=getBoundingBox(manager,BlockPos.ZERO,rotation);
        return new Vec3i(box.getXSpan(),box.getYSpan(),box.getZSpan());
    }
}
