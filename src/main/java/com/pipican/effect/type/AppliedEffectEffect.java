package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import com.pipican.player.ClassState;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:applied_effect} - when you give another creature {@code effect} (tipped arrows, thrown potions,
 * abilities...), it lasts {@code seconds} longer and {@code duration} more (0.05 = +5%), is at least level
 * {@code min_amplifier} + 1, and also brings the effects in {@code also} (effect -> amplifier) for as long.
 *
 * <pre>{@code { "type": "pipican:applied_effect", "effect": "minecraft:poison", "seconds": 1, "also": { "minecraft:slowness": 0 } } }</pre>
 * {@code scaling} applies to {@code seconds} and {@code duration}. Adds up over effects.
 */
public record AppliedEffectEffect(Holder<MobEffect> effect, double seconds, double duration, int minAmplifier,
                                  Map<Holder<MobEffect>, Integer> also, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<AppliedEffectEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(AppliedEffectEffect::effect),
            Codec.DOUBLE.optionalFieldOf("seconds", 0.0).forGetter(AppliedEffectEffect::seconds),
            Codec.DOUBLE.optionalFieldOf("duration", 0.0).forGetter(AppliedEffectEffect::duration),
            Codec.intRange(0, 9).optionalFieldOf("min_amplifier", 0).forGetter(AppliedEffectEffect::minAmplifier),
            Codec.unboundedMap(BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), Codec.intRange(0, 9)).optionalFieldOf("also", Map.of())
                    .forGetter(AppliedEffectEffect::also),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(AppliedEffectEffect::scaling)
    ).apply(i, AppliedEffectEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.APPLIED_EFFECT;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addAppliedRule(effect, new ClassState.AppliedRule(Scaling.evaluate(seconds, scaling, ctx), Scaling.evaluate(duration, scaling, ctx),
                minAmplifier, also));
    }
}
