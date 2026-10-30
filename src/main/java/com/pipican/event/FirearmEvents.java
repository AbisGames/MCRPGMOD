package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

/** Gunslinger firearms. Until guns exist, crossbows are the firearm (as in the class's gear table). */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class FirearmEvents {

    private static final Map<UUID, Long> DRAWN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SHOTS = new ConcurrentHashMap<>();
    private static final String RICOCHET_TAG = "pipican_ricochet";
    private static boolean bouncing;

    private FirearmEvents() {}

    @SubscribeEvent
    static void onSwap(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSlot() == EquipmentSlot.MAINHAND
                && event.getTo().getItem() instanceof CrossbowItem && !(event.getFrom().getItem() instanceof CrossbowItem)) {
            DRAWN.put(player.getUUID(), player.level().getGameTime());
        }
    }

    @SubscribeEvent
    static void onShot(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof ServerPlayer player)
                || arrow.tickCount > 0 || arrow.getWeaponItem() == null || !(arrow.getWeaponItem().getItem() instanceof CrossbowItem)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        long now = player.level().getGameTime();
        // Accuracy: pull the shot toward where the player aims (or scatter it).
        double accuracy = state.chance(Kind.ACCURACY);
        if (accuracy != 0) {
            Vec3 v = arrow.getDeltaMovement();
            double speed = v.length();
            Vec3 aim = player.getLookAngle();
            Vec3 dir = accuracy > 0 ? v.normalize().lerp(aim, Math.min(1, accuracy)).normalize()
                    : v.normalize().add(player.getRandom().triangle(0, -accuracy * 0.3), player.getRandom().triangle(0, -accuracy * 0.3),
                            player.getRandom().triangle(0, -accuracy * 0.3)).normalize();
            arrow.setDeltaMovement(dir.scale(speed));
        }
        // Loud: hostile mobs near you come for you.
        double alert = state.chance(Kind.SHOT_ALERT);
        if (alert > 0) {
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(alert),
                    m -> m instanceof Enemy && m.getTarget() == null && m.distanceToSqr(player) <= alert * alert)) {
                mob.setTarget(player);
            }
        }
        // Quick Draw: the first shot after drawing the gun.
        Long drawn = DRAWN.remove(player.getUUID());
        if (drawn != null && now - drawn <= 100) {
            arrow.setBaseDamage(arrow.baseDamage * (1 + state.chance(Kind.QUICK_DRAW)));
        }
        if (state.chance(Kind.RICOCHET) > 0) {
            arrow.getPersistentData().putBoolean(RICOCHET_TAG, true);
        }
        // Cylinder: the crossbow reloads itself until the cylinder is spent; the last shot can crit.
        int cylinder = (int) Math.floor(state.chance(Kind.CYLINDER) + 1e-9);
        if (cylinder > 1) {
            int shot = SHOTS.merge(player.getUUID(), 1, Integer::sum);
            if (shot >= cylinder) {
                SHOTS.remove(player.getUUID());
                if (state.chance(Kind.LAST_SHOT_CRIT) > 0) {
                    arrow.setCritArrow(true);
                    arrow.setBaseDamage(arrow.baseDamage * 1.5);
                }
            } else {
                ItemStack crossbow = arrow.getWeaponItem();
                player.level().getServer().execute(() -> reload(player, crossbow));
            }
        }
    }

    private static void reload(ServerPlayer player, ItemStack weapon) {
        ItemStack crossbow = player.getMainHandItem().getItem() instanceof CrossbowItem ? player.getMainHandItem() : player.getOffhandItem();
        if (!(crossbow.getItem() instanceof CrossbowItem) || CrossbowItem.isCharged(crossbow)) {
            return;
        }
        ItemStack ammo = player.getProjectile(crossbow);
        if (ammo.isEmpty()) {
            SHOTS.remove(player.getUUID());
            return;
        }
        ItemStack round = ammo.copyWithCount(1);
        if (!player.hasInfiniteMaterials()) {
            ammo.shrink(1);
        }
        crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(java.util.List.of(round)));
    }

    /** Ricochet: part of a shot's damage bounces to the nearest other mob. */
    @SubscribeEvent
    static void onHit(LivingDamageEvent.Post event) {
        if (bouncing || !(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || !arrow.getPersistentData().getBooleanOr(RICOCHET_TAG, false)
                || !(arrow.getOwner() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        arrow.getPersistentData().putBoolean(RICOCHET_TAG, false);
        LivingEntity hit = event.getEntity();
        double share = ClassEffectApplier.state(player).chance(Kind.RICOCHET);
        level.getEntitiesOfClass(LivingEntity.class, hit.getBoundingBox().inflate(4), e -> e != hit && e != player && e.isAlive() && !(e instanceof Player)
                        && com.pipican.ability.Pets.owner(e) != player)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(hit), b.distanceToSqr(hit)))
                .ifPresent(next -> {
                    bouncing = true;
                    try {
                        next.hurtServer(level, level.damageSources().arrow(arrow, player), (float) (event.getInflictedDamage() * share));
                    } finally {
                        bouncing = false;
                    }
                });
    }
}
