package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.Pets;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Being lost, unseen and watchful: mob memory, Faceless, pet sight, invisibility and danger cues. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class StealthEvents {

    /** Vanilla mobs keep chasing a target they can't see for about 3 seconds. */
    private static final int BASE_FORGET_TICKS = 60;
    private static final Map<UUID, Integer> UNSEEN = new ConcurrentHashMap<>();
    private static final Set<UUID> HIT_SNEAKING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Set<UUID>> NOTICED = new ConcurrentHashMap<>();

    private StealthEvents() {}

    /** Faceless: sneaking, and no hit landed in the last 2 seconds. */
    static boolean faceless(ServerPlayer player, ClassState state) {
        return state.chance(Kind.FACELESS_RANGE) > 0 && player.isShiftKeyDown() && player.tickCount - player.getLastHurtMobTimestamp() > 40;
    }

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer player && event.getEntity() instanceof Enemy) {
            ClassState state = ClassEffectApplier.state(player);
            if (faceless(player, state) && event.getEntity().distanceTo(player) > state.chance(Kind.FACELESS_RANGE)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    static void onHit(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && player.isShiftKeyDown() && event.getEntity() instanceof Mob mob) {
            HIT_SNEAKING.add(mob.getUUID());
        }
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || player.tickCount % 5 != 0) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        forget(player, level, state);
        if (player.tickCount % 20 == 0) {
            sight(player, level, state);
        }
    }

    private static void forget(ServerPlayer player, ServerLevel level, ClassState state) {
        double faster = state.chance(Kind.FORGET_FASTER);
        double sooner = state.chance(Kind.FORGET_SOONER);
        double cap = state.chance(Kind.FORGET_DELAY_MAX);
        double sneakHit = state.chance(Kind.FORGET_HIT_SNEAKING);
        boolean hidden = faceless(player, state);
        if (faster == 0 && sooner == 0 && cap == 0 && sneakHit == 0 && !hidden) {
            return;
        }
        double range = state.chance(Kind.FACELESS_RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m.getTarget() == player)) {
            if (hidden && mob instanceof Enemy && mob.distanceTo(player) > range) {
                lose(mob);
                continue;
            }
            if (mob.getSensing().hasLineOfSight(player)) {
                UNSEEN.remove(mob.getUUID());
                continue;
            }
            int unseen = UNSEEN.merge(mob.getUUID(), 5, Integer::sum);
            double delay = BASE_FORGET_TICKS * Math.max(0, 1 - faster) - sooner * 20;
            if (HIT_SNEAKING.contains(mob.getUUID())) {
                delay *= Math.max(0, 1 - sneakHit);
            }
            if (cap > 0) {
                delay = Math.min(delay, cap * 20);
            }
            if (unseen >= delay) {
                lose(mob);
            }
        }
    }

    private static void lose(Mob mob) {
        mob.setTarget(null);
        if (mob instanceof NeutralMob angry) {
            angry.stopBeingAngry();
        }
        UNSEEN.remove(mob.getUUID());
        HIT_SNEAKING.remove(mob.getUUID());
    }

    private static void sight(ServerPlayer player, ServerLevel level, ClassState state) {
        // Primal Bond: pets share your Night Vision and glow for you.
        if (state.chance(Kind.PET_SHARE_SIGHT) > 0) {
            boolean vision = player.hasEffect(MobEffects.NIGHT_VISION);
            for (LivingEntity pet : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32), e -> Pets.owner(e) == player)) {
                pet.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
                if (vision) {
                    pet.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false), player);
                }
            }
        }
        // No Hiding: invisible mobs you can see show up.
        double reveal = state.chance(Kind.NO_HIDING);
        if (reveal > 0) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(reveal),
                    m -> m.hasEffect(MobEffects.INVISIBILITY) && player.hasLineOfSight(m))) {
                mob.removeEffect(MobEffects.INVISIBILITY);
            }
        }
        // Alertness: a sound cue for each hostile mob that comes close.
        double sense = state.chance(Kind.DANGER_SENSE);
        if (sense > 0) {
            Set<UUID> before = NOTICED.getOrDefault(player.getUUID(), Set.of());
            Set<UUID> now = ConcurrentHashMap.newKeySet();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(sense),
                    e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(player) <= sense * sense)) {
                now.add(e.getUUID());
                if (!before.contains(e.getUUID())) {
                    // Only this player hears it.
                    player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_BELL, SoundSource.HOSTILE,
                            player.getX(), player.getY(), player.getZ(), 0.6f, 0.7f, player.getRandom().nextLong()));
                }
            }
            NOTICED.put(player.getUUID(), now);
        }
    }
}
