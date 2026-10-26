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
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:damage_taken} / {@code pipican:damage_dealt} - multiplies damage.
 *
 * <pre>{@code
 * { "type": "pipican:damage_taken", "damage_types": "#minecraft:is_fire", "multiplier": 1.5 }
 * { "type": "pipican:damage_dealt", "multiplier": 1.1 }
 * }</pre>
 * {@code damage_types} is a damage-type tag and {@code weapons} an item / list / #tag of the weapon
 * used (melee weapon, or the bow/crossbow that fired the projectile). Omit them to affect all damage.
 * Values above 1 increase damage, below 1 reduce it - so the same type works for weaknesses and
 * resistances. With a multiplier rank above 1 the bonus part stacks: 1.1 at rank 3 becomes 1.3.
 *
 * <pre>{@code { "type": "pipican:damage_dealt", "weapons": "#pipican:weapons/swords", "multiplier": 1.05 } }</pre>
 */
public record DamageMultiplierEffect(boolean incoming, Optional<TagKey<DamageType>> damageTypes, Optional<HolderSet<Item>> weapons,
                                     double multiplier, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<DamageMultiplierEffect> TAKEN_CODEC = codec(true);
    public static final MapCodec<DamageMultiplierEffect> DEALT_CODEC = codec(false);

    private static MapCodec<DamageMultiplierEffect> codec(boolean incoming) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("damage_types").forGetter(DamageMultiplierEffect::damageTypes),
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(DamageMultiplierEffect::weapons),
                Codec.DOUBLE.fieldOf("multiplier").forGetter(DamageMultiplierEffect::multiplier),
                Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(DamageMultiplierEffect::scaling)
        ).apply(i, (types, weapons, mult, scaling) -> new DamageMultiplierEffect(incoming, types, weapons, mult, scaling)));
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
            ctx.addDamageTakenMultiplier(damageTypes, weapons, value);
        } else {
            ctx.addDamageDealtMultiplier(damageTypes, weapons, value);
        }
    }
}
