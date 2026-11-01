package com.pipican.soul;

import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * What the soul blade calls into the world: summoned allies (the skeleton army, the healing beast, the grub, the
 * giant warrior), the blade's spirit in a trial, and delayed effects (the black coffin closing). None of it is saved:
 * a summon or spirit left over from before a restart is removed when it loads.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class SoulMinions {

    private static final String SUMMON_TAG = "pipican_soul_summon";
    private static final String SPIRIT_TAG = "pipican_soul_spirit";

    /** Summon -> its owner and the game time it fades. */
    private record Summon(UUID owner, long until, float shareToOwner) {}
    private static final Map<UUID, Summon> SUMMONS = new ConcurrentHashMap<>();
    /** Spirit -> the owner on trial and the stage at stake. */
    private record Spirit(UUID owner, int stage) {}
    private static final Map<UUID, Spirit> SPIRITS = new ConcurrentHashMap<>();
    /** Delayed effects: (game time, what). */
    private record Delayed(long at, Runnable run) {}
    private static final List<Delayed> LATER = new ArrayList<>();

    private SoulMinions() {}

    public static boolean isSummoned(Entity e) {
        return SUMMONS.containsKey(e.getUUID()) || SPIRITS.containsKey(e.getUUID()) || e.entityTags().contains(SUMMON_TAG) || e.entityTags().contains(SPIRIT_TAG);
    }

    public static boolean summonOf(Entity e, Player owner) {
        Summon s = SUMMONS.get(e.getUUID());
        return s != null && s.owner().equals(owner.getUUID());
    }

    public static void later(ServerLevel level, int ticks, Runnable run) {
        synchronized (LATER) {
            LATER.add(new Delayed(level.getGameTime() + ticks, run));
        }
    }

    /** Summons an ally for {@code ticks}; {@code shareToOwner} of the damage it takes goes to its owner too. */
    public static <T extends Mob> T summon(ServerPlayer owner, EntityType<T> type, Vec3 at, int ticks, float shareToOwner, Consumer<T> setup) {
        ServerLevel level = (ServerLevel) owner.level();
        T mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return null;
        }
        mob.snapTo(at.x, at.y, at.z, owner.getYRot(), 0);
        mob.addTag(SUMMON_TAG);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, true, false));
        setup.accept(mob);
        SUMMONS.put(mob.getUUID(), new Summon(owner.getUUID(), level.getGameTime() + ticks, shareToOwner));
        level.addFreshEntity(mob);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, at.x, at.y + 1, at.z, 20, 0.5, 1, 0.5, 0.02);
        return mob;
    }

    /** Removes the owner's summons of a type (only one giant at a time). */
    public static void dismiss(ServerPlayer owner, EntityType<?> type) {
        ServerLevel level = (ServerLevel) owner.level();
        for (var it = SUMMONS.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Entity mob = level.getEntity(e.getKey());
            if (e.getValue().owner().equals(owner.getUUID()) && mob != null && mob.getType() == type) {
                mob.discard();
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ the trial

    /** The inner-world trial: the blade's spirit appears and fights its owner, and only its owner (public for tests). */
    public static LivingEntity startTrial(ServerPlayer player, int stage) {
        for (var s : SPIRITS.values()) {
            if (s.owner().equals(player.getUUID())) {
                return null;   // already facing it
            }
        }
        ServerLevel level = (ServerLevel) player.level();
        var spirit = EntityType.WITHER_SKELETON.create(level, EntitySpawnReason.TRIGGERED);
        if (spirit == null) {
            return null;
        }
        Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(4));
        spirit.snapTo(at.x, player.getY(), at.z, player.getYRot() + 180, 0);
        spirit.addTag(SPIRIT_TAG);
        double health = stage == 5 ? 80 : 200;
        spirit.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        spirit.setHealth((float) health);
        spirit.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stage == 5 ? 9 : 16);
        spirit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        ItemStack sword = new ItemStack(SoulItems.BLADE.get());
        int color = Soul.PATHS.getOrDefault(Soul.get(player).path(), 0);
        if (color != 0) {
            sword.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(color));
        }
        spirit.setItemSlot(EquipmentSlot.MAINHAND, sword);
        spirit.setDropChance(EquipmentSlot.MAINHAND, 0);
        spirit.setCustomName(Component.translatable("pipican.soul.spirit", player.getMainHandItem().getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        spirit.setCustomNameVisible(true);
        spirit.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 600, 0, true, false));
        spirit.setPersistenceRequired();
        SPIRITS.put(spirit.getUUID(), new Spirit(player.getUUID(), stage));
        level.addFreshEntity(spirit);
        spirit.setTarget(player);
        player.sendSystemMessage(Component.translatable("pipican.soul.trial_start").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false));
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, at.x, player.getY() + 1, at.z, 60, 0.6, 1.2, 0.6, 0.05);
        return spirit;
    }

    // ------------------------------------------------------------------ events

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        long now = server.overworld().getGameTime();
        List<Runnable> due = new ArrayList<>();
        synchronized (LATER) {
            for (Iterator<Delayed> it = LATER.iterator(); it.hasNext(); ) {
                Delayed d = it.next();
                if (d.at() <= now) {
                    due.add(d.run());
                    it.remove();
                }
            }
        }
        due.forEach(Runnable::run);
        if (now % 20 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            // Summons: fade on time, follow their owner's fights.
            for (var it = SUMMONS.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                Entity entity = level.getEntity(e.getKey());
                if (entity == null) {
                    continue;
                }
                Player owner = level.getPlayerByUUID(e.getValue().owner());
                if (owner == null || e.getValue().until() <= now || !entity.isAlive()) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, entity.getX(), entity.getY() + 1, entity.getZ(), 12, 0.4, 0.8, 0.4, 0.02);
                    entity.discard();
                    it.remove();
                    continue;
                }
                if (entity instanceof Mob mob) {
                    if (mob.distanceTo(owner) > 24) {
                        mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                    }
                    LivingEntity target = mob.getTarget();
                    if (target == null || !target.isAlive() || !SoulAbilities.foe(owner, target)) {
                        mob.setTarget(nearestFoe(owner, mob, 16));
                    }
                }
            }
            // Spirits: the trial is lost if the owner dies, leaves or goes too far.
            for (var it = SPIRITS.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                Entity entity = level.getEntity(e.getKey());
                if (entity == null) {
                    continue;
                }
                Player owner = level.getPlayerByUUID(e.getValue().owner());
                if (owner == null || !owner.isAlive() || owner.distanceTo(entity) > 48) {
                    if (owner instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.translatable("pipican.soul.trial_failed").withStyle(ChatFormatting.GRAY));
                    }
                    entity.discard();
                    it.remove();
                } else if (entity instanceof Mob mob) {
                    mob.setTarget(owner);
                }
            }
        }
    }

    private static LivingEntity nearestFoe(Player owner, Mob self, double r) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : owner.level().getEntitiesOfClass(LivingEntity.class, self.getBoundingBox().inflate(r),
                e -> e != self && e.isAlive() && (e instanceof Enemy || (e instanceof Mob m && m.getTarget() == owner)) && SoulAbilities.foe(owner, e))) {
            double d = e.distanceToSqr(self);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        Summon s = SUMMONS.get(event.getEntity().getUUID());
        if (s != null && (target.getUUID().equals(s.owner()) || target instanceof Player || isSummoned(target))) {
            event.setCanceled(true);
            return;
        }
        Spirit spirit = SPIRITS.get(event.getEntity().getUUID());
        if (spirit != null && !target.getUUID().equals(spirit.owner())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // The spirit answers only to its owner.
        Spirit spirit = SPIRITS.get(victim.getUUID());
        if (spirit != null && (event.getSource().getEntity() == null || !event.getSource().getEntity().getUUID().equals(spirit.owner()))) {
            event.setCanceled(true);
            return;
        }
        // The giant warrior's wounds are partly its owner's.
        Summon s = SUMMONS.get(victim.getUUID());
        if (s != null && s.shareToOwner() > 0 && victim.level() instanceof ServerLevel level && level.getPlayerByUUID(s.owner()) instanceof ServerPlayer owner) {
            owner.hurtServer(level, level.damageSources().magic(), event.getAmount() * s.shareToOwner());
        }
        // Summons don't hurt each other or players.
        if (event.getSource().getEntity() != null && SUMMONS.containsKey(event.getSource().getEntity().getUUID())
                && (victim instanceof Player || isSummoned(victim))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        Spirit spirit = SPIRITS.remove(event.getEntity().getUUID());
        SUMMONS.remove(event.getEntity().getUUID());
        if (spirit != null && event.getSource().getEntity() instanceof ServerPlayer player && player.getUUID().equals(spirit.owner())) {
            Soul.winTrial(player);
        }
    }

    /** Leftovers from before a restart don't come back. */
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !(e.entityTags().contains(SUMMON_TAG) || e.entityTags().contains(SPIRIT_TAG))) {
            return;
        }
        if (!SUMMONS.containsKey(e.getUUID()) && !SPIRITS.containsKey(e.getUUID())) {
            event.setCanceled(true);
        }
    }

    static void clear(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        SUMMONS.entrySet().removeIf(e -> {
            if (e.getValue().owner().equals(player.getUUID())) {
                Entity m = level.getEntity(e.getKey());
                if (m != null) {
                    m.discard();
                }
                return true;
            }
            return false;
        });
        SPIRITS.entrySet().removeIf(e -> {
            if (e.getValue().owner().equals(player.getUUID())) {
                Entity m = level.getEntity(e.getKey());
                if (m != null) {
                    m.discard();
                }
                return true;
            }
            return false;
        });
    }
}
