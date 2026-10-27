package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.pipican.condition.Bounds;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:light} - light level at the player's feet (0-15, sky light dimmed by time of day).
 *
 * <pre>{@code { "type": "pipican:light", "max": 8 } }   light level 7 or lower</pre>
 */
public record LightCondition(Bounds bounds) implements ClassCondition {

    public static final MapCodec<LightCondition> CODEC = Bounds.MAP_CODEC.xmap(LightCondition::new, LightCondition::bounds);

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.LIGHT;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return bounds.test(player.level().getMaxLocalRawBrightness(player.blockPosition()));
    }
}
