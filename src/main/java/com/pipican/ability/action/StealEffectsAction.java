package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.ArrayList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:steal_effects} - takes the targets' beneficial potion effects for the user (Spell Thief Mastery).
 *
 * <pre>{@code { "type": "pipican:steal_effects", "target": "victim" } }</pre>
 */
public record StealEffectsAction(Targets targets) implements AbilityAction {

    public static final MapCodec<StealEffectsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.VICTIM).forGetter(StealEffectsAction::targets)
    ).apply(i, StealEffectsAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.STEAL_EFFECTS;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (target == ctx.player()) {
                continue;
            }
            var stolen = new ArrayList<MobEffectInstance>();
            for (MobEffectInstance e : target.getActiveEffects()) {
                if (e.getEffect().value().isBeneficial() && !e.isInfiniteDuration()) {
                    stolen.add(e);
                }
            }
            for (MobEffectInstance e : stolen) {
                target.removeEffect(e.getEffect());
                ctx.player().addEffect(new MobEffectInstance(e), target);
            }
        }
    }
}
