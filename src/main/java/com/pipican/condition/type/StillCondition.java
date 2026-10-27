package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Movement;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:still} - the player hasn't moved for {@code seconds} (default 1). Use {@code not} for "while moving".
 *
 * <pre>{@code { "type": "pipican:still", "seconds": 3 } }</pre>
 */
public record StillCondition(double seconds) implements ClassCondition {

    public static final MapCodec<StillCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 3600).optionalFieldOf("seconds", 1.0).forGetter(StillCondition::seconds)
    ).apply(i, StillCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.STILL;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return Movement.stillFor(player) >= Math.max(10, seconds * 20);
    }
}
