package com.pipican.ability;

import com.pipican.ability.action.StopAction;
import com.pipican.ability.action.DotAction;
import com.pipican.ability.action.RandomAction;
import com.pipican.ability.action.CheatDeathAction;
import com.pipican.ability.action.ShrugAction;
import com.pipican.ability.action.BoostAction;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.pipican.Pipican;
import com.pipican.ability.action.AbsorbAction;
import com.pipican.ability.action.BlinkAction;
import com.pipican.ability.action.BondAction;
import com.pipican.ability.action.CastAction;
import com.pipican.ability.action.CleanseAction;
import com.pipican.ability.action.ControlAction;
import com.pipican.ability.action.DamageAction;
import com.pipican.ability.action.DashAction;
import com.pipican.ability.action.DecoyAction;
import com.pipican.ability.action.EffectAction;
import com.pipican.ability.action.EidolonAction;
import com.pipican.ability.action.EmpowerAction;
import com.pipican.ability.action.ExplodeAction;
import com.pipican.ability.action.GiveAction;
import com.pipican.ability.action.HealAction;
import com.pipican.ability.action.ImbueAction;
import com.pipican.ability.action.LightningAction;
import com.pipican.ability.action.LocateAction;
import com.pipican.ability.action.MarkAction;
import com.pipican.ability.action.MountAction;
import com.pipican.ability.action.NegateHitAction;
import com.pipican.ability.action.OptimizeAction;
import com.pipican.ability.action.PushAction;
import com.pipican.ability.action.RecallAction;
import com.pipican.ability.action.RecallItemAction;
import com.pipican.ability.action.ResourceAction;
import com.pipican.ability.action.ScryAction;
import com.pipican.ability.action.StealAction;
import com.pipican.ability.action.SummonAction;
import com.pipican.ability.action.TeleportToAction;
import com.pipican.ability.action.TransferEffectsAction;
import com.pipican.ability.action.TransmuteAction;
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
    public static final Type MARK = register("mark", MarkAction.CODEC);
    public static final Type BOOST = register("boost", BoostAction.CODEC);
    public static final Type CHEAT_DEATH = register("cheat_death", CheatDeathAction.CODEC);
    public static final Type RANDOM = register("random", RandomAction.CODEC);
    public static final Type DOT = register("dot", DotAction.CODEC);
    public static final Type STOP = register("stop", StopAction.CODEC);
    public static final Type SHRUG = register("shrug", ShrugAction.CODEC);
    public static final Type EMPOWER = register("empower", EmpowerAction.CODEC);
    public static final Type IMBUE = register("imbue", ImbueAction.CODEC);
    public static final Type CONTROL = register("control", ControlAction.CODEC);
    public static final Type DECOY = register("decoy", DecoyAction.CODEC);
    public static final Type NEGATE_HIT = register("negate_hit", NegateHitAction.CODEC);
    public static final Type TELEPORT_TO = register("teleport_to", TeleportToAction.CODEC);
    public static final Type RECALL = register("recall", RecallAction.CODEC);
    public static final Type LOCATE = register("locate", LocateAction.CODEC);
    public static final Type SCRY = register("scry", ScryAction.CODEC);
    public static final Type GIVE = register("give", GiveAction.CODEC);
    public static final Type STEAL = register("steal", StealAction.CODEC);
    public static final Type TRANSMUTE = register("transmute", TransmuteAction.CODEC);
    public static final Type TRANSFER_EFFECTS = register("transfer_effects", TransferEffectsAction.CODEC);
    public static final Type CAST = register("cast", CastAction.CODEC);
    public static final Type EIDOLON = register("eidolon", EidolonAction.CODEC);
    public static final Type MOUNT = register("mount", MountAction.CODEC);
    public static final Type BOND = register("bond", BondAction.CODEC);
    public static final Type OPTIMIZE = register("optimize", OptimizeAction.CODEC);
    public static final Type RECALL_ITEM = register("recall_item", RecallItemAction.CODEC);

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
