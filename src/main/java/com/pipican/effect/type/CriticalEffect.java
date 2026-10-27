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
 * {@code pipican:critical} - melee critical hits (the vanilla falling-hit crit).
 *
 * <pre>{@code { "type": "pipican:critical", "damage": 0.1, "chance": 0.05, "weapons": "#pipican:weapons/swords" } }</pre>
 * {@code damage}: crits deal that much more (0.1 = +10% critical damage). {@code chance}: the chance that
 * a hit that isn't a crit becomes one. Both add up over effects and can scale ({@code scaling} applies to
 * {@code damage}; {@code chance_scaling} to {@code chance}).
 */
public record CriticalEffect(double damage, double chance, Optional<HolderSet<Item>> weapons, List<Scaling> scaling, List<Scaling> chanceScaling,
                             Optional<com.pipican.effect.VictimState> victim)
        implements ClassEffect {

    public static final MapCodec<CriticalEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(-1, 100).optionalFieldOf("damage", 0.0).forGetter(CriticalEffect::damage),
            Codec.doubleRange(0, 1).optionalFieldOf("chance", 0.0).forGetter(CriticalEffect::chance),
            RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("weapons").forGetter(CriticalEffect::weapons),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(CriticalEffect::scaling),
            Scaling.LIST_CODEC.optionalFieldOf("chance_scaling", List.of()).forGetter(CriticalEffect::chanceScaling),
            com.pipican.effect.VictimState.CODEC.optionalFieldOf("victim").forGetter(CriticalEffect::victim)
    ).apply(i, CriticalEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.CRITICAL;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addCritical(Scaling.evaluate(damage, scaling, ctx) * ctx.rank(), Scaling.evaluate(chance, chanceScaling, ctx) * ctx.rank(), weapons, victim);
    }
}
