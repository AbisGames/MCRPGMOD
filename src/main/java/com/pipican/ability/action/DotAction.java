package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Bleeds;
import com.pipican.ability.Targets;

/**
 * {@code pipican:dot} - damage over time on the targets: {@code damage} per second for {@code seconds} (like
 * bleeding, credited to the user). Default target: the mob a reaction is about.
 */
public record DotAction(Targets targets, double damage, double seconds) implements AbilityAction {

    public static final MapCodec<DotAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.VICTIM).forGetter(DotAction::targets),
            Codec.doubleRange(0.1, 1000).fieldOf("damage").forGetter(DotAction::damage),
            Codec.doubleRange(0.5, 600).fieldOf("seconds").forGetter(DotAction::seconds)
    ).apply(i, DotAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DOT;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (var target : targets.select(ctx)) {
            if (target != ctx.player()) {
                Bleeds.wound(ctx.player(), target, damage + ctx.mod().damage(), seconds + ctx.mod().seconds(), 1);
            }
        }
    }
}
