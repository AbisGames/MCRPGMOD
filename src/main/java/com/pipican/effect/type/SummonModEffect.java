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
 * Mobs you take over as servants ({@code pipican:control} in {@code ally} mode) get the same damage and health;
 * their cap ({@code max} on the control) rises by {@code cap} and is multiplied by 1 + {@code cap_multiplier}.
 *
 * <pre>{@code { "type": "pipican:summon_mod", "damage": 0.1, "seconds": 10 } }</pre>
 * {@code hearts} adds flat max health (in hearts). {@code scaling} applies to whichever of {@code damage}, {@code health} and {@code cap} the effect sets.
 */
public record SummonModEffect(java.util.Optional<Double> damage, java.util.Optional<Double> health, double seconds, int count, List<Scaling> scaling, double hearts, java.util.Optional<Double> cap, double capMultiplier,
                              java.util.Optional<Double> extra) implements ClassEffect {

    public static final MapCodec<SummonModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("damage").forGetter(SummonModEffect::damage),
            Codec.DOUBLE.optionalFieldOf("health").forGetter(SummonModEffect::health),
            Codec.DOUBLE.optionalFieldOf("seconds", 0.0).forGetter(SummonModEffect::seconds),
            Codec.intRange(0, 16).optionalFieldOf("count", 0).forGetter(SummonModEffect::count),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(SummonModEffect::scaling),
            Codec.DOUBLE.optionalFieldOf("hearts", 0.0).forGetter(SummonModEffect::hearts),
            Codec.DOUBLE.optionalFieldOf("cap").forGetter(SummonModEffect::cap),
            Codec.DOUBLE.optionalFieldOf("cap_multiplier", 0.0).forGetter(SummonModEffect::capMultiplier),
            Codec.DOUBLE.optionalFieldOf("extra").forGetter(SummonModEffect::extra)
    ).apply(i, SummonModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.SUMMON_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        // "extra" adds to the count with scaling ("+1 minion per 20 Summoning"), whole minions only.
        int more = (int) Math.floor(Scaling.evaluate(extra, scaling, ctx) + 1e-9);
        ctx.addSummonMod(Scaling.evaluate(damage, scaling, ctx), Scaling.evaluate(health, scaling, ctx), seconds, count + more, hearts,
                Scaling.evaluate(cap, scaling, ctx), capMultiplier);
    }
}
