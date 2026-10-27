package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:blink} - teleports the user up to {@code distance} blocks where they look, stopping short of walls.
 *
 * <pre>{@code { "type": "pipican:blink", "distance": 8 } }</pre>
 */
public record BlinkAction(double distance) implements AbilityAction {

    public static final MapCodec<BlinkAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 64).optionalFieldOf("distance", 8.0).forGetter(BlinkAction::distance)
    ).apply(i, BlinkAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BLINK;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        HitResult hit = player.pick(distance, 1.0f, false);
        Vec3 look = player.getLookAngle();
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(look.scale(distance))
                : hit.getLocation().subtract(look.scale(0.8));
        // Feet go where the eyes aimed, nudged up until the player fits.
        Vec3 feet = target.subtract(0, player.getEyeHeight(), 0);
        for (int i = 0; i < 4; i++) {
            if (ctx.level().noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())))) {
                player.teleportTo(feet.x, feet.y, feet.z);
                player.resetFallDistance();
                return;
            }
            feet = feet.add(0, 1, 0);
        }
    }
}
