package com.pipican.ability;

import com.pipican.player.ClassState;

import com.pipican.effect.type.ReactionEffect;
import com.pipican.player.ClassEffectApplier;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Runs {@code pipican:on} reactions from game events. Reactions never trigger other reactions (so a
 * "your hits burn" reaction that deals damage doesn't loop).
 */
public final class Reactions {

    private static final Map<UUID, Map<ReactionEffect, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static boolean running;
    /** {@code once_per_rest} reactions already used since the player last slept. */
    private static final Map<UUID, Map<ReactionEffect, Integer>> USED = new ConcurrentHashMap<>();

    private Reactions() {}

    /** {@code player} damaged {@code victim}. */
    public static void onHit(ServerPlayer player, LivingEntity victim, DamageSource source) {
        boolean ranged = source.getDirectEntity() != source.getEntity();
        fire(player, victim, source.getWeaponItem(), ReactionEffect.Event.HIT);
        fire(player, victim, source.getWeaponItem(), ranged ? ReactionEffect.Event.RANGED_HIT : ReactionEffect.Event.MELEE_HIT);
    }

    /** Runs these reactions now (chance and cooldown still apply). */
    public static void fireAll(ServerPlayer player, java.util.List<ReactionEffect> reactions) {
        run(player, null, ItemStack.EMPTY, null, reactions);
    }

    /** Fires {@code event}; returns true if an action cancelled it ({@code pipican:shrug} on a low_health hit). */
    public static boolean fire(ServerPlayer player, LivingEntity subject, ItemStack weapon, ReactionEffect.Event event) {
        return run(player, subject, weapon, event, ClassEffectApplier.state(player).reactions());
    }

    private static boolean run(ServerPlayer player, LivingEntity subject, ItemStack weapon, ReactionEffect.Event event, java.util.List<ReactionEffect> reactions) {
        if (running) {
            return false;
        }
        boolean cancelled = false;
        var state = ClassEffectApplier.state(player);
        if (reactions.isEmpty()) {
            return false;
        }
        long now = player.level().getGameTime();
        running = true;
        try {
            for (ReactionEffect r : reactions) {
                if (event != null && r.event() != event || player.getRandom().nextDouble() >= state.reactionChance(r)) {
                    continue;
                }
                if (r.weapons().isPresent() && (weapon == null || weapon.isEmpty() || !weapon.is(r.weapons().get()))) {
                    continue;
                }
                if (subject != null && r.victims().isPresent() && !r.victims().get().contains(subject.getType().builtInRegistryHolder())) {
                    continue;
                }
                if (r.victim().isPresent() && !(subject instanceof net.minecraft.world.entity.LivingEntity living && r.victim().get().test(player, living))) {
                    continue;
                }
                // Reactions that cost resource get cheaper with ability_cost bonuses too ("miracles cost half").
                double cost = r.cost() * state.bonus(com.pipican.effect.type.BonusEffect.Kind.ABILITY_COST);
                if (cost > 0 && com.pipican.player.ClassService.data(player).resource() < cost) {
                    continue;
                }
                int uses = r.oncePerRest() ? 1 : r.usesPerRest();
                if (uses > 0) {
                    uses += state.reactionUses(r);
                    if (USED.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>()).getOrDefault(r, 0) >= uses) {
                        continue;
                    }
                }
                var cooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
                if (cooldowns.getOrDefault(r, 0L) > now) {
                    continue;
                }
                if (r.cooldownTicks() > 0) {
                    double faster = r.name().map(n -> state.reactionMods().getOrDefault(n.toLowerCase(java.util.Locale.ROOT), ClassState.ReactionMod.NONE).cooldown())
                            .orElse(0.0);
                    cooldowns.put(r, now + Math.max(1, Math.round(r.cooldownTicks() * Math.max(0, 1 + faster))));
                }
                if (uses > 0) {
                    USED.get(player.getUUID()).merge(r, 1, Integer::sum);
                }
                if (cost > 0) {
                    var data = com.pipican.player.ClassService.data(player);
                    data.setResource(data.resource() - cost);
                }
                var mod = r.name().map(n -> state.reactionMods().getOrDefault(n.toLowerCase(java.util.Locale.ROOT), ClassState.ReactionMod.NONE))
                        .orElse(ClassState.ReactionMod.NONE);
                AbilityContext ctx = new AbilityContext(player, Optional.empty(), subject != null ? subject.position() : player.position(),
                        Optional.ofNullable(subject), 0).withMod(mod);
                for (var action : java.util.stream.Stream.concat(r.actions().stream(), mod.actions().stream()).toList()) {
                    action.run(ctx);
                    if (ctx.isCancelled()) {
                        cancelled = true;
                        break;
                    }
                    if (ctx.fizzled()) {
                        break;
                    }
                }
                if (ctx.fizzled()) {
                    cooldowns.remove(r);
                    continue;
                }
                r.name().ifPresent(n -> com.pipican.player.MasteryService.onAction(player, new com.pipican.mastery.SkillAction.Reacted(n)));
            }
        } finally {
            running = false;
        }
        return cancelled;
    }

    /** Uses left before the next rest of the player's reaction called {@code name} (Integer.MAX_VALUE if it has no limit). */
    public static int usesLeft(ServerPlayer player, String name) {
        var state = ClassEffectApplier.state(player);
        for (ReactionEffect r : state.reactions()) {
            if (r.name().map(n -> n.equalsIgnoreCase(name)).orElse(false)) {
                int uses = r.oncePerRest() ? 1 : r.usesPerRest();
                if (uses <= 0) {
                    return Integer.MAX_VALUE;
                }
                uses += state.reactionUses(r);
                return Math.max(0, uses - USED.getOrDefault(player.getUUID(), java.util.Map.of()).getOrDefault(r, 0));
            }
        }
        return 0;
    }

    /** {@code no_backstab} (Omens): while a premonition is left, hits from behind get no "from behind" bonus. */
    public static boolean backstabProof(net.minecraft.world.entity.LivingEntity victim) {
        return victim instanceof ServerPlayer p && ClassEffectApplier.state(p).chance(com.pipican.effect.type.BonusEffect.Kind.NO_BACKSTAB) > 0
                && usesLeft(p, "premonition") > 0;
    }

    private static final Map<UUID, Long> LAST_REFILL = new ConcurrentHashMap<>();

    /** Every second: {@code uses_refill} refills once-per-rest reactions on a timer (Oracle of Ages). */
    public static void refillTick(ServerPlayer player) {
        double seconds = ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.USES_REFILL);
        if (seconds <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        long last = LAST_REFILL.computeIfAbsent(player.getUUID(), k -> now);
        if (now - last >= seconds * 20) {
            LAST_REFILL.put(player.getUUID(), now);
            rest(player);
        }
    }

    /** After sleeping: {@code once_per_rest} reactions can trigger again. */
    public static void rest(ServerPlayer player) {
        USED.remove(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
    }
}
