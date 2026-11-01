package com.pipican.mixin;

import com.pipican.ability.RedstoneReach;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.redstone.RedstoneWireEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code redstone_reach} (Technomancer): reach dust loses no signal on one block in eight. */
@Mixin(RedstoneWireEvaluator.class)
public abstract class RedstoneWireEvaluatorMixin {

    @Inject(method = "getIncomingWireSignal", at = @At("RETURN"), cancellable = true)
    private void pipican$reach(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int signal = cir.getReturnValueI();
        if (signal > 0 && signal < 15 && RedstoneReach.carries(level, pos)) {
            cir.setReturnValue(signal + 1);
        }
    }
}
