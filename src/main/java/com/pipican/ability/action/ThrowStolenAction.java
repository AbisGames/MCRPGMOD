package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * {@code pipican:throw_stolen} - throws the last effect Steal Spell took from the user's attackers as a splash potion where
 * they look (Return to Sender). Cancelled (and refunded) when nothing has been stolen yet; each stolen effect can be thrown
 * once, unless {@code keep} ("stolen effects can be re-applied").
 *
 * <pre>{@code { "type": "pipican:throw_stolen", "power": 0.7 } }</pre>
 */
public record ThrowStolenAction(double power, boolean keep) implements AbilityAction {

    public static final MapCodec<ThrowStolenAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(0.1, 3).optionalFieldOf("power", 0.7).forGetter(ThrowStolenAction::power),
            Codec.BOOL.optionalFieldOf("keep", false).forGetter(ThrowStolenAction::keep)
    ).apply(i, ThrowStolenAction::new));

    /** Player -> the last effect their Steal Spell cancelled. Not saved. */
    private static final Map<UUID, MobEffectInstance> STOLEN = new ConcurrentHashMap<>();

    /** Steal Spell cancelled {@code effect} on its way to {@code player}. */
    public static void stole(ServerPlayer player, MobEffectInstance effect) {
        STOLEN.put(player.getUUID(), new MobEffectInstance(effect));
    }

    public static Optional<MobEffectInstance> last(ServerPlayer player) {
        return Optional.ofNullable(STOLEN.get(player.getUUID()));
    }

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.THROW_STOLEN;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        MobEffectInstance stolen = STOLEN.get(player.getUUID());
        if (stolen == null) {
            ctx.cancel(Component.translatable("pipican.ability.nothing_stolen"));
            return;
        }
        if (!keep && com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.KEEP_STOLEN) <= 0) {
            STOLEN.remove(player.getUUID());
        }
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        potion.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(stolen)), Optional.empty()));
        ThrownSplashPotion thrown = new ThrownSplashPotion(ctx.level(), player, potion);
        thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, (float) power, 1.0F);
        ctx.level().addFreshEntity(thrown);
    }
}
