package com.pipican.ability.action;

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
 * sleep (Assemble). {@code sneak_item} is given instead while sneaking.
 *
 * <pre>{@code { "type": "pipican:give", "item": "minecraft:iron_sword", "sneak_item": "minecraft:iron_chestplate", "seconds": 600 } }</pre>
 */
public record GiveAction(ItemStackTemplate item, Optional<ItemStackTemplate> sneakItem, double seconds) implements AbilityAction {

    public static final MapCodec<GiveAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(GiveAction::item),
            ItemStackTemplate.CODEC.optionalFieldOf("sneak_item").forGetter(GiveAction::sneakItem),
            Codec.doubleRange(1, 86_400).optionalFieldOf("seconds", 600.0).forGetter(GiveAction::seconds)
    ).apply(i, GiveAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.GIVE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var template = player.isShiftKeyDown() && sneakItem.isPresent() ? sneakItem.get() : item;
        var stack = TemporaryItems.mark(template.create(), ctx.level().getGameTime() + (long) (seconds * 20));
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
