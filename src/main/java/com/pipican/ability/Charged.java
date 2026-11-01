package com.pipican.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** How charged each player's current melee attack is (vanilla resets the charge before the damage lands). */
public final class Charged {

    private record Swing(float strength, long tick) {}

    private static final Map<UUID, Swing> LAST = new ConcurrentHashMap<>();

    private Charged() {}

    public static void remember(ServerPlayer player) {
        LAST.put(player.getUUID(), new Swing(player.getAttackStrengthScale(0.5f), player.level().getGameTime()));
    }

    /** The attack this tick was (nearly) fully charged. */
    public static boolean wasCharged(ServerPlayer player) {
        Swing s = LAST.get(player.getUUID());
        return s != null && s.tick == player.level().getGameTime() && s.strength >= 0.9f;
    }

    /** Mobs left exposed by a player's charged hit ({@code charged_exposure}): mob -> (player, until). Not saved. */
    private static final Map<UUID, Map<UUID, Long>> EXPOSED = new ConcurrentHashMap<>();

    public static void expose(ServerPlayer player, net.minecraft.world.entity.LivingEntity victim, long ticks) {
        EXPOSED.computeIfAbsent(victim.getUUID(), k -> new ConcurrentHashMap<>()).put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    /** {@code victim} is still exposed by one of {@code player}'s charged hits. */
    public static boolean exposed(ServerPlayer player, net.minecraft.world.entity.LivingEntity victim) {
        Map<UUID, Long> by = EXPOSED.get(victim.getUUID());
        Long until = by == null ? null : by.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (until < player.level().getGameTime()) {
            by.remove(player.getUUID());
            if (by.isEmpty()) {
                EXPOSED.remove(victim.getUUID());
            }
            return false;
        }
        return true;
    }
}
