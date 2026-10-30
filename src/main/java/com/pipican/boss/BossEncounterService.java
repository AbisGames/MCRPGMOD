package com.pipican.boss;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;

/** Summoning transaction boundary. Callers consume ingredients only after SUCCESS. */
public final class BossEncounterService {
    public enum Result { SUCCESS, INVALID_PLAYER, UNLOADED_ARENA, WRONG_BIOME, OBSTRUCTED, NEARBY_ENCOUNTER, SPAWN_REJECTED }
    private BossEncounterService(){}
    public static boolean morvathBiome(ServerLevel level,BlockPos pos){
        return level.getBiome(pos).unwrapKey().map(key->BossCatalog.MORVATH.matchesBiome(key.identifier())).orElse(false);
    }
    public static Result summonMorvath(ServerPlayer owner,BlockPos arena){
        var level=owner.level();
        if(!owner.isAlive() || owner.isSpectator() || owner.blockPosition().distSqr(arena)>48*48) return Result.INVALID_PLAYER;
        int radius=BossEncounters.ARENA_RADIUS;
        for(int x=arena.getX()-radius;x<=arena.getX()+radius;x+=8)
            for(int z=arena.getZ()-radius;z<=arena.getZ()+radius;z+=8)
                if(!level.hasChunkAt(new BlockPos(x,arena.getY(),z))) return Result.UNLOADED_ARENA;
        if(!morvathBiome(level,arena)) return Result.WRONG_BIOME;
        var encounters=BossEncounters.get(level);
        if(encounters.conflicts(arena)) return Result.NEARBY_ENCOUNTER;
        // Only inspect; player-built floors and surrounding blocks are never modified.
        for(var pos:BlockPos.betweenClosed(arena.offset(-5,0,-5),arena.offset(5,4,5)))
            if(!level.getBlockState(pos).getCollisionShape(level,pos).isEmpty() || !level.getFluidState(pos).isEmpty()) return Result.OBSTRUCTED;
        for(var floor:BlockPos.betweenClosed(arena.offset(-6,-1,-6),arena.offset(6,-1,6)))
            if(!level.getFluidState(floor).isEmpty() || level.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
                || level.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
                || level.getBlockState(floor).is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)
                || !level.getBlockState(floor).isFaceSturdy(level,floor,net.minecraft.core.Direction.UP)) return Result.OBSTRUCTED;
        var boss=BossRegistries.MORVATH.get().create(level,EntitySpawnReason.TRIGGERED);
        if(boss==null) return Result.SPAWN_REJECTED;
        boss.snapTo(arena.getX()+0.5,arena.getY(),arena.getZ()+0.5);
        if(!level.noCollision(boss,boss.getBoundingBox())) return Result.OBSTRUCTED;
        if(!encounters.reserve(boss.getUUID(),owner.getUUID(),BossCatalog.MORVATH.id(),arena)) return Result.NEARBY_ENCOUNTER;
        boss.activateEncounter();
        boolean spawned=false;
        try { spawned=level.addFreshEntity(boss); return spawned ? Result.SUCCESS : Result.SPAWN_REJECTED; }
        finally { if(!spawned) encounters.rollbackUnspawned(boss.getUUID()); }
    }
    public static boolean cancel(ServerLevel level,UUID boss,UUID owner){
        var data=BossEncounters.get(level); var encounter=data.find(boss).orElse(null);
        return encounter!=null && encounter.owner().equals(owner) && data.finish(boss,BossEncounters.Status.CANCELLED);
    }
}
