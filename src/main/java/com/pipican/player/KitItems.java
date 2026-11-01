package com.pipican.player;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Starting-kit items rolled when they are handed out: a kit entry whose custom data has {@code "pipican_random"} becomes
 * {@code "enchanted_book"} - an enchanted book with one random level-1 enchantment (no curses or treasure-only ones) - or
 * {@code "potion"} - a random brewable potion - or {@code "trim"} - a random armor trim smithing template; one roll per item
 * in the stack.
 */
public final class KitItems {

    private static final String KEY = "pipican_random";
    private static final List<Holder<Potion>> NOT_RANDOM = List.of(Potions.WATER, Potions.MUNDANE, Potions.THICK, Potions.AWKWARD);

    private KitItems() {}

    public static List<ItemStack> roll(ServerPlayer player, ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        String kind = data == null ? null : data.copyTag().getStringOr(KEY, null);
        if (kind == null) {
            return List.of(stack);
        }
        List<ItemStack> out = new ArrayList<>();
        var random = player.getRandom();
        for (int i = 0; i < stack.getCount(); i++) {
            switch (kind) {
                case "enchanted_book" -> {
                    var enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                            .filter(e -> !e.is(EnchantmentTags.CURSE) && !e.is(EnchantmentTags.TREASURE))
                            .map(e -> (Holder<Enchantment>) e).toList();
                    ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                    if (!enchantments.isEmpty()) {
                        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        stored.set(enchantments.get(random.nextInt(enchantments.size())), 1);
                        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                    }
                    out.add(book);
                }
                case "potion" -> {
                    var potions = BuiltInRegistries.POTION.listElements().map(p -> (Holder<Potion>) p)
                            .filter(p -> NOT_RANDOM.stream().noneMatch(n -> n.is(p))).toList();
                    out.add(PotionContents.createItemStack(Items.POTION, potions.get(random.nextInt(potions.size()))));
                }
                case "trim" -> {
                    var list = BuiltInRegistries.ITEM.keySet().stream()
                            .filter(id -> id.getNamespace().equals("minecraft") && id.getPath().endsWith("_armor_trim_smithing_template"))
                            .sorted().map(BuiltInRegistries.ITEM::getValue).toList();
                    out.add(list.isEmpty() ? stack.copyWithCount(1) : new ItemStack(list.get(random.nextInt(list.size()))));
                }
                default -> out.add(stack.copyWithCount(1));
            }
        }
        return out;
    }
}
