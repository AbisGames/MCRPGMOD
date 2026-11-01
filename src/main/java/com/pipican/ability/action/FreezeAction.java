package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Freezes;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * {@code pipican:freeze} - freezes mobs in time for {@code seconds}: they stop where they are, in mid-air too, and can't
 * act or attack until it ends (Time Stop).
 *
 * <pre>{@code { "type": "pipican:freeze", "target": "hostiles", "radius": 6, "seconds": 3 } }</pre>
 */
public record FreezeAction(Targets targets, double seconds) implements AbilityAction {

    public static final MapCodec<FreezeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.HOSTILES).forGetter(FreezeAction::targets),
            Codec.doubleRange(0.05, 600).optionalFieldOf("seconds", 3.0).forGetter(FreezeAction::seconds)
    ).apply(i, FreezeAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.FREEZE;
    }

    @Override
    public void run(AbilityContext ctx) {
        long ticks = Math.round((seconds + ctx.mod().seconds()) * 20);
        for (LivingEntity target : targets.select(ctx)) {
            if (target instanceof Mob mob) {
                Freezes.freeze(mob, ticks);
            }
        }
    }
}
