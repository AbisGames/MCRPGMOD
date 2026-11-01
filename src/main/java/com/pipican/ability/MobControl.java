package com.pipican.ability;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary control over mobs from abilities ({@code pipican:control}, ally summons, decoys).
 * Not saved: control ends on restart.
 * <ul>
 *   <li>{@code flee} - the mob runs away from the user and attacks nobody</li>
 *   <li>{@code pacify} - the mob won't target the user (charm)</li>
 *   <li>{@code ally} - the mob fights hostile mobs near the user and never targets players</li>
 *   <li>{@code taunt} - the mob attacks the user's decoy instead of them</li>
 * </ul>
 */
public final class MobControl {

    public enum Mode { FLEE, PACIFY, ALLY, TAUNT }

    private record Control(Mob mob, UUID owner, Mode mode, long endsAt, LivingEntity decoy) {}

    private static final Map<UUID, Control> CONTROLLED = new ConcurrentHashMap<>();

    private MobControl() {}

    public static void control(Mob mob, ServerPlayer owner, Mode mode, long ticks) {
        CONTROLLED.put(mob.getUUID(), new Control(mob, owner.getUUID(), mode, owner.level().getGameTime() + ticks, null));
        if (mode != Mode.ALLY) {
            mob.setTarget(null);
        }
    }

    public static void taunt(Mob mob, ServerPlayer owner, LivingEntity decoy, long ticks) {
        CONTROLLED.put(mob.getUUID(), new Control(mob, owner.getUUID(), Mode.TAUNT, owner.level().getGameTime() + ticks, decoy));
        mob.setTarget(decoy);
    }

    private static final net.minecraft.resources.Identifier SERVANT = com.pipican.Pipican.id("servant");

    /** How many mobs fight for {@code player} right now (ally controls and ally summons). */
    public static int allies(Player player) {
        long now = player.level().getGameTime();
        return (int) CONTROLLED.values().stream().filter(c -> c.mode() == Mode.ALLY && c.owner().equals(player.getUUID())
                && c.endsAt() > now && c.mob().isAlive()).count();
    }

    /** Summon upgrades for a mob taken over as a servant; removed when control ends. */
    public static void boostServant(Mob mob, double damage, double health) {
        servantModifier(mob, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, damage);
        servantModifier(mob, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, health);
    }

    private static void servantModifier(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount) {
        var instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(SERVANT);
        if (amount != 0) {
            instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SERVANT, amount,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** True if {@code player} has charmed (pacified) {@code entity}. */
    public static boolean isPacifiedBy(LivingEntity entity, Player player) {
        Control c = CONTROLLED.get(entity.getUUID());
        return c != null && c.mode() == Mode.PACIFY && c.owner().equals(player.getUUID());
    }

    /** Ends control of {@code mob}. */
    public static void release(Mob mob) {
        if (CONTROLLED.remove(mob.getUUID()) != null) {
            mob.setTarget(null);
        }
    }

    public static boolean isAllyOf(LivingEntity entity, Player player) {
        Control c = CONTROLLED.get(entity.getUUID());
        return c != null && c.mode() == Mode.ALLY && c.owner().equals(player.getUUID());
    }

    /**
     * Whether a mob may switch to {@code target}. Called from the target-change event: controlled mobs
     * never target their controller (and allies never target any player).
     */
    public static boolean mayTarget(LivingEntity mob, LivingEntity target) {
        Control c = CONTROLLED.get(mob.getUUID());
        if (c == null || target == null) {
            return true;
        }
        if (c.mode() == Mode.ALLY) {
            return !(target instanceof Player) && !isAllyOfOwner(target, c.owner());
        }
        if (c.mode() == Mode.TAUNT) {
            return target == c.decoy();
        }
        return c.mode() != Mode.FLEE && !target.getUUID().equals(c.owner());
    }

    private static boolean isAllyOfOwner(LivingEntity e, UUID owner) {
        Control c = CONTROLLED.get(e.getUUID());
        return c != null && c.mode() == Mode.ALLY && c.owner().equals(owner);
    }

    /** Called every server tick. */
    public static void tick(MinecraftServer server) {
        for (Iterator<Control> it = CONTROLLED.values().iterator(); it.hasNext(); ) {
            Control c = it.next();
            Mob mob = c.mob();
            ServerPlayer owner = server.getPlayerList().getPlayer(c.owner());
            if (mob.isRemoved() || !mob.isAlive() || owner == null || mob.level().getGameTime() >= c.endsAt()
                    || c.mode() == Mode.TAUNT && (c.decoy() == null || c.decoy().isRemoved())) {
                if (c.mode() == Mode.ALLY || c.mode() == Mode.TAUNT) {
                    mob.setTarget(null);
                }
                boostServant(mob, 0, 0);
                if (mob.getHealth() > mob.getMaxHealth()) {
                    mob.setHealth(mob.getMaxHealth());
                }
                it.remove();
                continue;
            }
            if (mob.tickCount % 5 != 0) {
                continue;
            }
            switch (c.mode()) {
                case FLEE -> {
                    mob.setTarget(null);
                    if (mob instanceof PathfinderMob pm && pm.getNavigation().isDone()) {
                        Vec3 away = DefaultRandomPos.getPosAway(pm, 16, 7, owner.position());
                        if (away != null) {
                            pm.getNavigation().moveTo(away.x, away.y, away.z, 1.4);
                        }
                    }
                }
                case PACIFY -> {
                    if (mob.getTarget() == owner) {
                        mob.setTarget(null);
                    }
                }
                case TAUNT -> mob.setTarget(c.decoy());
                case ALLY -> {
                    LivingEntity target = mob.getTarget();
                    if (target == null || !target.isAlive() || !mayTarget(mob, target)) {
                        mob.setTarget(nearestHostile(mob, owner).orElse(null));
                    }
                    if (mob.getTarget() == null && mob.distanceToSqr(owner) > 64 && mob instanceof PathfinderMob pm) {
                        pm.getNavigation().moveTo(owner, 1.1);
                    }
                    // Mobs with no attack of their own (copper golems) walk up and strike for 3.
                    LivingEntity foe = mob.getTarget();
                    if (foe != null && mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == null
                            && mob.level() instanceof net.minecraft.server.level.ServerLevel level) {
                        if (mob.distanceToSqr(foe) > 2.5 * 2.5) {
                            mob.getNavigation().moveTo(foe, 1.2);
                        } else if (mob.tickCount % 20 == 0) {
                            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                            foe.hurtServer(level, mob.damageSources().mobAttack(mob), 3);
                        }
                    }
                }
            }
        }
    }

    private static Optional<LivingEntity> nearestHostile(Mob mob, ServerPlayer owner) {
        return mob.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(16),
                        e -> e != mob && e.isAlive() && e instanceof Enemy && !isAllyOfOwner(e, owner.getUUID()))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)));
    }
}
