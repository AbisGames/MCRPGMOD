package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import com.pipican.player.ClassState;
import java.util.List;

/**
 * {@code pipican:reaction_mod} - upgrades the {@code pipican:on} reactions called {@code name}: their effects,
 * boosts and controls last {@code seconds} longer, their areas get {@code radius} blocks wider, their heals heal
 * {@code heal} more and their damage deals {@code damage} more, their boosts and empowered hits are {@code strength}
 * stronger (0.5 = +50% of the bonus), {@code amplify} (effect -> level, 1 = II) raises the level of effects they give,
 * {@code cooldown} changes their cooldown (-0.5 = half), {@code hits} adds empowered hits, and {@code actions} run after
 * theirs.
 *
 * <pre>{@code { "type": "pipican:reaction_mod", "name": "guarded casting", "seconds": 5 } }</pre>
 * {@code scaling} applies to every amount the effect sets. Mods with the same name add up.
 */
public record ReactionModEffect(String name, java.util.Optional<Double> seconds, java.util.Optional<Double> radius, java.util.Optional<Double> heal, java.util.Optional<Double> damage, List<AbilityAction> actions,
                                List<Scaling> scaling, java.util.Optional<Double> strength,
                                java.util.Map<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>, Integer> amplify,
                                java.util.Optional<Double> cooldown, int hits) implements ClassEffect {

    public static final MapCodec<ReactionModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(ReactionModEffect::name),
            Codec.DOUBLE.optionalFieldOf("seconds").forGetter(ReactionModEffect::seconds),
            Codec.DOUBLE.optionalFieldOf("radius").forGetter(ReactionModEffect::radius),
            Codec.DOUBLE.optionalFieldOf("heal").forGetter(ReactionModEffect::heal),
            Codec.DOUBLE.optionalFieldOf("damage").forGetter(ReactionModEffect::damage),
            Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().optionalFieldOf("actions", List.of()).forGetter(ReactionModEffect::actions),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ReactionModEffect::scaling),
            Codec.DOUBLE.optionalFieldOf("strength").forGetter(ReactionModEffect::strength),
            Codec.unboundedMap(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), Codec.intRange(0, 9)).optionalFieldOf("amplify", java.util.Map.of()).forGetter(ReactionModEffect::amplify),
            Codec.DOUBLE.optionalFieldOf("cooldown").forGetter(ReactionModEffect::cooldown),
            Codec.intRange(-100, 100).optionalFieldOf("hits", 0).forGetter(ReactionModEffect::hits)
    ).apply(i, ReactionModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.REACTION_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addReactionMod(name.toLowerCase(java.util.Locale.ROOT), new ClassState.ReactionMod(Scaling.evaluate(seconds, scaling, ctx),
                Scaling.evaluate(radius, scaling, ctx), Scaling.evaluate(heal, scaling, ctx), Scaling.evaluate(damage, scaling, ctx), actions,
                Scaling.evaluate(strength, scaling, ctx), amplify, Scaling.evaluate(cooldown, scaling, ctx), hits));
    }
}
