package com.pipican.stats;

import java.util.Objects;

/** FLAT adds units; PERCENT multiplies the accumulated base by (1 + value/100). */
public record EffectModifier(Effect effect, Operation operation, double value) {
    public enum Operation { FLAT, PERCENT }
    public EffectModifier {
        Objects.requireNonNull(effect);
        Objects.requireNonNull(operation);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("modifier value");
    }
}
