package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;

/** {@code pipican:cure} - the reaction's zombie villager turns back into a villager at once (Exorcism). */
public record CureAction() implements AbilityAction {

    public static final MapCodec<CureAction> CODEC = MapCodec.unit(CureAction::new);

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CURE;
    }

    @Override
    public void run(AbilityContext ctx) {
        ctx.subject().filter(e -> e instanceof ZombieVillager z && !z.isConverting())
                .ifPresent(e -> ((ZombieVillager) e).startConverting(ctx.player().getUUID(), 1));
    }
}
