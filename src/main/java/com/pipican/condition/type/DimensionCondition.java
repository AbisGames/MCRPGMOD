package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * {@code pipican:dimension} - the player is in one of these dimensions.
 *
 * <pre>{@code { "type": "pipican:dimension", "dimensions": [ "minecraft:the_nether" ] } }</pre>
 */
public record DimensionCondition(List<ResourceKey<Level>> dimensions) implements ClassCondition {

    public static final MapCodec<DimensionCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Level.RESOURCE_KEY_CODEC.listOf().fieldOf("dimensions").forGetter(DimensionCondition::dimensions)
    ).apply(i, DimensionCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.DIMENSION;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return dimensions.contains(player.level().dimension());
    }
}
