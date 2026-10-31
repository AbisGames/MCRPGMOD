package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:wearing} - how many worn armor pieces are one of the items. Both bounds inclusive.
 *
 * <pre>{@code { "type": "pipican:wearing", "items": "#pipican:armor/heavy", "min": 3 } }</pre>
 * Defaults: {@code min} 1, no {@code max}. Use {@code "max": 0} for "not wearing any".
 */
public record WearingCondition(HolderSet<Item> items, int min, Optional<Integer> max) implements ClassCondition {

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static final MapCodec<WearingCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(WearingCondition::items),
            Codec.intRange(0, 4).optionalFieldOf("min", 1).forGetter(WearingCondition::min),
            Codec.intRange(0, 4).optionalFieldOf("max").forGetter(WearingCondition::max)
    ).apply(i, WearingCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.WEARING;
    }

    @Override
    public boolean test(ServerPlayer player) {
        int count = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (player.getItemBySlot(slot).is(items)) {
                count++;
            }
        }
        return count >= min && (max.isEmpty() || count <= max.get());
    }
}
