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
 * One node of a {@link SkillTree}.
 *
 * <pre>{@code
 * {
 *   "id": "iron_skin",
 *   "name": "Iron Skin",
 *   "description": "+1 armor per rank.",
 *   "icon": "minecraft:iron_chestplate",
 *   "x": 0, "y": 1,
 *   "cost": 1,
 *   "max_rank": 3,
 *   "required_level": 5,
 *   "requires": [ "toughness" ],
 *   "effects": [ { "type": "pipican:attribute", "attribute": "minecraft:armor", "amount": 1 } ],
 *   "grants": [ "pipican:shield_bash" ]
 * }
 * }</pre>
 *
 * @param id            unique within its tree
 * @param x             column in the (future) tree screen
 * @param y             row in the (future) tree screen
 * @param cost          skill points per rank
 * @param maxRank       how many times the node can be bought
 * @param requiredLevel minimum class level to buy the first rank
 * @param requires      node ids that must have at least one rank first (all of them)
 * @param effects       applied once per rank (see {@link ClassEffect})
 * @param grants        ids of active abilities unlocked by this node. Stored now; the ability
 *                      system that reads them is a later task.
 */
public record SkillNode(
        String id,
        Component name,
        Component description,
        Item icon,
        int x,
        int y,
        int cost,
        int maxRank,
        int requiredLevel,
        List<String> requires,
        List<ClassEffect> effects,
        List<Identifier> grants) {

    public static final Codec<SkillNode> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(SkillNode::id),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(SkillNode::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(SkillNode::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.BOOK).forGetter(SkillNode::icon),
            Codec.INT.optionalFieldOf("x", 0).forGetter(SkillNode::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(SkillNode::y),
            Codec.intRange(0, 1000).optionalFieldOf("cost", 1).forGetter(SkillNode::cost),
            Codec.intRange(1, 1000).optionalFieldOf("max_rank", 1).forGetter(SkillNode::maxRank),
            Codec.INT.optionalFieldOf("required_level", 1).forGetter(SkillNode::requiredLevel),
            Codec.STRING.listOf().optionalFieldOf("requires", List.of()).forGetter(SkillNode::requires),
            ClassEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SkillNode::effects),
            Identifier.CODEC.listOf().optionalFieldOf("grants", List.of()).forGetter(SkillNode::grants)
    ).apply(i, SkillNode::new));
}
