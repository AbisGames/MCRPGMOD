package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.entity.Eidolon;
import com.pipican.entity.PipicanEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:eidolon} - builds the user's eidolon (Engineer), using up {@code count} x {@code item}
 * from their inventory. If they already have one, it is called to their side instead (free). When the
 * eidolon is destroyed, this ability goes on a {@code rebuild_ticks} cooldown.
 *
 * <pre>{@code { "type": "pipican:eidolon", "item": "minecraft:iron_block", "count": 4, "rebuild_ticks": 8000 } }</pre>
 */
public record EidolonAction(Item item, int count, int rebuildTicks) implements AbilityAction {

    public static final MapCodec<EidolonAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item", net.minecraft.world.item.Items.IRON_BLOCK).forGetter(EidolonAction::item),
            Codec.intRange(0, 64).optionalFieldOf("count", 4).forGetter(EidolonAction::count),
            Codec.intRange(0, 1_728_000).optionalFieldOf("rebuild_ticks", 8000).forGetter(EidolonAction::rebuildTicks)
    ).apply(i, EidolonAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.EIDOLON;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var existing = Eidolon.of(player);
        if (existing.isPresent()) {
            existing.get().snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
            existing.get().getNavigation().stop();
            ctx.cancel(Component.translatable("pipican.eidolon.called"));
            return;
        }
        var inventory = player.getInventory();
        if (!player.getAbilities().instabuild && inventory.countItem(item) < count) {
            ctx.cancel(Component.translatable("pipican.eidolon.needs", count, new ItemStack(item).getHoverName()));
            return;
        }
        Eidolon eidolon = PipicanEntities.EIDOLON.get().create(ctx.level(), EntitySpawnReason.MOB_SUMMONED);
        if (eidolon == null) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            int left = count;
            for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(item)) {
                    int take = Math.min(left, stack.getCount());
                    stack.shrink(take);
                    left -= take;
                }
            }
        }
        var look = player.getLookAngle().multiply(1, 0, 1).normalize().scale(2);
        eidolon.snapTo(player.getX() + look.x, player.getY(), player.getZ() + look.z, player.getYRot() + 180, 0);
        eidolon.setOwner(player, ctx.ability(), rebuildTicks);
        ctx.level().addFreshEntity(eidolon);
    }
}
