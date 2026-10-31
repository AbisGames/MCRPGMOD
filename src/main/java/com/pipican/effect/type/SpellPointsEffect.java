package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;

/**
 * {@code pipican:spell_points} - this many spell points refill every time the player sleeps. While
 * they have some, abilities that cost resource use one spell point instead (Wizard's Spell Mastery).
 *
 * <pre>{@code { "type": "pipican:spell_points", "amount": 6 } }</pre>
 */
public record SpellPointsEffect(int amount) implements ClassEffect {

    public static final MapCodec<SpellPointsEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 100).fieldOf("amount").forGetter(SpellPointsEffect::amount)
    ).apply(i, SpellPointsEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.SPELL_POINTS;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addSpellPoints(amount * ctx.rank());
    }
}
