package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.Ability;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.AbilityService;
import com.pipican.core.PipicanRegistries;
import java.util.List;
import net.minecraft.resources.ResourceKey;

/** {@code pipican:stop} - ends these running abilities (one form at a time: each form stops the others). */
public record StopAction(List<ResourceKey<Ability>> abilities) implements AbilityAction {

    public static final MapCodec<StopAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceKey.codec(PipicanRegistries.ABILITY).listOf().fieldOf("abilities").forGetter(StopAction::abilities)
    ).apply(i, StopAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.STOP;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (ResourceKey<Ability> key : abilities) {
            if (ctx.ability().map(k -> !k.equals(key)).orElse(true)) {
                AbilityService.stopIfActive(ctx.player(), key);
            }
        }
    }
}
