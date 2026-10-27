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
import net.minecraft.world.entity.EntityType;

/**
 * {@code pipican:execute} - a mob your hit leaves below {@code threshold} of its max health dies (never players or bosses).
 *
 * <pre>{@code { "type": "pipican:execute", "threshold": 0.15, "victims": "#pipican:mobs/unclean" } }</pre>
 * The highest matching threshold counts; {@code scaling} raises it ("Execute threshold +0.1% per Severance").
 */
public record ExecuteEffect(double threshold, Optional<HolderSet<EntityType<?>>> victims, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<ExecuteEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("threshold", 0.0).forGetter(ExecuteEffect::threshold),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("victims").forGetter(ExecuteEffect::victims),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(ExecuteEffect::scaling)
    ).apply(i, ExecuteEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.EXECUTE;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addExecute(Scaling.evaluate(threshold, scaling, ctx), victims);
    }
}
