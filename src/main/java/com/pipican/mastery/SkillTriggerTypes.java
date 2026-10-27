package com.pipican.mastery;

import com.pipican.mastery.trigger.AnyTrigger;
import com.pipican.mastery.trigger.FilteredTrigger;
import com.pipican.mastery.trigger.DidTrigger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import com.pipican.mastery.trigger.AbilityTrigger;
import com.pipican.mastery.trigger.HurtWearingTrigger;
import com.pipican.mastery.trigger.ItemActionTrigger;
import com.pipican.mastery.trigger.JumpTrigger;
import com.pipican.mastery.trigger.MoveTrigger;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Registry of skill trigger types usable in skill JSON via {@code "type": "<id>"}. Mirrors ClassEffectTypes. */
public final class SkillTriggerTypes {

    public record Type(Identifier id, MapCodec<? extends SkillTrigger> codec) {}

    private static final Map<Identifier, Type> TYPES = new LinkedHashMap<>();

    static final Codec<Type> TYPE_CODEC = Identifier.CODEC.comapFlatMap(
            id -> {
                Type type = TYPES.get(id);
                return type != null ? DataResult.success(type) : DataResult.error(() -> "Unknown skill trigger type: " + id + " (known: " + TYPES.keySet() + ")");
            },
            Type::id);

    public static final Type MELEE_HIT = register("melee_hit", ItemActionTrigger.codec(ItemActionTrigger.Kind.MELEE_HIT));
    public static final Type RANGED_HIT = register("ranged_hit", ItemActionTrigger.codec(ItemActionTrigger.Kind.RANGED_HIT));
    public static final Type KILL = register("kill", ItemActionTrigger.codec(ItemActionTrigger.Kind.KILL));
    public static final Type BLOCK = register("block", ItemActionTrigger.codec(ItemActionTrigger.Kind.BLOCK));
    public static final Type HURT_WEARING = register("hurt_wearing", HurtWearingTrigger.CODEC);
    public static final Type MOVE = register("move", MoveTrigger.CODEC);
    public static final Type JUMP = register("jump", JumpTrigger.CODEC);
    public static final Type ABILITY = register("ability", AbilityTrigger.CODEC);
    public static final Type DID = register("did", DidTrigger.CODEC);
    public static final Type FILTERED = register("filtered", FilteredTrigger.CODEC);
    public static final Type ANY = register("any", AnyTrigger.CODEC);

    private SkillTriggerTypes() {}

    public static Type register(Identifier id, MapCodec<? extends SkillTrigger> codec) {
        Type type = new Type(id, codec);
        if (TYPES.putIfAbsent(id, type) != null) {
            throw new IllegalStateException("Duplicate skill trigger type " + id);
        }
        return type;
    }

    private static Type register(String path, MapCodec<? extends SkillTrigger> codec) {
        return register(Pipican.id(path), codec);
    }
}
