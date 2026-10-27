package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:teleport_to} - teleports the user next to the first target that isn't them (e.g. the
 * ally they look at). Cancelled (and refunded) if there is none.
 *
 * <pre>{@code { "type": "pipican:teleport_to", "target": "look_ally", "range": 24 } }</pre>
 */
public record TeleportToAction(Targets targets) implements AbilityAction {

    public static final MapCodec<TeleportToAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK_ALLY).forGetter(TeleportToAction::targets)
    ).apply(i, TeleportToAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.TELEPORT_TO;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (target != ctx.player()) {
                Vec3 back = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.2));
                if (BlinkAction.tryAt(ctx, back) || BlinkAction.tryAt(ctx, target.position())) {
                    return;
                }
            }
        }
        ctx.cancel(Component.translatable("pipican.ability.no_target"));
    }
}
