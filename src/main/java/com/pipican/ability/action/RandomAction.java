package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.List;

/**
 * {@code pipican:random} - runs one of the {@code choices} (each a list of actions), picked at random
 * ("curse a mob with one of: Desire, Lethargy or Pain").
 */
public record RandomAction(List<List<AbilityAction>> choices) implements AbilityAction {

    // Lazy: actions contain actions.
    public static final MapCodec<RandomAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.lazyInitialized(() -> AbilityAction.CODEC).listOf().listOf().fieldOf("choices").forGetter(RandomAction::choices)
    ).apply(i, RandomAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RANDOM;
    }

    private static boolean bad(List<AbilityAction> choice) {
        return choice.stream().anyMatch(a -> a instanceof ExplodeAction || a instanceof HurtSelfAction
                || a instanceof DamageAction d && d.targets().target() == com.pipican.ability.Targets.Kind.SELF);
    }

    @Override
    public void run(AbilityContext ctx) {
        if (choices.isEmpty()) {
            return;
        }
        var random = ctx.player().getRandom();
        List<AbilityAction> pick = choices.get(random.nextInt(choices.size()));
        // chaos_luck: a bad outcome (one that hurts the user) may be rolled again.
        double luck = com.pipican.player.ClassEffectApplier.state(ctx.player()).chance(com.pipican.effect.type.BonusEffect.Kind.CHAOS_LUCK);
        if (luck > 0 && bad(pick) && random.nextDouble() < luck) {
            pick = choices.get(random.nextInt(choices.size()));
        }
        for (AbilityAction action : pick) {
            action.run(ctx);
            if (ctx.isCancelled()) {
                return;
            }
        }
        // Pandemonium: the outcome also hits the nearest other mob.
        if (ctx.subject().isPresent() && com.pipican.player.ClassEffectApplier.state(ctx.player()).chance(com.pipican.effect.type.BonusEffect.Kind.CHAOS_DOUBLE) > 0) {
            var subject = ctx.subject().get();
            var player = ctx.player();
            var mod = ctx.mod();
            var finalPick = pick;
            player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, subject.getBoundingBox().inflate(4),
                            e -> e != subject && e != player && e.isAlive() && !(e instanceof net.minecraft.world.entity.player.Player))
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(subject)))
                    .ifPresent(other -> {
                        AbilityContext second = new AbilityContext(player, java.util.Optional.empty(), other.position(), java.util.Optional.of(other), 0).withMod(mod);
                        for (AbilityAction action : finalPick) {
                            // Only what lands on the mob is repeated (not heals or blasts on yourself).
                            boolean onVictim = action instanceof DamageAction d && d.targets().target() == com.pipican.ability.Targets.Kind.VICTIM
                                    || action instanceof EffectAction e && e.targets().target() == com.pipican.ability.Targets.Kind.VICTIM;
                            if (!onVictim) {
                                continue;
                            }
                            action.run(second);
                            if (second.isCancelled()) {
                                return;
                            }
                        }
                    });
        }
    }
}
