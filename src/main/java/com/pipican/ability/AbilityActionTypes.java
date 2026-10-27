package com.pipican.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import com.pipican.ability.action.AbsorbAction;
import com.pipican.ability.action.BlinkAction;
import com.pipican.ability.action.CleanseAction;
import com.pipican.ability.action.DamageAction;
import com.pipican.ability.action.DashAction;
import com.pipican.ability.action.EffectAction;
import com.pipican.ability.action.ExplodeAction;
import com.pipican.ability.action.HealAction;
import com.pipican.ability.action.LightningAction;
import com.pipican.ability.action.PushAction;
import com.pipican.ability.action.ResourceAction;
import com.pipican.ability.action.SummonAction;
import com.pipican.ability.action.WeatherAction;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Registry of ability action types usable in JSON via {@code "type": "<id>"}. Mirrors ClassEffectTypes. */
public final class AbilityActionTypes {

    public record Type(Identifier id, MapCodec<? extends AbilityAction> codec) {}

    private static final Map<Identifier, Type> TYPES = new LinkedHashMap<>();

    static final Codec<Type> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
            id -> {
                Type type = TYPES.get(id);
                return type != null ? DataResult.success(type) : DataResult.error(() -> "Unknown ability action type: " + id + " (known: " + TYPES.keySet() + ")");
            },
            Type::id);

    public static final Type EFFECT = register("effect", EffectAction.CODEC);
    public static final Type HEAL = register("heal", HealAction.CODEC);
    public static final Type CLEANSE = register("cleanse", CleanseAction.CODEC);
    public static final Type DAMAGE = register("damage", DamageAction.CODEC);
    public static final Type PUSH = register("push", PushAction.CODEC);
    public static final Type DASH = register("dash", DashAction.CODEC);
    public static final Type BLINK = register("blink", BlinkAction.CODEC);
    public static final Type LIGHTNING = register("lightning", LightningAction.CODEC);
    public static final Type SUMMON = register("summon", SummonAction.CODEC);
    public static final Type ABSORB = register("absorb", AbsorbAction.CODEC);
    public static final Type EXPLODE = register("explode", ExplodeAction.CODEC);
    public static final Type WEATHER = register("weather", WeatherAction.CODEC);
    public static final Type RESOURCE = register("resource", ResourceAction.CODEC);

    private AbilityActionTypes() {}

    public static Type register(Identifier id, MapCodec<? extends AbilityAction> codec) {
        Type type = new Type(id, codec);
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("Duplicate ability action type " + id);
        }
        return type;
    }

    private static Type register(String path, MapCodec<? extends AbilityAction> codec) {
        return register(Pipican.id(path), codec);
    }
}
