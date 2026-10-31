package com.pipican.mixin;

import com.pipican.ability.Trackless;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@code map_range}: a map you carry fills in a wider circle around you each update (the first 128 is that radius). */
@Mixin(MapItem.class)
public abstract class MapItemMixin {

    /** The bonus of the player whose map is updating (updates run on the server thread, one at a time). */
    @Unique
    private static double pipican$range;

    @Inject(method = "update", at = @At("HEAD"))
    private void pipican$readRange(Level level, Entity player, MapItemSavedData data, CallbackInfo ci) {
        pipican$range = Trackless.mapRange(player);
    }

    @ModifyConstant(method = "update", constant = @Constant(intValue = 128, ordinal = 0))
    private int pipican$widerRadius(int radius) {
        return pipican$range > 0 ? (int) Math.round(radius * (1 + pipican$range)) : radius;
    }
}
