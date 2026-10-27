package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.pipican.ability.FavoredWeapon;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.server.level.ServerPlayer;

/** {@code pipican:holding_favored} - your main hand holds your favored weapon type (see {@code pipican:favored_weapon}). */
public record HoldingFavoredCondition() implements ClassCondition {

    public static final MapCodec<HoldingFavoredCondition> CODEC = MapCodec.unit(HoldingFavoredCondition::new);

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.HOLDING_FAVORED;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return FavoredWeapon.holding(player);
    }
}
