package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:recall_item} - pulls the nearest item the user dropped or threw (one of {@code items},
 * e.g. their weapon, or a thrown trident) within {@code radius} back into their inventory (Recall Weapon).
 * Cancelled (and refunded) if there is none.
 *
 * <pre>{@code { "type": "pipican:recall_item", "items": "#pipican:weapons/melee", "radius": 64 } }</pre>
 */
public record RecallItemAction(HolderSet<Item> items, double radius) implements AbilityAction {

    public static final MapCodec<RecallItemAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(RecallItemAction::items),
            Codec.doubleRange(1, 128).optionalFieldOf("radius", 64.0).forGetter(RecallItemAction::radius)
    ).apply(i, RecallItemAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.RECALL_ITEM;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity e : ctx.level().getEntities(player, player.getBoundingBox().inflate(radius), e -> e.isAlive()
                && (e instanceof ItemEntity item && item.getOwner() == player && item.getItem().is(items)
                || e instanceof ThrownTrident trident && trident.getOwner() == player))) {
            double d = e.distanceToSqr(player);
            if (d < bestDistance) {
                best = e;
                bestDistance = d;
            }
        }
        if (best == null) {
            ctx.cancel(Component.translatable("pipican.ability.nothing_to_recall"));
            return;
        }
        ItemStack stack = best instanceof ItemEntity item ? item.getItem().copy() : ((ThrownTrident) best).getWeaponItem().copy();
        best.discard();
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
