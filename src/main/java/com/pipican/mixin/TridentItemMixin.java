package com.pipican.mixin;

import com.pipican.ability.Trackless;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** {@code dry_riptide}: Riptide's "in water or rain" check passes anywhere (both when you start and when you let go). */
@Mixin(TridentItem.class)
public abstract class TridentItemMixin {

    @Redirect(method = {"releaseUsing", "use"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z"))
    private boolean pipican$dryRiptide(Player player) {
        return player.isInWaterOrRain() || Trackless.dryRiptide(player);
    }
}
