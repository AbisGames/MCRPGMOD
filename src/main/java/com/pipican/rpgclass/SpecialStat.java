package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.core.PipicanRegistries;
import com.pipican.stat.StatDefinition;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;

/**
 * A class's special stat. It is a normal {@link StatDefinition} (so it gives per-point attribute
 * bonuses and can be raised with stat points like any stat) that additionally:
 * <ul>
 *   <li>is the {@code special_stat} scaling source for the class's effects,</li>
 *   <li>can be raised even if the stat is marked {@code exclusive},</li>
 *   <li>optionally drives a <b>resource bar</b> (mana, rage, energy...) shown on the HUD.</li>
 * </ul>
 * Starting points go in the class's {@code base_stats} like any other stat.
 *
 * <pre>{@code
 * "special_stat": {
 *   "stat": "pipican:arcana",
 *   "resource": { "name": "Mana", "base_max": 50, "max_per_point": 5, "regen_per_second": 1, "color": -12627969 }
 * }
 * }</pre>
 */
public record SpecialStat(ResourceKey<StatDefinition> stat, Optional<Resource> resource) {

    public static final Codec<SpecialStat> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceKey.codec(PipicanRegistries.STAT).fieldOf("stat").forGetter(SpecialStat::stat),
            Resource.CODEC.optionalFieldOf("resource").forGetter(SpecialStat::resource)
    ).apply(i, SpecialStat::new));

    /**
     * A spendable pool tied to the special stat. Max = base_max + max_per_point * points,
     * regen per second = regen_per_second + regen_per_point * points.
     * Abilities spend it; {@code pipican:on} reactions with a {@code pipican:resource} action fill it.
     * A bar with negative regen drains; with {@code drains_in_combat: false} it pauses for 5s after each hit dealt or taken.
     */
    public record Resource(Component name, double baseMax, double maxPerPoint, double regenPerSecond, double regenPerPoint, int color, boolean startsFull,
                           boolean drainsInCombat) {
        public static final Codec<Resource> CODEC = RecordCodecBuilder.create(i -> i.group(
                ComponentSerialization.CODEC.optionalFieldOf("name", Component.literal("Resource")).forGetter(Resource::name),
                Codec.DOUBLE.optionalFieldOf("base_max", 100.0).forGetter(Resource::baseMax),
                Codec.DOUBLE.optionalFieldOf("max_per_point", 0.0).forGetter(Resource::maxPerPoint),
                Codec.DOUBLE.optionalFieldOf("regen_per_second", 1.0).forGetter(Resource::regenPerSecond),
                Codec.DOUBLE.optionalFieldOf("regen_per_point", 0.0).forGetter(Resource::regenPerPoint),
                Codec.INT.optionalFieldOf("color", 0xFF3F7FFF).forGetter(Resource::color),
                Codec.BOOL.optionalFieldOf("starts_full", true).forGetter(Resource::startsFull),
                Codec.BOOL.optionalFieldOf("drains_in_combat", true).forGetter(Resource::drainsInCombat)
        ).apply(i, Resource::new));

        public double max(double points) {
            return Math.max(0, baseMax + maxPerPoint * points);
        }

        public double regenPerSecond(double points) {
            return regenPerSecond + regenPerPoint * points;
        }
    }
}
