package com.pipican.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.condition.ClassCondition;
import com.pipican.effect.ClassEffect;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * An active ability, loaded from {@code data/<ns>/pipican/ability/<name>.json}. Classes get abilities
 * from their {@code abilities} list and from tree nodes that {@code grant} them; the first four are
 * bound to the ability keys.
 *
 * <pre>{@code
 * {
 *   "name": "Haste", "icon": "minecraft:sugar",
 *   "cost": 25, "cooldown_ticks": 400,
 *   "actions": [ { "type": "pipican:effect", "target": "self",
 *                  "effects": [ { "effect": "minecraft:speed", "seconds": 8, "amplifier": 2 } ] } ]
 * }
 * }</pre>
 *
 * Three shapes:
 * <ul>
 *   <li><b>instant</b> (default): runs {@code actions} once.</li>
 *   <li><b>timed state</b> ({@code duration_ticks} &gt; 0): runs {@code actions}, then {@code effects} count
 *       for the duration; {@code end_actions} run when it ends.</li>
 *   <li><b>toggle</b> ({@code toggle: true}): press to start and again to stop; drains
 *       {@code drain_per_second} of the resource while on and stops when the bar is empty.</li>
 * </ul>
 * States and toggles can also run {@code pulse_actions} every {@code pulse_ticks}; with {@code anchored}
 * the pulses stay where the ability was used instead of following the player.
 *
 * @param cost          resource spent on use (the class's resource bar)
 * @param spendAll      spend the whole bar instead of {@code cost} (actions can scale with what was spent)
 * @param cooldownTicks ticks before it can be used again; for toggles it starts when the toggle ends
 * @param usesPerRest   if &gt; 0, it can only be used this many times between sleeps
 * @param requires      may only be used while this holds
 * @param auto          starts by itself whenever this holds (and it is off cooldown and affordable)
 * @param endWhen       a running state stops early when this holds
 * @param effects       class effects that count while a state/toggle is active
 */
public record Ability(
        Component name,
        Component description,
        Item icon,
        double cost,
        boolean spendAll,
        int cooldownTicks,
        int usesPerRest,
        Timing timing,
        Optional<ClassCondition> requires,
        Optional<ClassCondition> auto,
        Optional<ClassCondition> endWhen,
        List<AbilityAction> actions,
        List<AbilityAction> endActions,
        List<AbilityAction> pulseActions,
        List<ClassEffect> effects) {

    /** How long a state lasts and how it pulses (grouped only because a codec takes at most 16 fields). */
    public record Timing(int durationTicks, boolean toggle, double drainPerSecond, int pulseTicks, boolean anchored) {
        static final MapCodec<Timing> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.intRange(0, 1_728_000).optionalFieldOf("duration_ticks", 0).forGetter(Timing::durationTicks),
                Codec.BOOL.optionalFieldOf("toggle", false).forGetter(Timing::toggle),
                Codec.doubleRange(0, 100_000).optionalFieldOf("drain_per_second", 0.0).forGetter(Timing::drainPerSecond),
                Codec.intRange(0, 72_000).optionalFieldOf("pulse_ticks", 0).forGetter(Timing::pulseTicks),
                Codec.BOOL.optionalFieldOf("anchored", false).forGetter(Timing::anchored)
        ).apply(i, Timing::new));
    }

    public static final Codec<Ability> CODEC = RecordCodecBuilder.<Ability>create(i -> i.group(
            ComponentSerialization.CODEC.fieldOf("name").forGetter(Ability::name),
            ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Ability::description),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("icon", Items.BOOK).forGetter(Ability::icon),
            Codec.doubleRange(0, 100_000).optionalFieldOf("cost", 0.0).forGetter(Ability::cost),
            Codec.BOOL.optionalFieldOf("spend_all", false).forGetter(Ability::spendAll),
            Codec.intRange(0, 1_728_000).optionalFieldOf("cooldown_ticks", 200).forGetter(Ability::cooldownTicks),
            Codec.intRange(0, 100).optionalFieldOf("uses_per_rest", 0).forGetter(Ability::usesPerRest),
            Timing.MAP_CODEC.forGetter(Ability::timing),
            ClassCondition.CODEC.optionalFieldOf("requires").forGetter(Ability::requires),
            ClassCondition.CODEC.optionalFieldOf("auto").forGetter(Ability::auto),
            ClassCondition.CODEC.optionalFieldOf("end_when").forGetter(Ability::endWhen),
            AbilityAction.CODEC.listOf().optionalFieldOf("actions", List.of()).forGetter(Ability::actions),
            AbilityAction.CODEC.listOf().optionalFieldOf("end_actions", List.of()).forGetter(Ability::endActions),
            AbilityAction.CODEC.listOf().optionalFieldOf("pulse_actions", List.of()).forGetter(Ability::pulseActions),
            ClassEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Ability::effects)
    ).apply(i, Ability::new)).validate(Ability::validate);

    private static DataResult<Ability> validate(Ability a) {
        if (a.toggle() && a.durationTicks() > 0) {
            return DataResult.error(() -> "An ability is either a toggle or has a duration, not both");
        }
        if (!a.pulseActions.isEmpty() && (a.timing.pulseTicks() <= 0 || !a.isState())) {
            return DataResult.error(() -> "pulse_actions need pulse_ticks > 0 and a duration or toggle");
        }
        return DataResult.success(a);
    }

    public int durationTicks() {
        return timing.durationTicks();
    }

    public boolean toggle() {
        return timing.toggle();
    }

    public double drainPerSecond() {
        return timing.drainPerSecond();
    }

    /** True for abilities that stay active for a while (timed states and toggles). */
    public boolean isState() {
        return toggle() || durationTicks() > 0;
    }
}
