package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Marks;
import com.pipican.ability.Targets;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:mark} - marks mobs: the user's damage to them is multiplied while the mark lasts.
 *
 * <pre>{@code { "type": "pipican:mark", "target": "look", "range": 32, "seconds": 60, "multiplier": 1.3 } }</pre>
 * {@code shared}: every player's damage counts. {@code next_hit}: the first hit uses the mark up.
 * {@code projectile_only}: only arrows/bolts. Marked mobs glow while marked (unless {@code glow: false}).
 */
public record MarkAction(Targets targets, double seconds, double multiplier, boolean shared, boolean nextHit, boolean projectileOnly,
                         boolean glow) implements AbilityAction {

    public static final MapCodec<MarkAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK).forGetter(MarkAction::targets),
            Codec.doubleRange(0.5, 3600).optionalFieldOf("seconds", 30.0).forGetter(MarkAction::seconds),
            Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.3).forGetter(MarkAction::multiplier),
            Codec.BOOL.optionalFieldOf("shared", false).forGetter(MarkAction::shared),
            Codec.BOOL.optionalFieldOf("next_hit", false).forGetter(MarkAction::nextHit),
            Codec.BOOL.optionalFieldOf("projectile_only", false).forGetter(MarkAction::projectileOnly),
            Codec.BOOL.optionalFieldOf("glow", true).forGetter(MarkAction::glow)
    ).apply(i, MarkAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.MARK;
    }

    @Override
    public void run(AbilityContext ctx) {
        long ticks = (long) (seconds * 20);
        for (LivingEntity target : targets.select(ctx)) {
            if (target == ctx.player()) {
                continue;
            }
            Marks.add(target, ctx.player(), ticks, multiplier, shared, nextHit, projectileOnly);
            if (glow) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) ticks, 0, false, false), ctx.player());
            }
        }
    }
}
