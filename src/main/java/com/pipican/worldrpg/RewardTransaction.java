package com.pipican.worldrpg;

import java.util.*;

/**
 * Inventory bridge for transactional quest rewards and profession services.
 * Implementations MUST rollback if any operation fails (including exceptions).
 */
public interface RewardTransaction {
    /** Validate capacity, player proximity, account state and all item IDs. No mutation. */
    void validate(Map<String,Integer> remove,Map<String,Integer> add,long coins);
    /** Atomically consume inventory and grant items; throws without partial effects. */
    void exchange(Map<String,Integer> remove,Map<String,Integer> add);

    static Map<String,Integer> single(String item,int count) {
        if(item==null || item.isBlank() || count<=0)throw new IllegalArgumentException();
        return Map.of(item,count);
    }
}
