package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Boosts;
import com.pipican.effect.type.BonusEffect;
import com.pipican.effect.type.StatEffect;
import com.pipican.player.ClassEffectApplier;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * {@code pipican:prodigy} - one of {@code stats} gets +{@code amount} points for {@code seconds} (Prodigy, on waking): a
 * random one, or with {@code prodigy_recent} the stat the player used most recently (melee hits: strength, shots:
 * dexterity, sprinting: agility, magic: intelligence, healing: wisdom, getting hurt: vitality, mining: endurance,
 * fishing and loot: luck). Reaction upgrades ({@code strength}) add to the amount.
 *
 * <pre>{@code { "type": "pipican:prodigy", "amount": 2, "seconds": 1200, "stats": [ "pipican:strength", "pipican:luck" ] } }</pre>
 */
public record ProdigyAction(int amount, double seconds, List<Identifier> stats) implements AbilityAction {

    public static final MapCodec<ProdigyAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 100).optionalFieldOf("amount", 2).forGetter(ProdigyAction::amount),
            Codec.doubleRange(1, 86_400).optionalFieldOf("seconds", 1200.0).forGetter(ProdigyAction::seconds),
            Identifier.CODEC.listOf().fieldOf("stats").forGetter(ProdigyAction::stats)
    ).apply(i, ProdigyAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.PRODIGY;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        if (stats.isEmpty()) {
            return;
        }
        Identifier stat = stats.get(player.getRandom().nextInt(stats.size()));
        if (ClassEffectApplier.state(player).chance(BonusEffect.Kind.PRODIGY_RECENT) > 0) {
            var recent = com.pipican.event.MechanicEvents.lastStat(player).map(Identifier::parse).filter(stats::contains);
            if (recent.isPresent()) {
                stat = recent.get();
            }
        }
        int points = (int) Math.round(amount * (1 + ctx.mod().strength()));
        var key = net.minecraft.resources.ResourceKey.create(com.pipican.core.PipicanRegistries.STAT, stat);
        Boosts.add(player, this, List.of(new StatEffect(key, points)), (int) Math.round(seconds * 20));
    }
}
