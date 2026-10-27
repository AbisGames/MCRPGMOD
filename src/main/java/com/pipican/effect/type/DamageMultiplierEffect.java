package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import com.pipican.effect.VictimState;
import com.pipican.player.ClassState;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
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
 * {@code against} (entity type / list / #tag) limits it to hitting those mobs (dealt) or being hit by
 * them (taken), e.g. {@code "#minecraft:undead"}. {@code victim} (see {@link VictimState}) requires something of the
 * mob hit (dealt) or of the attacker (taken), e.g. below half health or marked by you. {@code except} turns those two around: the bonus is for
 * every other mob ("+30% damage to undead, but -10% damage to everything else").
 * Values above 1 increase damage, below 1 reduce it - so the same type works for weaknesses and
 * resistances. With a multiplier rank above 1 the bonus part stacks: 1.1 at rank 3 becomes 1.3.
 *
 * <pre>{@code { "type": "pipican:damage_dealt", "weapons": "#pipican:weapons/swords", "multiplier": 1.05 } }</pre>
 */
public record DamageMultiplierEffect(boolean incoming, Optional<TagKey<DamageType>> damageTypes, Optional<HolderSet<Item>> weapons,
                                     Optional<HolderSet<EntityType<?>>> against, Optional<VictimState> victim,
                                     double multiplier, List<Scaling> scaling, boolean except) implements ClassEffect {

    public static final MapCodec<DamageMultiplierEffect> TAKEN_CODEC = codec(true);
    public static final MapCodec<DamageMultiplierEffect> DEALT_CODEC = codec(false);

    private static MapCodec<DamageMultiplierEffect> codec(boolean incoming) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                TagKey.hashedCodec(Registries.DAMAGE_TYPE).optionalFieldOf("damage_types").forGetter(DamageMultiplierEffect::damageTypes),
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(DamageMultiplierEffect::weapons),
                RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("against").forGetter(DamageMultiplierEffect::against),
                VictimState.CODEC.optionalFieldOf("victim").forGetter(DamageMultiplierEffect::victim),
                Codec.DOUBLE.fieldOf("multiplier").forGetter(DamageMultiplierEffect::multiplier),
                Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(DamageMultiplierEffect::scaling),
                Codec.BOOL.optionalFieldOf("except", false).forGetter(DamageMultiplierEffect::except)
        ).apply(i, (types, weapons, against, victim, mult, scaling, except) -> new DamageMultiplierEffect(incoming, types, weapons, against, victim, mult, scaling, except)));
    }

    @Override
    public ClassEffectTypes.Type type() {
        return incoming ? ClassEffectTypes.DAMAGE_TAKEN : ClassEffectTypes.DAMAGE_DEALT;
    }

    @Override
    public void apply(EffectContext ctx) {
        double value = 1 + (Scaling.evaluate(multiplier, scaling, ctx) - 1) * ctx.rank();
        value = Math.max(0, value);
        var rule = new ClassState.DamageRule(damageTypes, weapons, against, victim, value, except);
        if (incoming) {
            ctx.addDamageTaken(rule);
        } else {
            ctx.addDamageDealt(rule);
        }
    }
}
