package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:heal} - heals the targets by {@code amount} HP plus {@code percent} of their max health.
 *
 * <pre>{@code { "type": "pipican:heal", "target": "allies", "radius": 8, "amount": 6 } }</pre>
 */
public record HealAction(Targets targets, double amount, double percent) implements AbilityAction {

    public static final MapCodec<HealAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(HealAction::targets),
            Codec.doubleRange(0, 10_000).optionalFieldOf("amount", 0.0).forGetter(HealAction::amount),
            Codec.doubleRange(0, 1).optionalFieldOf("percent", 0.0).forGetter(HealAction::percent)
    ).apply(i, HealAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.HEAL;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            target.heal((float) (amount + percent * target.getMaxHealth()));
        }
    }
}
