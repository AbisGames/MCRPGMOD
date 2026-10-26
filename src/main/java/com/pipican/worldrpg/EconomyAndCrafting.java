package com.pipican.worldrpg;

import java.util.*;

/**
 * Single currency, finite stock, explicit recipes. Caller must validate player
 * proximity, NPC shop ownership, available inventory and ingredient consumption.
 */
public final class EconomyAndCrafting {
    public record Listing(String itemId,long buyPrice,long sellPrice,int startingStock) {
        public Listing {
            if(itemId==null || itemId.isBlank() || buyPrice<0 || sellPrice<0 || sellPrice>buyPrice || startingStock<0)
                throw new IllegalArgumentException("listing");
        }
    }
    public record Recipe(String id,Type type,Map<String,Integer> ingredients,String outputId,int outputCount) {
        public enum Type { COOKING, BREWING, FORGING, EQUIPMENT_UPGRADE }
        public Recipe {
            if(id==null||id.isBlank()||type==null||outputId==null||outputId.isBlank()||outputCount<=0)
                throw new IllegalArgumentException("recipe");
            ingredients=Map.copyOf(ingredients);
            ingredients.forEach((k,v)->{if(k.isBlank()||v<=0)throw new IllegalArgumentException("ingredient");});
        }
    }
    public static final class Shop {
        private final Map<String,Listing> listings;
        private final Map<String,Integer> stock = new HashMap<>();
        public Shop(Collection<Listing> definitions) {
            Map<String,Listing> m=new HashMap<>();
            for(Listing l:definitions) {
                if(m.putIfAbsent(l.itemId(),l)!=null)throw new IllegalArgumentException("duplicate listing");
                stock.put(l.itemId(),l.startingStock());
            }
            listings=Map.copyOf(m);
        }
        public int stock(String item) {return stock.getOrDefault(item,0);}
        /**
         * Monetary transaction only. The server adapter MUST atomically grant the item,
         * and rollback on failure; do not expose this directly to clients.
         */
        public void buy(WorldRpgState player,String item,int count) {
            if(count<=0)throw new IllegalArgumentException("count");
            Listing listing=Objects.requireNonNull(listings.get(item),"item unavailable");
            if(stock(item)<count)throw new IllegalStateException("out of stock");
            long cost=Math.multiplyExact(listing.buyPrice(),count);
            player.pay(cost); stock.put(item,stock(item)-count);
        }
        public void restock(String item,int quantity) {
            if(quantity<0 || !listings.containsKey(item))throw new IllegalArgumentException();
            stock.put(item,Math.addExact(stock(item),quantity));
        }
    }
    /** Recipe unlocks are persistent per player, not tied to currently equipped class. */
    public static boolean mayCraft(WorldRpgState state,Recipe recipe) {
        return state.unlockedRecipes().contains(recipe.id());
    }
    private EconomyAndCrafting() {}
}
