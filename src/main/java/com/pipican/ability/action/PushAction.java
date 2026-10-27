package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:push} - knocks the targets away from the user ({@code strength} like vanilla knockback).
 *
 * <pre>{@code { "type": "pipican:push", "target": "hostiles", "radius": 6, "cone": 90, "strength": 1.5 } }</pre>
 */
public record PushAction(Targets targets, double strength) implements AbilityAction {

    public static final MapCodec<PushAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.HOSTILES).forGetter(PushAction::targets),
            Codec.doubleRange(0, 10).optionalFieldOf("strength", 1.0).forGetter(PushAction::strength)
    ).apply(i, PushAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.PUSH;
    }

    @Override
    public void run(AbilityContext ctx) {
        Vec3 from = ctx.player().position();
        for (LivingEntity target : targets.select(ctx.player())) {
            if (target == ctx.player()) {
                continue;
            }
            Vec3 away = target.position().subtract(from);
            // knockback() pushes away from the given direction, so pass the vector pointing back at the user.
            target.knockback(strength, -away.x, -away.z);
            target.hurtMarked = true;
        }
    }
}
