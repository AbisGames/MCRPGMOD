package com.pipican.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 *   <li>{@code victim} - in a {@code pipican:on} reaction, the mob that was hit or killed (or that hit you)</li>
 *   <li>{@code companions} - your summons, tamed pets and eidolon within {@code radius}</li>
 *   <li>{@code mount} - what the user is riding</li>
 *   <li>{@code near_victim} - in a reaction, the hostile mob closest to the one hit, within {@code radius} of it</li>
 * </ul>
 * {@code cone} (degrees, default 360) keeps only targets in front of the user. {@code entities} (an entity
 * type, list or {@code #tag}, e.g. {@code "#minecraft:undead"}) keeps only those kinds of mob.
 * Areas are centred on the action's origin: the user, or an anchored state's spot. With {@code look} and
 * nothing in the crosshair the whole use is cancelled and refunded, unless {@code "required": false}.
 */
public record Targets(Kind target, double radius, double range, double cone, Optional<HolderSet<EntityType<?>>> entities, boolean required) {

    public enum Kind implements StringRepresentable {
        SELF("self"), ALLIES("allies"), PLAYERS("players"), HOSTILES("hostiles"), MOBS("mobs"), LOOK("look"), LOOK_ALLY("look_ally"),
        VICTIM("victim"), COMPANIONS("companions"), NEAR_VICTIM("near_victim"), MOUNT("mount");

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
                Codec.doubleRange(1, 360).optionalFieldOf("cone", 360.0).forGetter(Targets::cone),
                RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("entities").forGetter(Targets::entities),
                Codec.BOOL.optionalFieldOf("required", true).forGetter(Targets::required)
        ).apply(i, Targets::new));
    }

    public List<LivingEntity> select(AbilityContext ctx) {
        List<LivingEntity> out = new ArrayList<>(selectAll(ctx));
        entities.ifPresent(set -> out.removeIf(e -> !set.contains(e.getType().builtInRegistryHolder())));
        if (out.isEmpty() && required && target == Kind.LOOK) {
            ctx.cancel(net.minecraft.network.chat.Component.translatable("pipican.ability.no_target"));
        }
        return out;
    }

    private List<LivingEntity> selectAll(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        Vec3 origin = ctx.origin();
        List<LivingEntity> out = new ArrayList<>();
        switch (target) {
            case SELF -> out.add(player);
            case MOUNT -> {
                if (player.getVehicle() instanceof LivingEntity mount) {
                    out.add(mount);
                }
            }
            case VICTIM -> ctx.subject().filter(LivingEntity::isAlive).ifPresent(out::add);
            case COMPANIONS -> out.addAll(Companions.of(player, radius));
            case NEAR_VICTIM -> ctx.subject().ifPresent(subject -> player.level().getEntitiesOfClass(LivingEntity.class, subject.getBoundingBox().inflate(radius),
                            e -> e != subject && e != player && e.isAlive() && e instanceof Enemy && e.distanceTo(subject) <= radius).stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(subject))).ifPresent(out::add));
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
                AABB box = new AABB(origin, origin).inflate(radius);
                for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())) {
                    if (e.position().distanceToSqr(origin) > radius * radius || !inCone(player, e)) {
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
