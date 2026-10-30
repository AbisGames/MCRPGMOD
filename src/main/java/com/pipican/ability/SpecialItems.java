package com.pipican.ability;

import com.pipican.Pipican;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Class item rules: the bound weapon (hotbar slot 1) and sigil-marked gear (renamed weapons, trimmed armor). */
public final class SpecialItems {

    private static final TagKey<Item> MELEE = TagKey.create(Registries.ITEM, Pipican.id("weapons/melee"));
    private static final TagKey<Item> RANGED = TagKey.create(Registries.ITEM, Pipican.id("weapons/ranged"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private SpecialItems() {}

    public static boolean isWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(MELEE) || stack.is(RANGED));
    }

    /** The weapon in hotbar slot 1 is in the main hand. */
    public static boolean holdingBound(ServerPlayer player) {
        return player.getInventory().getSelectedSlot() == 0 && isWeapon(player.getMainHandItem());
    }

    /** The main-hand weapon was renamed at an anvil. */
    public static boolean holdingSigil(ServerPlayer player) {
        ItemStack hand = player.getMainHandItem();
        return isWeapon(hand) && hand.has(DataComponents.CUSTOM_NAME);
    }

    private static final TagKey<Item> HEAVY = TagKey.create(Registries.ITEM, Pipican.id("armor/heavy"));

    /** Worn heavy armor pieces (iron, diamond, netherite...). */
    public static int heavyArmor(ServerPlayer player) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (player.getItemBySlot(slot).is(HEAVY)) {
                n++;
            }
        }
        return n;
    }

    /** Sigil-marked gear: a weapon renamed at an anvil or a piece of armor with a trim. */
    public static boolean isSigil(ItemStack stack) {
        return isWeapon(stack) ? stack.has(DataComponents.CUSTOM_NAME) : stack.has(DataComponents.TRIM);
    }

    /** A staff: a blaze rod or a stick. */
    public static boolean isStaff(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.BLAZE_ROD) || stack.is(net.minecraft.world.item.Items.STICK);
    }

    /** Worn armor pieces without an armor trim. */
    public static int unmarkedArmor(ServerPlayer player) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && !stack.has(DataComponents.TRIM)) {
                n++;
            }
        }
        return n;
    }

    /** Worn armor pieces with an armor trim. */
    public static int sigilArmor(ServerPlayer player) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (player.getItemBySlot(slot).has(DataComponents.TRIM)) {
                n++;
            }
        }
        return n;
    }
}
