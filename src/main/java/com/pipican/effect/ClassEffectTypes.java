package com.pipican.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import com.pipican.effect.type.AttributeEffect;
import com.pipican.effect.type.BonusEffect;
import com.pipican.effect.type.ConditionalEffect;
import com.pipican.effect.type.CriticalEffect;
import com.pipican.effect.type.DamageMultiplierEffect;
import com.pipican.effect.type.DodgeEffect;
import com.pipican.effect.type.EidolonEffect;
import com.pipican.effect.type.FlightEffect;
import com.pipican.effect.type.ImmuneEffect;
import com.pipican.effect.type.MobEffectEffect;
import com.pipican.effect.type.OptimizationEffect;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.effect.type.ResourceEffect;
import com.pipican.effect.type.SpellPointsEffect;
import com.pipican.effect.type.StatEffect;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Registry of effect types usable in class/skill JSON via {@code "type": "<id>"}.
 *
 * <p>This is a plain code-side map (not a datapack registry) because each type needs Java logic.
 * Register new types from static init here, or from another mod during construction.
 */
public final class ClassEffectTypes {

    /** A named effect implementation and the codec that reads its JSON fields. */
    public record Type(Identifier id, MapCodec<? extends ClassEffect> codec) {}

    private static final Map<Identifier, Type> TYPES = new LinkedHashMap<>();

    static final Codec<Type> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
            id -> {
                Type type = TYPES.get(id);
                return type != null ? DataResult.success(type) : DataResult.error(() -> "Unknown class effect type: " + id + " (known: " + TYPES.keySet() + ")");
            },
            Type::id);

    public static final Type ATTRIBUTE = register("attribute", AttributeEffect.CODEC);
    public static final Type STAT = register("stat", StatEffect.CODEC);
    public static final Type MOB_EFFECT = register("mob_effect", MobEffectEffect.CODEC);
    public static final Type DAMAGE_TAKEN = register("damage_taken", DamageMultiplierEffect.TAKEN_CODEC);
    public static final Type DAMAGE_DEALT = register("damage_dealt", DamageMultiplierEffect.DEALT_CODEC);
    public static final Type RESOURCE = register("resource", ResourceEffect.CODEC);
    public static final Type CONDITIONAL = register("conditional", ConditionalEffect.CODEC);
    public static final Type FLIGHT = register("flight", FlightEffect.CODEC);
    public static final Type ON = register("on", ReactionEffect.CODEC);
    public static final Type EIDOLON = register("eidolon", EidolonEffect.CODEC);
    public static final Type SPELL_POINTS = register("spell_points", SpellPointsEffect.CODEC);
    public static final Type OPTIMIZATION = register("optimization", OptimizationEffect.CODEC);
    public static final Type CRITICAL = register("critical", CriticalEffect.CODEC);
    public static final Type DODGE = register("dodge", DodgeEffect.CODEC);
    public static final Type IMMUNE = register("immune", ImmuneEffect.CODEC);
    public static final Type BONUS = register("bonus", BonusEffect.CODEC);

    private ClassEffectTypes() {}

    public static Type register(Identifier id, MapCodec<? extends ClassEffect> codec) {
        Type type = new Type(id, codec);
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("Duplicate class effect type " + id);
        }
        return type;
    }

    private static Type register(String path, MapCodec<? extends ClassEffect> codec) {
        return register(Pipican.id(path), codec);
    }
}
