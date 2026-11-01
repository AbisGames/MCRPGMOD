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
                          Optional<HolderSet<EntityType<?>>> against, int[] hitsLeft, int bonusRoll,
                          Optional<com.pipican.condition.ClassCondition> requires) {}

    private static final Map<UUID, List<Empower>> EMPOWERS = new ConcurrentHashMap<>();

    private Empowers() {}

    public static void add(Player player, long ticks, double multiplier, double bonus, List<MobEffectInstance> effects,
                           Optional<HolderSet<EntityType<?>>> against, int hits) {
        add(player, ticks, multiplier, bonus, effects, against, hits, 0, Optional.empty());
    }

    /**
     * {@code bonusRoll}: a further 1..N damage rolled on each empowered hit ("+1d6 vs undead"). {@code requires}: only hits
     * made while it holds use the empower up ("with the favored weapon").
     */
    public static void add(Player player, long ticks, double multiplier, double bonus, List<MobEffectInstance> effects,
                           Optional<HolderSet<EntityType<?>>> against, int hits, int bonusRoll, Optional<com.pipican.condition.ClassCondition> requires) {
        EMPOWERS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>())
                .add(new Empower(player.level().getGameTime() + ticks, multiplier, bonus, List.copyOf(effects), against, new int[] {hits, hits},
                        bonusRoll, requires));
    }

    /** How many live empowers {@code player} has with this multiplier and bonus (stored charges of one kind). */
    public static int count(Player player, double multiplier, double bonus) {
        List<Empower> list = EMPOWERS.get(player.getUUID());
        if (list == null) {
            return 0;
        }
        long now = player.level().getGameTime();
        synchronized (list) {
            return (int) list.stream().filter(e -> e.endsAt() > now && e.multiplier() == multiplier && e.bonus() == bonus).count();
        }
    }

    /** New damage for {@code attacker}'s hit on {@code target}, using up empowered hits; also applies their effects. */
    public static float apply(Player attacker, LivingEntity target, float amount) {
        List<Empower> list = EMPOWERS.get(attacker.getUUID());
        if (list == null) {
            return amount;
        }
        long now = attacker.level().getGameTime();
        boolean used = false;
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
                if (e.requires().isPresent() && !(attacker instanceof net.minecraft.server.level.ServerPlayer req && e.requires().get().test(req))) {
                    continue;
                }
                double roll = e.bonusRoll() > 0 ? 1 + attacker.getRandom().nextInt(e.bonusRoll()) : 0;
                amount = (float) (amount * e.multiplier() + e.bonus() + roll);
                used = true;
                // empower_knockback (Charged Blade): the first empowered hit also knocks the mob back.
                double push = attacker instanceof net.minecraft.server.level.ServerPlayer sp
                        ? com.pipican.player.ClassEffectApplier.state(sp).chance(com.pipican.effect.type.BonusEffect.Kind.EMPOWER_KNOCKBACK) : 0;
                if (push > 0 && e.hitsLeft()[0] == e.hitsLeft()[1]) {
                    target.knockback(push, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
                    target.hurtMarked = true;
                }
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
        if (used && attacker instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.pipican.event.MasteryEvents.did(sp, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.EMPOWERED_HIT,
                    sp.getMainHandItem(), Optional.empty(), Optional.of(target), amount));
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
