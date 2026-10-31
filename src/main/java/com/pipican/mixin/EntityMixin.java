package com.pipican.mixin;

import com.pipican.ability.Trackless;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@code unpushable}: a player with it and the creatures around them don't shove each other - they pass by. */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void pipican$passBy(Entity other, CallbackInfo ci) {
        if (Trackless.unpushable((Entity) (Object) this) || Trackless.unpushable(other)) {
            ci.cancel();
        }
    }
}
