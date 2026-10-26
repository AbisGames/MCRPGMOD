package com.pipican.trinket;

import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The trinket inventory: one row per trinket slot type of the player's class, above the normal
 * player inventory. Slot counts come from the class JSON ({@code trinket_slots}), so the screen
 * looks different per class without any per-class code.
 */
public class TrinketMenu extends AbstractContainerMenu {

    public static final int SLOTS_PER_ROW = 5;
    public static final int ROW_HEIGHT = 20;
    public static final int TOP = 20;
    public static final int SLOT_X = 80;

    private final TrinketLayout layout;
    private final Container trinkets;
    private final Optional<ResourceKey<RpgClass>> openedWithClass;
    /** Y of the player inventory, below the trinket rows. */
    public final int inventoryTop;

    /** Client constructor, called by the menu type with the layout the server sent. */
    public static TrinketMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        TrinketLayout layout = TrinketLayout.STREAM_CODEC.decode(buf);
        return new TrinketMenu(containerId, inventory, layout, new SimpleContainer(layout.totalSlots()), Optional.empty());
    }

    private TrinketMenu(int containerId, Inventory inventory, TrinketLayout layout, Container trinkets, Optional<ResourceKey<RpgClass>> openedWithClass) {
        super(PipicanMenus.TRINKETS.get(), containerId);
        this.layout = layout;
        this.trinkets = trinkets;
        this.openedWithClass = openedWithClass;

        RegistryAccess access = inventory.player.level().registryAccess();
        int index = 0;
        int row = 0;
        for (TrinketLayout.Group group : layout.groups()) {
            TagKey<Item> accepts = TrinketLayout.acceptedItems(access, group.slot());
            for (int i = 0; i < group.count(); i++) {
                int x = SLOT_X + (i % SLOTS_PER_ROW) * 18;
                int y = TOP + (row + i / SLOTS_PER_ROW) * ROW_HEIGHT;
                addSlot(new TrinketSlot(trinkets, index++, x, y, accepts));
            }
            row += rowsFor(group);
        }
        this.inventoryTop = TOP + row * ROW_HEIGHT + 14;
        addStandardInventorySlots(inventory, 8, inventoryTop);
    }

    public static int rowsFor(TrinketLayout.Group group) {
        return Math.max(1, (group.count() + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW);
    }

    public TrinketLayout layout() {
        return layout;
    }

    /** Opens the menu for a player (server side). */
    public static void open(ServerPlayer player) {
        Optional<RpgClass> rpgClass = ClassService.activeClass(player);
        if (rpgClass.isEmpty()) {
            player.sendSystemMessage(Component.translatable("pipican.class.none"));
            return;
        }
        TrinketLayout layout = TrinketLayout.of(player.level().registryAccess(), rpgClass.get());
        if (layout.isEmpty()) {
            player.sendSystemMessage(Component.translatable("pipican.trinkets.none"));
            return;
        }
        ClassService.reconcileTrinkets(player);
        Container container = new PlayerTrinketContainer(player, layout);
        Optional<ResourceKey<RpgClass>> classKey = ClassService.data(player).activeClass();
        player.openMenu(
                new SimpleMenuProvider((id, inv, p) -> new TrinketMenu(id, inv, layout, container, classKey), Component.translatable("pipican.trinkets.title")),
                buf -> TrinketLayout.STREAM_CODEC.encode(buf, layout));
    }

    @Override
    public boolean stillValid(Player player) {
        // Close the menu if the class changed while it was open (slot layout would be stale).
        if (player instanceof ServerPlayer sp && openedWithClass.isPresent()) {
            return player.isAlive() && ClassService.data(sp).activeClass().equals(openedWithClass);
        }
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int trinketCount = trinkets.getContainerSize();
        if (slotIndex < trinketCount) {
            if (!moveItemStackTo(stack, trinketCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, trinketCount, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** A slot that holds one item from the slot type's tag. */
    public static class TrinketSlot extends Slot {
        private final TagKey<Item> accepts;

        TrinketSlot(Container container, int index, int x, int y, TagKey<Item> accepts) {
            super(container, index, x, y);
            this.accepts = accepts;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(accepts);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /**
     * Server-side container that reads and writes straight into the player's {@link PlayerClassData}
     * trinket lists, and re-applies class bonuses whenever a trinket changes.
     */
    private static final class PlayerTrinketContainer implements Container {
        private final ServerPlayer player;
        /** Flat slot index -> (slot type, index within that type). */
        private final List<ResourceKey<TrinketSlotType>> slotTypes = new ArrayList<>();
        private final List<Integer> slotIndices = new ArrayList<>();

        PlayerTrinketContainer(ServerPlayer player, TrinketLayout layout) {
            this.player = player;
            for (TrinketLayout.Group group : layout.groups()) {
                for (int i = 0; i < group.count(); i++) {
                    slotTypes.add(group.slot());
                    slotIndices.add(i);
                }
            }
        }

        private List<ItemStack> list(int slot) {
            return ClassService.data(player).trinkets(slotTypes.get(slot));
        }

        @Override
        public int getContainerSize() {
            return slotTypes.size();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            List<ItemStack> list = list(slot);
            int idx = slotIndices.get(slot);
            return idx < list.size() ? list.get(idx) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || count <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = stack.split(count);
            if (stack.isEmpty()) {
                setItem(slot, ItemStack.EMPTY);
            } else {
                setChanged();
            }
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            List<ItemStack> list = list(slot);
            int idx = slotIndices.get(slot);
            while (list.size() <= idx) {
                list.add(ItemStack.EMPTY);
            }
            list.set(idx, stack);
            setChanged();
        }

        @Override
        public void setChanged() {
            ClassEffectApplier.refresh(player);
        }

        @Override
        public boolean stillValid(Player player) {
            return player == this.player && player.isAlive();
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
