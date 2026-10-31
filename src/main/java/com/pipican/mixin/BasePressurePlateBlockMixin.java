package com.pipican.mixin;

import com.pipican.ability.Trackless;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code no_plates}: pressure plates (wooden, stone and weighted) don't count the player. */
@Mixin(BasePressurePlateBlock.class)
public abstract class BasePressurePlateBlockMixin {

    @Inject(method = "getEntityCount", at = @At("HEAD"), cancellable = true)
    private static void pipican$skipTrackless(Level level, AABB box, Class<? extends Entity> entityClass, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(level.getEntitiesOfClass(entityClass, box,
                EntitySelector.NO_SPECTATORS.and(e -> !e.isIgnoringBlockTriggers() && !Trackless.ignoresPlates(e))).size());
    }
}
