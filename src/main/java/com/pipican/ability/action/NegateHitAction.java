package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.HitShields;
import com.pipican.ability.Targets;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:negate_hit} - the next attack against each target within {@code seconds} misses
 * (Twist Fate). With {@code guard: true} the user takes that hit instead (Intercept).
 *
 * <pre>{@code { "type": "pipican:negate_hit", "target": "look_ally", "range": 16, "seconds": 10 } }</pre>
 */
public record NegateHitAction(Targets targets, double seconds, boolean guard) implements AbilityAction {

    public static final MapCodec<NegateHitAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK_ALLY).forGetter(NegateHitAction::targets),
            Codec.doubleRange(0.5, 600).optionalFieldOf("seconds", 10.0).forGetter(NegateHitAction::seconds),
            Codec.BOOL.optionalFieldOf("guard", false).forGetter(NegateHitAction::guard)
    ).apply(i, NegateHitAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.NEGATE_HIT;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (guard && target == ctx.player()) {
                continue;
            }
            HitShields.add(target, (long) (seconds * 20), guard ? Optional.of(ctx.player().getUUID()) : Optional.empty());
        }
    }
}
