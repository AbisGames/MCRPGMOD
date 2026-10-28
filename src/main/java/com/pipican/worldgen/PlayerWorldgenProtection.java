package com.pipican.worldgen;

import com.mojang.serialization.Codec;
import com.pipican.Pipican;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;

/** Persistent provenance: a player-placed natural block is still a player build.
 * Marks survive removal, preventing placed ores from becoming renewable nodes.
 * Older saves cannot retroactively identify player-placed natural blocks.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class PlayerWorldgenProtection extends SavedData {
    public static final Codec<PlayerWorldgenProtection> CODEC = Codec.LONG.listOf().xmap(
        PlayerWorldgenProtection::new, data -> List.copyOf(data.positions));
    public static final SavedDataType<PlayerWorldgenProtection> TYPE = new SavedDataType<>(
        Pipican.id("player_worldgen_protection"), PlayerWorldgenProtection::new, CODEC, null);
    private final Set<Long> positions = new HashSet<>();

    public PlayerWorldgenProtection() {}
    private PlayerWorldgenProtection(List<Long> saved) { positions.addAll(saved); }
    public static PlayerWorldgenProtection get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }
    public void protect(BlockPos pos) {
        if (positions.add(pos.asLong())) setDirty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource)) return;
        // RegionalMineEvents runs at NORMAL and records genuine renewable nodes
        // first. All other excavation must survive deferred city placement.
        if (!RenewableOreSavedData.get(level).contains(event.getPos())) get(level).protect(event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void moving(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource)) return;
        var resolver=event.getStructureHelper();
        if(resolver==null||!resolver.resolve())return;
        var protection=get(level);
        var renewal=RenewableOreSavedData.get(level);
        protection.protect(event.getPos());
        protection.protect(event.getFaceOffsetPos());
        renewal.remove(event.getFaceOffsetPos().asLong());
        for(var source:resolver.getToPush()){
            var destination=source.relative(resolver.getPushDirection());
            protection.protect(source);protection.protect(destination);
            renewal.remove(source.asLong());renewal.remove(destination.asLong());
        }
        for(var destroyed:resolver.getToDestroy()){
            protection.protect(destroyed);renewal.remove(destroyed.asLong());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void changed(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource)) return;
        var kind=event.getVanillaEvent();
        boolean fluid=kind.is(net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE.key())
            ||kind.is(net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP.key());
        boolean playerChange=event.getCause() instanceof net.minecraft.world.entity.player.Player
            &&kind.is(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE.key());
        if(fluid||playerChange){
            var position=BlockPos.containing(event.getEventPosition());
            get(level).protect(position);
            RenewableOreSavedData.get(level).remove(position.asLong());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof RegionalBiomeSource)) return;
        var protection = get(level);
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) protection.protect(snapshot.getPos());
        } else protection.protect(event.getPos());
        // A replacement ends any previous natural-node renewal at that position.
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots())
                RenewableOreSavedData.get(level).remove(snapshot.getPos().asLong());
        } else RenewableOreSavedData.get(level).remove(event.getPos().asLong());
    }
}
