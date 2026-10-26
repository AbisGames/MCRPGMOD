package com.pipican.trinket;

import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanRegistries;
import com.pipican.rpgclass.RpgClass;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Which trinket slots a class has, in display order. Computed from the class JSON on the server
 * and sent to the client when the trinket menu opens, so both sides build identical slots.
 */
public record TrinketLayout(List<Group> groups) {

    /** {@code count} slots of one slot type. */
    public record Group(ResourceKey<TrinketSlotType> slot, int count) {
        public static final StreamCodec<ByteBuf, Group> STREAM_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(PipicanRegistries.TRINKET_SLOT), Group::slot,
                ByteBufCodecs.VAR_INT, Group::count,
                Group::new);
    }

    public static final StreamCodec<ByteBuf, TrinketLayout> STREAM_CODEC =
            Group.STREAM_CODEC.apply(ByteBufCodecs.list()).map(TrinketLayout::new, TrinketLayout::groups);

    public static TrinketLayout of(RegistryAccess access, RpgClass rpgClass) {
        return new TrinketLayout(ClassLookup.trinketLayout(access, rpgClass).stream().map(e -> new Group(e.getKey(), e.getValue())).toList());
    }

    public int totalSlots() {
        return groups.stream().mapToInt(Group::count).sum();
    }

    public boolean isEmpty() {
        return totalSlots() == 0;
    }

    /** Items allowed in a slot type: its {@code accepts} tag, or {@code <ns>:trinkets/<name>}. */
    public static TagKey<Item> acceptedItems(RegistryAccess access, ResourceKey<TrinketSlotType> slot) {
        TrinketSlotType type = ClassLookup.trinketSlots(access).getValue(slot);
        if (type != null && type.accepts().isPresent()) {
            return type.accepts().get();
        }
        return TagKey.create(Registries.ITEM, slot.identifier().withPrefix("trinkets/"));
    }

    public static Component name(RegistryAccess access, ResourceKey<TrinketSlotType> slot) {
        TrinketSlotType type = ClassLookup.trinketSlots(access).getValue(slot);
        return type != null ? type.name() : Component.literal(slot.identifier().getPath());
    }
}
