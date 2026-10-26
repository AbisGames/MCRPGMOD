package com.pipican.worldgen;

import java.util.*;

/**
 * Persistent renewable instance bookkeeping. Caller must persist snapshots per
 * dimension and instance and validate proximity/ownership before granting loot.
 * Time is supplied by a server-authoritative monotonic clock, never a client.
 */
public final class RenewableSite {
    public enum Kind { DUNGEON, MINE }
    public record Snapshot(String id,Kind kind,long availableAt,boolean occupied) {}
    private final String id;
    private final Kind kind;
    private final long cooldownMillis;
    private long availableAt;
    private boolean occupied;
    public RenewableSite(String id,Kind kind,long cooldownMillis,long availableAt) {
        if(id==null||id.isBlank()||kind==null||cooldownMillis<=0)throw new IllegalArgumentException();
        this.id=id;this.kind=kind;this.cooldownMillis=cooldownMillis;this.availableAt=availableAt;
    }
    /** Called by a server-side manager after all players leave the site. */
    public synchronized void setOccupied(boolean occupied){this.occupied=occupied;}
    public synchronized boolean ready(long now){return !occupied&&now>=availableAt;}
    /** Marks consumed atomically; never respawn structures or rewrite player blocks. */
    public synchronized boolean consume(long now){
        if(!ready(now))return false;
        availableAt=Math.addExact(now,cooldownMillis);
        return true;
    }
    public synchronized Snapshot snapshot(){return new Snapshot(id,kind,availableAt,occupied);}
}
