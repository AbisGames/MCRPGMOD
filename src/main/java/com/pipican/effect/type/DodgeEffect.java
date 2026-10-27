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
 * {@code pipican:dodge} - a chance to completely avoid a hit from a mob or player (not falls, fire...).
 *
 * <pre>{@code { "type": "pipican:dodge", "chance": 0.08, "melee_only": true } }</pre>
 * {@code ranged_only}: only arrows and other projectiles ("arrow-deflect chance").
 * Chances add up, capped at 90%.
 */
public record DodgeEffect(double chance, boolean meleeOnly, boolean rangedOnly, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<DodgeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 1).fieldOf("chance").forGetter(DodgeEffect::chance),
            Codec.BOOL.optionalFieldOf("melee_only", false).forGetter(DodgeEffect::meleeOnly),
            Codec.BOOL.optionalFieldOf("ranged_only", false).forGetter(DodgeEffect::rangedOnly),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(DodgeEffect::scaling)
    ).apply(i, DodgeEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.DODGE;
    }

    @Override
    public void apply(EffectContext ctx) {
        double value = Scaling.evaluate(chance, scaling, ctx) * ctx.rank();
        if (rangedOnly) {
            ctx.addRangedDodge(value);
        } else {
            ctx.addDodge(value, meleeOnly);
        }
    }
}
