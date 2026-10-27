package com.pipican.ability;

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
                    cooldowns.put(r, now + r.cooldownTicks());
                }
                if (uses > 0) {
                    USED.get(player.getUUID()).merge(r, 1, Integer::sum);
                }
                AbilityContext ctx = new AbilityContext(player, Optional.empty(), subject != null ? subject.position() : player.position(),
                        Optional.ofNullable(subject), 0);
                for (var action : r.actions()) {
                    action.run(ctx);
                    if (ctx.isCancelled()) {
                        cancelled = true;
                        break;
                    }
                }
            }
        } finally {
            running = false;
        }
        return cancelled;
    }

    /** After sleeping: {@code once_per_rest} reactions can trigger again. */
    public static void rest(ServerPlayer player) {
        USED.remove(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
    }
}
