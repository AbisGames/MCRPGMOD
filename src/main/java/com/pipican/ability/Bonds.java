package com.pipican.ability;

import com.pipican.player.ClassService;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Lasting Bond (Witch): two bonded players each heal half of what the other heals while within
 * 32 blocks. The bond is stored on the player who made it.
 */
public final class Bonds {

    private static boolean sharing;

    private Bonds() {}

    /** Called when {@code healed} heals {@code amount}. */
    public static void onHeal(ServerPlayer healed, float amount) {
        if (sharing || amount <= 0) {
            return;
        }
        sharing = true;
        try {
            var server = healed.level().getServer();
            // Bond made by the healed player...
            ClassService.data(healed).extras().bond.map(id -> server.getPlayerList().getPlayer(id)).ifPresent(p -> share(healed, p, amount));
            // ...or by someone bonded to them.
            UUID id = healed.getUUID();
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                if (other != healed && ClassService.data(other).extras().bond.map(id::equals).orElse(false)) {
                    share(healed, other, amount);
                }
            }
        } finally {
            sharing = false;
        }
    }

    private static void share(ServerPlayer from, ServerPlayer to, float amount) {
        if (to != from && to.level() == from.level() && to.distanceToSqr(from) <= 32 * 32 && to.isAlive()) {
            to.heal(amount * 0.5f);
        }
    }
}
