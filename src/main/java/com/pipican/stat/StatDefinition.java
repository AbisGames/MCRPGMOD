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
 * the class's {@code base_stats}, from stat points the player earns with kill XP, and from
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
 * @param max         highest value players can reach by spending stat points (bonuses may exceed it)
 * @param perPoint    attribute modifiers applied once per point in this stat
 * @param exclusive   if true, players can only raise this stat when it is their class's special stat
 *                    (use this for signature stats like "Faith" that only one class should own)
 * @param color       ARGB colour used in UIs
 * @param effects     class effects that grow with this stat: in their {@code scaling}, the source {@code "points"} is the
 *                    player's points in it (times their class's {@code stat_growth} for it) - e.g. Intelligence's
 *                    {@code { "type": "pipican:damage_dealt", "damage_types": "#pipican:is_magic", "multiplier": 1.0,
 *                    "scaling": [{ "source": "points", "per": 0.005 }] }} is +0.5% magic damage per point
 * @param gear        items that need points in this stat: {@code { "items": "#minecraft:swords", "min": 15 }}. Below
 *                    {@code min}, the item counts one affinity tier worse for each requirement it misses (never blocked)
 */
public record StatDefinition(
        Component name,
        Component description,
        int max,
        List<AttributeBonus> perPoint,
        boolean exclusive,
        int color,
        List<com.pipican.effect.ClassEffect> effects,
        List<GearRequirement> gear) {

    /** {@code items} need at least {@code min} points in the stat. */
    public record GearRequirement(net.minecraft.core.HolderSet<net.minecraft.world.item.Item> items, int min) {
        public static final Codec<GearRequirement> CODEC = RecordCodecBuilder.create(i -> i.group(
                net.minecraft.core.RegistryCodecs.homogeneousList(net.minecraft.core.registries.Registries.ITEM).fieldOf("items").forGetter(GearRequirement::items),
                Codec.intRange(1, 10_000).fieldOf("min").forGetter(GearRequirement::min)
        ).apply(i, GearRequirement::new));
    }

    public static final Codec<StatDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(StatDefinition::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(StatDefinition::description),
            Codec.INT.optionalFieldOf("max", 100).forGetter(StatDefinition::max),
            AttributeBonus.CODEC.listOf().optionalFieldOf("per_point", List.of()).forGetter(StatDefinition::perPoint),
            Codec.BOOL.optionalFieldOf("exclusive", false).forGetter(StatDefinition::exclusive),
            Codec.INT.optionalFieldOf("color", 0xFFFFFFFF).forGetter(StatDefinition::color),
            // Lazy: class effects may refer back to stats.
            Codec.lazyInitialized(() -> com.pipican.effect.ClassEffect.CODEC).listOf().optionalFieldOf("effects", List.of()).forGetter(StatDefinition::effects),
            GearRequirement.CODEC.listOf().optionalFieldOf("gear", List.of()).forGetter(StatDefinition::gear)
    ).apply(i, StatDefinition::new));
}
