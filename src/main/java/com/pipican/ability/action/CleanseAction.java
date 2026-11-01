package com.pipican.ability.action;

import com.mojang.serialization.Codec;
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
 * {@code pipican:cleanse} - removes harmful potion effects from the targets (or only {@code effects}, if given). With
 * {@code last} only the harmful effect that reached the target most recently goes ("cancel the last harmful effect on you").
 *
 * <pre>{@code { "type": "pipican:cleanse", "target": "allies", "radius": 8 } }</pre>
 */
public record CleanseAction(Targets targets, Optional<HolderSet<MobEffect>> effects, boolean last) implements AbilityAction {

    public static final MapCodec<CleanseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(CleanseAction::targets),
            RegistryCodecs.homogeneousList(Registries.MOB_EFFECT).optionalFieldOf("effects").forGetter(CleanseAction::effects),
            Codec.BOOL.optionalFieldOf("last", false).forGetter(CleanseAction::last)
    ).apply(i, CleanseAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CLEANSE;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (last) {
                var recent = RECENT.get(target.getUUID());
                while (recent != null && !recent.isEmpty()) {
                    Holder<MobEffect> effect = recent.pollLast();
                    var instance = target.getEffect(effect);
                    if (instance != null && !instance.isInfiniteDuration()) {
                        target.removeEffect(effect);
                        break;
                    }
                }
                continue;
            }
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

    /** Harmful effects as they reach each player, newest last (for {@code last}). Not saved. */
    private static final java.util.Map<java.util.UUID, java.util.ArrayDeque<Holder<MobEffect>>> RECENT = new java.util.concurrent.ConcurrentHashMap<>();

    @net.neoforged.fml.common.EventBusSubscriber(modid = com.pipican.Pipican.MOD_ID)
    public static final class Recent {
        private Recent() {}

        @net.neoforged.bus.api.SubscribeEvent
        static void onAdded(net.neoforged.neoforge.event.entity.living.MobEffectEvent.Added event) {
            var effect = event.getEffectInstance().getEffect();
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                var recent = RECENT.computeIfAbsent(player.getUUID(), k -> new java.util.ArrayDeque<>());
                recent.remove(effect);
                recent.addLast(effect);
                while (recent.size() > 16) {
                    recent.pollFirst();
                }
            }
        }
    }
}
