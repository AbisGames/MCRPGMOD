package com.pipican.event;

import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import com.pipican.ability.Summons;
import net.minecraft.world.item.Items;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import java.util.Optional;
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
            Optional<LivingEntity> target = Optional.of(event.getEntity());
            if (source.getDirectEntity() instanceof Projectile) {
                MasteryService.onAction(attacker, new SkillAction.RangedHit(weapon, target, event.getInflictedDamage()));
            } else if (source.getDirectEntity() == attacker) {
                MasteryService.onAction(attacker, new SkillAction.MeleeHit(weapon, target, event.getInflictedDamage()));
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
                MasteryService.onAction(victim, new SkillAction.HurtWearing(armor, event.getInflictedDamage()));
            }
            did(victim, new SkillAction.Did(SkillAction.Did.Kind.HURT, ItemStack.EMPTY, Optional.empty(),
                    source.getEntity() instanceof LivingEntity l ? Optional.of(l) : Optional.empty(), event.getInflictedDamage()));
            if (source.is(DamageTypeTags.IS_FALL)) {
                did(victim, new SkillAction.Did(SkillAction.Did.Kind.TAKE_FALL, ItemStack.EMPTY, Optional.empty(), Optional.empty(), event.getInflictedDamage()));
            }
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer killer && event.getEntity() != killer) {
            ItemStack weapon = event.getSource().getWeaponItem();
            MasteryService.onAction(killer, new SkillAction.Kill(weapon != null ? weapon : ItemStack.EMPTY, Optional.of(event.getEntity())));
        }
        // Pets and summons: their owner trains "your minions killing mobs".
        Entity killerEntity = event.getSource().getEntity();
        ServerPlayer owner = killerEntity == null ? null
                : killerEntity instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer p ? p
                : Summons.ownerOf(killerEntity).orElse(null);
        if (owner != null && killerEntity != owner) {
            did(owner, new SkillAction.Did(SkillAction.Did.Kind.PET_KILL, ItemStack.EMPTY, Optional.empty(), Optional.of(event.getEntity()), 0));
        }
    }

    // ------------------------------------------------------------------ pipican:did actions

    public static void did(ServerPlayer player, SkillAction.Did.Kind kind) {
        MasteryService.onAction(player, SkillAction.Did.of(kind));
    }

    public static void did(ServerPlayer player, SkillAction.Did action) {
        MasteryService.onAction(player, action);
    }

    private static void didItem(Player player, SkillAction.Did.Kind kind, ItemStack item) {
        if (player instanceof ServerPlayer sp) {
            did(sp, new SkillAction.Did(kind, item, Optional.empty(), Optional.empty(), 0));
        }
    }

    private static void didBlock(Player player, SkillAction.Did.Kind kind, BlockState block) {
        if (player instanceof ServerPlayer sp) {
            did(sp, new SkillAction.Did(kind, ItemStack.EMPTY, Optional.of(block), Optional.empty(), 0));
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0 && player.getHealth() < player.getMaxHealth()) {
            did(player, new SkillAction.Did(SkillAction.Did.Kind.HEAL, ItemStack.EMPTY, Optional.empty(), Optional.empty(), event.getAmount()));
        }
    }

    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        did(event.getPlayer(), SkillAction.Did.Kind.TELEPORT);
    }

    @SubscribeEvent
    public static void onChorus(EntityTeleportEvent.ItemConsumption event) {
        if (event.getEntityLiving() instanceof ServerPlayer player) {
            did(player, SkillAction.Did.Kind.TELEPORT);
        }
    }

    @SubscribeEvent
    public static void onEnchant(PlayerEnchantItemEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.ENCHANT, event.getEnchantedItem());
    }

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.BREW, event.getStack());
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.CRAFT, event.getCrafting());
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.SMELT, event.getSmelting());
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.TRADE, event.getMerchantOffer().getResult());
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            did(player, new SkillAction.Did(SkillAction.Did.Kind.TAME, ItemStack.EMPTY, Optional.empty(), Optional.of(event.getAnimal()), 0));
        }
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player && event.getChild() != null) {
            did(player, new SkillAction.Did(SkillAction.Did.Kind.BREED, ItemStack.EMPTY, Optional.empty(), Optional.of(event.getChild()), 0));
        }
    }

    /** Mining (every block a player breaks) and harvesting (fully grown crops). */
    @SubscribeEvent
    public static void onBreak(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getState();
        didBlock(player, SkillAction.Did.Kind.MINE, state);
        if (state.getBlock() instanceof CropBlock crop ? crop.isMaxAge(state) : state.is(BlockTags.CROPS) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(BlockTags.FLOWERS) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM)) {
            didBlock(player, SkillAction.Did.Kind.HARVEST, state);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            didBlock(player, SkillAction.Did.Kind.PLANT, event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public static void onBoneMeal(BonemealEvent event) {
        if (event.getPlayer() != null && event.isValidBonemealTarget()) {
            didBlock(event.getPlayer(), SkillAction.Did.Kind.BONE_MEAL, event.getState());
        }
    }

    @SubscribeEvent
    public static void onAnvil(AnvilCraftEvent.Post event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.ANVIL, event.getOutput());
    }

    @SubscribeEvent
    public static void onLevelUp(PlayerXpEvent.LevelChange event) {
        if (event.getLevels() > 0) {
            didItem(event.getEntity(), SkillAction.Did.Kind.LEVEL_UP, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player && event.getItem().is(Items.POTION)) {
            didItem(player, SkillAction.Did.Kind.DRINK, event.getItem());
        }
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        didItem(event.getEntity(), SkillAction.Did.Kind.FISH, event.getDrops().isEmpty() ? ItemStack.EMPTY : event.getDrops().getFirst());
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
        if (last == null || player.isSpectator() || player.getAbilities().flying) {
            return;
        }
        double distance = now.distanceTo(last);
        if (player.isPassenger()) {
            if (distance > 0.01 && distance < MAX_STEP) {
                MasteryService.onAction(player, new SkillAction.Moved(MoveMode.RIDE, distance));
            }
            return;
        }
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
