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
 * times ("each hit gives +1% speed for 3s, stacks 10") - and restarts the timer. {@code until_sleep} boosts also end when
 * the player wakes up ("lowers your max health by 2 until you sleep"). With {@code others} above 0 the boost goes to the
 * other players within that many blocks instead of the user ("also gives +2 armor to players within 6 blocks").
 */
public record BoostAction(double seconds, List<ClassEffect> effects, int maxStacks, boolean untilSleep, double others) implements AbilityAction {

    // Lazy: class effects can contain actions (pipican:on), so this codec is built while they initialise.
    public static final MapCodec<BoostAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.05, 86_400).fieldOf("seconds").forGetter(BoostAction::seconds),
            Codec.lazyInitialized(() -> ClassEffect.CODEC).listOf().fieldOf("effects").forGetter(BoostAction::effects),
            Codec.intRange(1, 100).optionalFieldOf("max_stacks", 1).forGetter(BoostAction::maxStacks),
            Codec.BOOL.optionalFieldOf("until_sleep", false).forGetter(BoostAction::untilSleep),
            Codec.doubleRange(0, 64).optionalFieldOf("others", 0.0).forGetter(BoostAction::others)
    ).apply(i, BoostAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.BOOST;
    }

    @Override
    public void run(AbilityContext ctx) {
        double k = 1 + ctx.mod().strength();
        List<ClassEffect> given = k == 1 ? effects : effects.stream().<ClassEffect>map(e -> new com.pipican.effect.ScaledEffect(e, k, null)).toList();
        int ticks = (int) Math.round(Math.max(0.05, seconds + ctx.mod().seconds()) * 20);
        if (others > 0) {
            for (var other : ctx.level().getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, ctx.player().getBoundingBox().inflate(others),
                    p -> p != ctx.player() && p.isAlive() && p.distanceToSqr(ctx.player()) <= others * others)) {
                Boosts.add(other, this, given, ticks, maxStacks);
            }
            return;
        }
        Boosts.add(ctx.player(), this, given, ticks, maxStacks);
    }
}
