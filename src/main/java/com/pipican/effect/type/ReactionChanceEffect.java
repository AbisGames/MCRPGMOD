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
 * {@code pipican:reaction_chance} - changes the chance of the player's {@code pipican:on} reactions that carry
 * this {@code name} ("+5% ignite chance", "charm chance doubled").
 *
 * <pre>{@code { "type": "pipican:reaction_chance", "name": "ignite", "amount": 0.05 } }</pre>
 * Chance = (the reaction's own chance + every {@code amount}) x every {@code multiplier}, at most 100%.
 */
public record ReactionChanceEffect(String name, double amount, double multiplier, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<ReactionChanceEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ReactionChanceEffect::name),
            Codec.doubleRange(-1, 1).optionalFieldOf("amount", 0.0).forGetter(ReactionChanceEffect::amount),
            Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.0).forGetter(ReactionChanceEffect::multiplier),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ReactionChanceEffect::scaling)
    ).apply(i, ReactionChanceEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.REACTION_CHANCE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addReactionChance(name, Scaling.evaluate(amount, scaling, ctx) * ctx.rank(), multiplier);
    }
}
