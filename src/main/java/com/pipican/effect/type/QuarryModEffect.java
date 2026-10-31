package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.effect.Scaling;
import com.pipican.player.ClassState;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:quarry_mod} - upgrades the {@code pipican:quarry} called {@code name}: it lasts {@code duration} longer
 * (0.5 = +50%) or {@code forever} (until it dies), deals {@code multiplier} more damage taken from you (0.1 = +10 points),
 * {@code untouched} more while it hasn't hit you yet, gets {@code effects}; {@code melee} also picks it with melee hits,
 * {@code end_on_other_hit} ends it when you hit another mob. {@code scaling} applies to the amounts the mod sets.
 *
 * <pre>{@code { "type": "pipican:quarry_mod", "name": "prey", "melee": true, "multiplier": 0.1, "end_on_other_hit": true } }</pre>
 */
public record QuarryModEffect(String name, Optional<Double> duration, Optional<Double> multiplier, Optional<Double> untouched, boolean forever,
                              boolean melee, boolean endOnOtherHit, Map<Holder<MobEffect>, Integer> effects, List<Scaling> scaling) implements ClassEffect {

    public static final MapCodec<QuarryModEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(QuarryModEffect::name),
            Codec.DOUBLE.optionalFieldOf("duration").forGetter(QuarryModEffect::duration),
            Codec.DOUBLE.optionalFieldOf("multiplier").forGetter(QuarryModEffect::multiplier),
            Codec.DOUBLE.optionalFieldOf("untouched").forGetter(QuarryModEffect::untouched),
            Codec.BOOL.optionalFieldOf("forever", false).forGetter(QuarryModEffect::forever),
            Codec.BOOL.optionalFieldOf("melee", false).forGetter(QuarryModEffect::melee),
            Codec.BOOL.optionalFieldOf("end_on_other_hit", false).forGetter(QuarryModEffect::endOnOtherHit),
            Codec.unboundedMap(BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), Codec.intRange(0, 9)).optionalFieldOf("effects", Map.of()).forGetter(QuarryModEffect::effects),
            Scaling.LIST_CODEC.optionalFieldOf("scaling", List.of()).forGetter(QuarryModEffect::scaling)
    ).apply(i, QuarryModEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.QUARRY_MOD;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addQuarryMod(name.toLowerCase(java.util.Locale.ROOT), new ClassState.QuarryMod(Scaling.evaluate(duration, scaling, ctx),
                Scaling.evaluate(multiplier, scaling, ctx), Scaling.evaluate(untouched, scaling, ctx), forever, melee, endOnOtherHit, effects));
    }
}
