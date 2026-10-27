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
 * {@code pipican:aura} - other players within {@code radius} blocks get {@code effects} (and so do you, unless
 * {@code include_self} is false). Bonuses from several players' auras add up.
 *
 * <pre>{@code { "type": "pipican:aura", "radius": 10, "effects": [ { "type": "pipican:damage_dealt", "multiplier": 1.05 } ] } }</pre>
 * {@code scaling} applies to the radius. The effects are evaluated for the player who receives them.
 */
public record AuraEffect(double radius, boolean includeSelf, List<ClassEffect> effects, List<Scaling> scaling) implements ClassEffect {

    // Lazy: this codec is built while ClassEffect.CODEC itself is still initialising.
    public static final MapCodec<AuraEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 64).fieldOf("radius").forGetter(AuraEffect::radius),
            Codec.BOOL.optionalFieldOf("include_self", true).forGetter(AuraEffect::includeSelf),
            Codec.lazyInitialized(() -> ClassEffect.CODEC).listOf().fieldOf("effects").forGetter(AuraEffect::effects),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(AuraEffect::scaling)
    ).apply(i, AuraEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.AURA;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addAura(Scaling.evaluate(radius, scaling, ctx), effects);
        if (includeSelf) {
            effects.forEach(e -> e.apply(ctx));
        }
    }
}
