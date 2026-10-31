package com.pipican.condition;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;

/**
 * A yes/no question about the player right now ("is it night?", "below half health?"), used by
 * {@code pipican:conditional} effects. Chosen in JSON by {@code "type"}, e.g.
 * <pre>{@code { "type": "pipican:health", "max": 0.5 } }</pre>
 * Built-in types live in {@link ConditionTypes}; adding one is a small class plus one line there.
 *
 * <p>Conditions are re-tested twice a second, and the player's bonuses are rebuilt only when an
 * answer changes, so {@link #test} should be cheap.
 */
public interface ClassCondition {

    Codec<ClassCondition> CODEC = ConditionTypes.TYPE_CODEC.dispatch("type", ClassCondition::type, ConditionTypes.Type::codec);

    ConditionTypes.Type type();

    boolean test(ServerPlayer player);
}
