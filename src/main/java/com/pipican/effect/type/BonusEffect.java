package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * {@code pipican:bonus} - a percentage change to something that isn't an attribute.
 *
 * <pre>{@code { "type": "pipican:bonus", "kind": "experience", "amount": 0.1 } }</pre>
 * {@code amount} 0.1 = +10%, -0.2 = -20%. Amounts of the same kind add up. Kinds:
 * <ul>
 *   <li>{@code experience}: XP you gain</li>
 *   <li>{@code potion_duration} / {@code harmful_duration} / {@code beneficial_duration}: how long effects last on you
 *       (all / harmful / beneficial ones; endless effects are left alone)</li>
 *   <li>{@code applied_duration} / {@code applied_harmful_duration}: how long effects (all / harmful ones) you put on
 *       others last (tipped arrows, thrown potions, abilities...)</li>
 *   <li>{@code hunger}: how fast you get hungry (-0.15 = 15% slower)</li>
 *   <li>{@code trade_price}: villager and wandering trader prices (-0.05 = 5% cheaper)</li>
 *   <li>{@code draw_speed} / {@code reload_speed}: bow and trident draw / crossbow loading speed</li>
 *   <li>{@code resource_gain}: how much the resource bar fills from {@code pipican:resource} actions (hits, kills...)</li>
 *   <li>{@code ability_cost}: the resource cost of abilities (-0.1 = 10% cheaper)</li>
 *   <li>{@code natural_regen}: how fast health comes back from a full food bar</li>
 * </ul>
 */
public record BonusEffect(Kind kind, double amount, List<Scaling> scaling) implements ClassEffect {

    public enum Kind implements StringRepresentable {
        EXPERIENCE, POTION_DURATION, HARMFUL_DURATION, BENEFICIAL_DURATION, APPLIED_DURATION, APPLIED_HARMFUL_DURATION, HUNGER, TRADE_PRICE, DRAW_SPEED, RELOAD_SPEED, RESOURCE_GAIN, ABILITY_COST, NATURAL_REGEN;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final MapCodec<BonusEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Kind.CODEC.fieldOf("kind").forGetter(BonusEffect::kind),
            Codec.DOUBLE.fieldOf("amount").forGetter(BonusEffect::amount),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(BonusEffect::scaling)
    ).apply(i, BonusEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.BONUS;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addBonus(kind, Scaling.evaluate(amount, scaling, ctx) * ctx.rank());
    }
}
