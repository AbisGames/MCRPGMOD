package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Class levelling and starting skill points.
 *
 * <p>Class XP comes from kills and is separate from vanilla XP. The same kill XP also earns stat
 * points (see {@link com.pipican.stat.StatPoints}). Class level gates skill nodes
 * ({@code required_level}) and can scale effects, but does <b>not</b> give skill points: those come
 * from skill mastery tiers (see {@link com.pipican.mastery.Skill}).
 *
 * <pre>{@code "progression": { "max_level": 50, "starting_skill_points": 0, "starting_special_skill_points": 0 } }</pre>
 */
public record Progression(int maxLevel, int startingSkillPoints, int startingSpecialSkillPoints) {

    public static final Progression DEFAULT = new Progression(50, 0, 0);

    public static final Codec<Progression> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 10_000).optionalFieldOf("max_level", DEFAULT.maxLevel).forGetter(Progression::maxLevel),
            Codec.intRange(0, 1000).optionalFieldOf("starting_skill_points", 0).forGetter(Progression::startingSkillPoints),
            Codec.intRange(0, 1000).optionalFieldOf("starting_special_skill_points", 0).forGetter(Progression::startingSpecialSkillPoints)
    ).apply(i, Progression::new));
}
