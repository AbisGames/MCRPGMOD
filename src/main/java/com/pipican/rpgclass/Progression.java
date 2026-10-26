package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How a class levels. Class XP is separate from vanilla XP (vanilla XP buys stat points instead).
 * The XP needed per level comes from the server config curve; these numbers are per class.
 *
 * <pre>{@code "progression": { "max_level": 50, "skill_points_per_level": 1, "special_skill_points_per_level": 1 } }</pre>
 * Points are earned on every level-up after level 1, so a level-10 character has had 9 level-ups.
 */
public record Progression(int maxLevel, int skillPointsPerLevel, int specialSkillPointsPerLevel) {

    public static final Progression DEFAULT = new Progression(50, 1, 1);

    public static final Codec<Progression> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 10_000).optionalFieldOf("max_level", DEFAULT.maxLevel).forGetter(Progression::maxLevel),
            Codec.intRange(0, 1000).optionalFieldOf("skill_points_per_level", DEFAULT.skillPointsPerLevel).forGetter(Progression::skillPointsPerLevel),
            Codec.intRange(0, 1000).optionalFieldOf("special_skill_points_per_level", DEFAULT.specialSkillPointsPerLevel).forGetter(Progression::specialSkillPointsPerLevel)
    ).apply(i, Progression::new));
}
