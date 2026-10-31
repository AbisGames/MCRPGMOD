package com.pipican.event;

import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Item traits of gear sets, by item tag ({@code pipican:traits/<trait>}, generated from tools/ore_builder/sets.py):
 * {@code self_repair} mends 1 durability every 5 seconds anywhere in your inventory; {@code autosmelt} tools drop what a
 * furnace would make of the block's drops; {@code area_mine} tools also break the 3x3 square around the block (not while
 * sneaking); {@code soulbound} items stay with you when you die. {@code unbreakable} and {@code mine_all} are set on
 * the items themselves.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class GearTraitEvents {

    private static final TagKey<Item> SELF_REPAIR = trait("self_repair");
    private static final TagKey<Item> AUTOSMELT = trait("autosmelt");
    private static final TagKey<Item> AREA_MINE = trait("area_mine");
    private static final TagKey<Item> SOULBOUND = trait("soulbound");
    /** Players breaking the extra blocks of an area mine right now (so those breaks don't spread further). */
    private static final java.util.Set<UUID> AREA_BUSY = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, List<ItemStack>> KEPT = new ConcurrentHashMap<>();

    private GearTraitEvents() {}

    private static TagKey<Item> trait(String name) {
        return TagKey.create(Registries.ITEM, Pipican.id("traits/" + name));
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) {
            return;
        }
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(SELF_REPAIR) && stack.isDamaged()) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }

    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        if (!event.getTool().is(AUTOSMELT)) {
            return;
        }
        var level = event.getLevel();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level).ifPresent(recipe -> {
                ItemStack smelted = recipe.value().assemble(new SingleRecipeInput(stack));
                if (!smelted.isEmpty()) {
                    drop.setItem(smelted.copyWithCount(smelted.getCount() * stack.getCount()));
                }
            });
        }
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isShiftKeyDown() || AREA_BUSY.contains(player.getUUID())) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (!tool.is(AREA_MINE) || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }
        // The square faces the player: flat when looking up or down, upright otherwise.
        float pitch = player.getXRot();
        Direction.Axis normal = Math.abs(pitch) > 50 ? Direction.Axis.Y : player.getDirection().getAxis();
        BlockPos center = event.getPos();
        float hardness = event.getState().getDestroySpeed(event.getLevel(), center);
        AREA_BUSY.add(player.getUUID());
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos pos = switch (normal) {
                        case Y -> center.offset(a, 0, b);
                        case X -> center.offset(0, a, b);
                        case Z -> center.offset(a, b, 0);
                    };
                    var state = event.getLevel().getBlockState(pos);
                    float h = state.getDestroySpeed(event.getLevel(), pos);
                    // Only blocks the tool is right for and that aren't much harder than the one mined.
                    if (!state.isAir() && h >= 0 && h <= hardness * 2 + 1 && tool.isCorrectToolForDrops(state) && !tool.isEmpty()) {
                        player.gameMode.destroyBlock(pos);
                    }
                }
            }
        } finally {
            AREA_BUSY.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(SOULBOUND)) {
                kept.add(stack.copy());
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        if (!kept.isEmpty()) {
            KEPT.put(player.getUUID(), kept);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.Clone event) {
        List<ItemStack> kept = event.isWasDeath() ? KEPT.remove(event.getOriginal().getUUID()) : null;
        if (kept != null) {
            kept.forEach(stack -> event.getEntity().getInventory().add(stack));
        }
    }
}
