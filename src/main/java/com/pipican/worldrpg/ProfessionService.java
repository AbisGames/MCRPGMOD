package com.pipican.worldrpg;

import java.util.*;

/**
 * Profession-specific interactions independent of class definitions and NeoForge APIs.
 * Callers must verify NPC identity, interaction distance and player inventory server-side.
 * The returned plan is NOT permission to mint items: it must be fulfilled atomically.
 */
public final class ProfessionService {
    public enum Profession { BLACKSMITH, MINER, ALCHEMIST, COOK, MERCHANT, GENERAL }
    public record Service(String id, Profession profession, long coinCost,
                          Map<String,Integer> ingredients, String product, int quantity,
                          Optional<String> recipeId) {
        public Service {
            if (id == null || id.isBlank() || profession == null || coinCost < 0
                    || product == null || product.isBlank() || quantity <= 0)
                throw new IllegalArgumentException("service definition");
            ingredients = Map.copyOf(ingredients);
            ingredients.forEach((item,count)-> {
                if(item == null || item.isBlank() || count == null || count <= 0)
                    throw new IllegalArgumentException("ingredient");
            });
            recipeId = Objects.requireNonNull(recipeId);
        }
    }
    public record ExecutionPlan(Service service, long cost, Map<String,Integer> toConsume,
                                String toGrant, int grantCount) {}

    private final Map<String,Service> services;
    public ProfessionService(Collection<Service> definitions) {
        Map<String,Service> copy = new HashMap<>();
        for (Service service : definitions)
            if (copy.putIfAbsent(service.id(), service) != null)
                throw new IllegalArgumentException("duplicate service "+service.id());
        this.services = Map.copyOf(copy);
    }

    /**
     * Prepare a plan; never mutate player state here. For a crafting/brewing recipe,
     * the recipe must already be unlocked, except vanilla recipes explicitly supplied
     * with an empty recipeId.
     */
    public ExecutionPlan plan(String serviceId, Profession profession, WorldRpgState player,
                              Map<String,Integer> verifiedInventory) {
        Service service = Objects.requireNonNull(services.get(serviceId), "unknown service");
        if (service.profession() != profession) throw new IllegalStateException("wrong NPC profession");
        if (player.coins() < service.coinCost()) throw new IllegalStateException("insufficient coins");
        if (service.recipeId().isPresent() && !player.unlockedRecipes().contains(service.recipeId().get()))
            throw new IllegalStateException("recipe locked");
        for (var ingredient : service.ingredients().entrySet())
            if (verifiedInventory.getOrDefault(ingredient.getKey(), 0) < ingredient.getValue())
                throw new IllegalStateException("missing ingredient "+ingredient.getKey());
        return new ExecutionPlan(service,service.coinCost(),service.ingredients(),service.product(),service.quantity());
    }
    public Collection<Service> services() { return services.values(); }
}
