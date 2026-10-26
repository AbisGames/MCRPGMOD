package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A class's special weakness: a description shown to players plus ordinary {@link ClassEffect}s
 * that implement it (usually {@code pipican:damage_taken} above 1, or negative attributes).
 *
 * <pre>{@code
 * "weakness": {
 *   "description": "Takes 50% more fire damage.",
 *   "effects": [ { "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 } ]
 * }
 * }</pre>
 */
public record Weakness(Component description, List<ClassEffect> effects) {

    public static final Codec<Weakness> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Weakness::description),
            ClassEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Weakness::effects)
    ).apply(i, Weakness::new));
}
