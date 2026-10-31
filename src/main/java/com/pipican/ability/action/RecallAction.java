package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.player.ClassService;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:recall} - sneak + use saves a waypoint where the user stands (free); using it normally
 * teleports back to the waypoint (same dimension only).
 *
 * <pre>{@code { "type": "pipican:recall" } }</pre>
 */
public record RecallAction(boolean unused) implements AbilityAction {

    public static final MapCodec<RecallAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("unused", false).forGetter(RecallAction::unused)
    ).apply(i, RecallAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RECALL;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var extras = ClassService.data(player).extras();
        if (player.isShiftKeyDown()) {
            extras.waypoint = Optional.of(GlobalPos.of(player.level().dimension(), player.blockPosition()));
            ctx.cancel(Component.translatable("pipican.ability.waypoint_set"));
            return;
        }
        if (extras.waypoint.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.no_waypoint"));
            return;
        }
        GlobalPos pos = extras.waypoint.get();
        if (!pos.dimension().equals(player.level().dimension())) {
            ctx.cancel(Component.translatable("pipican.ability.waypoint_elsewhere"));
            return;
        }
        if (!BlinkAction.tryAt(ctx, Vec3.atBottomCenterOf(pos.pos()))) {
            ctx.cancel(Component.translatable("pipican.ability.no_room"));
        }
    }
}
