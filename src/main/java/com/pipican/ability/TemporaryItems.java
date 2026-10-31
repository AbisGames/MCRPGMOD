package com.pipican.ability;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Items from {@code pipican:give} that vanish after a while and whenever their owner sleeps (Assemble). */
public final class TemporaryItems {

    private static final String KEY = "pipican_temporary_until";

    private TemporaryItems() {}

    /** Marks {@code stack} to vanish at game time {@code until}. */
    public static ItemStack mark(ItemStack stack, long until) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(KEY, until));
        return stack;
    }

    public static void removeExpired(ServerPlayer player) {
        remove(player, player.level().getGameTime());
    }

    public static void removeAll(ServerPlayer player) {
        remove(player, Long.MAX_VALUE);
    }

    private static void remove(ServerPlayer player, long now) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null) {
                CompoundTag tag = data.copyTag();
                if (tag.contains(KEY) && tag.getLongOr(KEY, 0L) <= now) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
