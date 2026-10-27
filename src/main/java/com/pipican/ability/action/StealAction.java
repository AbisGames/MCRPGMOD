package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:steal} - takes the item in the hand (or a random armor piece) of the mob the user looks at.
 * Cancelled (and refunded) if it has nothing.
 *
 * <pre>{@code { "type": "pipican:steal", "range": 4 } }</pre>
 */
public record StealAction(double range) implements AbilityAction {

    public static final MapCodec<StealAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 16).optionalFieldOf("range", 4.0).forGetter(StealAction::range)
    ).apply(i, StealAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.STEAL;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        if (!(Targets.lookTarget(player, range) instanceof Mob mob)) {
            ctx.cancel(Component.translatable("pipican.ability.no_target"));
            return;
        }
        List<EquipmentSlot> slots = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            if (!mob.getItemBySlot(slot).isEmpty()) {
                slots.add(slot);
            }
        }
        if (slots.isEmpty()) {
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (!mob.getItemBySlot(slot).isEmpty()) {
                    slots.add(slot);
                }
            }
        }
        if (slots.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.nothing_to_steal"));
            return;
        }
        EquipmentSlot slot = slots.get(player.getRandom().nextInt(slots.size()));
        ItemStack stolen = mob.getItemBySlot(slot).copy();
        mob.setItemSlot(slot, ItemStack.EMPTY);
        if (!player.getInventory().add(stolen)) {
            player.drop(stolen, false);
        }
    }
}
