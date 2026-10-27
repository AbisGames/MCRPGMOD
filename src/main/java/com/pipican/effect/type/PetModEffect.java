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
 * {@code scaling} applies to every amount.
 */
public record PetModEffect(double damage, double health, double hearts, double armor, double damageTaken, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<PetModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("damage", 0.0).forGetter(PetModEffect::damage),
            Codec.DOUBLE.optionalFieldOf("health", 0.0).forGetter(PetModEffect::health),
            Codec.DOUBLE.optionalFieldOf("hearts", 0.0).forGetter(PetModEffect::hearts),
            Codec.DOUBLE.optionalFieldOf("armor", 0.0).forGetter(PetModEffect::armor),
            Codec.DOUBLE.optionalFieldOf("damage_taken", 0.0).forGetter(PetModEffect::damageTaken),
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
