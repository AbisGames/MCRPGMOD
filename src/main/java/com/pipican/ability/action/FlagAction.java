package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Flags;

/**
 * {@code pipican:flag} - starts a named timer on the user for {@code seconds}; the {@code pipican:flag} condition is true while it runs.
 *
 * <pre>{@code { "type": "pipican:flag", "name": "berserk lockout", "seconds": 15 } }</pre>
 */
public record FlagAction(String name, double seconds) implements AbilityAction {

    public static final MapCodec<FlagAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(FlagAction::name),
            Codec.doubleRange(0.05, 86400).fieldOf("seconds").forGetter(FlagAction::seconds)
    ).apply(i, FlagAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.FLAG;
    }

    @Override
    public void run(AbilityContext ctx) {
        Flags.set(ctx.player(), name, Math.round(seconds * 20));
        com.pipican.player.ClassService.changed(ctx.player());
    }
}
