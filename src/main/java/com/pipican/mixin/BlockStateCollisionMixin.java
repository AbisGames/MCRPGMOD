package com.pipican.mixin;

import com.pipican.ability.Phasing;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code phase_stone} (Earth Glide): stone has no collision for a phasing player, on both sides. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateCollisionMixin {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void pipican$phase(BlockGetter level, BlockPos pos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() != null
                && Phasing.passes(entityContext.getEntity(), (BlockBehaviour.BlockStateBase) (Object) this, pos)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
