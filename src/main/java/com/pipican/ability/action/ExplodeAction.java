package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;

/**
 * {@code pipican:explode} - an explosion centred on the user that never hurts the user or breaks blocks.
 * Reaction upgrades: {@code radius} adds power, {@code strength} multiplies the damage it deals (1 = double) and
 * {@code damage} adds damage to every mob it hurts. With {@code explode_self_share} the user takes that share of the
 * damage it dealt.
 *
 * <pre>{@code { "type": "pipican:explode", "power": 4, "fire": false } }</pre>
 * TNT is power 4.
 */
public record ExplodeAction(float power, boolean fire) implements AbilityAction {

    public static final MapCodec<ExplodeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.floatRange(0.5f, 10).optionalFieldOf("power", 3f).forGetter(ExplodeAction::power),
            Codec.BOOL.optionalFieldOf("fire", false).forGetter(ExplodeAction::fire)
    ).apply(i, ExplodeAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.EXPLODE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var mod = ctx.mod();
        float[] dealt = {0};
        ExplosionDamageCalculator spareUser = new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return entity != player && super.shouldDamageEntity(explosion, entity);
            }

            @Override
            public float getEntityDamageAmount(Explosion explosion, Entity entity, float exposure) {
                float base = super.getEntityDamageAmount(explosion, entity, exposure);
                float amount = base <= 0 ? 0 : (float) (base * Math.max(0, 1 + mod.strength()) + mod.damage() * exposure);
                dealt[0] += amount;
                return amount;
            }
        };
        float total = (float) Math.max(0.5, Math.min(10, power + mod.radius()));
        ctx.level().explode(player, null, spareUser, player.getX(), player.getY(), player.getZ(), total, fire, Level.ExplosionInteraction.NONE);
        double share = com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.EXPLODE_SELF_SHARE);
        if (share > 0 && dealt[0] > 0) {
            player.hurtServer(ctx.level(), ctx.level().damageSources().magic(), (float) (dealt[0] * share));
        }
    }
}
