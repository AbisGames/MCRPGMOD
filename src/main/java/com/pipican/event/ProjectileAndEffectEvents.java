package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.event.MasteryEvents;
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
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getSource().getDirectEntity() == attacker
                && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            double floor = ClassEffectApplier.state(attacker).chance(Kind.MIN_MELEE_DAMAGE);
            if (event.getAmount() < floor) {
                event.setAmount((float) floor);
            }
        }
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
        if (event.getEntity() instanceof ServerPlayer warded && event.getSource().is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)
                && event.getSource().getEntity() != warded && warded.getRandom().nextDouble() < ClassEffectApplier.state(warded).chance(Kind.SPELL_WARD)) {
            event.setCanceled(true);
            return;
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
        double ward = state.chance(Kind.SPELL_WARD);
        if (ward > 0 && event.getEffectSource() != null && event.getEffectSource() != player && player.getRandom().nextDouble() < ward) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
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
        if (event.getEffectInstance().getEffect().is(net.minecraft.world.effect.MobEffects.POISON)) {
            MasteryEvents.did(source, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.POISON, ItemStack.EMPTY,
                    java.util.Optional.empty(), java.util.Optional.of(event.getEntity()), 0));
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
    static void onPearl(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() && !event.getLevel().isClientSide()
                && event.getEntity() instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl pearl
                && pearl.getOwner() instanceof ServerPlayer player && pearl.tickCount == 0) {
            double range = ClassEffectApplier.state(player).bonus(Kind.PEARL_RANGE);
            if (range != 1) {
                pearl.setDeltaMovement(pearl.getDeltaMovement().scale(range));
            }
            double cooldown = ClassEffectApplier.state(player).bonus(Kind.PEARL_COOLDOWN);
            if (cooldown != 1) {
                // The item puts its own 1s cooldown on after the pearl is spawned; replace it next.
                int ticks = (int) Math.round(20 * cooldown);
                player.level().getServer().execute(() -> {
                    var cooldowns = player.getCooldowns();
                    var group = cooldowns.getCooldownGroup(new ItemStack(Items.ENDER_PEARL));
                    cooldowns.removeCooldown(group);
                    if (ticks > 0) {
                        cooldowns.addCooldown(group, ticks);
                    }
                });
            }
        }
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (busy || event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer player) || arrow.tickCount > 0) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        int pierce = (int) Math.floor(state.chance(Kind.ARROW_PIERCE) + 1e-9);
        if (pierce > 0) {
            arrow.setPierceLevel((byte) Math.min(127, arrow.getPierceLevel() + pierce));
        }
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
    static void onBonemeal(net.neoforged.neoforge.event.entity.player.BonemealEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !event.isValidBonemealTarget()) {
            return;
        }
        double chance = ClassEffectApplier.state(player).chance(Kind.BONE_MEAL);
        if (chance > 0 && player.getRandom().nextDouble() < chance
                && event.getState().getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock plant
                && plant.isBonemealSuccess(level, level.getRandom(), event.getPos(), event.getState())) {
            plant.performBonemeal(level, level.getRandom(), event.getPos(), event.getState());
        }
    }

    @SubscribeEvent
    static void onBreed(net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) {
            return;
        }
        double faster = Math.min(1, Math.max(0, ClassEffectApplier.state(player).chance(Kind.BREEDING_SPEED)));
        if (faster > 0) {
            // Runs before vanilla sets the parents' 5-minute cooldown, so shorten it right after.
            player.level().getServer().execute(() -> {
                for (var parent : java.util.List.of(event.getParentA(), event.getParentB())) {
                    if (parent instanceof net.minecraft.world.entity.AgeableMob a && a.getAge() > 0) {
                        a.setAge((int) Math.round(a.getAge() * (1 - faster)));
                    }
                }
            });
        }
    }

    @SubscribeEvent
    static void onSleep(net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent event) {
        if (event.getProblem() == null && ClassEffectApplier.state(event.getEntity()).chance(Kind.NO_SLEEP) > 0) {
            event.setProblem(net.minecraft.world.entity.player.Player.BedSleepingProblem.OTHER_PROBLEM);
        }
    }

    @SubscribeEvent
    static void onEat(net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.FOOD)) {
            com.pipican.ability.Reactions.fire(player, null, event.getItem(), com.pipican.effect.type.ReactionEffect.Event.EAT);
        }
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(DataComponents.POTION_CONTENTS)) {
            com.pipican.ability.Reactions.fire(player, null, event.getItem(), com.pipican.effect.type.ReactionEffect.Event.DRINK);
        }
    }

    @SubscribeEvent
    static void onUseItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.getCooldowns().isOnCooldown(event.getItemStack())) {
            com.pipican.ability.Reactions.fire(player, null, event.getItemStack(), com.pipican.effect.type.ReactionEffect.Event.USE_ITEM);
            MasteryEvents.did(player, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.USE_ITEM, event.getItemStack(),
                    java.util.Optional.empty(), java.util.Optional.empty(), 0));
        }
    }

    @SubscribeEvent
    static void onExplosionHit(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && event.getSource().getEntity() instanceof ServerPlayer player
                && event.getEntity() != player) {
            MasteryEvents.did(player, new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.EXPLOSION_HIT, ItemStack.EMPTY,
                    java.util.Optional.empty(), java.util.Optional.of(event.getEntity()), event.getInflictedDamage()));
        }
    }

    @SubscribeEvent
    static void onInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof net.minecraft.world.entity.npc.villager.AbstractVillager
                && ClassEffectApplier.state(player).chance(Kind.NO_TRADE) > 0) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("pipican.no_trade"));
            event.setCanceled(true);
        }
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                && event.getTarget() instanceof net.minecraft.world.entity.LivingEntity target) {
            com.pipican.ability.Reactions.fire(player, target, event.getItemStack(), com.pipican.effect.type.ReactionEffect.Event.INTERACT);
        }
    }

    @SubscribeEvent
    static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ClassEffectApplier.state(player).chance(Kind.NO_TRAMPLE) > 0) {
            event.setCanceled(true);
        }
    }
}
