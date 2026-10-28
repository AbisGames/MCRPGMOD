package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Ability;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import com.pipican.player.ClassState;
import com.pipican.core.PipicanRegistries;
import java.util.List;
import net.minecraft.resources.ResourceKey;

/**
 * {@code pipican:ability_mod} - upgrades one ability: {@code cooldown_seconds} added to its cooldown,
 * {@code cost} / {@code drain} change its resource cost / toggle drain (-0.3 = 30% less), {@code duration}
 * changes how long its state lasts (1 = twice as long), {@code radius} blocks are added to its areas,
 * {@code uses} more uses per rest, {@code strength} makes its "while active" bonuses stronger (0.5 = +50%), its effects and
 * controls last {@code seconds} longer, and {@code actions} run after its own ("charmed mobs also get Weakness I").
 *
 * <pre>{@code { "type": "pipican:ability_mod", "ability": "pipican:miracle_worker/stigmata", "duration": 1 } }</pre>
 * {@code scaling} applies to every amount. Mods for the same ability add up.
 */
public record AbilityModEffect(ResourceKey<Ability> ability, double cooldownSeconds, double cost, double drain, double duration,
                               double radius, int uses, double strength, List<Scaling> scaling, double seconds,
                               List<com.pipican.ability.AbilityAction> actions) implements ClassEffect {

    public static final MapCodec<AbilityModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.ABILITY).fieldOf("ability").forGetter(AbilityModEffect::ability),
            Codec.DOUBLE.optionalFieldOf("cooldown_seconds", 0.0).forGetter(AbilityModEffect::cooldownSeconds),
            Codec.DOUBLE.optionalFieldOf("cost", 0.0).forGetter(AbilityModEffect::cost),
            Codec.DOUBLE.optionalFieldOf("drain", 0.0).forGetter(AbilityModEffect::drain),
            Codec.DOUBLE.optionalFieldOf("duration", 0.0).forGetter(AbilityModEffect::duration),
            Codec.DOUBLE.optionalFieldOf("radius", 0.0).forGetter(AbilityModEffect::radius),
            Codec.intRange(0, 100).optionalFieldOf("uses", 0).forGetter(AbilityModEffect::uses),
            Codec.DOUBLE.optionalFieldOf("strength", 0.0).forGetter(AbilityModEffect::strength),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(AbilityModEffect::scaling),
            Codec.DOUBLE.optionalFieldOf("seconds", 0.0).forGetter(AbilityModEffect::seconds),
            Codec.lazyInitialized(() -> com.pipican.ability.AbilityAction.CODEC).listOf().optionalFieldOf("actions", List.of()).forGetter(AbilityModEffect::actions)
    ).apply(i, AbilityModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.ABILITY_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addAbilityMod(ability, new ClassState.AbilityMod(Scaling.evaluate(cooldownSeconds, scaling, ctx), Scaling.evaluate(cost, scaling, ctx),
                Scaling.evaluate(drain, scaling, ctx), Scaling.evaluate(duration, scaling, ctx), Scaling.evaluate(radius, scaling, ctx), uses,
                Scaling.evaluate(strength, scaling, ctx), Scaling.evaluate(seconds, scaling, ctx), actions));
    }
}
