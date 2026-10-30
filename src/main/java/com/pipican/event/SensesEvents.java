package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Senses and small reactions: aura breaks, calm animals, shield breaks, splash power, sparkles and structure hints. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class SensesEvents {

    /** Source for the "auras off" boost of {@code aura_break_at}. */
    private static final Object AURA_BREAK = new Object();
    /** Built on first use: creating effects while the mod loads would initialise the codecs in the wrong order. */
    private static List<com.pipican.effect.ClassEffect> aurasOff;
    /** Entities in a player's splash cloud this tick: their heals from it grow by splash_power. */
    private static final Map<UUID, Double> SPLASHED = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SPLASH_TICK = new ConcurrentHashMap<>();
    private static final List<TagKey<Structure>> STRUCTURES = List.of(net.minecraft.tags.StructureTags.VILLAGE, net.minecraft.tags.StructureTags.MINESHAFT,
            net.minecraft.tags.StructureTags.RUINED_PORTAL, net.minecraft.tags.StructureTags.OCEAN_RUIN, net.minecraft.tags.StructureTags.SHIPWRECK,
            net.minecraft.tags.StructureTags.ON_TREASURE_MAPS, net.minecraft.tags.StructureTags.EYE_OF_ENDER_LOCATED);

    private SensesEvents() {}

    @SubscribeEvent
    static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double at = ClassEffectApplier.state(player).chance(Kind.AURA_BREAK_AT);
            if (at > 0 && event.getInflictedDamage() > at) {
                if (aurasOff == null) {
                    aurasOff = List.of(new com.pipican.effect.type.AuraModEffect("", 0, -100, false, List.of(), List.of(), List.of()));
                }
                com.pipican.ability.Boosts.add(player, AURA_BREAK, aurasOff, 200);
            }
        }
        // Calm animals: an animal you hurt doesn't panic.
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() instanceof net.minecraft.world.entity.animal.Animal animal
                && ClassEffectApplier.state(player).chance(Kind.CALM_ANIMALS) > 0 && player.level().getServer() != null) {
            player.level().getServer().execute(() -> {
                animal.setLastHurtByMob(null);
                animal.getNavigation().stop();
            });
        }
    }

    /** {@code shield_break_time}: a shield you disable stays down longer. */
    @SubscribeEvent
    static void onShield(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && event.getDamageSource().getEntity() instanceof ServerPlayer attacker
                && attacker.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES) && victim.level().getServer() != null) {
            double more = ClassEffectApplier.state(attacker).chance(Kind.SHIELD_BREAK_TIME);
            if (more > 0) {
                var shield = victim.getUseItem().isEmpty() ? victim.getOffhandItem() : victim.getUseItem();
                victim.level().getServer().execute(() -> {
                    var cooldowns = victim.getCooldowns();
                    if (cooldowns.isOnCooldown(shield)) {
                        cooldowns.addCooldown(shield, (int) Math.round(100 * (1 + more)));
                    }
                });
            }
        }
    }

    /** {@code splash_power}: Instant Damage / Health from your splash potions is stronger. */
    @SubscribeEvent
    static void onSplash(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion potion
                && potion.getOwner() instanceof ServerPlayer player) {
            double more = ClassEffectApplier.state(player).chance(Kind.SPLASH_POWER);
            if (more > 0) {
                long now = potion.level().getGameTime();
                for (LivingEntity e : potion.level().getEntitiesOfClass(LivingEntity.class, potion.getBoundingBox().inflate(4.5))) {
                    SPLASHED.put(e.getUUID(), more);
                    SPLASH_TICK.put(e.getUUID(), now);
                }
            }
        }
    }

    private static double splashed(LivingEntity e) {
        Long tick = SPLASH_TICK.get(e.getUUID());
        return tick != null && e.level().getGameTime() - tick <= 1 ? SPLASHED.getOrDefault(e.getUUID(), 0.0) : 0;
    }

    @SubscribeEvent
    static void onSplashHeal(LivingHealEvent event) {
        double more = splashed(event.getEntity());
        if (more > 0) {
            event.setAmount((float) (event.getAmount() * (1 + more)));
        }
    }

    @SubscribeEvent
    static void onSplashDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            double more = ClassEffectApplier.state(player).chance(Kind.SPLASH_POWER);
            if (more > 0) {
                event.setAmount((float) (event.getAmount() * (1 + more)));
            }
        }
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        // Master Hunter: quarries can't stay invisible.
        if (state.chance(Kind.PREY_REVEAL) >= 1) {
            com.pipican.ability.Quarries.targets(player).forEach(t -> t.removeEffect(MobEffects.INVISIBILITY));
        }
        if (player.isShiftKeyDown()) {
            senses(player, level, state);
        }
        double structures = state.chance(Kind.STRUCTURE_SENSE);
        if (structures > 0 && player.tickCount % 100 == 0 && (player.getMainHandItem().is(Items.COMPASS) || player.getOffhandItem().is(Items.COMPASS))) {
            hint(player, level, structures);
        }
    }

    private static void senses(ServerPlayer player, ServerLevel level, ClassState state) {
        double magic = state.chance(Kind.SENSE_MAGIC);
        if (magic > 0) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(magic), i -> i.getItem().isEnchanted())) {
                item.setGlowingTag(true);
            }
            for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(magic),
                    e -> e != player && !(e instanceof Player) && !e.getActiveEffects().isEmpty() && !e.hasEffect(MobEffects.GLOWING))) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false), player);
            }
        }
        int loot = (int) state.chance(Kind.LOOT_SENSE);
        int ores = (int) state.chance(Kind.ORE_SENSE);
        int range = Math.max(loot, ores);
        if (range <= 0) {
            return;
        }
        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            int d = Math.max(Math.abs(pos.getX() - center.getX()), Math.max(Math.abs(pos.getY() - center.getY()), Math.abs(pos.getZ() - center.getZ())));
            boolean sparkle = false;
            if (ores > 0 && d <= ores && level.getBlockState(pos).is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) {
                sparkle = true;
            } else if (loot > 0 && d <= loot && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity chest
                    && chest.getLootTable() != null) {
                sparkle = true;
            }
            if (sparkle) {
                // Only this player sees them.
                level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true, false, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    private static final Map<UUID, Set<BlockPos>> HINTED = new ConcurrentHashMap<>();

    /** Diviner: names structures near you while you hold a compass (each one once). */
    private static void hint(ServerPlayer player, ServerLevel level, double radius) {
        Set<BlockPos> told = HINTED.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet());
        int chunks = Math.max(1, (int) Math.ceil(radius / 16));
        for (TagKey<Structure> tag : STRUCTURES) {
            BlockPos found = level.findNearestMapStructure(tag, player.blockPosition(), chunks, false);
            if (found != null && found.distSqr(player.blockPosition().atY(found.getY())) <= radius * radius && told.add(found.immutable())) {
                String name = tag.location().getPath().replace('_', ' ');
                player.sendSystemMessage(Component.translatable("pipican.structure_hint", name, (int) Math.sqrt(found.distSqr(player.blockPosition().atY(found.getY())))));
            }
        }
    }
}
