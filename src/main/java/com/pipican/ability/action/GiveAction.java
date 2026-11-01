package com.pipican.ability.action;

import net.minecraft.network.chat.Component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.TemporaryItems;
import java.util.Optional;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * {@code pipican:give} - gives the user a temporary item that vanishes after {@code seconds} or when they
 * sleep (Assemble); with {@code seconds: 0} only sleeping removes it. {@code sneak_item} is given instead while sneaking;
 * {@code sneak_items} (armor pieces) gives the first one whose armor slot is empty, worn straight away.
 *
 * <pre>{@code { "type": "pipican:give", "item": "minecraft:iron_sword", "sneak_item": "minecraft:iron_chestplate", "seconds": 600 } }</pre>
 */
public record GiveAction(ItemStackTemplate item, Optional<ItemStackTemplate> sneakItem, double seconds,
                         java.util.List<ItemStackTemplate> sneakItems) implements AbilityAction {

    public static final MapCodec<GiveAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveAction::item),
            ItemStackTemplate.CODEC.optionalFieldOf("sneak_item").forGetter(GiveAction::sneakItem),
            Codec.doubleRange(0, 86_400).optionalFieldOf("seconds", 600.0).forGetter(GiveAction::seconds),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("sneak_items", java.util.List.of()).forGetter(GiveAction::sneakItems)
    ).apply(i, GiveAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.GIVE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        long until = seconds <= 0 ? Long.MAX_VALUE - 1 : ctx.level().getGameTime() + (long) (seconds * 20);
        if (player.isShiftKeyDown() && !sneakItems.isEmpty()) {
            for (ItemStackTemplate piece : sneakItems) {
                var stack = piece.create();
                var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                if (equippable != null && player.getItemBySlot(equippable.slot()).isEmpty()) {
                    player.setItemSlot(equippable.slot(), TemporaryItems.mark(stack, until));
                    return;
                }
            }
            ctx.cancel(Component.translatable("pipican.ability.armor_full"));
            return;
        }
        var template = player.isShiftKeyDown() && sneakItem.isPresent() ? sneakItem.get() : item;
        var stack = TemporaryItems.mark(template.create(), until);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
