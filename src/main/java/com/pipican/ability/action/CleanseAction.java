package com.pipican.ability.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:cleanse} - removes harmful potion effects from the targets (or only {@code effects}, if given).
 *
 * <pre>{@code { "type": "pipican:cleanse", "target": "allies", "radius": 8 } }</pre>
 */
public record CleanseAction(Targets targets, Optional<HolderSet<MobEffect>> effects) implements AbilityAction {

    public static final MapCodec<CleanseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(CleanseAction::targets),
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).optionalFieldOf("effects").forGetter(CleanseAction::effects)
    ).apply(i, CleanseAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CLEANSE;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx.player())) {
            for (Holder<MobEffect> effect : new ArrayList<>(target.getActiveEffectsMap().keySet())) {
                boolean match = effects.map(set -> set.contains(effect)).orElse(effect.value().getCategory() == MobEffectCategory.HARMFUL);
                // The class system's own permanent effects (weaknesses) are not removable.
                var instance = target.getEffect(effect);
                if (match && instance != null && !instance.isInfiniteDuration()) {
                    target.removeEffect(effect);
                }
            }
        }
    }
}
