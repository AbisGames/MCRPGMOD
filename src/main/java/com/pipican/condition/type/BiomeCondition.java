package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

/**
 * {@code pipican:biome} - the player stands in one of these biomes (an id, a list, or a {@code #tag}).
 *
 * <pre>{@code { "type": "pipican:biome", "biomes": "#minecraft:is_forest" } }</pre>
 */
public record BiomeCondition(HolderSet<Biome> biomes) implements ClassCondition {

    public static final MapCodec<BiomeCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(BiomeCondition::biomes)
    ).apply(i, BiomeCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.BIOME;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return biomes.contains(player.level().getBiome(player.blockPosition()));
    }
}
