package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * {@code pipican:damage_taken} / {@code pipican:damage_dealt} - multiplies damage.
 *
 * <pre>{@code
 * { "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 }
 * { "type": "pipican:damage_dealt", "multiplier": 1.1 }
 * }</pre>
 * {@code damage_types} is a damage-type tag; omit it to affect all damage. Values above 1 increase
 * damage, below 1 reduce it - so the same type works for weaknesses and resistances.
 * With ranks, the bonus part stacks: 1.1 at rank 3 becomes 1.3.
 */
public record DamageMultiplierEffect(boolean incoming, Optional<TagKey<DamageType>> damageTypes, double multiplier, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<DamageMultiplierEffect> TAKEN_CODEC = codec(true);
    public static final MapCodec<DamageMultiplierEffect> DEALT_CODEC = codec(false);

    private static MapCodec<DamageMultiplierEffect> codec(boolean incoming) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("damage_types").forGetter(DamageMultiplierEffect::damageTypes),
                Codec.DOUBLE.fieldOf("multiplier").forGetter(DamageMultiplierEffect::multiplier),
                Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(DamageMultiplierEffect::scaling)
        ).apply(i, (types, mult, scaling) -> new DamageMultiplierEffect(incoming, types, mult, scaling)));
    }

    @Override
    public ClassEffectTypes.Type type() {
        return incoming ? ClassEffectTypes.DAMAGE_TAKEN : ClassEffectTypes.DAMAGE_DEALT;
    }

    @Override
    public void apply(EffectContext ctx) {
        double value = 1 + (Scaling.evaluate(multiplier, scaling, ctx) - 1) * ctx.rank();
        value = Math.max(0, value);
        if (incoming) {
            ctx.addDamageTakenMultiplier(damageTypes, value);
        } else {
            ctx.addDamageDealtMultiplier(damageTypes, value);
        }
    }
}
