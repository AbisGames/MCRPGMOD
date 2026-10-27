package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.Scaling;
import java.util.List;

/**
 * {@code pipican:optimization} - the Transmuter's Optimization: after each sleep the player picks one
 * stat to raise by {@code amount} and one to lower by {@code amount + extra_loss} (clickable chat
 * buttons, or {@code /pipican optimize <raise> <lower>}). Several of these effects add up; {@code scaling}
 * may use stats (e.g. +1 per 20 of the special stat).
 *
 * <pre>{@code { "type": "pipican:optimization", "amount": 3, "extra_loss": 3 } }</pre>
 * Applied right after stat points are counted (see ClassEffectApplier), so it can't scale with itself.
 */
public record OptimizationEffect(double amount, double extraLoss, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<OptimizationEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 100).optionalFieldOf("amount", 0.0).forGetter(OptimizationEffect::amount),
            Codec.doubleRange(0, 100).optionalFieldOf("extra_loss", 0.0).forGetter(OptimizationEffect::extraLoss),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(OptimizationEffect::scaling)
    ).apply(i, OptimizationEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.OPTIMIZATION;
    }
}
