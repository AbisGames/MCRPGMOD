package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * {@code pipican:counter} - an instant counterattack: the targets take the user's attack damage at once, as a melee
 * hit (stronger by the reaction's strength upgrades: +0.5 = a critical's +50%).
 *
 * <pre>{@code { "type": "pipican:counter", "target": "victim" } }</pre>
 */
public record CounterAction(Targets targets) implements AbilityAction {

    public static final MapCodec<CounterAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.VICTIM).forGetter(CounterAction::targets)
    ).apply(i, CounterAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.COUNTER;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1 + ctx.mod().strength()));
        for (LivingEntity target : targets.select(ctx)) {
            if (target != player && target.isAlive()) {
                target.invulnerableTime = 0;
                target.hurtServer(ctx.level(), ctx.level().damageSources().playerAttack(player), damage);
                player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            }
        }
    }
}
