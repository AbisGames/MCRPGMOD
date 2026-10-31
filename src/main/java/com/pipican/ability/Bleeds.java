package com.pipican.ability;

import java.util.Optional;
import com.pipican.mastery.SkillAction;
import com.pipican.event.MasteryEvents;
import com.pipican.player.ClassState;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Bleeding mobs ({@code pipican:bleed}): damage once a second, credited to the player who caused it. */
public final class Bleeds {

    private record Wound(UUID attacker, double damage, long until) {}

    private static final Map<LivingEntity, List<Wound>> WOUNDS = new WeakHashMap<>();
    /** True while bleed damage is being dealt, so it doesn't count as a new hit (no reactions, no new bleed). */
    private static boolean ticking;

    private Bleeds() {}

    public static boolean ticking() {
        return ticking;
    }

    public static boolean isBleeding(LivingEntity entity) {
        List<Wound> wounds = WOUNDS.get(entity);
        return wounds != null && !wounds.isEmpty();
    }

    /** A damage-over-time wound from {@code player} ({@code pipican:dot}): {@code damage} per second for {@code seconds}. */
    public static void wound(ServerPlayer player, LivingEntity victim, double damage, double seconds, int maxStacks) {
        long now = victim.level().getGameTime();
        List<Wound> wounds = WOUNDS.computeIfAbsent(victim, k -> new ArrayList<>());
        wounds.removeIf(w -> w.until <= now);
        if (wounds.size() >= Math.max(1, maxStacks) + 8) {
            wounds.removeFirst();
        }
        wounds.add(new Wound(player.getUUID(), damage, now + Math.round(seconds * 20)));
    }

    /** {@code player} hit {@code victim}; {@code crit} if it was a critical hit. */
    public static void onHit(ServerPlayer player, LivingEntity victim, ClassState.Bleed bleed, boolean crit) {
        if (bleed.damage() <= 0 || bleed.seconds() <= 0 || !(crit || bleed.anyHit()) || !victim.isAlive()) {
            return;
        }
        if (bleed.weapons().isPresent() && !player.getMainHandItem().is(bleed.weapons().get())) {
            return;
        }
        long now = victim.level().getGameTime();
        List<Wound> wounds = WOUNDS.computeIfAbsent(victim, k -> new ArrayList<>());
        wounds.removeIf(w -> w.until <= now);
        if (wounds.size() >= Math.max(1, bleed.maxStacks())) {
            wounds.removeFirst();
        }
        long ticks = Math.round(bleed.seconds() * 20);
        wounds.add(new Wound(player.getUUID(), bleed.damage(), now + ticks));
        MasteryEvents.did(player, new SkillAction.Did(SkillAction.Did.Kind.BLEED, player.getMainHandItem(), Optional.empty(), Optional.of(victim), bleed.damage()));
        if (bleed.slows()) {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) ticks, 0), player);
        }
    }

    public static void tick(MinecraftServer server) {
        if (WOUNDS.isEmpty() || server.getTickCount() % 20 != 0) {
            return;
        }
        ticking = true;
        try {
            Iterator<Map.Entry<LivingEntity, List<Wound>>> it = WOUNDS.entrySet().iterator();
            List<Runnable> hits = new ArrayList<>();
            while (it.hasNext()) {
                var e = it.next();
                LivingEntity victim = e.getKey();
                long now = victim.level().getGameTime();
                e.getValue().removeIf(w -> w.until < now);
                if (!victim.isAlive() || e.getValue().isEmpty()) {
                    it.remove();
                    continue;
                }
                for (Wound w : e.getValue()) {
                    ServerPlayer attacker = server.getPlayerList().getPlayer(w.attacker);
                    if (victim.level() instanceof ServerLevel level) {
                        var source = attacker != null ? level.damageSources().indirectMagic(attacker, attacker) : level.damageSources().magic();
                        hits.add(() -> {
                            victim.invulnerableTime = 0;
                            victim.hurtServer(level, source, (float) w.damage);
                        });
                    }
                }
            }
            // Outside the loop: a bleeding mob dying changes the map.
            hits.forEach(Runnable::run);
        } finally {
            ticking = false;
        }
    }
}
