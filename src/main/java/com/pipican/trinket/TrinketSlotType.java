package com.pipican.trinket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * A kind of trinket slot ("ring", "amulet", "shoulderpad", ...), loaded from
 * {@code data/<ns>/pipican/trinket_slot/<name>.json}.
 *
 * <p>Slot <b>types</b> are global; how many of each slot a player gets is decided by their class
 * ({@code trinket_slots} in the class JSON). A mage could have 2 rings + 1 amulet while a fighter
 * has a single shoulderpad.
 *
 * @param name     display name
 * @param accepts  item tag of items that fit in this slot. Defaults to {@code <ns>:trinkets/<name>}
 *                 (e.g. {@code pipican:trinkets/ring}) when omitted.
 * @param order    sort order in the trinket screen (lower = higher up)
 */
public record TrinketSlotType(Component name, Optional<TagKey<Item>> accepts, int order) {

    public static final Codec<TrinketSlotType> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(TrinketSlotType::name),
            TagKey.codec(Registries.ITEM).optionalFieldOf("accepts").forGetter(TrinketSlotType::accepts),
            Codec.INT.optionalFieldOf("order", 0).forGetter(TrinketSlotType::order)
    ).apply(i, TrinketSlotType::new));
}
