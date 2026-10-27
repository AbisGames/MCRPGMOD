package com.pipican.condition;

import com.pipican.condition.type.HoldingFavoredCondition;
import com.pipican.condition.type.StillCondition;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import com.pipican.condition.type.AbilityActiveCondition;
import com.pipican.condition.type.BiomeCondition;
import com.pipican.condition.type.CompositeCondition;
import com.pipican.condition.type.DimensionCondition;
import com.pipican.condition.type.EffectCondition;
import com.pipican.condition.type.HasEidolonCondition;
import com.pipican.condition.type.HealthCondition;
import com.pipican.condition.type.HoldingCondition;
import com.pipican.condition.type.LightCondition;
import com.pipican.condition.type.NearbyCondition;
import com.pipican.condition.type.NotCondition;
import com.pipican.condition.type.ResourceCondition;
import com.pipican.condition.type.StandingOnCondition;
import com.pipican.condition.type.StateCondition;
import com.pipican.condition.type.WearingCondition;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Registry of condition types usable in JSON via {@code "type": "<id>"}. Mirrors ClassEffectTypes. */
public final class ConditionTypes {

    public record Type(Identifier id, MapCodec<? extends ClassCondition> codec) {}

    private static final Map<Identifier, Type> TYPES = new LinkedHashMap<>();

    static final Codec<Type> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
            id -> {
                Type type = TYPES.get(id);
                return type != null ? DataResult.success(type) : DataResult.error(() -> "Unknown condition type: " + id + " (known: " + TYPES.keySet() + ")");
            },
            Type::id);

    public static final Type STATE = register("state", StateCondition.CODEC);
    public static final Type HEALTH = register("health", HealthCondition.CODEC);
    public static final Type RESOURCE = register("resource", ResourceCondition.CODEC);
    public static final Type LIGHT = register("light", LightCondition.CODEC);
    public static final Type DIMENSION = register("dimension", DimensionCondition.CODEC);
    public static final Type BIOME = register("biome", BiomeCondition.CODEC);
    public static final Type HOLDING = register("holding", HoldingCondition.CODEC);
    public static final Type WEARING = register("wearing", WearingCondition.CODEC);
    public static final Type NEARBY = register("nearby", NearbyCondition.CODEC);
    public static final Type EFFECT = register("effect", EffectCondition.CODEC);
    public static final Type STANDING_ON = register("standing_on", StandingOnCondition.CODEC);
    public static final Type ABILITY_ACTIVE = register("ability_active", AbilityActiveCondition.CODEC);
    public static final Type STILL = register("still", StillCondition.CODEC);
    public static final Type HOLDING_FAVORED = register("holding_favored", HoldingFavoredCondition.CODEC);
    public static final Type HAS_EIDOLON = register("has_eidolon", HasEidolonCondition.CODEC);
    public static final Type NOT = register("not", NotCondition.CODEC);
    public static final Type ALL_OF = register("all_of", CompositeCondition.ALL_CODEC);
    public static final Type ANY_OF = register("any_of", CompositeCondition.ANY_CODEC);

    private ConditionTypes() {}

    public static Type register(Identifier id, MapCodec<? extends ClassCondition> codec) {
        Type type = new Type(id, codec);
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("Duplicate condition type " + id);
        }
        return type;
    }

    private static Type register(String path, MapCodec<? extends ClassCondition> codec) {
        return register(Pipican.id(path), codec);
    }
}
