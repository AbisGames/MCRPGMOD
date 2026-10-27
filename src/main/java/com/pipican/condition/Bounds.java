package com.pipican.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Optional {@code "min"} / {@code "max"} fields shared by numeric conditions. {@code min} is
 * inclusive and {@code max} exclusive, so {@code "max": 0.5} reads as "below half".
 */
public record Bounds(Optional<Double> min, Optional<Double> max) {

    public static final MapCodec<Bounds> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("min").forGetter(Bounds::min),
            Codec.DOUBLE.optionalFieldOf("max").forGetter(Bounds::max)
    ).apply(i, Bounds::new));

    public boolean test(double value) {
        return (min.isEmpty() || value >= min.get()) && (max.isEmpty() || value < max.get());
    }
}
