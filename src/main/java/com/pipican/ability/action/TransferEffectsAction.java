package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.ArrayList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:transfer_effects} - moves the user's harmful potion effects onto the targets (Return to
 * Sender). Cancelled (and refunded) if the user has none.
 *
 * <pre>{@code { "type": "pipican:transfer_effects", "target": "look", "range": 16 } }</pre>
 */
public record TransferEffectsAction(Targets targets) implements AbilityAction {

    public static final MapCodec<TransferEffectsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK).forGetter(TransferEffectsAction::targets)
    ).apply(i, TransferEffectsAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.TRANSFER_EFFECTS;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var harmful = new ArrayList<MobEffectInstance>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !e.isInfiniteDuration()) {
                harmful.add(e);
            }
        }
        if (harmful.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.no_effects"));
            return;
        }
        var targetsFound = targets.select(ctx);
        if (ctx.isCancelled()) {
            return;
        }
        for (MobEffectInstance e : harmful) {
            player.removeEffect(e.getEffect());
            for (LivingEntity target : targetsFound) {
                target.addEffect(new MobEffectInstance(e), player);
            }
        }
    }
}
