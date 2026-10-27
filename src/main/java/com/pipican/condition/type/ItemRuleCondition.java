package com.pipican.condition.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.SpecialItems;
import com.pipican.condition.ClassCondition;
import com.pipican.condition.ConditionTypes;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;

/**
 * {@code pipican:item_rule} - class item rules: {@code bound_weapon} (holding the weapon from hotbar slot 1),
 * {@code sigil_weapon} (holding a weapon renamed at an anvil), {@code sigil_armor} (wearing at least {@code min}
 * trimmed armor pieces).
 *
 * <pre>{@code { "type": "pipican:item_rule", "rule": "sigil_armor", "min": 4 } }</pre>
 */
public record ItemRuleCondition(Rule rule, int min) implements ClassCondition {

    public enum Rule implements StringRepresentable {
        BOUND_WEAPON, SIGIL_WEAPON, SIGIL_ARMOR;

        public static final Codec<Rule> CODEC = StringRepresentable.fromEnum(Rule::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<ItemRuleCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Rule.CODEC.fieldOf("rule").forGetter(ItemRuleCondition::rule),
            Codec.intRange(1, 4).optionalFieldOf("min", 1).forGetter(ItemRuleCondition::min)
    ).apply(i, ItemRuleCondition::new));

    @Override
    public ConditionTypes.Type type() {
        return ConditionTypes.ITEM_RULE;
    }

    @Override
    public boolean test(ServerPlayer player) {
        return switch (rule) {
            case BOUND_WEAPON -> SpecialItems.holdingBound(player);
            case SIGIL_WEAPON -> SpecialItems.holdingSigil(player);
            case SIGIL_ARMOR -> SpecialItems.sigilArmor(player) >= min;
        };
    }
}
