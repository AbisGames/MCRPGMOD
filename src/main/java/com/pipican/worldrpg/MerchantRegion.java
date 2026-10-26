package com.pipican.worldrpg;

import java.util.*;

/** A merchant shop pool selected by matching biome and structure tags. */
public record MerchantRegion(String id, Set<String> biomeTags, Set<String> structureTags,
                             String shopId, int priority) {
    public MerchantRegion {
        if(id==null || id.isBlank() || shopId==null || shopId.isBlank()) throw new IllegalArgumentException();
        biomeTags=Set.copyOf(biomeTags); structureTags=Set.copyOf(structureTags);
    }
    public boolean matches(Set<String> biomes, Set<String> structures) {
        return (biomeTags.isEmpty() || !Collections.disjoint(biomeTags,biomes))
            && (structureTags.isEmpty() || !Collections.disjoint(structureTags,structures));
    }
    public static Optional<String> choose(Collection<MerchantRegion> regions,Set<String> biomes,Set<String> structures) {
        return regions.stream().filter(r->r.matches(biomes,structures))
            .sorted(Comparator.comparingInt(MerchantRegion::priority).reversed().thenComparing(MerchantRegion::id))
            .map(MerchantRegion::shopId).findFirst();
    }
}
