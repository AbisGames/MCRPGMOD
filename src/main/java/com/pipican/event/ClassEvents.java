package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.AbilityService;
import com.pipican.ability.Bonds;
import com.pipican.ability.Empowers;
import com.pipican.ability.HitShields;
import com.pipican.ability.Marks;
import com.pipican.ability.MobControl;
import com.pipican.ability.Optimize;
import com.pipican.ability.Reactions;
import com.pipican.ability.Scry;
import com.pipican.ability.Summons;
import com.pipican.core.PipicanConfig;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game-event glue for the class system. Everything here is thin: it decides <i>when</i> to act and
 * delegates the <i>what</i> to {@link ClassService} / {@link ClassEffectApplier}.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class ClassEvents {

    private ClassEvents() {}

    // ------------------------------------------------------------------ lifecycle -> refresh

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassService.changed(player);
            if (PipicanConfig.openSelectionOnJoin()) {
                ClassService.promptSelection(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.forget(player);
            Reactions.forget(player);
            Empowers.forget(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.refresh(player);
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassEffectApplier.refresh(player);
        }
    }

    /** /reload can change item tags, which changes affinities and trinket slot rules. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            event.getRelevantPlayers().forEach(ClassService::changed);
        }
    }

    // ------------------------------------------------------------------ equipment rules

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassEffectApplier.refresh(player);
        warnAboutAffinity(player, event.getSlot(), event.getTo());
    }

    /** Tells the player (action bar) when they equip something their class is bad with. */
    private static void warnAboutAffinity(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        // Skip the first seconds after joining/respawning: equipment "changes" then are just syncing.
        if (stack.isEmpty() || slot == EquipmentSlot.OFFHAND || player.tickCount < 40) {
            return;
        }
        Optional<RpgClass> rpgClass = ClassService.activeClass(player);
        if (rpgClass.isEmpty()) {
            return;
        }
        var affinity = rpgClass.get().affinities().resolve(stack);
        if (affinity.ordinal() > com.pipican.rpgclass.Affinity.NEUTRAL.ordinal()) {
            player.sendOverlayMessage(Component.translatable("pipican.affinity.warning",
                    Component.translatable(affinity.translationKey()).withStyle(affinity.color()), stack.getHoverName()));
        }
    }

    // ------------------------------------------------------------------ damage

    /** Applies damage_taken / damage_dealt effects and weapon affinity. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Twist Fate / Intercept: the next hit misses, or lands on the guard instead.
        if (!event.getEntity().level().isClientSide()) {
            var shield = HitShields.take(event.getEntity());
            if (shield.isPresent()) {
                event.setCanceled(true);
                shield.get().guard().map(id -> event.getEntity().level().getServer().getPlayerList().getPlayer(id))
                        .ifPresent(guard -> guard.hurtServer(guard.level(), event.getSource(), event.getAmount()));
                return;
            }
        }
        float amount = event.getAmount();
        if (event.getEntity() instanceof ServerPlayer victim) {
            amount *= (float) ClassState.multiplier(ClassEffectApplier.state(victim).damageTaken(), event.getSource());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            ClassState state = ClassEffectApplier.state(attacker);
            amount *= (float) ClassState.multiplier(state.damageDealt(), event.getSource());
            ItemStack weapon = event.getSource().getWeaponItem();
            if (weapon != null && !weapon.isEmpty() && state.rpgClass().isPresent()) {
                amount *= (float) state.rpgClass().get().affinities().resolve(weapon).tuning().damage();
            }
            // Ability marks on the target and "your next hit" buffs.
            amount *= (float) Marks.multiplier(attacker, event.getEntity(), event.getSource());
            amount = Empowers.apply(attacker, event.getEntity(), amount);
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    // ------------------------------------------------------------------ class XP, death

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // Killing a mob gives kill XP based on its max health: class XP and stat points.
        if (event.getSource().getEntity() instanceof ServerPlayer killer && !(event.getEntity() instanceof Player)) {
            long xp = Math.round(event.getEntity().getMaxHealth() * PipicanConfig.killXpPerMaxHealth());
            ClassService.addXp(killer, xp);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            Reactions.fire(killer, event.getEntity(), event.getSource().getWeaponItem(), ReactionEffect.Event.KILL);
        }
        Marks.forget(event.getEntity());
        // Running ability states end on death; so does Optimization.
        if (event.getEntity() instanceof ServerPlayer player) {
            AbilityService.clearActive(player);
            Empowers.forget(player);
            Optimize.onDeath(player);
        }
        // Trinkets drop on death like the rest of the inventory, unless keepInventory is on.
        if (event.getEntity() instanceof ServerPlayer player && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
            ClassService.dropTrinkets(player);
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        if (state.rpgClass().isEmpty()) {
            return;
        }
        // Special-stat resource regen (or drain, for bars with negative regen). While it changes, the
        // client is synced twice a second.
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        double regen = state.resourceRegenPerTick();
        if (state.resourceMax() > 0 && (regen > 0 && data.resource() < state.resourceMax() || regen < 0 && data.resource() > 0)) {
            data.setResource(Math.clamp(data.resource() + regen, 0, state.resourceMax()));
            if (player.tickCount % 10 == 0 || data.resource() >= state.resourceMax() || data.resource() <= 0) {
                player.syncData(PipicanAttachments.CLASS_DATA);
            }
        }
        AbilityService.tick(player);
        // Conditional effects ("while wet", "at night"...): rebuild bonuses when a condition flips.
        if (player.tickCount % 10 == 0 && ClassEffectApplier.recheckConditions(player)) {
            state = ClassEffectApplier.state(player);
        }
        if (player.tickCount % 40 == 0) {
            ClassEffectApplier.maintainMobEffects(player);
        }
    }

    // ------------------------------------------------------------------ ability summons

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Summons.tick(event.getServer());
        MobControl.tick(event.getServer());
        Scry.tick(event.getServer());
    }

    /** Summons left over from before a restart are not loaded back in. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && Summons.isLeftover(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ ability reactions & control

    /** {@code pipican:on} reactions for hits dealt and taken (after armor, when the hit really landed). */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            Reactions.onHit(attacker, event.getEntity(), event.getSource());
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity l ? l : null;
            Reactions.fire(victim, attacker, event.getSource().getWeaponItem(), ReactionEffect.Event.HURT);
        }
    }

    @SubscribeEvent
    public static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player) {
            LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity l ? l : null;
            Reactions.fire(player, attacker, player.getUseItem(), ReactionEffect.Event.BLOCK);
        }
    }

    /** Charmed, frightened and allied mobs may not pick some targets. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!MobControl.mayTarget(event.getEntity(), event.getNewAboutToBeSetTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Bonds.onHeal(player, event.getAmount());
        }
    }

    @SubscribeEvent
    public static void onWake(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.wakeImmediately()) {
            AbilityService.onWake(player);
        }
    }
}
