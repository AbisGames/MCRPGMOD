package com.pipican.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-dimension reservations survive chunk unload. Terminal entries prevent replayed rewards. */
public final class BossEncounters extends SavedData {
    public static final int ARENA_RADIUS=24;
    public enum Status { ACTIVE, CANCELLED, DEFEATED }
    public record Encounter(UUID boss, UUID owner, String type, BlockPos arena, Status status) {
        private static final Codec<Encounter> CODEC=RecordCodecBuilder.create(instance->instance.group(
            UUIDUtil.CODEC.fieldOf("boss").forGetter(Encounter::boss),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Encounter::owner),
            Codec.STRING.fieldOf("type").forGetter(Encounter::type),
            BlockPos.CODEC.fieldOf("arena").forGetter(Encounter::arena),
            Codec.STRING.comapFlatMap(name->{
                try { return com.mojang.serialization.DataResult.success(Status.valueOf(name)); }
                catch(IllegalArgumentException invalid){ return com.mojang.serialization.DataResult.error(()->"Unknown encounter status: "+name); }
            },Status::name).fieldOf("status").forGetter(Encounter::status)
        ).apply(instance,Encounter::new));
        Encounter terminal(Status next){ return new Encounter(boss,owner,type,arena,next); }
    }
    public static final Codec<BossEncounters> CODEC=Encounter.CODEC.listOf().xmap(BossEncounters::new,data->List.copyOf(data.encounters.values()));
    public static final SavedDataType<BossEncounters> TYPE=new SavedDataType<>(Pipican.id("boss_encounters"),BossEncounters::new,CODEC,null);
    private final Map<UUID,Encounter> encounters=new LinkedHashMap<>();
    public BossEncounters(){}
    private BossEncounters(List<Encounter> saved){ for(var entry:saved) encounters.put(entry.boss(),entry); }
    public static BossEncounters get(ServerLevel level){ return level.getDataStorage().computeIfAbsent(TYPE); }
    public Optional<Encounter> find(UUID boss){ return Optional.ofNullable(encounters.get(boss)); }
    public boolean conflicts(BlockPos arena){
        long distance=2L*ARENA_RADIUS+8;
        return encounters.values().stream().anyMatch(entry->entry.status()==Status.ACTIVE && entry.arena().distSqr(arena)<distance*distance);
    }
    /** Called before spawning or consuming a sigil; never releases an unloaded active boss. */
    public boolean reserve(UUID boss,UUID owner,String type,BlockPos arena){
        if(encounters.containsKey(boss) || conflicts(arena)) return false;
        encounters.put(boss,new Encounter(boss,owner,type,arena.immutable(),Status.ACTIVE)); setDirty(); return true;
    }
    /** Only a failed spawn can roll back a reservation. Never use this to cancel a live fight. */
    public boolean rollbackUnspawned(UUID boss){
        var entry=encounters.get(boss);
        if(entry==null || entry.status()!=Status.ACTIVE) return false;
        encounters.remove(boss); setDirty(); return true;
    }
    public boolean finish(UUID boss,Status outcome){
        var entry=encounters.get(boss);
        if(outcome==Status.ACTIVE || entry==null || entry.status()!=Status.ACTIVE) return false;
        encounters.put(boss,entry.terminal(outcome)); setDirty(); return true;
    }
}
