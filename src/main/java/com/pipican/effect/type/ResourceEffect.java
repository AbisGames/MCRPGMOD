package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * {@code pipican:resource} - changes the class resource bar (mana, rage...): its max or its regen.
 *
 * <pre>{@code
 * { "type": "pipican:resource", "target": "regen", "amount": 0.1, "operation": "add_multiplied_base" }   +10% regen
 * { "type": "pipican:resource", "target": "max", "amount": 10 }                                         +10 max
 * }</pre>
 * {@code operation}: {@code add_value} (default; for regen it is per second) or
 * {@code add_multiplied_base} (a fraction of the class's own numbers; -1 = no regen at all).
 * Percentages are added together, then applied once. A draining bar (negative regen) drains
 * faster with +% regen. Final amount = (amount + scaling) * rank.
 */
public record ResourceEffect(Target target, double amount, AttributeModifier.Operation operation, List<Scaling> scaling) implements ClassEffect {

    public enum Target implements StringRepresentable {
        MAX("max"), REGEN("regen");

        public static final Codec<Target> CODEC = StringRepresentable.fromEnum(Target::values);
        private final String name;

        Target(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<ResourceEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Target.CODEC.fieldOf("target").forGetter(ResourceEffect::target),
            Codec.DOUBLE.fieldOf("amount").forGetter(ResourceEffect::amount),
            AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(ResourceEffect::operation),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ResourceEffect::scaling)
    ).apply(i, ResourceEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.RESOURCE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addResource(target, Scaling.evaluate(amount, scaling, ctx) * ctx.rank(), operation);
    }
}
