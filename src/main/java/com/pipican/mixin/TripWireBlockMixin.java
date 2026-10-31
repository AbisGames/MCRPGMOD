package com.pipican.mixin;

import com.pipican.ability.Trackless;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.TripWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** {@code no_tripwires}: tripwires treat the player like one that ignores block triggers. */
@Mixin(TripWireBlock.class)
public abstract class TripWireBlockMixin {

    @Redirect(method = "checkPressed(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/List;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isIgnoringBlockTriggers()Z"))
    private boolean pipican$ignoreTripwire(Entity entity) {
        return entity.isIgnoringBlockTriggers() || Trackless.ignoresTripwires(entity);
    }
}
