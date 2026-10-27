package com.pipican.ability;

import com.pipican.ability.action.EffectAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * "Your next hit..." buffs from {@code pipican:empower} and {@code pipican:imbue}: a damage multiplier,
 * flat bonus damage and/or potion effects for the player's next {@code hits} hits within a time limit,
 * optionally only against some kinds of mob. Not saved.
 */
public final class Empowers {

    public record Empower(long endsAt, double multiplier, double bonus, List<MobEffectInstance> effects,
                          Optional<HolderSet<EntityType<?>>> against, int[] hitsLeft) {}

    private static final Map<UUID, List<Empower>> EMPOWERS = new ConcurrentHashMap<>();

    private Empowers() {}

    public static void add(Player player, long ticks, double multiplier, double bonus, List<MobEffectInstance> effects,
                           Optional<HolderSet<EntityType<?>>> against, int hits) {
        EMPOWERS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                .add(new Empower(player.level().getGameTime() + ticks, multiplier, bonus, List.copyOf(effects), against, new int[] {hits}));
    }

    /** New damage for {@code attacker}'s hit on {@code target}, using up empowered hits; also applies their effects. */
    public static float apply(Player attacker, LivingEntity target, float amount) {
        List<Empower> list = EMPOWERS.get(attacker.getUUID());
        if (list == null) {
            return amount;
        }
        long now = attacker.level().getGameTime();
        synchronized (list) {
            for (Iterator<Empower> it = list.iterator(); it.hasNext(); ) {
                Empower e = it.next();
                if (e.endsAt() <= now) {
                    it.remove();
                    continue;
                }
                if (e.against().isPresent() && !e.against().get().contains(target.getType().builtInRegistryHolder())) {
                    continue;
                }
                amount = (float) (amount * e.multiplier() + e.bonus());
                for (MobEffectInstance effect : e.effects()) {
                    target.addEffect(new MobEffectInstance(effect), attacker);
                }
                if (--e.hitsLeft()[0] <= 0) {
                    it.remove();
                }
            }
            if (list.isEmpty()) {
                EMPOWERS.remove(attacker.getUUID());
            }
        }
        return amount;
    }

    public static boolean has(Player player) {
        return EMPOWERS.containsKey(player.getUUID());
    }

    public static void forget(Player player) {
        EMPOWERS.remove(player.getUUID());
    }

    /** Converts an effect entry list into instances (helper for actions). */
    public static List<MobEffectInstance> instances(List<EffectAction.Entry> entries) {
        return entries.stream().map(EffectAction.Entry::create).toList();
    }
}
