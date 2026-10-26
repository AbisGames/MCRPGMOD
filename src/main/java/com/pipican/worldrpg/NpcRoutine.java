package com.pipican.worldrpg;

import java.util.*;

/**
 * Structure marker based NPC schedule model; actual pathfinding is provided
 * by the NeoForge entity AI. Hours are Minecraft daytime ticks [0,24000).
 */
public record NpcRoutine(String id,List<Stop> stops,int wanderRadius) {
    public record Stop(int startTick,int endTick,String markerId) {
        public Stop {
            if(startTick<0 || startTick>=24000 || endTick<=startTick || endTick>24000
                || markerId==null || markerId.isBlank()) throw new IllegalArgumentException("stop");
        }
        public boolean active(int tick) { return tick>=startTick && tick<endTick; }
    }
    public NpcRoutine {
        if(id==null || id.isBlank() || stops==null || stops.isEmpty() || wanderRadius<0)
            throw new IllegalArgumentException("routine");
        stops=List.copyOf(stops);
        for (int i=0;i<stops.size();i++)
            for(int j=i+1;j<stops.size();j++)
                if(stops.get(i).startTick()<stops.get(j).endTick() &&
                   stops.get(j).startTick()<stops.get(i).endTick())
                    throw new IllegalArgumentException("overlapping schedule");
    }
    public Optional<String> markerAt(long daytime) {
        int tick=(int)Math.floorMod(daytime,24000L);
        return stops.stream().filter(s->s.active(tick)).map(Stop::markerId).findFirst();
    }
}
