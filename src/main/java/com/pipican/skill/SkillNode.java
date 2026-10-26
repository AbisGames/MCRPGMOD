package com.pipican.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * One node of a passive tree.
 *
 * <pre>{@code
 * {
 *   "id": "iron_skin",
 *   "name": "Iron Skin",
 *   "description": "+2 armor.",
 *   "type": "small",
 *   "icon": "minecraft:iron_chestplate",
 *   "x": 120, "y": -40,
 *   "links": [ "warrior_start", "iron_will" ],
 *   "effects": [ { "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 2 } ]
 * }
 * }</pre>
 *
 * @param id            unique within the whole tree (across all of its files). Saved on players -
 *                      don't rename after release.
 * @param type          small / notable / keystone / start (see {@link NodeType})
 * @param x             position in the tree screen; 0,0 is the centre, +y is down
 * @param y             position in the tree screen
 * @param links         ids of connected nodes. Links are two-way, so declaring them on one side is enough.
 *                      May point at nodes in other files of the same tree.
 * @param cost          skill points to allocate (normally 1)
 * @param requiredLevel minimum class level to allocate
 * @param effects       applied while the node is allocated (see {@link ClassEffect})
 * @param grants        ids of active abilities unlocked by this node (read by the future ability system)
 */
public record SkillNode(
        String id,
        Component name,
        Component description,
        NodeType type,
        Item icon,
        double x,
        double y,
        List<String> links,
        int cost,
        int requiredLevel,
        List<ClassEffect> effects,
        List<Identifier> grants) {

    public static final Codec<SkillNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(SkillNode::id),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillNode::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillNode::description),
            NodeType.CODEC.optionalFieldOf("type", NodeType.SMALL).forGetter(SkillNode::type),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.BOOK).forGetter(SkillNode::icon),
            Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(SkillNode::x),
            Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(SkillNode::y),
            Codec.STRING.listOf().optionalFieldOf("links", List.of()).forGetter(SkillNode::links),
            Codec.intRange(0, 1000).optionalFieldOf("cost", 1).forGetter(SkillNode::cost),
            Codec.INT.optionalFieldOf("required_level", 1).forGetter(SkillNode::requiredLevel),
            ClassEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SkillNode::effects),
            Identifier.CODEC.listOf().optionalFieldOf("grants", List.of()).forGetter(SkillNode::grants)
    ).apply(i, SkillNode::new));
}
