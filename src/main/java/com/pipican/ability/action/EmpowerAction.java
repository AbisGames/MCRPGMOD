package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Empowers;
import com.pipican.ability.Targets;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * {@code pipican:empower} - the targets' next hit(s) deal more: damage x {@code multiplier} + {@code bonus}
 * (+ {@code bonus_per_spent} for every point of resource this use spent), and can apply potion effects.
 *
 * <pre>{@code { "type": "pipican:empower", "target": "self", "multiplier": 3, "hits": 1, "seconds": 10,
 *   "against": "#minecraft:undead" } }</pre>
 * Only players can be empowered. {@code against} limits it to some kinds of mob. With {@code charges} above 0 these
 * are stored charges: at most that many (plus {@code empower_charges} bonuses) wait at once, extra ones are lost.
 */
public record EmpowerAction(Targets targets, double multiplier, double bonus, double bonusPerSpent, int hits, double seconds,
                            Optional<HolderSet<EntityType<?>>> against, List<EffectAction.Entry> effects, int charges) implements AbilityAction {

    public static final MapCodec<EmpowerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.SELF).forGetter(EmpowerAction::targets),
            Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.0).forGetter(EmpowerAction::multiplier),
            Codec.doubleRange(0, 1000).optionalFieldOf("bonus", 0.0).forGetter(EmpowerAction::bonus),
            Codec.doubleRange(0, 100).optionalFieldOf("bonus_per_spent", 0.0).forGetter(EmpowerAction::bonusPerSpent),
            Codec.intRange(1, 100).optionalFieldOf("hits", 1).forGetter(EmpowerAction::hits),
            Codec.doubleRange(0.5, 3600).optionalFieldOf("seconds", 10.0).forGetter(EmpowerAction::seconds),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("against").forGetter(EmpowerAction::against),
            EffectAction.Entry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(EmpowerAction::effects),
            Codec.intRange(0, 100).optionalFieldOf("charges", 0).forGetter(EmpowerAction::charges)
    ).apply(i, EmpowerAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.EMPOWER;
    }

    @Override
    public void run(AbilityContext ctx) {
        for (LivingEntity target : targets.select(ctx)) {
            if (target instanceof Player p) {
                double mult = 1 + (multiplier - 1) * (1 + ctx.mod().strength());
                double flat = bonus + bonusPerSpent * ctx.spent();
                if (charges > 0 && p instanceof net.minecraft.server.level.ServerPlayer sp) {
                    if (sp.level().dimension() == net.minecraft.world.level.Level.NETHER
                            && com.pipican.player.ClassEffectApplier.state(sp).chance(com.pipican.effect.type.BonusEffect.Kind.CHARGES_FIZZLE_NETHER) > 0) {
                        continue;
                    }
                    int max = charges + (int) Math.floor(com.pipican.player.ClassEffectApplier.state(sp).chance(com.pipican.effect.type.BonusEffect.Kind.EMPOWER_CHARGES) + 1e-9);
                    if (Empowers.count(p, mult, flat) >= max) {
                        continue;
                    }
                }
                Empowers.add(p, (long) ((seconds + ctx.mod().seconds()) * 20), mult,
                        flat, Empowers.instances(effects), against, Math.max(1, hits + ctx.mod().hits()));
            }
        }
    }
}
