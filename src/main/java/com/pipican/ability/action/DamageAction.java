package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:damage} - damages the targets. Class damage bonuses apply as usual.
 *
 * <pre>{@code { "type": "pipican:damage", "target": "hostiles", "radius": 2.5, "amount": 6, "kind": "melee",
 *   "execute_below": 20, "fire_seconds": 0 } }</pre>
 * {@code kind}: {@code magic} (default), {@code melee} (counts as a player attack), {@code fire}.
 * {@code execute_below}: targets at or below this much health die instead.
 */
public record DamageAction(Targets targets, double amount, Kind kind, double executeBelow, double fireSeconds) implements AbilityAction {

    public enum Kind implements StringRepresentable {
        MAGIC("magic"), MELEE("melee"), FIRE("fire");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<DamageAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.HOSTILES).forGetter(DamageAction::targets),
            Codec.doubleRange(0, 10_000).fieldOf("amount").forGetter(DamageAction::amount),
            Kind.CODEC.optionalFieldOf("kind", Kind.MAGIC).forGetter(DamageAction::kind),
            Codec.doubleRange(0, 10_000).optionalFieldOf("execute_below", 0.0).forGetter(DamageAction::executeBelow),
            Codec.doubleRange(0, 60).optionalFieldOf("fire_seconds", 0.0).forGetter(DamageAction::fireSeconds)
    ).apply(i, DamageAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DAMAGE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var sources = player.damageSources();
        DamageSource source = switch (kind) {
            case MAGIC -> sources.indirectMagic(player, player);
            case MELEE -> sources.playerAttack(player);
            case FIRE -> sources.onFire();
        };
        for (LivingEntity target : targets.select(ctx)) {
            if (target == player) {
                continue;
            }
            if (executeBelow > 0 && target.getHealth() <= executeBelow) {
                target.hurtServer(ctx.level(), sources.playerAttack(player), Float.MAX_VALUE);
                continue;
            }
            if (fireSeconds > 0) {
                target.igniteForSeconds((float) fireSeconds);
            }
            if (amount > 0) {
                target.hurtServer(ctx.level(), source, (float) amount);
            }
        }
    }
}
