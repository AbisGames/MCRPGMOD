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
        ExplosionDamageCalculator spareUser = new ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
                return entity != player && super.shouldDamageEntity(explosion, entity);
            }
        };
        ctx.level().explode(player, null, spareUser, player.getX(), player.getY(), player.getZ(), power, fire, Level.ExplosionInteraction.NONE);
    }
}
