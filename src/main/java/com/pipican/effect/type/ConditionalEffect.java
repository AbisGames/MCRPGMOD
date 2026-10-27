package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import java.util.List;

/**
 * {@code pipican:conditional} - other effects that only count while a condition holds.
 *
 * <pre>{@code
 * { "type": "pipican:conditional",
 *   "when": { "type": "pipican:state", "state": "wet" },
 *   "effects": [ { "type": "pipican:damage_dealt", "multiplier": 0.75 } ] }
 * }</pre>
 * Conditions are re-checked twice a second; when one flips, the player's bonuses are rebuilt.
 * The inner effects can be anything, including stats and other conditionals.
 */
public record ConditionalEffect(ClassCondition when, List<ClassEffect> effects) implements ClassEffect {

    // Lazy: this codec is built while ClassEffect.CODEC itself is still initialising.
    public static final MapCodec<ConditionalEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ClassCondition.CODEC.fieldOf("when").forGetter(ConditionalEffect::when),
            Codec.lazyInitialized(() -> ClassEffect.CODEC).listOf().fieldOf("effects").forGetter(ConditionalEffect::effects)
    ).apply(i, ConditionalEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.CONDITIONAL;
    }

    @Override
    public void collectStats(EffectContext ctx) {
        if (ctx.test(when)) {
            effects.forEach(e -> e.collectStats(ctx));
        }
    }

    @Override
    public void apply(EffectContext ctx) {
        if (ctx.test(when)) {
            effects.forEach(e -> e.apply(ctx));
        }
    }
}
