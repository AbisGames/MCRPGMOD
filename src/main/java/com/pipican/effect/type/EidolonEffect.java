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
 * {@code pipican:eidolon} - changes an attribute of the player's eidolon (Engineer), like
 * {@code pipican:attribute} does for the player.
 *
 * <pre>{@code { "type": "pipican:eidolon", "attribute": "minecraft:armor", "amount": 2 } }</pre>
 */
public record EidolonEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<EidolonEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(EidolonEffect::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(EidolonEffect::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(EidolonEffect::operation),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(EidolonEffect::scaling)
    ).apply(i, EidolonEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.EIDOLON;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addEidolonUpgrade(attribute, Scaling.evaluate(amount, scaling, ctx) * ctx.rank(), operation);
    }
}
