package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:transmute} - turns the stack in the main hand into another item of the same count, if
 * it matches one of the {@code pairs}. Cancelled (and refunded) otherwise.
 *
 * <pre>{@code { "type": "pipican:transmute", "pairs": [ { "from": "minecraft:cobblestone", "to": "minecraft:stone" } ] } }</pre>
 */
public record TransmuteAction(List<Pair> pairs) implements AbilityAction {

    public record Pair(Item from, Item to) {
        public static final Codec<Pair> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("from").forGetter(Pair::from),
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(Pair::to)
        ).apply(i, Pair::new));
    }

    public static final MapCodec<TransmuteAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Pair.CODEC.listOf().fieldOf("pairs").forGetter(TransmuteAction::pairs)
    ).apply(i, TransmuteAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.TRANSMUTE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        for (Pair pair : pairs) {
            if (held.is(pair.from())) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(pair.to(), held.getCount()));
                return;
            }
        }
        ctx.cancel(Component.translatable("pipican.ability.cannot_transmute"));
    }
}
