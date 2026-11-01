package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.SpecialItems;
import com.pipican.ability.StoredSpells;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * {@code pipican:store_spell} - pours the potion in the user's off hand into the weapon or armor piece in their main hand
 * (Store Spell): the gear keeps the potion's effects for {@code charges} casts (see {@link StoredSpells}). The potion
 * becomes an empty bottle. Cancelled (and refunded) without a potion or a piece of gear.
 *
 * <pre>{@code { "type": "pipican:store_spell", "charges": 8 } }</pre>
 */
public record StoreSpellAction(int charges) implements AbilityAction {

    public static final MapCodec<StoreSpellAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 1000).optionalFieldOf("charges", 8).forGetter(StoreSpellAction::charges)
    ).apply(i, StoreSpellAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.STORE_SPELL;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ItemStack potion = player.getOffhandItem();
        ItemStack gear = player.getMainHandItem();
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        List<MobEffectInstance> effects = new ArrayList<>();
        if (contents != null) {
            contents.getAllEffects().forEach(e -> effects.add(new MobEffectInstance(e)));
        }
        if (effects.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.needs_potion"));
            return;
        }
        if (!SpecialItems.isWeapon(gear) && !gear.has(DataComponents.EQUIPPABLE)) {
            ctx.cancel(Component.translatable("pipican.ability.needs_gear"));
            return;
        }
        StoredSpells.store(gear, effects, charges + ctx.mod().hits());
        potion.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (potion.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, bottle);
        } else if (!player.getInventory().add(bottle)) {
            player.drop(bottle, false);
        }
    }
}
