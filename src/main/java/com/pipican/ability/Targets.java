package com.pipican.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Who an action affects. Shared JSON fields of most actions:
 * <pre>{@code "target": "hostiles", "radius": 6, "range": 16, "cone": 90 }</pre>
 * <ul>
 *   <li>{@code self} - the user</li>
 *   <li>{@code allies} - the user and every other player within {@code radius}</li>
 *   <li>{@code players} - other players within {@code radius}</li>
 *   <li>{@code hostiles} - hostile mobs within {@code radius}</li>
 *   <li>{@code mobs} - every non-player living entity within {@code radius}</li>
 *   <li>{@code look} - the mob (not a player) in the crosshair within {@code range} blocks</li>
 *   <li>{@code look_ally} - the player in the crosshair within {@code range} blocks, otherwise the user</li>
 * </ul>
 * {@code cone} (degrees, default 360) keeps only targets in front of the user.
 */
public record Targets(Kind target, double radius, double range, double cone) {

    public enum Kind implements StringRepresentable {
        SELF("self"), ALLIES("allies"), PLAYERS("players"), HOSTILES("hostiles"), MOBS("mobs"), LOOK("look"), LOOK_ALLY("look_ally");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static MapCodec<Targets> mapCodec(Kind defaultKind) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Kind.CODEC.optionalFieldOf("target", defaultKind).forGetter(Targets::target),
                Codec.doubleRange(0, 64).optionalFieldOf("radius", 6.0).forGetter(Targets::radius),
                Codec.doubleRange(0, 128).optionalFieldOf("range", 16.0).forGetter(Targets::range),
                Codec.doubleRange(1, 360).optionalFieldOf("cone", 360.0).forGetter(Targets::cone)
        ).apply(i, Targets::new));
    }

    public List<LivingEntity> select(ServerPlayer player) {
        List<LivingEntity> out = new ArrayList<>();
        switch (target) {
            case SELF -> out.add(player);
            case LOOK -> {
                LivingEntity hit = lookTarget(player, range);
                if (hit != null && !(hit instanceof Player)) {
                    out.add(hit);
                }
            }
            case LOOK_ALLY -> {
                LivingEntity hit = lookTarget(player, range);
                out.add(hit instanceof Player p && !p.isSpectator() ? hit : player);
            }
            default -> {
                if (target == Kind.ALLIES) {
                    out.add(player);
                }
                AABB box = player.getBoundingBox().inflate(radius);
                for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
                    if (e.distanceToSqr(player) > radius * radius || !inCone(player, e)) {
                        continue;
                    }
                    boolean isPlayer = e instanceof Player p && !p.isSpectator();
                    boolean keep = switch (target) {
                        case ALLIES, PLAYERS -> isPlayer;
                        case HOSTILES -> e instanceof Enemy;
                        case MOBS -> !(e instanceof Player);
                        default -> false;
                    };
                    if (keep) {
                        out.add(e);
                    }
                }
            }
        }
        return out;
    }

    private boolean inCone(ServerPlayer player, Entity e) {
        if (cone >= 360) {
            return true;
        }
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 to = e.position().subtract(player.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0e-4) {
            return true;
        }
        double angle = Math.toDegrees(Math.acos(Math.clamp(look.dot(to.normalize()), -1, 1)));
        return angle <= cone / 2;
    }

    /** The living entity in the crosshair within {@code range} blocks, not behind a block. */
    public static LivingEntity lookTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        var blockHit = player.pick(range, 1.0f, false);
        if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l ? l : null;
    }
}
