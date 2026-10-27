package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;

/**
 * {@code pipican:scrutiny} - a mob you keep looking at for {@code look_seconds} gets marked (it glows; "marked"
 * bonuses apply to it) for {@code seconds}. Several effects: the shortest look time and longest mark count.
 *
 * <pre>{@code { "type": "pipican:scrutiny", "look_seconds": 2, "seconds": 30 } }</pre>
 * {@code scaling} lowers the look time ("per 10 Scrutiny, scrutinise 1s faster" = per -0.1).
 */
public record ScrutinyEffect(double lookSeconds, double seconds, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<ScrutinyEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 60).optionalFieldOf("look_seconds", 0.0).forGetter(ScrutinyEffect::lookSeconds),
            Codec.doubleRange(0, 3600).optionalFieldOf("seconds", 0.0).forGetter(ScrutinyEffect::seconds),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ScrutinyEffect::scaling)
    ).apply(i, ScrutinyEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.SCRUTINY;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addScrutiny(Scaling.evaluate(lookSeconds, scaling, ctx), seconds);
    }
}
