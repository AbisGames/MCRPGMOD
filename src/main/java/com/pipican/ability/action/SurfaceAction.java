package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:surface} - if the user is stuck inside blocks (Earth Glide ending mid-wall), lifts them to the nearest
 * free space above, up to 32 blocks.
 *
 * <pre>{@code { "type": "pipican:surface" } }</pre>
 */
public record SurfaceAction() implements AbilityAction {

    public static final MapCodec<SurfaceAction> CODEC = MapCodec.unit(SurfaceAction::new);

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.SURFACE;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        for (int up = 0; up <= 32; up++) {
            var box = player.getBoundingBox().move(0, up, 0);
            if (ctx.level().noCollision(player, box)) {
                if (up > 0) {
                    player.teleportTo(player.getX(), player.getY() + up, player.getZ());
                }
                return;
            }
        }
    }
}
