package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.player.ClassState;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:nth_hit} - every {@code every}-th melee swing (on the same mob in a row with {@code same_target};
 * only with {@code weapons} if given) deals {@code multiplier} times the damage and, with {@code strike_twice},
 * hits the mob a second time for the same damage.
 *
 * <pre>{@code { "type": "pipican:nth_hit", "every": 4, "same_target": true, "multiplier": 1.25 } }</pre>
 */
public record NthHitEffect(int every, boolean sameTarget, double multiplier, boolean strikeTwice, Optional<HolderSet<Item>> weapons) implements ClassEffect {

    public static final MapCodec<NthHitEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(2, 100).fieldOf("every").forGetter(NthHitEffect::every),
            Codec.BOOL.optionalFieldOf("same_target", false).forGetter(NthHitEffect::sameTarget),
            Codec.doubleRange(0, 10).optionalFieldOf("multiplier", 1.0).forGetter(NthHitEffect::multiplier),
            Codec.BOOL.optionalFieldOf("strike_twice", false).forGetter(NthHitEffect::strikeTwice),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(NthHitEffect::weapons)
    ).apply(i, NthHitEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.NTH_HIT;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addNthHit(new ClassState.NthHit(every, sameTarget, multiplier, strikeTwice, weapons));
    }
}
