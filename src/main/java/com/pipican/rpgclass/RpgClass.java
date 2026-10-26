package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.core.PipicanRegistries;
import com.pipican.effect.ClassEffect;
import com.pipican.skill.SkillTree;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketSlotType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

/**
 * The class template. Every class is one JSON file at {@code data/<ns>/pipican/class/<name>.json};
 * the class id is {@code <ns>:<name>}. No Java is needed to add a class.
 *
 * <p>Every field except {@code name} is optional. See docs/CLASS_SYSTEM.md for the full reference
 * and docs/templates/class_template.json for a copy-paste starting point.
 *
 * @param name             display name
 * @param description      shown on the class-selection screen
 * @param icon             item shown as the class icon
 * @param selectable       false hides the class from the selection screen (admins can still set it)
 * @param baseHealth       starting max health (vanilla player is 20 = 10 hearts). Does not scale with level.
 * @param baseStats        starting points per stat, e.g. {@code {"pipican:strength": 5}}
 * @param specialStat      the class's signature stat (+ optional resource bar)
 * @param startingKit      items given once when the class is chosen ({@code "minecraft:bread"} or
 *                         {@code {"id": "minecraft:bread", "count": 8, "components": {...}}})
 * @param skillTree        id of the class's general skill tree
 * @param specialSkillTree id of the class's special (signature) skill tree
 * @param progression      max level and skill points per level
 * @param passives         always-on bonuses
 * @param specialPassives  always-on signature bonuses; typically scale with {@code special_stat}
 * @param weakness         the class's special weakness
 * @param affinities       equipment rules: affinity tier per weapon / armor / trinket
 * @param trinketSlots     how many of each trinket slot this class has, e.g. {@code {"pipican:ring": 2}}
 */
public record RpgClass(
        Component name,
        Component description,
        Item icon,
        boolean selectable,
        double baseHealth,
        Map<ResourceKey<StatDefinition>, Integer> baseStats,
        Optional<SpecialStat> specialStat,
        List<ItemStackTemplate> startingKit,
        Optional<ResourceKey<SkillTree>> skillTree,
        Optional<ResourceKey<SkillTree>> specialSkillTree,
        Progression progression,
        List<ClassEffect> passives,
        List<ClassEffect> specialPassives,
        Optional<Weakness> weakness,
        AffinityRules affinities,
        Map<ResourceKey<TrinketSlotType>, Integer> trinketSlots) {

    public static final Codec<RpgClass> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(RpgClass::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(RpgClass::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PLAYER_HEAD).forGetter(RpgClass::icon),
            Codec.BOOL.optionalFieldOf("selectable", true).forGetter(RpgClass::selectable),
            Codec.doubleRange(1, 2048).optionalFieldOf("base_health", 20.0).forGetter(RpgClass::baseHealth),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.STAT), Codec.INT).optionalFieldOf("base_stats", Map.of()).forGetter(RpgClass::baseStats),
            SpecialStat.CODEC.optionalFieldOf("special_stat").forGetter(RpgClass::specialStat),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("starting_kit", List.of()).forGetter(RpgClass::startingKit),
            ResourceKey.codec(PipicanRegistries.SKILL_TREE).optionalFieldOf("skill_tree").forGetter(RpgClass::skillTree),
            ResourceKey.codec(PipicanRegistries.SKILL_TREE).optionalFieldOf("special_skill_tree").forGetter(RpgClass::specialSkillTree),
            Progression.CODEC.optionalFieldOf("progression", Progression.DEFAULT).forGetter(RpgClass::progression),
            ClassEffect.CODEC.listOf().optionalFieldOf("passives", List.of()).forGetter(RpgClass::passives),
            ClassEffect.CODEC.listOf().optionalFieldOf("special_passives", List.of()).forGetter(RpgClass::specialPassives),
            Weakness.CODEC.optionalFieldOf("weakness").forGetter(RpgClass::weakness),
            AffinityRules.CODEC.optionalFieldOf("affinities", AffinityRules.NEUTRAL).forGetter(RpgClass::affinities),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.TRINKET_SLOT), Codec.intRange(0, 64)).optionalFieldOf("trinket_slots", Map.of()).forGetter(RpgClass::trinketSlots)
    ).apply(i, RpgClass::new));

    public Optional<ResourceKey<SkillTree>> tree(TreeKind kind) {
        return kind == TreeKind.GENERAL ? skillTree : specialSkillTree;
    }

    public int pointsPerLevel(TreeKind kind) {
        return kind == TreeKind.GENERAL ? progression.skillPointsPerLevel() : progression.specialSkillPointsPerLevel();
    }

    public int baseStat(ResourceKey<StatDefinition> stat) {
        return baseStats.getOrDefault(stat, 0);
    }

    public boolean isSpecialStat(ResourceKey<StatDefinition> stat) {
        return specialStat.isPresent() && specialStat.get().stat().equals(stat);
    }
}
