package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Boosts;
import com.pipican.effect.ClassEffect;
import java.util.List;

/**
 * {@code pipican:boost} - gives the player class effects for a few seconds. Mostly used in {@code pipican:on}
 * reactions: "after you take damage, +15% movement speed for 2s".
 *
 * <pre>{@code { "type": "pipican:boost", "seconds": 2, "effects": [
 *     { "type": "pipican:attribute", "attribute": "minecraft:movement_speed", "amount": 0.15, "operation": "add_multiplied_base" } ] } }</pre>
 * {@code max_stacks} (default 1): triggering it again while it runs adds a stack - the effects count that many
 * times ("each hit gives +1% speed for 3s, stacks 10") - and restarts the timer.
 */
public record BoostAction(double seconds, List<ClassEffect> effects, int maxStacks) implements AbilityAction {

    // Lazy: class effects can contain actions (pipican:on), so this codec is built while they initialise.
    public static final MapCodec<BoostAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.05, 3600).fieldOf("seconds").forGetter(BoostAction::seconds),
            Codec.lazyInitialized(() -> ClassEffect.CODEC).listOf().fieldOf("effects").forGetter(BoostAction::effects),
            Codec.intRange(1, 100).optionalFieldOf("max_stacks", 1).forGetter(BoostAction::maxStacks)
    ).apply(i, BoostAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BOOST;
    }

    @Override
    public void run(AbilityContext ctx) {
        Boosts.add(ctx.player(), this, effects, (int) Math.round(Math.max(0.05, seconds + ctx.mod().seconds()) * 20), maxStacks);
    }
}
