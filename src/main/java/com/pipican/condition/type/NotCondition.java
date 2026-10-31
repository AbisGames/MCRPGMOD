package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:not} - the opposite of another condition.
 *
 * <pre>{@code { "type": "pipican:not", "condition": { "type": "pipican:state", "state": "sees_sky" } } }</pre>
 */
public record NotCondition(ClassCondition condition) implements ClassCondition {

    // Lazy: this codec is built while ClassCondition.CODEC itself is still initialising.
    public static final MapCodec<NotCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.lazyInitialized(() -> ClassCondition.CODEC).fieldOf("condition").forGetter(NotCondition::condition)
    ).apply(i, NotCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.NOT;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return !condition.test(player);
    }
}
