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
 * with stats (that would be circular). Final amount = amount * rank.
 */
public record StatEffect(ResourceKey<StatDefinition> stat, double amount) implements ClassEffect {

    public static final MapCodec<StatEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.STAT).fieldOf("stat").forGetter(StatEffect::stat),
            Codec.DOUBLE.fieldOf("amount").forGetter(StatEffect::amount)
    ).apply(i, StatEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.STAT;
    }

    @Override
    public void collectStats(EffectContext ctx) {
        ctx.addStat(stat, amount * ctx.rank());
    }
}
