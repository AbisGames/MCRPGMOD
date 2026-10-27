package com.pipican.worldgen;

import com.pipican.Pipican;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
 * <p>Only nodes inside structure pieces tagged as RPG mines enter the
 * renewal queue. Arbitrary player-placed ore blocks are not renewable.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class RegionalMineEvents {
    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final int CHECK_INTERVAL = 80;
    private static final int RESTORE_BATCH = 64;
    /** Only ores inside naturally generated RPG mines are renewable. */
    private static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> MINE_STRUCTURES =
        TagKey.create(Registries.STRUCTURE,Pipican.id("renewable_mines"));
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
            // Only evaluate expensive structure provenance after identifying
            // one of our regional mining nodes, never for ordinary blocks.
            if (!level.structureManager().getStructureWithPieceAt(event.getPos(), MINE_STRUCTURES).isValid()) return;
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
            restoreDue(level,now,RESTORE_BATCH);
        }
    }
    /** Actual server-side restoration, callable by GameTest without waiting hours. */
    public static int restoreDue(ServerLevel level,long now,int limit) {
        RenewableOreSavedData data=RenewableOreSavedData.get(level);
        int restored=0;
        for(RenewableOreSavedData.Node node:data.due(now,limit,n->
                    level.hasChunkAt(BlockPos.of(n.position())))){
            BlockPos position=BlockPos.of(node.position());
            var ore=RegionalOreRegistries.NODES.get(node.oreId());
            if(ore==null){
                data.remove(node.position());
                continue;
            }
            // Player modifications and inventories always win over renewal.
            if(!level.getBlockState(position).isAir())continue;
            if(level.setBlock(position,ore.get().defaultBlockState(),3)){
                data.remove(node.position());
                restored++;
            }
        }
        return restored;
    }

}