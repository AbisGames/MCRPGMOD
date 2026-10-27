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
 * {@code pipican:summon_mod} - your summons ({@code pipican:summon}) get {@code damage} / {@code health} more
 * (0.1 = +10%), last {@code seconds} longer and come {@code count} more at a time. Adds up over effects.
 *
 * <pre>{@code { "type": "pipican:summon_mod", "damage": 0.1, "seconds": 10 } }</pre>
 * {@code scaling} applies to {@code damage} and {@code health}.
 */
public record SummonModEffect(double damage, double health, double seconds, int count, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<SummonModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("damage", 0.0).forGetter(SummonModEffect::damage),
            Codec.DOUBLE.optionalFieldOf("health", 0.0).forGetter(SummonModEffect::health),
            Codec.DOUBLE.optionalFieldOf("seconds", 0.0).forGetter(SummonModEffect::seconds),
            Codec.intRange(0, 16).optionalFieldOf("count", 0).forGetter(SummonModEffect::count),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(SummonModEffect::scaling)
    ).apply(i, SummonModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.SUMMON_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addSummonMod(Scaling.evaluate(damage, scaling, ctx), Scaling.evaluate(health, scaling, ctx), seconds, count);
    }
}
