package com.pipican.player;

import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatDefinition;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The computed (never saved) result of a player's class: total stats, damage multipliers, granted
 * potion effects and resource numbers. Rebuilt by {@link ClassEffectApplier#refresh} whenever
 * anything that affects it changes, then read by event handlers every hit / tick.
 */
public record ClassState(
        Optional<RpgClass> rpgClass,
        Map<ResourceKey<StatDefinition>, Double> stats,
        List<DamageRule> damageTaken,
        List<DamageRule> damageDealt,
        Map<Holder<MobEffect>, Integer> mobEffects,
        Set<Holder<Attribute>> touchedAttributes,
        double resourceMax,
        double resourceRegenPerTick) {

    public static final ClassState EMPTY = new ClassState(Optional.empty(), Map.of(), List.of(), List.of(), Map.of(), Set.of(), 0, 0);

    /** A damage multiplier, optionally limited to damage types in a tag and/or the weapon used. */
    public record DamageRule(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier) {
        public boolean matches(DamageSource source) {
            if (types.isPresent() && !source.is(types.get())) {
                return false;
            }
            if (weapons.isPresent()) {
                ItemStack weapon = source.getWeaponItem();
                return weapon != null && !weapon.isEmpty() && weapon.is(weapons.get());
            }
            return true;
        }
    }

    public double stat(ResourceKey<StatDefinition> stat) {
        return stats.getOrDefault(stat, 0.0);
    }

    public static double multiplier(List<DamageRule> rules, DamageSource source) {
        double mult = 1;
        for (DamageRule rule : rules) {
            if (rule.matches(source)) {
                mult *= rule.multiplier();
            }
        }
        return mult;
    }
}
