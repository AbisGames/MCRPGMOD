package com.pipican.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Puppet Master: one charmed mob kept as an ally until the player sleeps. Not saved. */
public final class Puppets {

    private static final Map<UUID, Mob> PUPPET = new ConcurrentHashMap<>();

    private Puppets() {}

    /** Makes {@code mob} the player's puppet if they have no living one; true if it did. */
    public static boolean take(ServerPlayer player, Mob mob) {
        Mob current = PUPPET.get(player.getUUID());
        if (current != null && current.isAlive() && !current.isRemoved()) {
            return false;
        }
        PUPPET.put(player.getUUID(), mob);
        return true;
    }

    /** On waking: the puppet goes free. */
    public static void onWake(ServerPlayer player) {
        Mob puppet = PUPPET.remove(player.getUUID());
        if (puppet != null) {
            MobControl.release(puppet);
        }
    }
}
