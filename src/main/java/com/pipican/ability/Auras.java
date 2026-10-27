package com.pipican.ability;

import com.pipican.effect.ClassEffect;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** {@code pipican:aura}: which other players' auras reach each player; rebuilds bonuses when that changes. */
public final class Auras {

    private static final Map<UUID, List<Object>> LAST = new ConcurrentHashMap<>();

    private Auras() {}

    /** The aura effects other players give {@code player} right now (read while bonuses are rebuilt). */
    public static List<ClassEffect> effects(ServerPlayer player) {
        List<ClassEffect> out = new ArrayList<>();
        for (ServerPlayer other : player.level().players()) {
            if (other == player || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            for (ClassState.Aura aura : ClassEffectApplier.state(other).auras()) {
                if (other.distanceToSqr(player) <= aura.radius() * aura.radius()) {
                    out.addAll(aura.effects());
                }
            }
        }
        return out;
    }

    /** Every 20 ticks: rebuild if the set of auras reaching this player changed. */
    public static void tick(ServerPlayer player) {
        List<Object> key = new ArrayList<>();
        for (ServerPlayer other : player.level().players()) {
            if (other == player || !other.isAlive() || other.isSpectator()) {
                continue;
            }
            for (ClassState.Aura aura : ClassEffectApplier.state(other).auras()) {
                if (other.distanceToSqr(player) <= aura.radius() * aura.radius()) {
                    key.add(other.getUUID());
                    key.add(aura);
                }
            }
        }
        List<Object> last = LAST.put(player.getUUID(), key);
        if (last == null ? !key.isEmpty() : !last.equals(key)) {
            ClassService.changed(player);
        }
    }

    public static void forget(ServerPlayer player) {
        LAST.remove(player.getUUID());
    }
}
