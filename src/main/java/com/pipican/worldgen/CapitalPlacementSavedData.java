package com.pipican.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent one-time capital placement state, scoped to a single saved RPG
 * overworld. Progress is committed after every placed architectural tile to
 * prevent rebuilding a city when a singleplayer world is reloaded.
 */
public final class CapitalPlacementSavedData extends SavedData {
    public record Progress(String id, int nextTile, int foundationY) {}
    private static final Codec<Progress> PROGRESS_CODEC =
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Progress::id),
            Codec.INT.fieldOf("next_tile").forGetter(Progress::nextTile),
            Codec.INT.fieldOf("foundation_y").forGetter(Progress::foundationY)
        ).apply(instance,Progress::new));

    private static final Codec<CapitalPlacementSavedData> CODEC =
        PROGRESS_CODEC.listOf().xmap(CapitalPlacementSavedData::fromList,
            state -> List.copyOf(state.progress.values()));

    public static final SavedDataType<CapitalPlacementSavedData> TYPE =
        new SavedDataType<>(Pipican.id("capital_placement"),
            CapitalPlacementSavedData::new,CODEC,null);
    private final Map<String,Progress> progress = new HashMap<>();

    public CapitalPlacementSavedData() {}
    private static CapitalPlacementSavedData fromList(List<Progress> list){
        CapitalPlacementSavedData result=new CapitalPlacementSavedData();
        for(Progress p:list)result.progress.put(p.id(),p);
        return result;
    }
    public static CapitalPlacementSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
    public Progress progress(String id) {return progress.get(id);}
    public void advance(String id,int nextTile,int baseY) {
        progress.put(id,new Progress(id,nextTile,baseY));
        setDirty();
    }
}
