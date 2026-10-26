package com.pipican.worldrpg;

import java.util.*;

/**
 * Per-miner supply unlocks. Each ore has a required delivery amount.
 * Rare ores should require more deliveries and have limited merchant stock.
 * Item removal is performed by the NeoForge integration, never by a client event.
 */
public final class MinerSupplyService {
    public record OreContract(String oreId, int needed, long purchasePrice, int restockAmount) {
        public OreContract {
            if(oreId==null || oreId.isBlank() || needed<=0 || purchasePrice<0 || restockAmount<0)
                throw new IllegalArgumentException("ore contract");
        }
    }
    private final Map<String,OreContract> contracts;
    private final Map<String,Integer> delivered = new HashMap<>();
    public MinerSupplyService(Collection<OreContract> contracts) {
        Map<String,OreContract> c = new HashMap<>();
        for (var contract : contracts)
            if(c.putIfAbsent(contract.oreId(),contract)!=null) throw new IllegalArgumentException("duplicate ore");
        this.contracts = Map.copyOf(c);
    }
    public int remaining(String oreId) {
        OreContract c = Objects.requireNonNull(contracts.get(oreId));
        return Math.max(0,c.needed()-delivered.getOrDefault(oreId,0));
    }
    public boolean unlocked(String oreId) { return remaining(oreId)==0; }
    /** Invoke only after the server has removed exactly the specified items. */
    public void acknowledgeDelivery(String oreId,int quantity) {
        if(quantity<=0 || quantity>remaining(oreId)) throw new IllegalArgumentException("delivery");
        delivered.merge(oreId,quantity,Math::addExact);
    }
    public Map<String,Integer> snapshot() { return Map.copyOf(delivered); }
    public void restore(Map<String,Integer> snapshot) {
        for (var e : snapshot.entrySet())
            if(!contracts.containsKey(e.getKey()) || e.getValue()<0 ||
                e.getValue()>contracts.get(e.getKey()).needed()) throw new IllegalArgumentException("save");
        delivered.clear(); delivered.putAll(snapshot);
    }
}
