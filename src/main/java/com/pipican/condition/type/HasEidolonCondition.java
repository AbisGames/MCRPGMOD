package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import com.pipican.entity.Eidolon;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code pipican:has_eidolon} - the player's eidolon exists and is loaded. Use {@code not} for
 * "while your eidolon is destroyed".
 */
public record HasEidolonCondition() implements ClassCondition {

    public static final MapCodec<HasEidolonCondition> CODEC = MapCodec.unit(HasEidolonCondition::new);

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.HAS_EIDOLON;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return Eidolon.of(player).isPresent();
    }
}
