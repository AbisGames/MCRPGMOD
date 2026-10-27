package com.pipican.effect;

import java.util.function.ToDoubleFunction;
import org.jspecify.annotations.Nullable;

/**
 * A class effect from someone else's aura, applied at the aura's strength and scaled by the aura owner's stats
 * ({@code source}, null = the receiver's own). Never written in JSON, built at runtime.
 */
public record ScaledEffect(ClassEffect inner, double strength, @Nullable ToDoubleFunction<String> source) implements ClassEffect {

    @Override
    public ClassEffectTypes.Type type() {
        return inner.type();
    }

    @Override
    public void collectStats(EffectContext ctx) {
        ctx.scaled(strength, source, () -> inner.collectStats(ctx));
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.scaled(strength, source, () -> inner.apply(ctx));
    }
}
