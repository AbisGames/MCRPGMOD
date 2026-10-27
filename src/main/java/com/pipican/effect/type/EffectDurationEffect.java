package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:effect_duration} - the listed effects last {@code multiplier} times as long on you
 * (2 = twice as long, 0.5 = wear off twice as fast). Multipliers multiply.
 *
 * <pre>{@code { "type": "pipican:effect_duration", "effects": "minecraft:hero_of_the_village", "multiplier": 2 } }</pre>
 */
public record EffectDurationEffect(HolderSet<MobEffect> effects, double multiplier) implements ClassEffect {

    public static final MapCodec<EffectDurationEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects").forGetter(EffectDurationEffect::effects),
            Codec.doubleRange(0, 100).fieldOf("multiplier").forGetter(EffectDurationEffect::multiplier)
    ).apply(i, EffectDurationEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.EFFECT_DURATION;
    }

    @Override
    public void apply(EffectContext ctx) {
        double k = 1 + (multiplier - 1) * ctx.rank();
        effects.forEach(effect -> ctx.addEffectDuration(effect, k));
    }
}
