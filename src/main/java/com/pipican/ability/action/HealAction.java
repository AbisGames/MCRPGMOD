package com.pipican.ability.action;

import com.pipican.effect.type.BonusEffect;
import com.pipican.player.ClassEffectApplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Targets;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:heal} - heals the targets by {@code amount} HP plus {@code percent} of their max health.
 *
 * With {@code require_target}, finding no one to heal fizzles (a reaction's cooldown isn't used).
 * <pre>{@code { "type": "pipican:heal", "target": "allies", "radius": 8, "amount": 6 } }</pre>
 */
public record HealAction(Targets targets, double amount, double percent, boolean requireTarget) implements AbilityAction {

    public static final MapCodec<HealAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(HealAction::targets),
            Codec.doubleRange(0, 10_000).optionalFieldOf("amount", 0.0).forGetter(HealAction::amount),
            Codec.doubleRange(0, 1).optionalFieldOf("percent", 0.0).forGetter(HealAction::percent),
            Codec.BOOL.optionalFieldOf("require_target", false).forGetter(HealAction::requireTarget)
    ).apply(i, HealAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.HEAL;
    }

    @Override
    public void run(AbilityContext ctx) {
        var selected = targets.select(ctx);
        if (requireTarget && selected.isEmpty()) {
            // "Heals a downed player": with no one there, nothing happens and a reaction keeps its cooldown.
            ctx.fizzle();
            return;
        }
        for (LivingEntity target : selected) {
            double given = ClassEffectApplier.state(ctx.player()).bonus(BonusEffect.Kind.HEALING_GIVEN);
            if (target != ctx.player() && target instanceof net.minecraft.world.entity.player.Player && target.getHealth() < target.getMaxHealth() * 0.3) {
                given *= 1 + ClassEffectApplier.state(ctx.player()).chance(BonusEffect.Kind.TRIAGE);
            }
            float healed = (float) ((amount + ctx.mod().heal() + percent * target.getMaxHealth()) * given);
            target.heal(healed);
            if (target != ctx.player() && target instanceof net.minecraft.world.entity.player.Player) {
                com.pipican.event.MasteryEvents.did(ctx.player(), new com.pipican.mastery.SkillAction.Did(com.pipican.mastery.SkillAction.Did.Kind.HEAL_OTHER,
                        net.minecraft.world.item.ItemStack.EMPTY, java.util.Optional.empty(), java.util.Optional.of(target), healed));
            }
        }
    }
}
