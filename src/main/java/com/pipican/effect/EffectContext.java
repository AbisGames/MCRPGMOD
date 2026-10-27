package com.pipican.effect;

import com.pipican.condition.ClassCondition;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.effect.type.ResourceEffect;
import com.pipican.stat.StatDefinition;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

/**
 * Collects the results of {@link ClassEffect}s while a player's bonuses are being recalculated.
 * Implemented by {@link com.pipican.player.ClassEffectApplier}; effects only ever talk to this
 * interface, so new effect types never need to touch the applier.
 */
public interface EffectContext {

    /** Value of a {@link Scaling} source ({@code class_level}, {@code special_stat}, or a stat id). */
    double sourceValue(String source);

    /**
     * Multiplier the effect is applied at. Always 1 today; kept so effects stay ready for stacking
     * sources (e.g. jewels or repeated bonuses) without changing every effect type.
     */
    int rank();

    void addStat(ResourceKey<StatDefinition> stat, double amount);

    void addAttribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation);

    void addMobEffect(Holder<MobEffect> effect, int amplifier);

    /** Multiplies incoming damage, optionally only of {@code types} and/or dealt with one of {@code weapons}. */
    void addDamageTakenMultiplier(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier);

    /** Multiplies outgoing damage, optionally only of {@code types} and/or dealt with one of {@code weapons}. */
    void addDamageDealtMultiplier(Optional<TagKey<DamageType>> types, Optional<HolderSet<Item>> weapons, double multiplier);

    /** Changes the resource bar's max or regen; {@code operation} is add_value or add_multiplied_base. */
    void addResource(ResourceEffect.Target target, double amount, AttributeModifier.Operation operation);

    /** Registers a {@code pipican:on} reaction for this player. */
    void addReaction(ReactionEffect reaction);

    /** Changes an attribute of the player's eidolon. */
    void addEidolonUpgrade(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation);

    /** Spell points that refill on every sleep. */
    void addSpellPoints(int amount);

    /** Lets the player fly (creative-style) while this effect counts. */
    void grantFlight();

    /** Tests a condition now, and remembers it so the bonuses are rebuilt when its answer changes. */
    boolean test(ClassCondition condition);
}
