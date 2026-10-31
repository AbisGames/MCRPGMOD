package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.action.HobbleAction;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Poison you give, shields blocking from the sides, hobbled mobs and copper golems near you. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class AfflictionEvents {

    private static final TagKey<DamageType> MAGIC = TagKey.create(Registries.DAMAGE_TYPE, Pipican.id("is_magic"));
    private static final Identifier COPPER_HEART = Pipican.id("copper_heart");
    /** Mob -> the player whose Poison (or Wither) it carries. */
    private static final Map<UUID, UUID> POISONER = new ConcurrentHashMap<>();
    private static boolean busy;

    private AfflictionEvents() {}

    /** {@code poison_undead}: Poison you give an undead mob becomes Wither, as long and as strong. */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (busy || !effect.is(MobEffects.POISON) || !event.getEntity().is(EntityTypeTags.UNDEAD)
                || !(event.getEffectSource() instanceof ServerPlayer player) || ClassEffectApplier.state(player).chance(Kind.POISON_UNDEAD) <= 0) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        busy = true;
        try {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, effect.getDuration(), effect.getAmplifier()), player);
        } finally {
            busy = false;
        }
    }

    @SubscribeEvent
    static void onAdded(MobEffectEvent.Added event) {
        MobEffectInstance effect = event.getEffectInstance();
        if ((effect.is(MobEffects.POISON) || effect.is(MobEffects.WITHER)) && event.getEffectSource() instanceof ServerPlayer player && player != event.getEntity()) {
            POISONER.put(event.getEntity().getUUID(), player.getUUID());
        }
    }

    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        var victim = event.getEntity();
        // poison_damage: Poison / Wither ticks from an effect a player gave.
        boolean poison = event.getSource().is(NeoForgeMod.POISON_DAMAGE) || event.getSource().is(DamageTypes.MAGIC) && event.getSource().getEntity() == null;
        if (poison && victim.hasEffect(MobEffects.POISON) || event.getSource().is(DamageTypes.WITHER) && victim.hasEffect(MobEffects.WITHER)) {
            UUID who = POISONER.get(victim.getUUID());
            if (who != null && victim.level() instanceof ServerLevel level && level.getServer().getPlayerList().getPlayer(who) instanceof ServerPlayer player) {
                double more = ClassEffectApplier.state(player).chance(Kind.POISON_DAMAGE);
                if (more != 0) {
                    event.setAmount((float) Math.max(0, event.getAmount() * (1 + more)));
                }
            }
        }
        // magic_armor_pierce: your magic damage ignores part of the target's armor.
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().is(MAGIC)) {
            double kept = 1 - Math.min(1, Math.max(0, ClassEffectApplier.state(player).chance(Kind.MAGIC_ARMOR_PIERCE)));
            if (kept < 1) {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> (float) (reduction * kept));
            }
        }
    }

    /** {@code side_block}: a raised shield also stops projectiles from the sides (anything less than 135 degrees off your view). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onShieldBlock(LivingShieldBlockEvent event) {
        if (event.getBlocked() || !(event.getEntity() instanceof ServerPlayer player) || !player.isBlocking()
                || !(event.getDamageSource().getDirectEntity() instanceof Projectile projectile) || event.getDamageSource().is(DamageTypeTags.BYPASSES_SHIELD)
                || ClassEffectApplier.state(player).chance(Kind.SIDE_BLOCK) <= 0) {
            return;
        }
        Vec3 look = player.getViewVector(1).multiply(1, 0, 1);
        Vec3 to = projectile.position().subtract(player.position()).multiply(1, 0, 1);
        if (look.lengthSqr() < 1.0e-4 || to.lengthSqr() < 1.0e-4 || look.normalize().dot(to.normalize()) > -0.7) {
            event.setBlocked(true);
            event.setBlockedDamage(event.getDamageContainer().getNewDamage());
        }
    }

    /** Hobbled mobs ({@code pipican:hobble}) don't leave the ground. */
    @SubscribeEvent
    static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player) && HobbleAction.noJump(event.getEntity())) {
            Vec3 v = event.getEntity().getDeltaMovement();
            event.getEntity().setDeltaMovement(v.x, Math.min(0, v.y), v.z);
        }
    }

    @SubscribeEvent
    static void onMobTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && HobbleAction.rooted(mob)) {
            mob.getNavigation().stop();
            mob.setJumping(false);
            Vec3 v = mob.getDeltaMovement();
            mob.setDeltaMovement(0, Math.min(0, v.y), 0);
        }
    }

    /** {@code copper_heart}: copper golems within 16 blocks move faster (checked every second, for 2 seconds). */
    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        frenzy(player);
        ClassState state = ClassEffectApplier.state(player);
        double faster = state.chance(Kind.COPPER_HEART);
        if (faster <= 0) {
            return;
        }
        long until = player.level().getGameTime() + 40;
        for (var golem : player.level().getEntities(EntityType.COPPER_GOLEM, player.getBoundingBox().inflate(16), g -> g.distanceToSqr(player) <= 256)) {
            var speed = golem.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(COPPER_HEART);
                speed.addTransientModifier(new AttributeModifier(COPPER_HEART, faster, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                BOOSTED.put(golem.getUUID(), until);
            }
        }
    }

    /**
     * While the {@code frenzy} flag runs (Violent Madness), the player swings at the nearest creature within 4 blocks once a
     * second - allies too, players included unless {@code frenzy_spares_players}.
     */
    private static void frenzy(ServerPlayer player) {
        if (!com.pipican.ability.Flags.has(player, "frenzy") || player.isSpectator() || !player.isAlive()) {
            return;
        }
        boolean spare = ClassEffectApplier.state(player).chance(Kind.FRENZY_SPARES_PLAYERS) > 0;
        var near = player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(4),
                e -> e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                        && !(e instanceof Player p && (spare || p.isSpectator() || p.isCreative())) && e.distanceToSqr(player) <= 16);
        near.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player))).ifPresent(target -> {
            player.attack(target);
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            player.resetAttackStrengthTicker();
        });
    }

    /** Golem -> game time its Copper Heart boost runs out. */
    private static final Map<UUID, Long> BOOSTED = new ConcurrentHashMap<>();

    @SubscribeEvent
    static void onGolemTick(EntityTickEvent.Post event) {
        if (event.getEntity().getType() != EntityType.COPPER_GOLEM || event.getEntity().level().isClientSide()) {
            return;
        }
        Long until = BOOSTED.get(event.getEntity().getUUID());
        if (until != null && event.getEntity().level().getGameTime() > until && event.getEntity() instanceof Mob golem) {
            BOOSTED.remove(golem.getUUID());
            var speed = golem.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.removeModifier(COPPER_HEART);
            }
        }
    }
}
