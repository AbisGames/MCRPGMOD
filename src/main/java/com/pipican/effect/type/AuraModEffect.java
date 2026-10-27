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
 * {@code pipican:aura_mod} - upgrades your auras named like {@code name} ("song" matches "Inspiring Song"; empty = all):
 * {@code radius} more blocks, {@code strength} more of their bonuses (1 = doubled), extra {@code effects}, and
 * {@code include_self} to get them yourself too.
 *
 * <pre>{@code { "type": "pipican:aura_mod", "name": "song", "radius": 1, "scaling": [ { "source": "special_stat", "per": 0.1 } ] } }</pre>
 * {@code scaling} applies to {@code radius}; {@code strength_scaling} to {@code strength}.
 */
public record AuraModEffect(String name, double radius, double strength, boolean includeSelf, List<ClassEffect> effects,
                            List<Scaling> scaling, List<Scaling> strengthScaling) implements ClassEffect {

    public static final MapCodec<AuraModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(AuraModEffect::name),
            Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(AuraModEffect::radius),
            Codec.DOUBLE.optionalFieldOf("strength", 0.0).forGetter(AuraModEffect::strength),
            Codec.BOOL.optionalFieldOf("include_self", false).forGetter(AuraModEffect::includeSelf),
            Codec.lazyInitialized(() -> ClassEffect.CODEC).listOf().optionalFieldOf("effects", List.of()).forGetter(AuraModEffect::effects),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(AuraModEffect::scaling),
            Scaling.LIST_CODEC.optionalFieldOf("strength_scaling", List.of()).forGetter(AuraModEffect::strengthScaling)
    ).apply(i, AuraModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.AURA_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addAuraMod(name, Scaling.evaluate(radius, scaling, ctx), Scaling.evaluate(strength, strengthScaling, ctx), includeSelf, effects);
    }
}
