package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Marks;
import com.pipican.ability.Targets;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code pipican:mark} - marks mobs: the user's damage to them is multiplied while the mark lasts.
 *
 * <pre>{@code { "type": "pipican:mark", "target": "look", "range": 32, "seconds": 60, "multiplier": 1.3 } }</pre>
 * {@code shared}: every player's damage counts. {@code next_hit}: the first hit uses the mark up.
 * {@code projectile_only}: only arrows/bolts. Marked mobs glow while marked (unless {@code glow: false}).
 * {@code max} (0 = no limit): how many mobs this mark may be on at once, plus the user's {@code mark_targets} bonus; when that many
 * are marked, a new mark fizzles (a reaction then stops and keeps its cooldown) - "you can curse 3 mobs at once".
 */
public record MarkAction(Targets targets, double seconds, double multiplier, boolean shared, boolean nextHit, boolean projectileOnly,
                         boolean glow, int max) implements AbilityAction {

    public static final MapCodec<MarkAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.LOOK).forGetter(MarkAction::targets),
            Codec.doubleRange(0.5, 3600).optionalFieldOf("seconds", 30.0).forGetter(MarkAction::seconds),
            Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.3).forGetter(MarkAction::multiplier),
            Codec.BOOL.optionalFieldOf("shared", false).forGetter(MarkAction::shared),
            Codec.BOOL.optionalFieldOf("next_hit", false).forGetter(MarkAction::nextHit),
            Codec.BOOL.optionalFieldOf("projectile_only", false).forGetter(MarkAction::projectileOnly),
            Codec.BOOL.optionalFieldOf("glow", true).forGetter(MarkAction::glow),
            Codec.intRange(0, 64).optionalFieldOf("max", 0).forGetter(MarkAction::max)
    ).apply(i, MarkAction::new));

    /** Player -> (capped mark -> mob -> game time its mark ends). */
    private static final java.util.Map<java.util.UUID, java.util.Map<MarkAction, java.util.Map<LivingEntity, Long>>> CAPPED = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.MARK;
    }

    @Override
    public void run(AbilityContext ctx) {
        long ticks = (long) ((seconds + ctx.mod().seconds()) * 20);
        for (LivingEntity target : targets.select(ctx)) {
            if (target == ctx.player()) {
                continue;
            }
            if (max > 0) {
                long now = ctx.level().getGameTime();
                var live = CAPPED.computeIfAbsent(ctx.player().getUUID(), k -> new java.util.concurrent.ConcurrentHashMap<>())
                        .computeIfAbsent(this, k -> new java.util.concurrent.ConcurrentHashMap<>());
                live.entrySet().removeIf(e -> e.getValue() <= now || !e.getKey().isAlive());
                int cap = max + (int) Math.floor(com.pipican.player.ClassEffectApplier.state(ctx.player())
                        .chance(com.pipican.effect.type.BonusEffect.Kind.MARK_TARGETS) + 1e-9);
                if (!live.containsKey(target) && live.size() >= cap) {
                    ctx.fizzle();
                    return;
                }
                live.put(target, now + ticks);
            }
            // Reaction / ability strength upgrades make the mark's bonus bigger ("retribution bonus +20%").
            double boosted = 1 + (multiplier - 1) * (1 + ctx.mod().strength());
            Marks.add(target, ctx.player(), ticks, boosted, shared, nextHit, projectileOnly);
            if (glow) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int) ticks, 0, false, false), ctx.player());
            }
        }
    }
}
