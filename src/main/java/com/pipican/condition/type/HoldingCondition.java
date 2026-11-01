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
public record HoldingCondition(HolderSet<Item> items, Hand hand, java.util.Optional<String> named) implements ClassCondition {

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
            Hand.CODEC.optionalFieldOf("hand", Hand.EITHER).forGetter(HoldingCondition::hand),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("named").forGetter(HoldingCondition::named)
    ).apply(i, HoldingCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.HOLDING;
    }

    @Override
    public boolean test(ServerPlayer player) {
        boolean main = player.getMainHandItem().is(items) && named(player.getMainHandItem());
        boolean off = player.getOffhandItem().is(items) && named(player.getOffhandItem());
        // Blessed Symbol: a shield with a banner pattern counts as an Icon of Faith (the icon list holds a totem).
        if (!off && hand == Hand.OFF && items.contains(net.minecraft.world.item.Items.TOTEM_OF_UNDYING.builtInRegistryHolder())
                && player.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD)
                && !player.getOffhandItem().getOrDefault(net.minecraft.core.component.DataComponents.BANNER_PATTERNS,
                        net.minecraft.world.level.block.entity.BannerPatternLayers.EMPTY).layers().isEmpty()
                && com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.ICON_SHIELD) > 0) {
            off = true;
        }
        // Second Tome: a book anywhere in the hotbar counts as one in the off-hand.
        if (!off && hand == Hand.OFF
                && com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.BOOK_HOTBAR) > 0
                && items.contains(net.minecraft.world.item.Items.BOOK.builtInRegistryHolder())) {
            for (int slot = 0; slot < 9 && !off; slot++) {
                off = player.getInventory().getItem(slot).is(items);
            }
        }
        return switch (hand) {
            case MAIN -> main;
            case OFF -> off;
            case EITHER -> main || off;
        };
    }

    /** {@code named}: the item must carry an anvil name containing this text, ignoring case ("a renamed stick" broom). */
    private boolean named(net.minecraft.world.item.ItemStack stack) {
        if (named.isEmpty()) {
            return true;
        }
        var name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        return name != null && name.getString().toLowerCase(java.util.Locale.ROOT).contains(named.get().toLowerCase(java.util.Locale.ROOT));
    }
}
