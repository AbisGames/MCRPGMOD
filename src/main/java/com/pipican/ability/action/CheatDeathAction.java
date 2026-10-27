package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.network.chat.Component;

/**
 * {@code pipican:cheat_death} - in a {@code death} reaction, you don't die: you're left at {@code health}
 * ("once per rest, when you would die, go Berserk instead with 4 HP"). Put it last: it ends the reaction's actions.
 */
public record CheatDeathAction(double health) implements AbilityAction {

    public static final MapCodec<CheatDeathAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 1000).optionalFieldOf("health", 1.0).forGetter(CheatDeathAction::health)
    ).apply(i, CheatDeathAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CHEAT_DEATH;
    }

    @Override
    public void run(AbilityContext ctx) {
        ctx.player().setHealth((float) Math.min(health, ctx.player().getMaxHealth()));
        ctx.cancel(Component.translatable("pipican.cheated_death"));
    }
}
