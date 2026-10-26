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
 */
public record Scaling(String source, double per) {

    public static final Codec<Scaling> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("source").forGetter(Scaling::source),
            Codec.DOUBLE.fieldOf("per").forGetter(Scaling::per)
    ).apply(i, Scaling::new));

    public static final Codec<List<Scaling>> LIST_CODEC = CODEC.listOf();

    /** {@code base} plus every scaling term evaluated against {@code ctx}. */
    public static double evaluate(double base, List<Scaling> scaling, EffectContext ctx) {
        double value = base;
        for (Scaling s : scaling) {
            value += s.per() * ctx.sourceValue(s.source());
        }
        return value;
    }
}
