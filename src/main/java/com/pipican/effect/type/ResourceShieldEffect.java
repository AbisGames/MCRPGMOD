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
 * {@code pipican:resource_shield} - your resource bar takes damage before your health: each point absorbs
 * {@code absorb} damage (Earthen Defense: "1 point per damage"). {@code efficiency} (0.1 = +10%) makes every point
 * absorb more without switching the shield on by itself. Put it in a {@code pipican:conditional} for "while you
 * stand on stone".
 *
 * <pre>{@code { "type": "pipican:resource_shield", "absorb": 1 } }</pre>
 * {@code scaling} applies to {@code efficiency}.
 */
public record ResourceShieldEffect(double absorb, double efficiency, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<ResourceShieldEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 100).optionalFieldOf("absorb", 0.0).forGetter(ResourceShieldEffect::absorb),
            Codec.DOUBLE.optionalFieldOf("efficiency", 0.0).forGetter(ResourceShieldEffect::efficiency),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ResourceShieldEffect::scaling)
    ).apply(i, ResourceShieldEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.RESOURCE_SHIELD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addResourceShield(absorb, Scaling.evaluate(efficiency, scaling, ctx));
    }
}
