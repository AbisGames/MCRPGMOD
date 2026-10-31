package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.core.PipicanRegistries;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.stat.StatDefinition;
import net.minecraft.resources.ResourceKey;

/**
 * {@code pipican:stat} - grants flat points in a stat.
 *
 * <pre>{@code { "type": "pipican:stat", "stat": "pipican:strength", "amount": 2 } }</pre>
 * Applied before every other effect, so stat-scaled effects see these points. Cannot itself scale
 * with stats (that would be circular). Final amount = amount * rank. The one exception: {@code per_special} points
 * per whole {@code step} points of the class's special stat ("+0.2 to every stat per 10 points"), added after the other
 * stats are counted (so they don't feed back into the special stat).
 */
public record StatEffect(ResourceKey<StatDefinition> stat, double amount, double perSpecial, double step) implements ClassEffect {

    public static final MapCodec<StatEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.STAT).fieldOf("stat").forGetter(StatEffect::stat),
            Codec.DOUBLE.optionalFieldOf("amount", 0.0).forGetter(StatEffect::amount),
            Codec.DOUBLE.optionalFieldOf("per_special", 0.0).forGetter(StatEffect::perSpecial),
            Codec.doubleRange(1, 1000).optionalFieldOf("step", 1.0).forGetter(StatEffect::step)
    ).apply(i, StatEffect::new));

    public StatEffect(ResourceKey<StatDefinition> stat, double amount) {
        this(stat, amount, 0, 1);
    }

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.STAT;
    }

    @Override
    public void collectStats(EffectContext ctx) {
        if (amount != 0) {
            ctx.addStat(stat, amount * ctx.rank());
        }
    }

    /** The {@code per_special} part, once the special stat's points are known. */
    public void collectSpecial(EffectContext ctx, double specialPoints) {
        if (perSpecial != 0) {
            ctx.addStat(stat, perSpecial * Math.floor(specialPoints / step + 1e-9) * ctx.rank());
        }
    }
}
