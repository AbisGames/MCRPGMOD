package com.pipican.ability;

import com.pipican.Pipican;
import com.pipican.player.ClassService;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The Weapon Master's favored weapon type, picked from the main hand on waking up and saved on the player. */
public final class FavoredWeapon {

    public static final List<String> KINDS = List.of("swords", "axes", "spears", "maces", "tridents", "bows", "crossbows");

    private FavoredWeapon() {}

    private static TagKey<Item> tag(String kind) {
        return TagKey.create(Registries.ITEM, Pipican.id("weapons/" + kind));
    }

    public static Optional<String> kindOf(ItemStack stack) {
        return KINDS.stream().filter(k -> !stack.isEmpty() && stack.is(tag(k))).findFirst();
    }

    public static boolean holding(ServerPlayer player) {
        Optional<String> favored = ClassService.data(player).extras().favoredWeapon;
        return favored.isPresent() && player.getMainHandItem().is(tag(favored.get()));
    }

    /** On waking: the weapon in hand becomes the favored type (empty hand keeps the old one). */
    public static void choose(ServerPlayer player) {
        kindOf(player.getMainHandItem()).ifPresent(kind -> {
            ClassService.data(player).extras().favoredWeapon = Optional.of(kind);
            player.sendSystemMessage(Component.translatable("pipican.favored_weapon", Component.translatable("pipican.weapon_kind." + kind))
                    .withStyle(ChatFormatting.GOLD));
        });
    }

    static Identifier id(String kind) {
        return Pipican.id(kind);
    }
}
