package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Ability;
import com.pipican.core.PipicanRegistries;
import com.pipican.effect.ClassEffect;
import com.pipican.skill.TreeKind;
import com.pipican.skill.TreeRef;
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
 * @param skillTree        the general passive tree and this class's start node in it (trees can be shared)
 * @param specialSkillTree the special (signature, ascendancy-like) tree and its start node
 * @param statTree         the stat tree (paid with stat points) and this class's start node in it
 * @param progression      max level and starting skill points
 * @param passives         always-on bonuses
 * @param specialPassives  always-on signature bonuses; typically scale with {@code special_stat}
 * @param weakness         the class's special weakness
 * @param affinities       equipment rules: affinity tier per weapon / armor / trinket
 * @param trinketSlots     how many of each trinket slot this class has, e.g. {@code {"pipican:ring": 2}}
 * @param abilities        active abilities the class always has (tree nodes can {@code grant} more)
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
        Optional<TreeRef> skillTree,
        Optional<TreeRef> specialSkillTree,
        Optional<TreeRef> statTree,
        Progression progression,
        List<ClassEffect> passives,
        List<ClassEffect> specialPassives,
        Optional<Weakness> weakness,
        AffinityRules affinities,
        Map<ResourceKey<TrinketSlotType>, Integer> trinketSlots,
        List<ResourceKey<Ability>> abilities) {

    public static final Codec<RpgClass> CODEC = RecordCodecBuilder.create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(RpgClass::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(RpgClass::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.PLAYER_HEAD).forGetter(RpgClass::icon),
            Codec.BOOL.optionalFieldOf("selectable", true).forGetter(RpgClass::selectable),
            Codec.doubleRange(1, 2048).optionalFieldOf("base_health", 20.0).forGetter(RpgClass::baseHealth),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.STAT), Codec.INT).optionalFieldOf("base_stats", Map.of()).forGetter(RpgClass::baseStats),
            SpecialStat.CODEC.optionalFieldOf("special_stat").forGetter(RpgClass::specialStat),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("starting_kit", List.of()).forGetter(RpgClass::startingKit),
            Trees.MAP_CODEC.forGetter(c -> new Trees(c.skillTree, c.specialSkillTree, c.statTree)),
            Progression.CODEC.optionalFieldOf("progression", Progression.DEFAULT).forGetter(RpgClass::progression),
            ClassEffect.CODEC.listOf().optionalFieldOf("passives", List.of()).forGetter(RpgClass::passives),
            ClassEffect.CODEC.listOf().optionalFieldOf("special_passives", List.of()).forGetter(RpgClass::specialPassives),
            Weakness.CODEC.optionalFieldOf("weakness").forGetter(RpgClass::weakness),
            AffinityRules.CODEC.optionalFieldOf("affinities", AffinityRules.NEUTRAL).forGetter(RpgClass::affinities),
            Codec.unboundedMap(ResourceKey.codec(PipicanRegistries.TRINKET_SLOT), Codec.intRange(0, 64)).optionalFieldOf("trinket_slots", Map.of()).forGetter(RpgClass::trinketSlots),
            ResourceKey.codec(PipicanRegistries.ABILITY).listOf().optionalFieldOf("abilities", List.of()).forGetter(RpgClass::abilities)
    ).apply(i, (name, description, icon, selectable, baseHealth, baseStats, specialStat, startingKit, trees,
                progression, passives, specialPassives, weakness, affinities, trinketSlots, abilities) -> new RpgClass(
                name, description, icon, selectable, baseHealth, baseStats, specialStat, startingKit,
                trees.skill(), trees.special(), trees.stat(),
                progression, passives, specialPassives, weakness, affinities, trinketSlots, abilities)));

    /** The three tree fields, grouped only because a codec takes at most 16 fields. Still flat in JSON. */
    private record Trees(Optional<TreeRef> skill, Optional<TreeRef> special, Optional<TreeRef> stat) {
        static final MapCodec<Trees> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                TreeRef.CODEC.optionalFieldOf("skill_tree").forGetter(Trees::skill),
                TreeRef.CODEC.optionalFieldOf("special_skill_tree").forGetter(Trees::special),
                TreeRef.CODEC.optionalFieldOf("stat_tree").forGetter(Trees::stat)
        ).apply(i, Trees::new));
    }

    public Optional<TreeRef> tree(TreeKind kind) {
        return switch (kind) {
            case GENERAL -> skillTree;
            case SPECIAL -> specialSkillTree;
            case STAT -> statTree;
        };
    }

    /** Starting skill points of a mastery-fed tree. The stat tree uses stat points instead (always 0 here). */
    public int startingPoints(TreeKind kind) {
        return switch (kind) {
            case GENERAL -> progression.startingSkillPoints();
            case SPECIAL -> progression.startingSpecialSkillPoints();
            case STAT -> 0;
        };
    }

    public int baseStat(ResourceKey<StatDefinition> stat) {
        return baseStats.getOrDefault(stat, 0);
    }

    public boolean isSpecialStat(ResourceKey<StatDefinition> stat) {
        return specialStat.isPresent() && specialStat.get().stat().equals(stat);
    }
}
