package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * {@code pipican:holding} - one of the items is in the player's hand.
 *
 * <pre>{@code { "type": "pipican:holding", "items": "#pipican:weapons/shields", "hand": "either" } }</pre>
 * {@code hand}: {@code main}, {@code off} or {@code either} (default).
 */
public record HoldingCondition(HolderSet<Item> items, Hand hand) implements ClassCondition {

    public enum Hand implements StringRepresentable {
        MAIN("main"), OFF("off"), EITHER("either");

        public static final Codec<Hand> CODEC = StringRepresentable.fromEnum(Hand::values);
        private final String name;

        Hand(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<HoldingCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(HoldingCondition::items),
            Hand.CODEC.optionalFieldOf("hand", Hand.EITHER).forGetter(HoldingCondition::hand)
    ).apply(i, HoldingCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.HOLDING;
    }

    @Override
    public boolean test(ServerPlayer player) {
        boolean main = player.getMainHandItem().is(items);
        boolean off = player.getOffhandItem().is(items);
        return switch (hand) {
            case MAIN -> main;
            case OFF -> off;
            case EITHER -> main || off;
        };
    }
}
