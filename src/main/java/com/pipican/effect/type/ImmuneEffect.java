package com.pipican.effect.type;

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
 * {@code pipican:immune} - these potion effects can't be applied to the player (and are removed if present).
 *
 * <pre>{@code { "type": "pipican:immune", "effects": [ "minecraft:poison", "minecraft:wither" ] } }</pre>
 * For immunity to a kind of damage, use {@code pipican:damage_taken} with {@code "multiplier": 0}.
 */
public record ImmuneEffect(HolderSet<MobEffect> effects) implements ClassEffect {

    public static final MapCodec<ImmuneEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).fieldOf("effects").forGetter(ImmuneEffect::effects)
    ).apply(i, ImmuneEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.IMMUNE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addImmunity(effects);
    }
}
