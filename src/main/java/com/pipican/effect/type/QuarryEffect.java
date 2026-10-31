package com.pipican.effect.type;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;
import com.pipican.player.ClassState;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.effect.MobEffect;

/**
 * {@code pipican:quarry} - one chosen mob at a time (a vow, prey, a challenged foe), called {@code name}.
 * {@code pick} says how it is chosen: {@code hurt_by} (the next mob to hurt you or a player within 16 blocks),
 * {@code arrow_hit} (the last mob you hit with a projectile), {@code first_hit} (the first mob you hit).
 * It lasts {@code seconds}; {@code replace} lets a new pick take over (otherwise one at a time).
 * While it lasts it counts as marked by you, glows for you ({@code glow}), takes {@code multiplier} times your
 * damage, deals {@code others_damage} less damage to anyone but you (0.2 = 20% less) and has {@code effects}.
 *
 * <pre>{@code { "type": "pipican:quarry", "name": "prey", "pick": "arrow_hit", "seconds": 60, "replace": true, "multiplier": 1.15 } }</pre>
 */
public record QuarryEffect(String name, Pick pick, double seconds, boolean replace, boolean glow, double multiplier, double othersDamage,
                           Map<Holder<MobEffect>, Integer> effects) implements ClassEffect {

    public enum Pick implements StringRepresentable {
        HURT_BY("hurt_by"), ARROW_HIT("arrow_hit"), FIRST_HIT("first_hit");

        public static final Codec<Pick> CODEC = StringRepresentable.fromEnum(Pick::values);
        private final String name;

        Pick(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<QuarryEffect> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(QuarryEffect::name),
            Pick.CODEC.fieldOf("pick").forGetter(QuarryEffect::pick),
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 30.0).forGetter(QuarryEffect::seconds),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(QuarryEffect::replace),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(QuarryEffect::glow),
            Codec.doubleRange(0, 10).optionalFieldOf("multiplier", 1.0).forGetter(QuarryEffect::multiplier),
            Codec.doubleRange(0, 1).optionalFieldOf("others_damage", 0.0).forGetter(QuarryEffect::othersDamage),
            Codec.unboundedMap(BuiltInRegistries.MOB_EFFECT.holderByNameCodec(), Codec.intRange(0, 9)).optionalFieldOf("effects", Map.of()).forGetter(QuarryEffect::effects)
    ).apply(i, QuarryEffect::new));

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.QUARRY;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.addQuarry(name.toLowerCase(java.util.Locale.ROOT), new ClassState.Quarry(pick, seconds, replace, glow, multiplier, othersDamage,
                effects, false, false, false, 0));
    }
}
