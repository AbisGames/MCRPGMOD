package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:effect} - the player has one of these potion effects (an id, a list, or a {@code #tag}).
 * The class system's own permanent effects count too.
 *
 * <pre>{@code { "type": "pipican:effect", "effects": "minecraft:slowness" } }</pre>
 */
public record EffectCondition(HolderSet<MobEffect> effects) implements ClassCondition {

    public static final MapCodec<EffectCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects").forGetter(EffectCondition::effects)
    ).apply(i, EffectCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.EFFECT;
    }

    @Override
    public boolean test(ServerPlayer player) {
        for (var effect : effects) {
            if (player.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }
}
