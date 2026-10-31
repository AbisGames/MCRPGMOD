package com.pipican.ability;

import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * A Templar's temple: around their respawn point (a bed or respawn anchor they set) in the same dimension, 10 blocks
 * plus {@code temple_radius}; with {@code temple_mobile} it is around the player instead, that many blocks wide.
 */
public final class Temples {

    private Temples() {}

    /** The temple's centre, or empty when it is in another dimension (or there is none). */
    public static Optional<Vec3> center(ServerPlayer player) {
        if (mobile(player)) {
            return Optional.of(player.position());
        }
        ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
        if (respawn == null || !respawn.respawnData().globalPos().dimension().equals(player.level().dimension())) {
            return Optional.empty();
        }
        return Optional.of(respawn.respawnData().globalPos().pos().getCenter());
    }

    public static double radius(ServerPlayer player) {
        double mobile = ClassEffectApplier.state(player).chance(Kind.TEMPLE_MOBILE);
        return mobile > 0 ? mobile : 10 + ClassEffectApplier.state(player).chance(Kind.TEMPLE_RADIUS);
    }

    public static boolean mobile(ServerPlayer player) {
        return ClassEffectApplier.state(player).chance(Kind.TEMPLE_MOBILE) > 0;
    }

    /** True if {@code pos} is inside {@code player}'s temple ({@code radius} overrides the usual one). */
    public static boolean contains(ServerPlayer player, Vec3 pos, double radius) {
        return center(player).map(c -> c.distanceToSqr(pos) <= radius * radius).orElse(false);
    }

    public static boolean contains(ServerPlayer player, Vec3 pos) {
        return contains(player, pos, radius(player));
    }

    /** How strong temple bonuses are: 1 plus {@code temple_strength} ("temple bonuses doubled" = +1). */
    public static double strength(ServerPlayer player) {
        return Math.max(0, 1 + ClassEffectApplier.state(player).chance(Kind.TEMPLE_STRENGTH));
    }
}
