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
}
