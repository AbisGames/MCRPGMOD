package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.mastery.MoveMode;
import com.pipican.mastery.SkillAction;
import com.pipican.player.MasteryService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Turns game events into {@link SkillAction}s for skill mastery. One handler per kind of action;
 * which skills react is decided by their JSON triggers, not here.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class MasteryEvents {

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Moves longer than this in one tick are teleports and don't count. */
    private static final double MAX_STEP = 8.0;
    private static final Map<UUID, Vec3> LAST_POSITION = new ConcurrentHashMap<>();

    private MasteryEvents() {}

    /** Hits dealt (melee / ranged) and hits taken while wearing armor. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            ItemStack weapon = source.getWeaponItem() != null ? source.getWeaponItem() : ItemStack.EMPTY;
            if (source.getDirectEntity() instanceof Projectile) {
                MasteryService.onAction(attacker, new SkillAction.RangedHit(weapon));
            } else if (source.getDirectEntity() == attacker) {
                MasteryService.onAction(attacker, new SkillAction.MeleeHit(weapon));
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim) {
            List<ItemStack> armor = new ArrayList<>();
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = victim.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    armor.add(stack);
                }
            }
            if (!armor.isEmpty()) {
                MasteryService.onAction(victim, new SkillAction.HurtWearing(armor));
            }
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer) {
            ItemStack weapon = event.getSource().getWeaponItem();
            MasteryService.onAction(killer, new SkillAction.Kill(weapon != null ? weapon : ItemStack.EMPTY));
        }
    }

    @SubscribeEvent
    public static void onBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() && event.getEntity() instanceof ServerPlayer player) {
            MasteryService.onAction(player, new SkillAction.Block(player.getUseItem()));
        }
    }

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MasteryService.onAction(player, new SkillAction.Jumped());
        }
    }

    /** Movement distance per tick, plus batched mastery sync. */
    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MasteryService.tick(player);
        Vec3 now = player.position();
        Vec3 last = LAST_POSITION.put(player.getUUID(), now);
        if (last == null || player.isPassenger() || player.isSpectator() || player.getAbilities().flying) {
            return;
        }
        double distance = now.distanceTo(last);
        if (distance > 0.01 && distance < MAX_STEP) {
            MasteryService.onAction(player, new SkillAction.Moved(MoveMode.of(player), distance));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_POSITION.remove(player.getUUID());
            MasteryService.forget(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_POSITION.remove(event.getEntity().getUUID());
    }
}
