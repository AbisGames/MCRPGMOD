package com.pipican.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * Makes an effect grow with something about the player.
 *
 * <p>JSON: {@code { "source": "class_level", "per": 0.5 }} adds {@code 0.5 * classLevel} to the
 * effect's amount. Valid sources:
 * <ul>
 *   <li>{@code class_level} - the player's level in their class</li>
 *   <li>{@code special_stat} - total points in the class's special stat</li>
 *   <li>any stat id, e.g. {@code pipican:strength} - total points in that stat</li>
 * </ul>
 * {@code max} caps how much this term can add (or take away): {@code { "source": "special_stat", "per": -0.002, "max": 0.2 }} is
 * "-0.2% per point (max -20%)".
 */
public record Scaling(String source, double per, java.util.Optional<Double> max) {

    public Scaling(String source, double per) {
        this(source, per, java.util.Optional.empty());
    }

    /** {@code per * value}, kept within the cap. */
    public double term(double value) {
        double t = per * value;
        return max.map(m -> Math.clamp(t, -Math.abs(m), Math.abs(m))).orElse(t);
    }

    public static final Codec<Scaling> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("source").forGetter(Scaling::source),
            Codec.DOUBLE.fieldOf("per").forGetter(Scaling::per),
            Codec.DOUBLE.optionalFieldOf("max").forGetter(Scaling::max)
    ).apply(i, Scaling::new));

    public static final Codec<List<Scaling>> LIST_CODEC = CODEC.listOf();

    /** An amount the effect may leave out: a missing one stays 0 and isn't scaled. */
    public static double evaluate(java.util.Optional<Double> base, List<Scaling> scaling, EffectContext ctx) {
        return base.map(b -> evaluate(b, scaling, ctx)).orElse(0.0);
    }

    /** {@code base} plus every scaling term evaluated against {@code ctx}. */
    public static double evaluate(double base, List<Scaling> scaling, EffectContext ctx) {
        double value = base;
        for (Scaling s : scaling) {
            value += s.term(ctx.sourceValue(s.source()));
        }
        return value;
    }
}
