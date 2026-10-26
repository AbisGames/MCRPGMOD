package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:mob_effect} - a permanent potion effect (e.g. night vision for a rogue).
 *
 * <pre>{@code { "type": "pipican:mob_effect", "effect": "minecraft:night_vision", "amplifier": 0 } }</pre>
 * Re-applied automatically if removed (milk, death). Amplifier grows with rank - 1.
 */
public record MobEffectEffect(Holder<MobEffect> effect, int amplifier) implements ClassEffect {

    public static final MapCodec<MobEffectEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(MobEffectEffect::effect),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(MobEffectEffect::amplifier)
    ).apply(i, MobEffectEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.MOB_EFFECT;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addMobEffect(effect, amplifier + ctx.rank() - 1);
    }
}
