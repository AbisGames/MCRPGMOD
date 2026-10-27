package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Scry;
import com.pipican.ability.Targets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:scry} - the user sees through another player's eyes for {@code seconds}: the player
 * they look at, otherwise the nearest one within {@code range}. Cancelled (and refunded) if nobody is there.
 *
 * <pre>{@code { "type": "pipican:scry", "seconds": 5, "range": 128 } }</pre>
 */
public record ScryAction(double seconds, double range) implements AbilityAction {

    public static final MapCodec<ScryAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 60).optionalFieldOf("seconds", 5.0).forGetter(ScryAction::seconds),
            Codec.doubleRange(1, 1024).optionalFieldOf("range", 128.0).forGetter(ScryAction::range)
    ).apply(i, ScryAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.SCRY;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        ServerPlayer target = Targets.lookTarget(player, 64) instanceof ServerPlayer p ? p : null;
        if (target == null) {
            target = ctx.level().players().stream().filter(p -> p != player && !p.isSpectator() && p.distanceToSqr(player) <= range * range)
                    .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))).orElse(null);
        }
        if (target == null) {
            ctx.cancel(Component.translatable("pipican.ability.no_player"));
            return;
        }
        Scry.start(player, target, (long) (seconds * 20));
    }
}
