package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * {@code pipican:attribute} - modifies any vanilla or modded attribute.
 *
 * <pre>{@code
 * { "type": "pipican:attribute", "attribute": "minecraft:movement_speed",
 *   "amount": 0.05, "operation": "add_multiplied_base",
 *   "scaling": [ { "source": "class_level", "per": 0.001 } ] }
 * }</pre>
 * Final amount = (amount + scaling) * rank.
 */
public record AttributeEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<AttributeEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(AttributeEffect::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeEffect::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(AttributeEffect::operation),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(AttributeEffect::scaling)
    ).apply(i, AttributeEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.ATTRIBUTE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addAttribute(attribute, Scaling.evaluate(amount, scaling, ctx) * ctx.rank(), operation);
    }
}
