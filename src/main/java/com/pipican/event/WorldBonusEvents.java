package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.Pets;
import com.pipican.ability.Reactions;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.player.ClassEffectApplier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Containers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Bonus kinds hooked into thrown potions, brewing stands, loot chests, taming and mob attacks. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class WorldBonusEvents {

    /** Brewing stands by position, with the player who last opened them (for {@code brew_speed}). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, UUID>> BREWERS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Double> BREW_PROGRESS = new ConcurrentHashMap<>();

    private WorldBonusEvents() {}

    @SubscribeEvent
    static void onThrown(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) {
            return;
        }
        var entity = event.getEntity();
        var owner = entity instanceof AbstractThrownPotion potion ? potion.getOwner() : entity instanceof Fireball fireball ? fireball.getOwner() : null;
        if (owner instanceof ServerPlayer player && entity.tickCount == 0) {
            double speed = ClassEffectApplier.state(player).bonus(Kind.THROWN_SPEED);
            if (speed != 1) {
                entity.setDeltaMovement(entity.getDeltaMovement().scale(speed));
            }
        }
    }

    /** {@code splash_undead}: the potion also hurts undead around where it breaks. */
    @SubscribeEvent
    static void onPotionImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractThrownPotion potion) || !(potion.getOwner() instanceof ServerPlayer player)
                || !(potion.level() instanceof ServerLevel level)) {
            return;
        }
        double damage = ClassEffectApplier.state(player).chance(Kind.SPLASH_UNDEAD);
        if (damage <= 0) {
            return;
        }
        for (LivingEntity undead : level.getEntitiesOfClass(LivingEntity.class, potion.getBoundingBox().inflate(AbstractThrownPotion.SPLASH_RANGE),
                e -> e.is(EntityTypeTags.UNDEAD) && e.distanceToSqr(potion) <= 16)) {
            undead.hurtServer(level, level.damageSources().indirectMagic(potion, player), (float) damage);
        }
    }

    /** {@code share_drink}: effects from a drunk potion also go to the nearest tamed animal. */
    @SubscribeEvent
    static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ClassEffectApplier.state(player).chance(Kind.SHARE_DRINK) <= 0) {
            return;
        }
        PotionContents contents = event.getItem().get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> Pets.owner(e) == player)
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .ifPresent(pet -> contents.forEachEffect(e -> pet.addEffect(new MobEffectInstance(e), player), 1));
    }

    @SubscribeEvent
    static void onPetDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof OwnableEntity) {
            ServerPlayer owner = Pets.owner(event.getEntity());
            if (owner != null) {
                Reactions.fire(owner, event.getEntity(), ItemStack.EMPTY, ReactionEffect.Event.PET_DEATH);
            }
        }
    }

    /** {@code max_wolves}: no more wolves once that many of yours are around. */
    @SubscribeEvent
    static void onTame(AnimalTameEvent event) {
        if (event.getAnimal().getType() != EntityType.WOLF || !(event.getTamer() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int max = (int) Math.round(ClassEffectApplier.state(player).chance(Kind.MAX_WOLVES));
        if (max > 0 && level.getEntities(EntityType.WOLF, w -> w.isTame() && Pets.owner(w) == player).size() >= max) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("pipican.max_wolves", max));
        }
    }

    /** {@code redirect_hit} and {@code ill_fortune}: mob hits that miss or land on another mob. */
    @SubscribeEvent
    static void onMobHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker) || !(attacker instanceof Enemy) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        double fortune = 0;
        for (Player p : level.getEntitiesOfClass(Player.class, attacker.getBoundingBox().inflate(8), p -> p.distanceToSqr(attacker) <= 64)) {
            if (p instanceof ServerPlayer sp) {
                fortune = Math.max(fortune, ClassEffectApplier.state(sp).chance(Kind.ILL_FORTUNE));
            }
        }
        if (fortune > 0 && attacker.getRandom().nextDouble() < fortune) {
            event.setCanceled(true);
            return;
        }
        if (victim instanceof ServerPlayer player && player.getRandom().nextDouble() < ClassEffectApplier.state(player).chance(Kind.REDIRECT_HIT)) {
            List<Mob> others = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6),
                    m -> m != attacker && m.isAlive() && Pets.owner(m) != player);
            if (!others.isEmpty()) {
                event.setCanceled(true);
                others.get(player.getRandom().nextInt(others.size())).hurtServer(level, level.damageSources().mobAttack(attacker), event.getAmount());
            }
        }
    }

    @SubscribeEvent
    static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var be = level.getBlockEntity(event.getPos());
        if (be instanceof BrewingStandBlockEntity) {
            BREWERS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(event.getPos().immutable(), player.getUUID());
        }
        // Open Locks: one extra roll of a loot chest's table, added once vanilla has filled it.
        if (be instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null
                && player.getRandom().nextDouble() < ClassEffectApplier.state(player).chance(Kind.OPEN_LOCKS)) {
            ResourceKey<LootTable> key = chest.getLootTable();
            BlockPos pos = event.getPos().immutable();
            level.getServer().execute(() -> {
                LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
                LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withLuck(player.getLuck()).withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
                for (ItemStack stack : table.getRandomItems(params)) {
                    for (int slot = 0; slot < chest.getContainerSize() && !stack.isEmpty(); slot++) {
                        if (chest.getItem(slot).isEmpty()) {
                            chest.setItem(slot, stack.copy());
                            stack.setCount(0);
                        }
                    }
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, stack);
                    }
                }
                player.sendOverlayMessage(Component.translatable("pipican.open_locks"));
            });
        }
    }

    /** {@code brew_speed}: extra brewing progress on stands the player last opened. */
    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, UUID> stands = BREWERS.get(level.dimension());
        if (stands == null) {
            return;
        }
        stands.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                return false;
            }
            if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)) {
                BREW_PROGRESS.remove(pos);
                return true;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getValue());
            if (player == null || stand.brewTime <= 1) {
                return false;
            }
            double extra = ClassEffectApplier.state(player).chance(Kind.BREW_SPEED);
            if (extra <= 0) {
                return false;
            }
            double progress = BREW_PROGRESS.getOrDefault(pos, 0.0) + extra;
            int skip = (int) Math.min(progress, stand.brewTime - 1);
            BREW_PROGRESS.put(pos, progress - skip);
            stand.brewTime -= skip;
            return false;
        });
    }
}
