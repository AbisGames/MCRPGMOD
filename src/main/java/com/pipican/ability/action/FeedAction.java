package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;

/**
 * {@code pipican:feed} - restores {@code hunger} food points (and {@code saturation}) to the user ("eating berries
 * restores +1 hunger").
 *
 * <pre>{@code { "type": "pipican:feed", "hunger": 1 } }</pre>
 */
public record FeedAction(int hunger, double saturation) implements AbilityAction {

    public static final MapCodec<FeedAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 20).optionalFieldOf("hunger", 0).forGetter(FeedAction::hunger),
            Codec.doubleRange(0, 20).optionalFieldOf("saturation", 0.0).forGetter(FeedAction::saturation)
    ).apply(i, FeedAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.FEED;
    }

    @Override
    public void run(AbilityContext ctx) {
        ctx.player().getFoodData().eat(hunger, (float) saturation);
    }
}
