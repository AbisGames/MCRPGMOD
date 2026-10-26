package com.pipican.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * A plain "attribute + amount + operation" triple, used for per-stat-point bonuses.
 *
 * <p>JSON: {@code { "attribute": "minecraft:max_health", "amount": 1.0, "operation": "add_value" }}.
 * {@code operation} is one of {@code add_value}, {@code add_multiplied_base}, {@code add_multiplied_total}
 * and defaults to {@code add_value}.
 */
public record AttributeBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {

    public static final Codec<AttributeBonus> CODEC = RecordCodecBuilder.create(i -> i.group(
            Attribute.CODEC.fieldOf("attribute").forGetter(AttributeBonus::attribute),
            Codec.DOUBLE.fieldOf("amount").forGetter(AttributeBonus::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(AttributeBonus::operation)
    ).apply(i, AttributeBonus::new));
}
