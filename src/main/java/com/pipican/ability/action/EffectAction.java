package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:effect} - gives potion effects to the targets.
 *
 * <pre>{@code { "type": "pipican:effect", "target": "hostiles", "radius": 10,
 *   "effects": [ { "effect": "minecraft:weakness", "seconds": 6 }, { "effect": "minecraft:slowness", "seconds": 6, "amplifier": 1 } ] } }</pre>
 * A "stun" is Slowness with a high amplifier (e.g. 9) for a short time.
 */
public record EffectAction(Targets targets, List<Entry> effects) implements AbilityAction {

    public record Entry(Holder<MobEffect> effect, double seconds, int amplifier) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(Entry::effect),
                Codec.doubleRange(0.05, 3600).fieldOf("seconds").forGetter(Entry::seconds),
                Codec.intRange(0, 127).optionalFieldOf("amplifier", 0).forGetter(Entry::amplifier)
        ).apply(i, Entry::new));

        public MobEffectInstance create() {
            return new MobEffectInstance(effect, (int) Math.round(seconds * 20), amplifier);
        }
    }

    public static final MapCodec<EffectAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(EffectAction::targets),
            Entry.CODEC.listOf().fieldOf("effects").forGetter(EffectAction::effects)
    ).apply(i, EffectAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.EFFECT;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx.player())) {
            for (Entry e : effects) {
                target.addEffect(e.create(), ctx.player());
            }
        }
    }
}
