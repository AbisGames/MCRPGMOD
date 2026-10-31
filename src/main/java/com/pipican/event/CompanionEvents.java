package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Light you give off, staffs that come back, pearls that carry a friend, mounts that return, sweeping executes. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class CompanionEvents {

    private static final Map<UUID, BlockPos> LIGHT = new ConcurrentHashMap<>();
    private static final Map<UUID, List<ItemStack>> STAFFS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SWEEP_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> SWEEP_TARGET = new ConcurrentHashMap<>();
    private static final String RETURNING = "pipican_returning_mounts";

    private CompanionEvents() {}

    private static boolean staff(ItemStack stack) {
        return com.pipican.ability.SpecialItems.isStaff(stack);
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 5 == 0) {
            light(player, level);
        }
        if (player.tickCount % 200 == 0) {
            returnMounts(player, level);
        }
    }

    /** {@code emit_light}: an invisible light block follows the player. */
    private static void light(ServerPlayer player, ServerLevel level) {
        int value = (int) Math.min(15, Math.floor(ClassEffectApplier.state(player).chance(Kind.EMIT_LIGHT) + 1e-9));
        BlockPos old = LIGHT.get(player.getUUID());
        BlockPos want = value > 0 && player.isAlive() ? BlockPos.containing(player.getEyePosition()) : null;
        if (old != null && (want == null || !old.equals(want) || level.getBlockState(old).getValue(LightBlock.LEVEL) != value)) {
            if (level.getBlockState(old).is(Blocks.LIGHT)) {
                level.setBlock(old, Blocks.AIR.defaultBlockState(), 3);
            }
            LIGHT.remove(player.getUUID());
        }
        if (want != null && !want.equals(LIGHT.get(player.getUUID())) && level.getBlockState(want).isAir()) {
            level.setBlock(want, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, value), 3);
            LIGHT.put(player.getUUID(), want.immutable());
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BlockPos old = LIGHT.remove(event.getEntity().getUUID());
        if (old != null && event.getEntity().level().getBlockState(old).is(Blocks.LIGHT)) {
            event.getEntity().level().setBlock(old, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** {@code staff_return}: a dropped staff goes straight back into the inventory. */
    @SubscribeEvent
    static void onDrop(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof ItemEntity item) || !staff(item.getItem())
                || !(item.getOwner() instanceof ServerPlayer player) || ClassEffectApplier.state(player).chance(Kind.STAFF_RETURN) <= 0 || !player.isAlive()) {
            return;
        }
        if (player.getInventory().add(item.getItem().copy())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onDeathDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ClassEffectApplier.state(player).chance(Kind.STAFF_RETURN) <= 0) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>();
        event.getDrops().removeIf(drop -> {
            if (staff(drop.getItem())) {
                kept.add(drop.getItem().copy());
                return true;
            }
            return false;
        });
        if (!kept.isEmpty()) {
            STAFFS.put(player.getUUID(), kept);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.Clone event) {
        List<ItemStack> kept = event.isWasDeath() ? STAFFS.remove(event.getOriginal().getUUID()) : null;
        if (kept != null) {
            kept.forEach(stack -> event.getEntity().getInventory().add(stack));
        }
    }

    /** {@code pearl_carry}: the nearest player within 3 blocks lands with you. */
    @SubscribeEvent
    static void onPearl(EntityTeleportEvent.EnderPearl event) {
        ServerPlayer player = event.getPlayer();
        if (ClassEffectApplier.state(player).chance(Kind.PEARL_CARRY) <= 0) {
            return;
        }
        player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(3), p -> p != player && p.isAlive() && !p.isSpectator())
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .ifPresent(friend -> friend.teleportTo(event.getTargetX() + 0.5, event.getTargetY(), event.getTargetZ() + 0.5));
    }

    /** {@code mount_return}: a mount that dies is saved and comes back at the respawn point after a day. */
    @SubscribeEvent
    static void onMountDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractHorse horse) || !horse.isTamed() || !(horse.level() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = com.pipican.ability.Pets.owner(horse);
        if (owner == null || ClassEffectApplier.state(owner).chance(Kind.MOUNT_RETURN) <= 0) {
            return;
        }
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        horse.saveAsPassenger(output);
        CompoundTag saved = output.buildResult();
        saved.putLong("pipican_due", level.getGameTime() + 24000);
        ListTag list = owner.getPersistentData().getListOrEmpty(RETURNING);
        list.add(saved);
        owner.getPersistentData().put(RETURNING, list);
    }

    private static void returnMounts(ServerPlayer player, ServerLevel level) {
        ListTag list = player.getPersistentData().getListOrEmpty(RETURNING);
        if (list.isEmpty()) {
            return;
        }
        ListTag waiting = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saved = list.getCompoundOrEmpty(i);
            if (saved.getLongOr("pipican_due", 0) > level.getGameTime()) {
                waiting.add(saved);
                continue;
            }
            var config = player.getRespawnConfig();
            var home = config != null ? config.respawnData() : level.getRespawnData();
            ServerLevel homeLevel = level.getServer().getLevel(home.dimension());
            if (homeLevel == null) {
                continue;
            }
            CompoundTag copy = saved.copy();
            copy.remove("pipican_due");
            BlockPos at = home.pos().above();
            var horse = EntityType.loadEntityRecursive(copy, homeLevel, EntitySpawnReason.MOB_SUMMONED, e -> {
                e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, e.getYRot(), e.getXRot());
                return e;
            });
            if (horse instanceof LivingEntity living) {
                living.setHealth(living.getMaxHealth());
                living.clearFire();
                homeLevel.addFreshEntity(living);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("pipican.mount_returned"));
            }
        }
        player.getPersistentData().put(RETURNING, waiting);
    }

    /** {@code sweep_execute}: remembers the main target of a sweep... */
    @SubscribeEvent
    static void onSweep(SweepAttackEvent event) {
        if (event.isSweeping() && event.getEntity() instanceof ServerPlayer player) {
            SWEEP_TICK.put(player.getUUID(), player.level().getGameTime());
            SWEEP_TARGET.put(player.getUUID(), event.getTarget().getUUID());
        }
    }

    /** ...so the other mobs it hits that tick can be finished off. */
    @SubscribeEvent
    static void onSwept(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() instanceof Player || !event.getEntity().isAlive()) {
            return;
        }
        Long tick = SWEEP_TICK.get(player.getUUID());
        double below = ClassEffectApplier.state(player).chance(Kind.SWEEP_EXECUTE);
        LivingEntity victim = event.getEntity();
        if (below > 0 && tick != null && tick == player.level().getGameTime() && !victim.getUUID().equals(SWEEP_TARGET.get(player.getUUID()))
                && victim.getHealth() < victim.getMaxHealth() * below && player.level() instanceof ServerLevel level) {
            victim.hurtServer(level, level.damageSources().source(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL, player), Float.MAX_VALUE);
        }
    }
}
