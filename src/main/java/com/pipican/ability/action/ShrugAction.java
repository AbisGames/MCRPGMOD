package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.network.chat.Component;

/**
 * {@code pipican:shrug} - in a {@code low_health} reaction, the hit that would have dropped you low misses entirely
 * (Zeal: "30% chance to shrug it off"). Put it last: it ends the reaction's actions.
 */
public record ShrugAction() implements AbilityAction {

    public static final MapCodec<ShrugAction> CODEC = MapCodec.unit(ShrugAction::new);

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.SHRUG;
    }

    @Override
    public void run(AbilityContext ctx) {
        ctx.cancel(Component.translatable("pipican.shrugged"));
    }
}
