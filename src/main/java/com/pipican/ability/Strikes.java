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

    private Strikes() {}

    public static void after(ServerPlayer player, LivingEntity target, float damage) {
        if (striking || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        striking = true;
        try {
            cleave(player, target, damage, state.cleave(), level);
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
        target.hurtServer(level, level.damageSources().playerAttack(player), (float) attack[0]);
        player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
    }
}
