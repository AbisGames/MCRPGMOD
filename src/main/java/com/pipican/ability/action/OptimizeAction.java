package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Optimize;

/**
 * {@code pipican:optimize} - offers the Optimization choice again right now (Alter Self). See
 * {@code pipican:optimization}.
 *
 * <pre>{@code { "type": "pipican:optimize" } }</pre>
 */
public record OptimizeAction(boolean unused) implements AbilityAction {

    public static final MapCodec<OptimizeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("unused", false).forGetter(OptimizeAction::unused)
    ).apply(i, OptimizeAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.OPTIMIZE;
    }

    @Override
    public void run(AbilityContext ctx) {
        Optimize.prompt(ctx.player());
    }
}
