package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.Boosts;
import com.pipican.ability.Charged;
import com.pipican.ability.Reactions;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.effect.type.DamageMultiplierEffect;
import com.pipican.entity.Eidolon;
import com.pipican.player.ClassEffectApplier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Smaller class mechanics: charged-hit exposure, Omens' outlines, Snuff, guardians that can't hold you, the Mecha
 * cockpit, and a player's most recently used stat (Prodigy). See the {@code BonusEffect} kinds of the same names.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class MechanicEvents {

    /** Players whose Snuff is flickering a torch right now (hostile mobs can't see them). */
    private static final Set<UUID> SNUFFED = ConcurrentHashMap.newKeySet();
    /** Player -> the stat they used last ("pipican:strength"...). Not saved. */
    private static final Map<UUID, String> LAST_STAT = new ConcurrentHashMap<>();

    private MechanicEvents() {}

    // ------------------------------------------------------------------ Prodigy: the stat you used most recently

    /** Called for every skill action: which stat it used. */
    public static void used(ServerPlayer player, com.pipican.mastery.SkillAction action) {
        String stat = switch (action) {
            case com.pipican.mastery.SkillAction.MeleeHit m -> "strength";
            case com.pipican.mastery.SkillAction.RangedHit r -> "dexterity";
            case com.pipican.mastery.SkillAction.Moved m when m.mode() == com.pipican.mastery.MoveMode.SPRINT -> "agility";
            case com.pipican.mastery.SkillAction.Did d -> switch (d.kind()) {
                case MAGIC_HIT, ENCHANT, BREW -> "intelligence";
                case HEAL_OTHER, HEAL -> "wisdom";
                case HURT -> "vitality";
                case MINE, HARVEST -> "endurance";
                case FISH, LOOT_CHEST, TRADE -> "luck";
                case DODGE -> "agility";
                default -> null;
            };
            default -> null;
        };
        if (stat != null) {
            LAST_STAT.put(player.getUUID(), "pipican:" + stat);
        }
    }

    public static java.util.Optional<String> lastStat(ServerPlayer player) {
        return java.util.Optional.ofNullable(LAST_STAT.get(player.getUUID()));
    }

    // ------------------------------------------------------------------ hits

    @SubscribeEvent
    static void onDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getSource().getDirectEntity() == player && Charged.wasCharged(player)) {
            double seconds = ClassEffectApplier.state(player).chance(Kind.CHARGED_EXPOSURE);
            if (seconds > 0) {
                Charged.expose(player, event.getEntity(), Math.round(seconds * 20));
            }
        }
    }

    /** Mecha: the eidolon hits 25% harder while its owner pilots it. */
    @SubscribeEvent
    static void onEidolonHit(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Eidolon eidolon && eidolon.getControllingPassenger() instanceof ServerPlayer pilot
                && ClassEffectApplier.state(pilot).chance(Kind.MECHA) > 0) {
            event.setAmount(event.getAmount() * 1.25f);
        }
    }

    /** Mecha: after climbing out of the eidolon, your own weapons deal nothing for a while. */
    @SubscribeEvent
    static void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityMounting() instanceof ServerPlayer player && event.getEntityBeingMounted() instanceof Eidolon) {
            double seconds = ClassEffectApplier.state(player).chance(Kind.MECHA);
            if (seconds > 0) {
                var melee = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrThrow(
                        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Pipican.id("weapons/melee")));
                var ranged = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOrThrow(
                        net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, Pipican.id("weapons/ranged")));
                List<ClassEffect> locked = List.of(
                        new DamageMultiplierEffect(false, java.util.Optional.empty(), java.util.Optional.of(melee), java.util.Optional.empty(), java.util.Optional.empty(), 0, List.of(), false),
                        new DamageMultiplierEffect(false, java.util.Optional.empty(), java.util.Optional.of(ranged), java.util.Optional.empty(), java.util.Optional.empty(), 0, List.of(), false));
                Boosts.add(player, MechanicEvents.class, locked, (int) Math.round(seconds * 20));
            }
        }
    }

    // ------------------------------------------------------------------ targeting

    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer player)) {
            return;
        }
        var state = ClassEffectApplier.state(player);
        // Slippery: guardians can't lock their beam on you.
        if (event.getEntity() instanceof Guardian && state.chance(Kind.UNHOLDABLE) > 0) {
            event.setCanceled(true);
            return;
        }
        // Snuff: while a torch near you flickers, hostile mobs can't see you.
        if (event.getEntity() instanceof Enemy && SNUFFED.contains(player.getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Slippery: the elder guardians' hold (Mining Fatigue) doesn't take. */
    @SubscribeEvent
    static void onEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect().is(MobEffects.MINING_FATIGUE)
                && ClassEffectApplier.state(player).chance(Kind.UNHOLDABLE) > 0) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    // ------------------------------------------------------------------ every second

    private static boolean torch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH) || state.is(Blocks.COPPER_TORCH) || state.is(Blocks.COPPER_WALL_TORCH);
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0) {
            return;
        }
        tick(player, level);
    }

    /** Snuff and Omens' outlines (also called by tests, whose players don't tick). */
    public static void tick(ServerPlayer player, ServerLevel level) {
        var state = ClassEffectApplier.state(player);
        int snuff = (int) Math.ceil(state.chance(Kind.TORCH_SNUFF));
        boolean flickering = false;
        if (snuff > 0) {
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-snuff, -snuff, -snuff), center.offset(snuff, snuff, snuff))) {
                if (torch(level.getBlockState(pos)) && pos.distSqr(center) <= snuff * snuff) {
                    flickering = true;
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 3, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
        if (flickering) {
            if (SNUFFED.add(player.getUUID())) {
                for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32), m -> m instanceof Enemy && m.getTarget() == player)) {
                    mob.setTarget(null);
                }
            }
        } else {
            SNUFFED.remove(player.getUUID());
        }
        double outlines = state.chance(Kind.NO_BACKSTAB);
        if (outlines > 0 && Reactions.usesLeft(player, "premonition") > 0) {
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(outlines),
                    e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(player) <= outlines * outlines)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
            }
        }
    }

    public static boolean snuffed(ServerPlayer player) {
        return SNUFFED.contains(player.getUUID());
    }
}
