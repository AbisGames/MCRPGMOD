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
 * {@code pipican:pet_mod} - your tamed animals deal {@code damage} more (0.1 = +10%), get {@code health} more
 * max health (0.1 = +10%) plus {@code hearts} flat hearts and {@code armor} armor, and take {@code damage_taken}
 * more damage (-0.15 = 15% less). Adds up over effects; health and armor only apply within 32 blocks of you.
 *
 * <pre>{@code { "type": "pipican:pet_mod", "damage": 0.05, "armor": 1 } }</pre>
 * {@code scaling} applies to every amount the effect sets.
 */
public record PetModEffect(java.util.Optional<Double> damage, java.util.Optional<Double> health, java.util.Optional<Double> hearts, java.util.Optional<Double> armor, java.util.Optional<Double> damageTaken, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<PetModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("damage").forGetter(PetModEffect::damage),
            Codec.DOUBLE.optionalFieldOf("health").forGetter(PetModEffect::health),
            Codec.DOUBLE.optionalFieldOf("hearts").forGetter(PetModEffect::hearts),
            Codec.DOUBLE.optionalFieldOf("armor").forGetter(PetModEffect::armor),
            Codec.DOUBLE.optionalFieldOf("damage_taken").forGetter(PetModEffect::damageTaken),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(PetModEffect::scaling)
    ).apply(i, PetModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.PET_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addPetMod(new com.pipican.player.ClassState.PetMods(Scaling.evaluate(damage, scaling, ctx), Scaling.evaluate(health, scaling, ctx),
                Scaling.evaluate(hearts, scaling, ctx), Scaling.evaluate(armor, scaling, ctx), Scaling.evaluate(damageTaken, scaling, ctx)));
    }
}
