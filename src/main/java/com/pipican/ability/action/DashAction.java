package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:dash} - launches the targets (default: the user, or their mount) the way the user is looking.
 *
 * <pre>{@code { "type": "pipican:dash", "strength": 1.5, "up": 0.3 } }</pre>
 * {@code strength} is horizontal speed in blocks per tick (1.5 ~ 8 blocks), {@code up} is added upward speed.
 * With {@code "target": "allies"} it sends nearby players charging where the user looks.
 * Fall damage from the dash itself is not prevented.
 */
public record DashAction(Targets targets, double strength, double up) implements AbilityAction {

    public static final MapCodec<DashAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(DashAction::targets),
            Codec.doubleRange(0, 5).optionalFieldOf("strength", 1.2).forGetter(DashAction::strength),
            Codec.doubleRange(-2, 3).optionalFieldOf("up", 0.2).forGetter(DashAction::up)
    ).apply(i, DashAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DASH;
    }

    @Override
    public void run(AbilityContext ctx) {
        Vec3 look = ctx.player().getLookAngle().multiply(1, 0, 1).normalize().scale(strength);
        for (LivingEntity target : targets.select(ctx)) {
            Entity mover = target.getVehicle() != null ? target.getVehicle() : target;
            mover.setDeltaMovement(mover.getDeltaMovement().add(look.x, up, look.z));
            mover.hurtMarked = true;
        }
    }
}
