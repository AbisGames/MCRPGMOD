package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;

/**
 * {@code pipican:cloud} - leaves a lingering cloud of {@code effect} (like a lingering potion) where the
 * reaction's mob is (or at the user), {@code radius} blocks wide for {@code seconds}.
 *
 * <pre>{@code { "type": "pipican:cloud", "effect": "minecraft:instant_damage", "seconds": 5, "radius": 3 } }</pre>
 */
public record CloudAction(Holder<MobEffect> effect, int amplifier, double seconds, double radius) implements AbilityAction {

    public static final MapCodec<CloudAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(CloudAction::effect),
            Codec.intRange(0, 9).optionalFieldOf("amplifier", 0).forGetter(CloudAction::amplifier),
            Codec.doubleRange(0.5, 120).optionalFieldOf("seconds", 5.0).forGetter(CloudAction::seconds),
            Codec.doubleRange(0.5, 16).optionalFieldOf("radius", 3.0).forGetter(CloudAction::radius)
    ).apply(i, CloudAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CLOUD;
    }

    @Override
    public void run(AbilityContext ctx) {
        var at = ctx.subject().map(e -> e.position()).orElse(ctx.player().position());
        AreaEffectCloud cloud = new AreaEffectCloud(ctx.level(), at.x, at.y, at.z);
        cloud.setOwner(ctx.player());
        cloud.setRadius((float) radius);
        cloud.setDuration((int) Math.round(seconds * 20));
        cloud.setRadiusPerTick(-(float) radius / cloud.getDuration() / 2);
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(effect, effect.value().isInstantenous() ? 1 : 100, amplifier));
        ctx.level().addFreshEntity(cloud);
    }
}
