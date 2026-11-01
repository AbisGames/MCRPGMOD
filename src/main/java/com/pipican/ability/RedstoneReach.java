package com.pipican.ability;

import com.mojang.serialization.Codec;
import com.pipican.Pipican;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Technomancer redstone ({@code redstone_reach}): dust placed by a player with the bonus is remembered per level, and every
 * eighth block of it (by position) passes its signal on without the usual loss of 1 - a 15-block run of dust then reaches
 * about 2 blocks farther. Only positions that still hold redstone dust count; dust someone else places there is ordinary.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class RedstoneReach extends SavedData {

    private static final Codec<RedstoneReach> CODEC = Codec.LONG_STREAM.xmap(
            s -> {
                RedstoneReach r = new RedstoneReach();
                s.forEach(r.wires::add);
                return r;
            },
            r -> Arrays.stream(r.wires.toLongArray()));
    private static final SavedDataType<RedstoneReach> TYPE = new SavedDataType<>(Pipican.id("redstone_reach"), RedstoneReach::new, CODEC.fieldOf("wires").codec(), null);

    private final LongOpenHashSet wires = new LongOpenHashSet();

    public RedstoneReach() {}

    private static RedstoneReach of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** True if the dust at {@code pos} is reach dust on one of its lossless blocks. */
    public static boolean carries(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && Math.floorMod(pos.getX() + pos.getZ(), 8) == 0
                && level.getBlockState(pos).is(Blocks.REDSTONE_WIRE) && of(server).wires.contains(pos.asLong());
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getPlacedBlock().is(Blocks.REDSTONE_WIRE) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // Dust placed by anyone else over an old spot is ordinary dust again.
        boolean reach = event.getEntity() instanceof ServerPlayer player
                && com.pipican.player.ClassEffectApplier.state(player).chance(com.pipican.effect.type.BonusEffect.Kind.REDSTONE_REACH) > 0;
        RedstoneReach data = of(level);
        if (reach ? data.wires.add(event.getPos().asLong()) : data.wires.remove(event.getPos().asLong())) {
            data.setDirty();
        }
    }
}
