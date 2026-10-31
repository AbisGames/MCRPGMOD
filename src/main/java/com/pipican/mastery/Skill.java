package com.pipican.mastery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.core.PipicanRegistries;
import com.pipican.effect.ClassEffect;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.TreeKind;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * A masterable skill, loaded from {@code data/<ns>/pipican/skill/<name>.json}.
 *
 * <p>Using the skill (as defined by its {@link SkillTrigger}) builds mastery. There are always
 * {@value #TIERS} mastery tiers. Reaching a tier:
 * <ul>
 *   <li>applies that tier's effects (tiers stack: tier 3 = effects of tiers 1, 2 and 3),</li>
 *   <li>gives one skill point in the tree named by {@code points_to}.</li>
 * </ul>
 * This is the <b>only</b> source of skill-tree points (besides a class's starting points).
 *
 * <pre>{@code
 * {
 *   "name": "Swordsmanship",
 *   "icon": "minecraft:iron_sword",
 *   "trigger": { "type": "pipican:melee_hit", "items": "#pipican:weapons/swords" },
 *   "thresholds": [ 100, 400, 1200, 3000, 7000 ],
 *   "tiers": [
 *     { "description": "+5% sword damage", "effects": [
 *         { "type": "pipican:damage_dealt", "weapons": "#pipican:weapons/swords", "multiplier": 1.05 } ] },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * @param trigger        what counts as a use
 * @param gainPerUse     mastery per use (before affinity scaling)
 * @param cooldownTicks  minimum ticks between counted uses (20 = 1 s). Movement ignores it.
 * @param thresholds     total mastery needed for tiers 1..5, ascending
 * @param tiers          bonus of each tier, in order (fewer than 5 entries is fine)
 * @param pointsTo       which tree the tier-up points go to (general or special)
 * @param classes        if non-empty, only these classes can train the skill or get its bonuses/points
 * @param affinityScaled if true, mastery gain is scaled by the class's affinity for the item used
 */
public record Skill(
        Component name,
        Component description,
        Item icon,
        SkillTrigger trigger,
        double gainPerUse,
        int cooldownTicks,
        List<Integer> thresholds,
        List<Tier> tiers,
        TreeKind pointsTo,
        List<ResourceKey<RpgClass>> classes,
        boolean affinityScaled) {

    public static final int TIERS = 5;
    public static final List<Integer> DEFAULT_THRESHOLDS = List.of(100, 400, 1200, 3000, 7000);

    public static final Codec<Skill> CODEC = RecordCodecBuilder.<Skill>create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Skill::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Skill::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.BOOK).forGetter(Skill::icon),
            SkillTrigger.CODEC.fieldOf("trigger").forGetter(Skill::trigger),
            Codec.doubleRange(0, 1_000_000).optionalFieldOf("gain_per_use", 1.0).forGetter(Skill::gainPerUse),
            Codec.intRange(0, 72_000).optionalFieldOf("cooldown_ticks", 10).forGetter(Skill::cooldownTicks),
            Codec.INT.listOf().optionalFieldOf("thresholds", DEFAULT_THRESHOLDS).forGetter(Skill::thresholds),
            Tier.CODEC.listOf().optionalFieldOf("tiers", List.of()).forGetter(Skill::tiers),
            TreeKind.CODEC.validate(k -> k == TreeKind.STAT
                    ? DataResult.error(() -> "points_to must be general or special: the stat tree is paid with stat points")
                    : DataResult.success(k)).optionalFieldOf("points_to", TreeKind.GENERAL).forGetter(Skill::pointsTo),
            ResourceKey.codec(PipicanRegistries.CLASS).listOf().optionalFieldOf("classes", List.of()).forGetter(Skill::classes),
            Codec.BOOL.optionalFieldOf("affinity_scaled", true).forGetter(Skill::affinityScaled)
    ).apply(i, Skill::new)).validate(Skill::validate);

    private static DataResult<Skill> validate(Skill skill) {
        if (skill.thresholds.size() != TIERS) {
            return DataResult.error(() -> "Skill needs exactly " + TIERS + " thresholds, got " + skill.thresholds);
        }
        for (int t = 1; t < TIERS; t++) {
            if (skill.thresholds.get(t) <= skill.thresholds.get(t - 1)) {
                return DataResult.error(() -> "Skill thresholds must be ascending: " + skill.thresholds);
            }
        }
        if (skill.tiers.size() > TIERS) {
            return DataResult.error(() -> "Skill has more than " + TIERS + " tiers");
        }
        return DataResult.success(skill);
    }

    /** Tier reached with this much mastery (0..5). */
    public int tierFor(double mastery) {
        int tier = 0;
        while (tier < TIERS && mastery >= thresholds.get(tier)) {
            tier++;
        }
        return tier;
    }

    /** Whether a player of this class can train / benefit from the skill. */
    public boolean allowedFor(Optional<ResourceKey<RpgClass>> rpgClass) {
        return classes.isEmpty() || rpgClass.map(classes::contains).orElse(false);
    }

    /** One mastery tier's bonus. {@code description} is shown to players. */
    /** One mastery tier: its bonus, and any abilities it unlocks ({@code grants}, ability ids). */
    public record Tier(Component description, List<ClassEffect> effects, List<Identifier> grants) {
        public static final Codec<Tier> CODEC = RecordCodecBuilder.create(i -> i.group(
                ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Tier::description),
                ClassEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Tier::effects),
                Identifier.CODEC.listOf().optionalFieldOf("grants", List.of()).forGetter(Tier::grants)
        ).apply(i, Tier::new));
    }
}
