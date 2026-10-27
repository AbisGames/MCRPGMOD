package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:absorb} - a shield that soaks up the next {@code amount} damage for up to {@code seconds}.
 * Uses the vanilla Absorption effect, so the amount is rounded up to whole hearts of 4 (8 = two levels).
 *
 * <pre>{@code { "type": "pipican:absorb", "amount": 8, "seconds": 30 } }</pre>
 */
public record AbsorbAction(Targets targets, double amount, double seconds) implements AbilityAction {

    public static final MapCodec<AbsorbAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(AbsorbAction::targets),
            Codec.doubleRange(1, 512).fieldOf("amount").forGetter(AbsorbAction::amount),
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 30.0).forGetter(AbsorbAction::seconds)
    ).apply(i, AbsorbAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.ABSORB;
    }

    @Override
    public void run(AbilityContext ctx) {
        int amplifier = (int) Math.ceil(amount / 4.0) - 1;
        for (LivingEntity target : targets.select(ctx.player())) {
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int) (seconds * 20), amplifier), ctx.player());
        }
    }
}
