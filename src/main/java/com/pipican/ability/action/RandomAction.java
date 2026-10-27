package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.List;

/**
 * {@code pipican:random} - runs one of the {@code choices} (each a list of actions), picked at random
 * ("curse a mob with one of: Desire, Lethargy or Pain").
 */
public record RandomAction(List<List<AbilityAction>> choices) implements AbilityAction {

    // Lazy: actions contain actions.
    public static final MapCodec<RandomAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().listOf().fieldOf("choices").forGetter(RandomAction::choices)
    ).apply(i, RandomAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RANDOM;
    }

    @Override
    public void run(AbilityContext ctx) {
        if (choices.isEmpty()) {
            return;
        }
        for (AbilityAction action : choices.get(ctx.player().getRandom().nextInt(choices.size()))) {
            action.run(ctx);
            if (ctx.isCancelled()) {
                return;
            }
        }
    }
}
