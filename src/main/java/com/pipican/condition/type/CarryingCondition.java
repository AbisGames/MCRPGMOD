package com.pipican.condition.type;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/** {@code pipican:carrying} - one of {@code items} is anywhere in the player's inventory ("with no book in your inventory" = {@code not}). */
public record CarryingCondition(HolderSet<Item> items) implements ClassCondition {

    public static final MapCodec<CarryingCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(CarryingCondition::items)
    ).apply(i, CarryingCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.CARRYING;
    }

    @Override
    public boolean test(ServerPlayer player) {
        var inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            if (!inv.getItem(slot).isEmpty() && inv.getItem(slot).is(items)) {
                return true;
            }
        }
        return false;
    }
}
