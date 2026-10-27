package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.pipican.condition.Bounds;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:health} - current health as a fraction of max health (0-1).
 *
 * <pre>{@code { "type": "pipican:health", "max": 0.5 } }   below half health</pre>
 */
public record HealthCondition(Bounds bounds) implements ClassCondition {

    public static final MapCodec<HealthCondition> CODEC = Bounds.MAP_CODEC.xmap(HealthCondition::new, HealthCondition::bounds);

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.HEALTH;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return player.getMaxHealth() > 0 && bounds.test(player.getHealth() / player.getMaxHealth());
    }
}
