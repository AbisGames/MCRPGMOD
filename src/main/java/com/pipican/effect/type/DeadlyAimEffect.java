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
 * {@code pipican:deadly_aim} - holding a fully drawn bow {@code hold_seconds} longer makes the shot deal {@code damage}
 * more (0.5 = +50%), fly perfectly straight and, with {@code crit}, always crit. Only an effect with {@code base}
 * switches it on; others (upgrades) add their numbers ("aim time -0.5s", "+1% per Precision").
 *
 * <pre>{@code { "type": "pipican:deadly_aim", "base": true, "hold_seconds": 2, "damage": 0.5 } }</pre>
 * {@code scaling} applies to {@code hold_seconds} and {@code damage}.
 */
public record DeadlyAimEffect(boolean base, double holdSeconds, double damage, boolean crit, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<DeadlyAimEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("base", false).forGetter(DeadlyAimEffect::base),
            Codec.DOUBLE.optionalFieldOf("hold_seconds", 0.0).forGetter(DeadlyAimEffect::holdSeconds),
            Codec.DOUBLE.optionalFieldOf("damage", 0.0).forGetter(DeadlyAimEffect::damage),
            Codec.BOOL.optionalFieldOf("crit", false).forGetter(DeadlyAimEffect::crit),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(DeadlyAimEffect::scaling)
    ).apply(i, DeadlyAimEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.DEADLY_AIM;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addDeadlyAim(base, Scaling.evaluate(holdSeconds, scaling, ctx), Scaling.evaluate(damage, scaling, ctx), crit);
    }
}
