package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.world.entity.ExperienceOrb;

/**
 * {@code pipican:experience} - drops {@code amount} experience points as orbs where the reaction's mob is
 * (or at the user).
 *
 * <pre>{@code { "type": "pipican:experience", "amount": 2 } }</pre>
 */
public record ExperienceAction(int amount) implements AbilityAction {

    public static final MapCodec<ExperienceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 1000).fieldOf("amount").forGetter(ExperienceAction::amount)
    ).apply(i, ExperienceAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.EXPERIENCE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var at = ctx.subject().map(e -> e.position()).orElse(ctx.player().position());
        ExperienceOrb.award(ctx.level(), at, amount);
    }
}
