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

    private Reactions() {}

    /** {@code player} damaged {@code victim}. */
    public static void onHit(ServerPlayer player, LivingEntity victim, DamageSource source) {
        boolean ranged = source.getDirectEntity() != source.getEntity();
        fire(player, victim, source.getWeaponItem(), ReactionEffect.Event.HIT);
        fire(player, victim, source.getWeaponItem(), ranged ? ReactionEffect.Event.RANGED_HIT : ReactionEffect.Event.MELEE_HIT);
    }

    public static void fire(ServerPlayer player, LivingEntity subject, ItemStack weapon, ReactionEffect.Event event) {
        if (running) {
            return;
        }
        var state = ClassEffectApplier.state(player);
        var reactions = state.reactions();
        if (reactions.isEmpty()) {
            return;
        }
        long now = player.level().getGameTime();
        running = true;
        try {
            for (ReactionEffect r : reactions) {
                if (r.event() != event || player.getRandom().nextDouble() >= state.reactionChance(r)) {
                    continue;
                }
                if (r.weapons().isPresent() && (weapon == null || weapon.isEmpty() || !weapon.is(r.weapons().get()))) {
                    continue;
                }
                if (subject != null && r.victims().isPresent() && !r.victims().get().contains(subject.getType().builtInRegistryHolder())) {
                    continue;
                }
                var cooldowns = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
                if (cooldowns.getOrDefault(r, 0L) > now) {
                    continue;
                }
                if (r.cooldownTicks() > 0) {
                    cooldowns.put(r, now + r.cooldownTicks());
                }
                AbilityContext ctx = new AbilityContext(player, Optional.empty(), subject != null ? subject.position() : player.position(),
                        Optional.ofNullable(subject), 0);
                for (var action : r.actions()) {
                    action.run(ctx);
                    if (ctx.isCancelled()) {
                        break;
                    }
                }
            }
        } finally {
            running = false;
        }
    }

    public static void forget(ServerPlayer player) {
        COOLDOWNS.remove(player.getUUID());
    }
}
