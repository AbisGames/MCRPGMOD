package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:dash} - launches the user (or their mount) the way they are looking.
 *
 * <pre>{@code { "type": "pipican:dash", "strength": 1.5, "up": 0.3 } }</pre>
 * {@code strength} is horizontal speed in blocks per tick (1.5 ~ 8 blocks), {@code up} is added upward speed.
 * Fall damage from the dash itself is not prevented.
 */
public record DashAction(double strength, double up) implements AbilityAction {

    public static final MapCodec<DashAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0, 5).optionalFieldOf("strength", 1.2).forGetter(DashAction::strength),
            Codec.doubleRange(-2, 3).optionalFieldOf("up", 0.2).forGetter(DashAction::up)
    ).apply(i, DashAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DASH;
    }

    @Override
    public void run(AbilityContext ctx) {
        Entity mover = ctx.player().getVehicle() != null ? ctx.player().getVehicle() : ctx.player();
        Vec3 look = ctx.player().getLookAngle().multiply(1, 0, 1).normalize().scale(strength);
        mover.setDeltaMovement(mover.getDeltaMovement().add(look.x, up, look.z));
        mover.hurtMarked = true;
    }
}
