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
 * {@code pipican:mount} - changes an attribute of whatever the player is riding (horse, camel, pig...).
 *
 * <pre>{@code { "type": "pipican:mount", "attribute": "minecraft:movement_speed", "amount": 0.1, "operation": "add_multiplied_base" } }</pre>
 * Supported: max health, armor, armor toughness, movement speed, jump strength, attack damage.
 */
public record MountEffect(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<MountEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(MountEffect::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(MountEffect::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(MountEffect::operation),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(MountEffect::scaling)
    ).apply(i, MountEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.MOUNT;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addMountUpgrade(attribute, Scaling.evaluate(amount, scaling, ctx) * ctx.rank(), operation);
    }
}
