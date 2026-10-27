package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Empowers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * {@code pipican:imbue} - uses up the potion in the off hand: its effects are applied to whatever the
 * user hits with their next {@code hits} hits within {@code seconds} (Store Spell). Cancelled (and refunded)
 * without a potion in the off hand.
 *
 * <pre>{@code { "type": "pipican:imbue", "hits": 5, "seconds": 60 } }</pre>
 */
public record ImbueAction(int hits, double seconds) implements AbilityAction {

    public static final MapCodec<ImbueAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 100).optionalFieldOf("hits", 5).forGetter(ImbueAction::hits),
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 60.0).forGetter(ImbueAction::seconds)
    ).apply(i, ImbueAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.IMBUE;
    }

    @Override
    public void run(AbilityContext ctx) {
        var stack = ctx.player().getItemInHand(InteractionHand.OFF_HAND);
        var contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            ctx.cancel(Component.translatable("pipican.ability.needs_potion"));
            return;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        contents.getAllEffects().forEach(effects::add);
        if (effects.isEmpty()) {
            ctx.cancel(Component.translatable("pipican.ability.needs_potion"));
            return;
        }
        stack.consume(1, ctx.player());
        Empowers.add(ctx.player(), (long) (seconds * 20), 1, 0, effects, Optional.empty(), hits);
    }
}
