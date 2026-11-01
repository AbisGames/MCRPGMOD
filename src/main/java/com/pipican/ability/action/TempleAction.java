package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Temples;

/**
 * {@code pipican:temple} - raises a temporary Temple of Faith where the user stands for {@code seconds}: every temple
 * bonus and upgrade counts around that spot instead of the respawn point (Consecrate Ground).
 *
 * <pre>{@code { "type": "pipican:temple", "seconds": 60 } }</pre>
 */
public record TempleAction(double seconds) implements AbilityAction {

    public static final MapCodec<TempleAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 60.0).forGetter(TempleAction::seconds)
    ).apply(i, TempleAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.TEMPLE;
    }

    @Override
    public void run(AbilityContext ctx) {
        Temples.consecrate(ctx.player(), ctx.origin(), Math.round((seconds + ctx.mod().seconds()) * 20));
    }
}
