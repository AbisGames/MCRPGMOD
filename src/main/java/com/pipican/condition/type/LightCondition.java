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
        // Cloak of Shadows: "you count as being in darkness" for darkness checks (a max with no min).
        if (bounds.max().isPresent() && bounds.min().isEmpty()
                && com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.ALWAYS_DARK) > 0) {
            return true;
        }
        return bounds.test(player.level().getMaxLocalRawBrightness(player.blockPosition()));
    }
}
