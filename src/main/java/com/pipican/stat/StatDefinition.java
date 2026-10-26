package com.pipican.stat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.AttributeBonus;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A stat such as "Strength" or "Faith", loaded from {@code data/<ns>/pipican/stat/<name>.json}.
 *
 * <p>The number of stats is not fixed: every JSON file is one stat. Points in a stat come from
 * the class's {@code base_stats}, from points the player buys with vanilla XP levels, and from
 * skill-tree / passive effects of type {@code pipican:stat}.
 *
 * <p>Example:
 * <pre>{@code
 * {
 *   "name": "Strength",
 *   "description": "Raw melee power.",
 *   "max": 100,
 *   "per_point": [
 *     { "attribute": "minecraft:attack_damage", "amount": 0.25, "operation": "add_value" }
 *   ]
 * }
 * }</pre>
 *
 * @param name        display name
 * @param description tooltip text
 * @param max         highest value players can reach by spending vanilla XP (bonuses may exceed it)
 * @param perPoint    attribute modifiers applied once per point in this stat
 * @param exclusive   if true, players can only raise this stat when it is their class's special stat
 *                    (use this for signature stats like "Faith" that only one class should own)
 * @param color       ARGB colour used in UIs
 */
public record StatDefinition(
        Component name,
        Component description,
        int max,
        List<AttributeBonus> perPoint,
        boolean exclusive,
        int color) {

    public static final Codec<StatDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(StatDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(StatDefinition::description),
            Codec.INT.optionalFieldOf("max", 100).forGetter(StatDefinition::max),
            AttributeBonus.CODEC.listOf().optionalFieldOf("per_point", List.of()).forGetter(StatDefinition::perPoint),
            Codec.BOOL.optionalFieldOf("exclusive", false).forGetter(StatDefinition::exclusive),
            Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(StatDefinition::color)
    ).apply(i, StatDefinition::new));
}
