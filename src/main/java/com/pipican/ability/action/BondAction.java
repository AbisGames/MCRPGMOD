package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import com.pipican.player.ClassService;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:bond} - bonds the user with the player they look at (Lasting Bond): while within 32
 * blocks, each heals half of what the other heals. Replaces any earlier bond. Cancelled (and refunded)
 * without a player in sight.
 *
 * <pre>{@code { "type": "pipican:bond", "range": 16 } }</pre>
 */
public record BondAction(double range) implements AbilityAction {

    public static final MapCodec<BondAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 64).optionalFieldOf("range", 16.0).forGetter(BondAction::range)
    ).apply(i, BondAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BOND;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        if (!(Targets.lookTarget(player, range) instanceof ServerPlayer other)) {
            ctx.cancel(Component.translatable("pipican.ability.no_player"));
            return;
        }
        ClassService.data(player).extras().bond = Optional.of(other.getUUID());
        player.sendSystemMessage(Component.translatable("pipican.bond.made", other.getName()));
        other.sendSystemMessage(Component.translatable("pipican.bond.made", player.getName()));
    }
}
