package com.pipican.boss;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import java.util.Map;
import java.util.function.Supplier;

/** Summoning transaction boundary. Callers consume ingredients only after SUCCESS. */
public final class BossEncounterService {
    public enum Result { SUCCESS, INVALID_PLAYER, INVALID_HEIGHT, UNLOADED_ARENA, WRONG_BIOME, OBSTRUCTED, NEARBY_ENCOUNTER, SPAWN_REJECTED, UNSUPPORTED_BOSS }
    private record Binding(BossCatalog.Definition definition,Supplier<? extends EntityType<? extends RegionalBossEntity>> entity) {}
    private static final Map<String,Binding> BINDINGS=Map.of(
        BossCatalog.MORVATH.id(),new Binding(BossCatalog.MORVATH,BossRegistries.MORVATH::get),
        "frostbound_colossus",new Binding(BossCatalog.require("frostbound_colossus"),BossRegistries.FROSTBOUND::get),
        "skollvar_winterfang",new Binding(BossCatalog.require("skollvar_winterfang"),BossRegistries.SKOLLVAR::get),
        "eldaroth_treant",new Binding(BossCatalog.require("eldaroth_treant"),BossRegistries.ELDAROTH::get));
    private BossEncounterService(){}
    public static boolean morvathBiome(ServerLevel level,BlockPos pos){
        return matchesBiome(level,pos,BossCatalog.MORVATH.id());
    }
    public static boolean matchesBiome(ServerLevel level,BlockPos pos,String bossId){
        var binding=BINDINGS.get(bossId);
        return binding!=null && level.getBiome(pos).unwrapKey().map(key->binding.definition().matchesBiome(key.identifier())).orElse(false);
    }
    public static Result summonMorvath(ServerPlayer owner,BlockPos arena){
        return summon(owner,arena,BossCatalog.MORVATH.id());
    }
    public static Result summon(ServerPlayer owner,BlockPos arena,String bossId){
        var binding=BINDINGS.get(bossId);
        if(binding==null) return Result.UNSUPPORTED_BOSS;
        var level=owner.level();
        if(!owner.isAlive() || owner.isSpectator() || owner.blockPosition().distSqr(arena)>48*48) return Result.INVALID_PLAYER;
        if(arena.getY()<=level.getMinY() || arena.getY()+5>level.getMaxY()) return Result.INVALID_HEIGHT;
        int radius=BossEncounters.ARENA_RADIUS;
        for(int x=arena.getX()-radius;x<=arena.getX()+radius;x+=8)
            for(int z=arena.getZ()-radius;z<=arena.getZ()+radius;z+=8)
                if(!level.hasChunkAt(new BlockPos(x,arena.getY(),z))) return Result.UNLOADED_ARENA;
        if(!matchesBiome(level,arena,bossId)) return Result.WRONG_BIOME;
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
        var boss=binding.entity().get().create(level,EntitySpawnReason.TRIGGERED);
        if(boss==null) return Result.SPAWN_REJECTED;
        boss.snapTo(arena.getX()+0.5,arena.getY(),arena.getZ()+0.5);
        if(!level.noCollision(boss,boss.getBoundingBox())) return Result.OBSTRUCTED;
        if(!encounters.reserve(boss.getUUID(),owner.getUUID(),bossId,arena)) return Result.NEARBY_ENCOUNTER;
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
