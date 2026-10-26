package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.core.PipicanConfig;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

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
        if (stack.isEmpty() || slot == EquipmentSlot.OFFHAND) {
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
        }
        if (amount != event.getAmount()) {
            event.setAmount(amount);
        }
    }

    // ------------------------------------------------------------------ class XP, death

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        // Killing a mob gives class XP based on its max health.
        if (event.getSource().getEntity() instanceof ServerPlayer killer && !(event.getEntity() instanceof Player)) {
            long xp = Math.round(event.getEntity().getMaxHealth() * PipicanConfig.killXpPerMaxHealth());
            ClassService.addXp(killer, xp);
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
        // Special-stat resource regen. While regenerating, the client is synced twice a second.
        PlayerClassData data = player.getData(PipicanAttachments.CLASS_DATA);
        if (state.resourceMax() > 0 && data.resource() < state.resourceMax()) {
            data.setResource(Math.min(state.resourceMax(), data.resource() + state.resourceRegenPerTick()));
            if (player.tickCount % 10 == 0 || data.resource() >= state.resourceMax()) {
                player.syncData(PipicanAttachments.CLASS_DATA);
            }
        }
        if (player.tickCount % 40 == 0) {
            ClassEffectApplier.maintainMobEffects(player);
        }
    }
}
