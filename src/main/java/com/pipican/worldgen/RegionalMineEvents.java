package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Restores mined regional nodes in generated RPG mine structures without ever
 * respawning the whole building, overwriting player-built blocks or force
 * loading unexplored chunks. This deliberately targets singleplayer first.
 *
 * <p>It does not currently prove a node came from a mine structure. That
 * provenance restriction is a separate worldgen-integration requirement.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class RegionalMineEvents {
    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final int CHECK_INTERVAL = 80;
    private static final int RESTORE_BATCH = 64;
    private RegionalMineEvents() {}

    @SubscribeEvent
    public static void broken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(level.getChunkSource().getGenerator().getBiomeSource()
                instanceof RegionalBiomeSource)) return;
        Block block = event.getState().getBlock();
        for (Map.Entry<String, ?> entry : RegionalOreRegistries.NODES.entrySet()) {
            String ore = entry.getKey();
            if (block != RegionalOreRegistries.NODES.get(ore).get()) continue;
            int minutes = RegionalOreCatalog.get(ore).orElseThrow().regenerationMinutes();
            long now = level.getGameTime();
            long readyAt = Math.addExact(now, Math.multiplyExact(minutes, TICKS_PER_MINUTE));
            RenewableOreSavedData.get(level).record(event.getPos(), ore, readyAt);
            return;
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!(level.getChunkSource().getGenerator().getBiomeSource()
                    instanceof RegionalBiomeSource)) continue;
            long now = level.getGameTime();
            if (now % CHECK_INTERVAL != 0L) continue;
            RenewableOreSavedData data = RenewableOreSavedData.get(level);
            for (RenewableOreSavedData.Node node : data.due(now, RESTORE_BATCH)) {
                BlockPos position = BlockPos.of(node.position());
                if (!level.hasChunkAt(position)) continue;
                // A player-placed block is never destroyed by a renewal.
                if (!level.getBlockState(position).isAir()) continue;
                var ore = RegionalOreRegistries.NODES.get(node.oreId());
                if (ore == null) {
                    data.remove(node.position());
                    continue;
                }
                level.setBlock(position, ore.get().defaultBlockState(), 3);
                data.remove(node.position());
            }
        }
    }
}