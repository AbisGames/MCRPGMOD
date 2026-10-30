package com.pipican.ability;

import com.pipican.effect.type.QuarryEffect.Pick;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The mobs players have chosen with {@code pipican:quarry} (vows, prey, challenged foes). A quarry also counts as
 * marked by its player, so "marked target" bonuses apply to it. Not saved: quarries end on restart.
 */
public final class Quarries {

    private static final class Active {
        final LivingEntity target;
        final long endsAt;
        boolean hitYou;
        double lastDistance = -1;

        Active(LivingEntity target, long endsAt) {
            this.target = target;
            this.endsAt = endsAt;
        }
    }

    private static final Map<UUID, Map<String, Active>> ACTIVE = new ConcurrentHashMap<>();

    private Quarries() {}

    private static boolean live(Active a, long now) {
        return a != null && a.target.isAlive() && !a.target.isRemoved() && a.endsAt > now;
    }

    /** True if {@code player} has a live quarry called {@code name}. */
    public static boolean has(ServerPlayer player, String name) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        return mine != null && live(mine.get(name), player.level().getGameTime());
    }

    /** {@code player} hit {@code target}: ends quarries that end on another hit, then picks by hit. */
    public static void onHit(ServerPlayer player, LivingEntity target, boolean projectile, boolean melee) {
        if (target instanceof Player) {
            return;
        }
        Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(player).quarries();
        if (quarries.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        quarries.forEach((name, q) -> {
            Active a = mine == null ? null : mine.get(name);
            if (q.endOnOtherHit() && live(a, now) && a.target != target) {
                end(player, name);
            }
            if (projectile && q.pick() == Pick.ARROW_HIT || melee && q.melee() || q.pick() == Pick.FIRST_HIT) {
                set(player, name, q, target);
            }
        });
    }

    /** A mob hurt {@code victim}: players within 16 blocks with a {@code hurt_by} quarry may choose it. */
    public static void onPlayerHurt(ServerPlayer victim, LivingEntity attacker) {
        if (attacker instanceof Player) {
            return;
        }
        for (ServerPlayer p : victim.level().getEntitiesOfClass(ServerPlayer.class, victim.getBoundingBox().inflate(16), p -> p.distanceToSqr(victim) <= 256)) {
            ClassEffectApplier.state(p).quarries().forEach((name, q) -> {
                if (q.pick() == Pick.HURT_BY) {
                    set(p, name, q, attacker);
                }
            });
        }
        Map<String, Active> mine = ACTIVE.get(victim.getUUID());
        if (mine != null) {
            mine.values().forEach(a -> {
                if (a.target == attacker) {
                    a.hitYou = true;
                }
            });
        }
    }

    private static void set(ServerPlayer player, String name, ClassState.Quarry q, LivingEntity target) {
        long now = player.level().getGameTime();
        Map<String, Active> mine = ACTIVE.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        Active old = mine.get(name);
        if (live(old, now) && (!q.replace() || old.target == target)) {
            return;
        }
        if (old != null) {
            Marks.remove(old.target, player);
        }
        long ticks = q.forever() ? Long.MAX_VALUE / 4 : Math.round(q.seconds() * 20);
        mine.put(name, new Active(target, now + ticks));
        PICKED.put(player.getUUID(), now);
        Marks.add(target, player, ticks, 1.0, false, false, false);
        int duration = q.forever() || ticks > Integer.MAX_VALUE ? MobEffectInstance.INFINITE_DURATION : (int) ticks;
        if (q.glow()) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false), player);
        }
        q.effects().forEach((effect, level) -> target.addEffect(new MobEffectInstance(effect, duration, level), player));
    }

    private static void end(ServerPlayer player, String name) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        Active a = mine == null ? null : mine.remove(name);
        if (a != null) {
            Marks.remove(a.target, player);
            a.target.removeEffect(MobEffects.GLOWING);
        }
    }

    /** {@code player}'s live quarries. */
    public static java.util.List<LivingEntity> targets(ServerPlayer player) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        long now = player.level().getGameTime();
        return mine == null ? java.util.List.of() : mine.values().stream().filter(a -> live(a, now)).map(a -> a.target).toList();
    }

    /** Player -> game time they last picked a quarry (for {@code quarry_lockout}). */
    private static final Map<UUID, Long> PICKED = new ConcurrentHashMap<>();

    /** {@code quarry_lockout}: right after picking a quarry, {@code player} can't damage mobs that aren't one. */
    public static boolean lockedOut(ServerPlayer player, LivingEntity victim) {
        double lock = ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_LOCKOUT);
        Long at = PICKED.get(player.getUUID());
        return lock > 0 && at != null && player.level().getGameTime() - at < lock * 20 && !(victim instanceof Player) && !isQuarryOf(victim, player);
    }

    /** True if {@code entity} is one of {@code player}'s live quarries. */
    public static boolean isQuarryOf(LivingEntity entity, ServerPlayer player) {
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        long now = player.level().getGameTime();
        return mine != null && mine.values().stream().anyMatch(a -> a.target == entity && live(a, now));
    }

    /** Damage multiplier for {@code attacker} hitting {@code victim}. */
    public static double multiplier(ServerPlayer attacker, LivingEntity victim) {
        Map<String, Active> mine = ACTIVE.get(attacker.getUUID());
        if (mine == null) {
            return 1;
        }
        long now = attacker.level().getGameTime();
        Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(attacker).quarries();
        double mult = 1;
        for (var entry : mine.entrySet()) {
            Active a = entry.getValue();
            ClassState.Quarry q = quarries.get(entry.getKey());
            if (q != null && a.target == victim && live(a, now)) {
                double k = 1 + ClassEffectApplier.state(attacker).chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_DOUBLE);
                mult *= (1 + (q.multiplier() - 1) * k) * (a.hitYou ? 1 : 1 + q.untouched() * k);
            }
        }
        return mult;
    }

    /** Multiplier for damage {@code attacker} deals to {@code victim}: challenged foes hit others for less. */
    public static double othersMultiplier(LivingEntity attacker, LivingEntity victim, MinecraftServer server) {
        double mult = 1;
        long now = attacker.level().getGameTime();
        for (var entry : ACTIVE.entrySet()) {
            if (entry.getKey().equals(victim.getUUID())) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
                continue;
            }
            Map<String, ClassState.Quarry> quarries = ClassEffectApplier.state(owner).quarries();
            for (var a : entry.getValue().entrySet()) {
                ClassState.Quarry q = quarries.get(a.getKey());
                if (q != null && q.othersDamage() > 0 && a.getValue().target == attacker && live(a.getValue(), now)) {
                    mult *= Math.max(0, 1 - q.othersDamage() * (1 + ClassEffectApplier.state(owner).chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_DOUBLE)));
                }
            }
        }
        return mult;
    }

    private static final net.minecraft.resources.Identifier PURSUIT = com.pipican.Pipican.id("quarry/pursuit");
    private static final net.minecraft.resources.Identifier FOE_SPEED = com.pipican.Pipican.id("quarry/attack_speed");
    private static final net.minecraft.resources.Identifier FOE_REACH = com.pipican.Pipican.id("quarry/reach");
    private static final Map<UUID, net.minecraft.world.phys.Vec3> LAST_POS = new ConcurrentHashMap<>();

    /** Every second: forgets ended quarries; a quarry moving away from its player fires quarry_flee. */
    public static void tick(MinecraftServer server) {
        ACTIVE.forEach((uuid, mine) -> {
            mine.values().removeIf(a -> !live(a, a.target.level().getGameTime()));
            if (mine.isEmpty()) {
                ACTIVE.remove(uuid);
                return;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(uuid);
            if (owner == null) {
                return;
            }
            for (Active a : mine.values()) {
                double d = a.target.level() == owner.level() ? a.target.distanceTo(owner) : -1;
                if (d > 4 && a.lastDistance >= 0 && d > a.lastDistance + 1) {
                    Reactions.fire(owner, a.target, net.minecraft.world.item.ItemStack.EMPTY, com.pipican.effect.type.ReactionEffect.Event.QUARRY_FLEE);
                }
                a.lastDistance = d;
            }
        });
    }

    /**
     * Every 5 ticks: {@code quarry_pursuit} speed while moving toward a quarry, {@code quarry_attack_speed} /
     * {@code quarry_reach} while looking at one.
     */
    public static void playerTick(ServerPlayer player) {
        ClassState state = ClassEffectApplier.state(player);
        double pursuit = state.chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_PURSUIT);
        double speed = state.chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_ATTACK_SPEED);
        double reach = state.chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_REACH);
        double k = 1 + state.chance(com.pipican.effect.type.BonusEffect.Kind.QUARRY_DOUBLE);
        pursuit *= k;
        speed *= k;
        reach *= k;
        Map<String, Active> mine = ACTIVE.get(player.getUUID());
        long now = player.level().getGameTime();
        var pos = player.position();
        var last = LAST_POS.put(player.getUUID(), pos);
        boolean toward = false;
        boolean looking = false;
        if (mine != null && (pursuit != 0 || speed != 0 || reach != 0)) {
            LivingEntity look = speed != 0 || reach != 0 ? Targets.lookTarget(player, 6 + reach) : null;
            for (Active a : mine.values()) {
                if (!live(a, now) || a.target.level() != player.level()) {
                    continue;
                }
                if (last != null) {
                    var moved = pos.subtract(last).multiply(1, 0, 1);
                    var to = a.target.position().subtract(pos).multiply(1, 0, 1);
                    toward |= moved.lengthSqr() > 0.01 && to.lengthSqr() > 1.0e-4 && moved.normalize().dot(to.normalize()) > 0.5;
                }
                looking |= look == a.target;
            }
        }
        modifier(player, net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, PURSUIT, toward ? pursuit : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, FOE_SPEED, looking ? speed : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        modifier(player, net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE, FOE_REACH, looking ? reach : 0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
    }

    private static void modifier(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                 net.minecraft.resources.Identifier id, double amount, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation op) {
        var instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        var current = instance.getModifier(id);
        if (current != null && current.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, amount, op));
        }
    }
}
