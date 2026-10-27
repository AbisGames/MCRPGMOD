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
import net.minecraft.world.entity.EntityType;

/**
 * {@code pipican:ignored_by} - these mobs never target you; with {@code unless_attacked}, a mob you hurt
 * may fight back.
 *
 * <pre>{@code { "type": "pipican:ignored_by", "mobs": "#minecraft:undead", "unless_attacked": true } }</pre>
 */
public record IgnoredByEffect(HolderSet<EntityType<?>> mobs, boolean unlessAttacked) implements ClassEffect {

    public static final MapCodec<IgnoredByEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("mobs").forGetter(IgnoredByEffect::mobs),
            Codec.BOOL.optionalFieldOf("unless_attacked", false).forGetter(IgnoredByEffect::unlessAttacked)
    ).apply(i, IgnoredByEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.IGNORED_BY;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addIgnoredBy(this);
    }
}
