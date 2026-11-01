package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.entity.PipicanEntities;
import com.pipican.entity.WitchFireball;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/**
 * {@code pipican:steer} - the user's steerable green fireball (Witch Fire). Run every tick as a pulse: the first run
 * conjures it in front of the user, later runs turn it toward whatever the user looks at. It burns the mobs it touches
 * ({@code damage}, plus 5s of fire); after a hit a new one forms {@code respawn_seconds} later. {@code stop: true} (an end
 * action) puts it out.
 *
 * <pre>{@code { "type": "pipican:steer", "speed": 0.6, "damage": 3, "respawn_seconds": 1 } }</pre>
 */
public record SteerAction(double speed, double damage, double respawnSeconds, boolean stop) implements AbilityAction {

    public static final MapCodec<SteerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.05, 3).optionalFieldOf("speed", 0.6).forGetter(SteerAction::speed),
            Codec.doubleRange(0, 1000).optionalFieldOf("damage", 3.0).forGetter(SteerAction::damage),
            Codec.doubleRange(0, 60).optionalFieldOf("respawn_seconds", 1.0).forGetter(SteerAction::respawnSeconds),
            Codec.BOOL.optionalFieldOf("stop", false).forGetter(SteerAction::stop)
    ).apply(i, SteerAction::new));

    private record Steered(UUID fireball, long lostAt) {}

    private static final Map<UUID, Steered> STEERED = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.STEER;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Steered steered = STEERED.get(player.getUUID());
        WitchFireball ball = steered != null && steered.fireball() != null
                && ctx.level().getEntity(steered.fireball()) instanceof WitchFireball f && f.isAlive() ? f : null;
        if (stop) {
            if (ball != null) {
                ball.discard();
            }
            STEERED.remove(player.getUUID());
            return;
        }
        long now = ctx.level().getGameTime();
        if (ball == null) {
            if (steered != null && steered.fireball() != null) {
                // It hit something: a new one forms after a moment.
                STEERED.put(player.getUUID(), new Steered(null, now));
                return;
            }
            if (steered != null && now - steered.lostAt() < respawnSeconds * 20) {
                return;
            }
            ball = PipicanEntities.WITCH_FIREBALL.get().create(ctx.level(), EntitySpawnReason.TRIGGERED);
            if (ball == null) {
                return;
            }
            Vec3 at = player.getEyePosition().add(player.getLookAngle().scale(1.5));
            ball.snapTo(at.x, at.y, at.z, player.getYRot(), player.getXRot());
            ball.setOwner(player);
            ball.setDamage((float) (damage + ctx.mod().damage()));
            ctx.level().addFreshEntity(ball);
            STEERED.put(player.getUUID(), new Steered(ball.getUUID(), now));
        }
        // Steer toward the point under the crosshair.
        Vec3 aim = player.pick(48, 1.0f, false).getLocation();
        Vec3 to = aim.subtract(ball.position());
        Vec3 velocity = to.lengthSqr() < 0.25 ? Vec3.ZERO : to.normalize().scale(speed);
        ball.setDeltaMovement(velocity);
        ball.hurtMarked = true;
        ball.keepAlive();
    }
}
