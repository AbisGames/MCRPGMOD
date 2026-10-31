package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:cleave} - your melee hits (fully charged ones, with {@code charged_only}) also hit mobs within
 * {@code radius} blocks of the target for {@code fraction} of the damage, at most {@code max_targets} of them.
 * Fractions and radii add up over effects ("sweep damage 60%" = {@code fraction: 0.2} on top of 40%).
 *
 * <pre>{@code { "type": "pipican:cleave", "fraction": 0.4, "radius": 2, "charged_only": true, "weapons": "#pipican:weapons/axes" } }</pre>
 */
public record CleaveEffect(double fraction, double radius, int maxTargets, boolean chargedOnly, Optional<HolderSet<Item>> weapons,
                           List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<CleaveEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 10).optionalFieldOf("fraction", 0.0).forGetter(CleaveEffect::fraction),
            Codec.doubleRange(0, 16).optionalFieldOf("radius", 0.0).forGetter(CleaveEffect::radius),
            Codec.intRange(0, 64).optionalFieldOf("max_targets", 0).forGetter(CleaveEffect::maxTargets),
            Codec.BOOL.optionalFieldOf("charged_only", false).forGetter(CleaveEffect::chargedOnly),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(CleaveEffect::weapons),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(CleaveEffect::scaling)
    ).apply(i, CleaveEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.CLEAVE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addCleave(Scaling.evaluate(fraction, scaling, ctx), radius, maxTargets, chargedOnly, weapons);
    }
}
