package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;

/**
 * {@code pipican:hurt_self} - the user takes {@code amount} magic damage (a backlash). A reaction upgrade's
 * {@code damage} adds to it, so -1 cancels a 1-damage backlash.
 *
 * <pre>{@code { "type": "pipican:hurt_self", "amount": 1 } }</pre>
 */
public record HurtSelfAction(double amount) implements AbilityAction {

    public static final MapCodec<HurtSelfAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 1000).fieldOf("amount").forGetter(HurtSelfAction::amount)
    ).apply(i, HurtSelfAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.HURT_SELF;
    }

    @Override
    public void run(AbilityContext ctx) {
        double total = amount + ctx.mod().damage();
        if (total > 0) {
            ctx.player().hurtServer(ctx.level(), ctx.level().damageSources().magic(), (float) total);
        }
    }
}
