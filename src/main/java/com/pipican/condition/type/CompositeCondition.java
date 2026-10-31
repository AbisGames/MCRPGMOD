package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:all_of} / {@code pipican:any_of} - every / at least one of several conditions.
 *
 * <pre>{@code { "type": "pipican:any_of", "conditions": [ { ... }, { ... } ] } }</pre>
 */
public record CompositeCondition(boolean all, List<ClassCondition> conditions) implements ClassCondition {

    public static final MapCodec<CompositeCondition> ALL_CODEC = codec(true);
    public static final MapCodec<CompositeCondition> ANY_CODEC = codec(false);

    private static MapCodec<CompositeCondition> codec(boolean all) {
        // Lazy: built while ClassCondition.CODEC itself is still initialising.
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.lazyInitialized(() -> ClassCondition.CODEC).listOf().fieldOf("conditions").forGetter(CompositeCondition::conditions)
        ).apply(i, list -> new CompositeCondition(all, list)));
    }

    @Override
    public ConditionTypes.Type type() {
        return all ? ConditionTypes.ALL_OF : ConditionTypes.ANY_OF;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return all ? conditions.stream().allMatch(c -> c.test(player)) : conditions.stream().anyMatch(c -> c.test(player));
    }
}
