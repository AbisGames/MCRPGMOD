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
 * With {@code toward_look} each target dashes toward the spot the user looks at (up to 32 blocks): "allies dash toward a point".
 * Fall damage from the dash itself is not prevented.
 */
public record DashAction(Targets targets, double strength, double up, boolean towardLook) implements AbilityAction {

    public static final MapCodec<DashAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(DashAction::targets),
            Codec.doubleRange(0, 5).optionalFieldOf("strength", 1.2).forGetter(DashAction::strength),
            Codec.doubleRange(-2, 3).optionalFieldOf("up", 0.2).forGetter(DashAction::up),
            Codec.BOOL.optionalFieldOf("toward_look", false).forGetter(DashAction::towardLook)
    ).apply(i, DashAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DASH;
    }

    @Override
    public void run(AbilityContext ctx) {
        Vec3 look = ctx.player().getLookAngle().multiply(1, 0, 1).normalize().scale(strength);
        Vec3 point = towardLook ? ctx.player().pick(32, 1.0f, false).getLocation() : null;
        for (LivingEntity target : targets.select(ctx)) {
            Entity mover = target.getVehicle() != null ? target.getVehicle() : target;
            if (point != null) {
                Vec3 to = point.subtract(mover.position()).multiply(1, 0, 1);
                if (to.lengthSqr() < 1) {
                    continue;
                }
                look = to.normalize().scale(Math.min(strength, Math.sqrt(to.lengthSqr()) / 5));
            }
            mover.setDeltaMovement(mover.getDeltaMovement().add(look.x, up, look.z));
            mover.hurtMarked = true;
        }
    }
}
