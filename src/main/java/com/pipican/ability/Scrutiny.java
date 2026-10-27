package com.pipican.ability;

import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** {@code pipican:scrutiny}: keep looking at a mob long enough and it is marked (and glows). */
public final class Scrutiny {

    private record Look(UUID target, long since) {}

    private static final Map<UUID, Look> LOOKING = new ConcurrentHashMap<>();

    private Scrutiny() {}

    /** Every 5 ticks. */
    public static void tick(ServerPlayer player) {
        ClassState.Scrutiny s = ClassEffectApplier.state(player).scrutiny();
        if (s.seconds() <= 0) {
            LOOKING.remove(player.getUUID());
            return;
        }
        LivingEntity target = Targets.lookTarget(player, 24);
        if (target == null || target instanceof Player) {
            LOOKING.remove(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Look look = LOOKING.get(player.getUUID());
        if (look == null || !look.target.equals(target.getUUID())) {
            LOOKING.put(player.getUUID(), new Look(target.getUUID(), now));
            return;
        }
        if (now - look.since >= Math.max(0, s.lookSeconds()) * 20 && !Marks.isMarkedBy(target, player)) {
            long ticks = Math.round(s.seconds() * 20);
            Marks.add(target, player, ticks, 1.0, false, false, false);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) ticks, 0, false, false), player);
        }
    }

    public static void forget(ServerPlayer player) {
        LOOKING.remove(player.getUUID());
    }
}
