package com.pipican.mastery.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.effect.VictimState;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillTrigger;
import com.pipican.mastery.SkillTriggerTypes;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

/**
 * {@code pipican:filtered} - another trigger, but only counted in a situation or against some mobs.
 *
 * <pre>{@code { "type": "pipican:filtered", "trigger": { "type": "pipican:kill" },
 *   "victims": "#pipican:mobs/undead", "when": { "type": "pipican:state", "state": "wet" } } }</pre>
 * {@code when}: a condition on the player; {@code victims}: the kind of mob hit or killed; {@code victim}: its state
 * (low health, harmful effect, marked, bleeding...); {@code min_amount}: at least that much damage / spending / healing.
 */
public record FilteredTrigger(SkillTrigger trigger, Optional<ClassCondition> when, Optional<HolderSet<EntityType<?>>> victims,
                              Optional<VictimState> victim, double minAmount) implements SkillTrigger {

    // Lazy: this codec is part of SkillTrigger.CODEC itself.
    public static final MapCodec<FilteredTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.lazyInitialized(() -> SkillTrigger.CODEC).fieldOf("trigger").forGetter(FilteredTrigger::trigger),
            ClassCondition.CODEC.optionalFieldOf("when").forGetter(FilteredTrigger::when),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("victims").forGetter(FilteredTrigger::victims),
            VictimState.CODEC.optionalFieldOf("victim").forGetter(FilteredTrigger::victim),
            Codec.DOUBLE.optionalFieldOf("min_amount", 0.0).forGetter(FilteredTrigger::minAmount)
    ).apply(i, FilteredTrigger::new));

    @Override
    public SkillTriggerTypes.Type type() {
        return SkillTriggerTypes.FILTERED;
    }

    @Override
    public double uses(SkillAction action) {
        return trigger.uses(action);
    }

    @Override
    public double uses(ServerPlayer player, SkillAction action) {
        double uses = trigger.uses(player, action);
        if (uses <= 0 || action.amount() < minAmount) {
            return 0;
        }
        if (victims.isPresent() && action.target().map(t -> !victims.get().contains(t.getType().builtInRegistryHolder())).orElse(true)) {
            return 0;
        }
        if (victim.isPresent() && action.target().map(t -> !victim.get().test(player, t)).orElse(true)) {
            return 0;
        }
        return when.isEmpty() || when.get().test(player) ? uses : 0;
    }
}
