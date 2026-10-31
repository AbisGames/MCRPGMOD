package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.entity.Eidolon;
import net.minecraft.network.chat.Component;

/**
 * {@code pipican:mount} - climbs into (rides) the user's eidolon if it is within {@code range}, or
 * climbs out if already inside (Cockpit). Cancelled (and refunded) if there is no eidolon nearby.
 *
 * <pre>{@code { "type": "pipican:mount", "range": 6 } }</pre>
 */
public record MountAction(double range) implements AbilityAction {

    public static final MapCodec<MountAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 32).optionalFieldOf("range", 6.0).forGetter(MountAction::range)
    ).apply(i, MountAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.MOUNT;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        if (player.getVehicle() instanceof Eidolon) {
            player.stopRiding();
            return;
        }
        var eidolon = Eidolon.of(player).filter(e -> e.distanceToSqr(player) <= range * range);
        if (eidolon.isEmpty() || !player.startRiding(eidolon.get(), true, true)) {
            ctx.cancel(Component.translatable("pipican.eidolon.not_near"));
        }
    }
}
