package com.pipican.ability;

import com.pipican.effect.type.BonusEffect;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Extra strikes after a melee hit lands: {@code pipican:cleave} (nearby mobs) and off-hand swings. */
public final class Strikes {

    private static final Map<UUID, Double> OFFHAND = new ConcurrentHashMap<>();
    /** True while extra strikes are being dealt, so they don't trigger more extra strikes. */
    private static boolean striking;

    private static final Map<UUID, int[]> NTH = new ConcurrentHashMap<>();
    private static final Map<UUID, LivingEntity> LAST_TARGET = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> STRIKE_AGAIN = ConcurrentHashMap.newKeySet();

    private Strikes() {}

    /**
     * {@code pipican:nth_hit}: counts the player's melee swings and returns the damage multiplier for this one
     * (and queues the second strike). Extra strikes don't count.
     */
    public static double nthHit(ServerPlayer player, LivingEntity target, net.minecraft.world.damagesource.DamageSource source) {
        if (striking || source.getDirectEntity() != player || !source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            return 1;
        }
        var hits = ClassEffectApplier.state(player).nthHits();
        if (hits.isEmpty()) {
            return 1;
        }
        int[] counts = NTH.compute(player.getUUID(), (k, v) -> v == null || v.length != hits.size() ? new int[hits.size()] : v);
        LivingEntity last = LAST_TARGET.put(player.getUUID(), target);
        double mult = 1;
        for (int i = 0; i < hits.size(); i++) {
            ClassState.NthHit h = hits.get(i);
            if (h.weapons().isPresent() && !player.getMainHandItem().is(h.weapons().get())) {
                continue;
            }
            if (h.sameTarget() && last != target) {
                counts[i] = 0;
            }
            if (++counts[i] >= h.every()) {
                counts[i] = 0;
                mult *= h.multiplier();
                if (h.strikeTwice()) {
                    STRIKE_AGAIN.add(player.getUUID());
                }
            }
        }
        return mult;
    }

    public static void after(ServerPlayer player, LivingEntity target, float damage) {
        if (striking || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        striking = true;
        try {
            if (STRIKE_AGAIN.remove(player.getUUID()) && target.isAlive() && damage > 0) {
                target.invulnerableTime = 0;
                target.hurtServer(level, level.damageSources().playerAttack(player), damage);
            }
            cleave(player, target, damage, state.cleave(), level);
            // Magic portion: a share of the hit again as magic (ignores armor).
            double portion = state.chance(BonusEffect.Kind.MAGIC_PORTION);
            if (portion > 0 && damage > 0 && target.isAlive()) {
                target.invulnerableTime = 0;
                target.hurtServer(level, level.damageSources().indirectMagic(player, player), (float) (damage * portion));
            }
            offhand(player, target, state, level);
        } finally {
            striking = false;
        }
    }

    private static void cleave(ServerPlayer player, LivingEntity target, float damage, ClassState.Cleave c, ServerLevel level) {
        if (c.fraction() <= 0 || c.radius() <= 0 || damage <= 0) {
            return;
        }
        if (c.chargedOnly() && !Charged.wasCharged(player)) {
            return;
        }
        if (c.weapons().isPresent() && !player.getMainHandItem().is(c.weapons().get())) {
            return;
        }
        int hit = 0;
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(c.radius()),
                e -> e != target && e != player && e.isAlive() && !(e instanceof Player) && !Summons.isSummonOf(e, player))) {
            if (c.maxTargets() > 0 && hit >= c.maxTargets()) {
                break;
            }
            other.hurtServer(level, level.damageSources().playerAttack(player), (float) (damage * c.fraction()));
            hit++;
        }
    }

    private static void offhand(ServerPlayer player, LivingEntity target, ClassState state, ServerLevel level) {
        double share = state.chance(BonusEffect.Kind.OFFHAND_SWING);
        ItemStack off = player.getOffhandItem();
        if (share <= 0 || !SpecialItems.isWeapon(off) || !target.isAlive()) {
            return;
        }
        double total = OFFHAND.getOrDefault(player.getUUID(), 0.0) + Math.min(1, share);
        if (total < 1) {
            OFFHAND.put(player.getUUID(), total);
            return;
        }
        OFFHAND.put(player.getUUID(), total - 1);
        double[] attack = {1};
        off.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                attack[0] += modifier.amount();
            }
        });
        target.invulnerableTime = 0;
        // Two Weapon Mastery: the off-hand follow-up on the same mob hits harder.
        target.hurtServer(level, level.damageSources().playerAttack(player), (float) (attack[0] * state.bonus(BonusEffect.Kind.OFFHAND_COMBO)));
        player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
    }
}
