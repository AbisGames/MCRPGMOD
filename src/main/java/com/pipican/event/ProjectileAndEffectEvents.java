package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Bonus kinds about projectiles, armor, food and effects: {@code armor_pierce}, {@code projectile_speed},
 * {@code extra_arrow}, {@code food_safe}, {@code no_trample}, {@code witch_ward}, and {@code pipican:applied_effect}.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class ProjectileAndEffectEvents {

    /** Set while this class adds effects or arrows itself, so it doesn't react to its own work. */
    private static boolean busy;

    private ProjectileAndEffectEvents() {}

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        // Clouds from pipican:cloud reactions never hurt the player who left them.
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.AreaEffectCloud && event.getSource().getEntity() == event.getEntity()
                && event.getEntity() instanceof ServerPlayer) {
            event.setCanceled(true);
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ClassState state = ClassEffectApplier.state(player);
            double pierce = state.chance(Kind.ARMOR_PIERCE);
            if (event.getSource().getDirectEntity() instanceof Projectile) {
                pierce += state.chance(Kind.RANGED_ARMOR_PIERCE);
            }
            double kept = 1 - Math.min(1, Math.max(0, pierce));
            if (kept < 1) {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> (float) (reduction * kept));
            }
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() instanceof AbstractThrownPotion
                && event.getSource().getEntity() instanceof Witch && ClassEffectApplier.state(player).chance(Kind.WITCH_WARD) > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getEffectInstance().getEffect().value().isBeneficial()) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        if (event.getEffectSource() instanceof Witch && state.chance(Kind.WITCH_WARD) > 0) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        } else if (state.chance(Kind.FOOD_SAFE) > 0 && player.isUsingItem() && player.getUseItem().has(DataComponents.FOOD)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Runs before {@link BonusEvents#onEffectAdded}'s duration bonuses (normal priority there). */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    static void onEffectAdded(MobEffectEvent.Added event) {
        if (busy || !(event.getEffectSource() instanceof ServerPlayer source) || source == event.getEntity()) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        ClassState.AppliedRule rule = ClassEffectApplier.state(source).appliedRules().get(effect.getEffect());
        if (rule == null || effect.isInfiniteDuration()) {
            return;
        }
        if (!effect.getEffect().value().isInstantenous()) {
            effect.duration = Math.max(1, (int) Math.round(effect.duration * (1 + rule.duration()) + rule.seconds() * 20));
        }
        effect.amplifier = Math.max(effect.amplifier, rule.minAmplifier());
        int duration = effect.duration;
        busy = true;
        try {
            for (Map.Entry<Holder<MobEffect>, Integer> also : rule.also().entrySet()) {
                event.getEntity().addEffect(new MobEffectInstance(also.getKey(), duration, also.getValue()), source);
            }
        } finally {
            busy = false;
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (busy || event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player) || arrow.tickCount > 0) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        double speed = state.bonus(Kind.PROJECTILE_SPEED);
        if (speed != 1) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(speed));
        }
        double extra = state.chance(Kind.EXTRA_ARROW);
        if (extra > 0 && player.getRandom().nextDouble() < extra) {
            Arrow copy = new Arrow(event.getLevel(), player, new ItemStack(Items.ARROW), arrow.getWeaponItem());
            copy.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            copy.setCritArrow(arrow.isCritArrow());
            Vec3 v = arrow.getDeltaMovement();
            copy.setDeltaMovement(v.yRot((float) Math.toRadians(4)));
            busy = true;
            try {
                event.getLevel().addFreshEntity(copy);
            } finally {
                busy = false;
            }
        }
    }

    @SubscribeEvent
    static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ClassEffectApplier.state(player).chance(Kind.NO_TRAMPLE) > 0) {
            event.setCanceled(true);
        }
    }
}
