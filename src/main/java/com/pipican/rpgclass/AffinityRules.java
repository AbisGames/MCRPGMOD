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
 * broad tags. A rule can be limited to one {@code hand} ({@code "main"} or {@code "off"}: "a book in the off-hand"), and
 * with {@code counts_as} (a weapon category, e.g. {@code "#pipican:weapons/spears"}) its items also get every bonus for
 * that kind of weapon ("sticks count as a staff (spear)"). Items matching no rule use {@code default}. The mod ships category tags under
 * {@code pipican:weapons/*}, {@code pipican:armor/*} and {@code pipican:trinkets/*}.
 */
public record AffinityRules(Affinity fallback, List<Rule> rules) {

    public static final AffinityRules NEUTRAL = new AffinityRules(Affinity.NEUTRAL, List.of());

    public static final Codec<AffinityRules> CODEC = RecordCodecBuilder.create(i -> i.group(
            Affinity.CODEC.optionalFieldOf("default", Affinity.NEUTRAL).forGetter(AffinityRules::fallback),
            Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(AffinityRules::rules)
    ).apply(i, AffinityRules::new));

    /** The tier of {@code stack} in any hand (tooltips, mastery). */
    public Affinity resolve(ItemStack stack) {
        return resolve(stack, null);
    }

    /** The tier of {@code stack} held in {@code hand} (null: any hand, so hand-limited rules count too). */
    public Affinity resolve(ItemStack stack, @org.jspecify.annotations.Nullable Hand hand) {
        Rule rule = rule(stack, hand);
        return stack.isEmpty() ? Affinity.NEUTRAL : rule != null ? rule.tier() : fallback;
    }

    /** The first rule for {@code stack} in {@code hand}, or null. */
    public @org.jspecify.annotations.Nullable Rule rule(ItemStack stack, @org.jspecify.annotations.Nullable Hand hand) {
        if (stack.isEmpty()) {
            return null;
        }
        for (Rule rule : rules) {
            if (stack.is(rule.items()) && (hand == null || rule.hand().map(h -> h == hand).orElse(true))) {
                return rule;
            }
        }
        return null;
    }

    /** True if a rule makes {@code stack} (in the main hand) count as one of {@code weapons}. */
    public boolean countsAs(ItemStack stack, HolderSet<Item> weapons) {
        Rule rule = rule(stack, Hand.MAIN);
        return rule != null && rule.countsAs().map(set -> set.stream().anyMatch(weapons::contains)).orElse(false);
    }

    public enum Hand implements net.minecraft.util.StringRepresentable {
        MAIN("main"), OFF("off");

        public static final Codec<Hand> CODEC = net.minecraft.util.StringRepresentable.fromEnum(Hand::values);
        private final String name;

        Hand(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /** One line of the rules list: a set of items (id, list of ids, or #tag), their tier, and optionally a hand and a category. */
    public record Rule(HolderSet<Item> items, Affinity tier, java.util.Optional<Hand> hand, java.util.Optional<HolderSet<Item>> countsAs) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(Rule::items),
                Affinity.CODEC.fieldOf("tier").forGetter(Rule::tier),
                Hand.CODEC.optionalFieldOf("hand").forGetter(Rule::hand),
                RegistryCodecs.homogeneousList(Registries.ITEM).optionalFieldOf("counts_as").forGetter(Rule::countsAs)
        ).apply(i, Rule::new));
    }
}
