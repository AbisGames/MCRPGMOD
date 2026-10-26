package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A class's equipment rules: which {@link Affinity} tier each item has for this class.
 *
 * <pre>{@code
 * "affinities": {
 *   "default": "neutral",
 *   "rules": [
 *     { "items": "minecraft:trident",       "tier": "mastery" },
 *     { "items": "#pipican:weapons/swords", "tier": "proficient" },
 *     { "items": "#pipican:armor/heavy",    "tier": "forbidden" },
 *     { "items": ["minecraft:bow", "minecraft:crossbow"], "tier": "clumsy" }
 *   ]
 * }
 * }</pre>
 * Rules are checked <b>top to bottom and the first match wins</b>, so put specific items before
 * broad tags. Items matching no rule use {@code default}. The mod ships category tags under
 * {@code pipican:weapons/*}, {@code pipican:armor/*} and {@code pipican:trinkets/*}.
 */
public record AffinityRules(Affinity fallback, List<Rule> rules) {

    public static final AffinityRules NEUTRAL = new AffinityRules(Affinity.NEUTRAL, List.of());

    public static final Codec<AffinityRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Affinity.CODEC.optionalFieldOf("default", Affinity.NEUTRAL).forGetter(AffinityRules::fallback),
            Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(AffinityRules::rules)
    ).apply(i, AffinityRules::new));

    public Affinity resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return Affinity.NEUTRAL;
        }
        for (Rule rule : rules) {
            if (stack.is(rule.items())) {
                return rule.tier();
            }
        }
        return fallback;
    }

    /** One line of the rules list: a set of items (id, list of ids, or #tag) and their tier. */
    public record Rule(HolderSet<Item> items, Affinity tier) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(Rule::items),
                Affinity.CODEC.fieldOf("tier").forGetter(Rule::tier)
        ).apply(i, Rule::new));
    }
}
