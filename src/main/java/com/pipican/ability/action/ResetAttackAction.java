package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;

/**
 * {@code pipican:reset_attack} - your attack cooldown is over at once: the next swing is fully charged
 * (Brutal Swing: "when you kill a mob with an axe or mace, your attack cooldown resets instantly").
 */
public record ResetAttackAction() implements AbilityAction {

    public static final MapCodec<ResetAttackAction> CODEC = MapCodec.unit(ResetAttackAction::new);

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RESET_ATTACK;
    }

    @Override
    public void run(AbilityContext ctx) {
        ctx.player().attackStrengthTicker = Math.max(ctx.player().attackStrengthTicker, 1000);
    }
}
