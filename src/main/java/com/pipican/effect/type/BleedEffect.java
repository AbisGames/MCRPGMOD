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
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:bleed} - your critical hits (or every hit, with {@code any_hit}) make mobs bleed: they take
 * {@code damage} per second for {@code seconds}. Every bleed effect the player has adds up, so later ones can be
 * pure upgrades ({@code "seconds": 1} = bleed lasts 1s longer).
 *
 * <pre>{@code { "type": "pipican:bleed", "damage": 1, "seconds": 4, "weapons": "#pipican:weapons/swords" } }</pre>
 * {@code max_stacks}: how many bleeds can run on one mob at once (the highest value counts); {@code slows}: bleeding
 * mobs get Slowness I. {@code scaling} applies to {@code seconds}.
 */
public record BleedEffect(double damage, double seconds, int maxStacks, boolean anyHit, boolean slows,
                          Optional<HolderSet<Item>> weapons, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<BleedEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 1000).optionalFieldOf("damage", 0.0).forGetter(BleedEffect::damage),
            Codec.doubleRange(0, 600).optionalFieldOf("seconds", 0.0).forGetter(BleedEffect::seconds),
            Codec.intRange(0, 20).optionalFieldOf("max_stacks", 0).forGetter(BleedEffect::maxStacks),
            Codec.BOOL.optionalFieldOf("any_hit", false).forGetter(BleedEffect::anyHit),
            Codec.BOOL.optionalFieldOf("slows", false).forGetter(BleedEffect::slows),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(BleedEffect::weapons),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(BleedEffect::scaling)
    ).apply(i, BleedEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.BLEED;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addBleed(damage * ctx.rank(), Scaling.evaluate(seconds, scaling, ctx) * ctx.rank(), maxStacks, anyHit, slows, weapons);
    }
}
