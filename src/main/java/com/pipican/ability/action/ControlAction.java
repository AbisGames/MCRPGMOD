package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.MobControl;
import com.pipican.ability.Targets;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * {@code pipican:control} - takes temporary control of mobs (never players).
 *
 * <pre>{@code { "type": "pipican:control", "target": "hostiles", "radius": 8, "entities": "#minecraft:undead",
 *   "mode": "flee", "seconds": 10 } }</pre>
 * Modes: {@code flee} (runs away from the user), {@code pacify} (won't attack the user),
 * {@code ally} (fights hostile mobs for the user). With {@code max} above 0, an {@code ally} control only takes a mob
 * while the user has fewer allies than that (raised by {@code pipican:summon_mod} {@code cap} / {@code cap_multiplier});
 * such servants also get the user's summon damage and health upgrades while they serve.
 */
public record ControlAction(Targets targets, Mode mode, double seconds, int max) implements AbilityAction {

    public enum Mode implements StringRepresentable {
        FLEE("flee", MobControl.Mode.FLEE), PACIFY("pacify", MobControl.Mode.PACIFY), ALLY("ally", MobControl.Mode.ALLY);

        public static final Codec<Mode> CODEC = StringRepresentable.fromEnum(Mode::values);
        private final String name;
        final MobControl.Mode mode;

        Mode(String name, MobControl.Mode mode) {
            this.name = name;
            this.mode = mode;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<ControlAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK).forGetter(ControlAction::targets),
            Mode.CODEC.fieldOf("mode").forGetter(ControlAction::mode),
            Codec.doubleRange(0.5, 600).optionalFieldOf("seconds", 10.0).forGetter(ControlAction::seconds),
            Codec.intRange(0, 100).optionalFieldOf("max", 0).forGetter(ControlAction::max)
    ).apply(i, ControlAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.CONTROL;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (target instanceof Mob mob) {
                if (max > 0 && mode == Mode.ALLY) {
                    var mods = com.pipican.player.ClassEffectApplier.state(ctx.player()).summonMods();
                    if (MobControl.isAllyOf(mob, ctx.player()) || MobControl.allies(ctx.player()) >= Math.floor((max + mods.cap()) * (1 + mods.capMultiplier()))) {
                        continue;
                    }
                    MobControl.boostServant(mob, mods.damage(), mods.health());
                }
                MobControl.control(mob, ctx.player(), mode.mode, (long) (Math.max(0.5, seconds + ctx.mod().seconds()) * 20));
                if (mode != Mode.FLEE) {
                    com.pipican.event.MasteryEvents.did(ctx.player(), new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.CHARM,
                            net.minecraft.world.item.ItemStack.EMPTY, java.util.Optional.empty(), java.util.Optional.of(mob), 0));
                }
            }
        }
    }
}
