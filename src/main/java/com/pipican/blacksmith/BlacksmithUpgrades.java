package com.pipican.blacksmith;

import com.pipican.Pipican;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Server-only inventory transaction API. No NPC classes, screens or class-system overrides. */
public final class BlacksmithUpgrades {
    public enum Tier { REGIONAL, ELITE, LEGENDARY, MYTHIC }
    public enum Action { UPGRADE, EVOLVE }
    public enum Result { SUCCESS, WRONG_THREAD, DUPLICATE_REQUEST, INVALID_SLOT, INVALID_ITEM, UNKNOWN_PROFILE,
        STALE_ITEM, STALE_REVISION, WRONG_REGION, MAX_LEVEL, EVOLUTION_UNAVAILABLE, INSUFFICIENT_MATERIALS, INTERNAL_ERROR }
    public record State(int level,Tier tier,String region,long revision,String set){
        public static State read(ItemStack stack){
            var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompoundOrEmpty("pipican_blacksmith");
            int level=tag.getIntOr("level",0); long revision=tag.getLongOr("revision",0);
            var tier=Tier.valueOf(tag.getStringOr("tier","REGIONAL"));
            if(level<0 || level>10 || revision<0 || revision==Long.MAX_VALUE) throw new IllegalArgumentException("Invalid equipment state");
            return new State(level,tier,tag.getStringOr("region",""),revision,tag.getStringOr("set",""));
        }
    }
    public record Request(UUID nonce,int slot,long expectedRevision,ItemStack expected,Action action,String profile){
        public Request { Objects.requireNonNull(nonce); Objects.requireNonNull(action); Objects.requireNonNull(profile); expected=Objects.requireNonNull(expected).copy(); }
    }
    public record Cost(Identifier item,int count){}
    public record Profile(String id,Identifier regional,Identifier boss,Identifier rare){}
    private static final Map<String,Profile> PROFILES=loadProfiles();
    private static final Map<ServerPlayer,LinkedHashSet<UUID>> REQUESTS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Identifier ARMOR_MOD=Pipican.id("blacksmith/armor"), WEAPON_MOD=Pipican.id("blacksmith/weapon");
    private BlacksmithUpgrades(){}
    public static Set<String> profiles(){ return PROFILES.keySet(); }
    private static Map<String,Profile> loadProfiles(){
        var input=BlacksmithUpgrades.class.getResourceAsStream("/data/pipican/blacksmith_profiles.json");
        if(input==null) throw new IllegalStateException("Missing blacksmith profiles");
        try(var reader=new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)){
            var profiles=new LinkedHashMap<String,Profile>();
            for(var entry:com.google.gson.JsonParser.parseReader(reader).getAsJsonArray()){
                var object=entry.getAsJsonObject(); var profile=new Profile(object.get("id").getAsString(),Identifier.parse(object.get("regional_material").getAsString()),
                    Identifier.parse(object.get("boss_material").getAsString()),Identifier.parse(object.get("rare_material").getAsString()));
                if(profiles.putIfAbsent(profile.id(),profile)!=null) throw new IllegalStateException("Duplicate blacksmith profile");
            }
            return Collections.unmodifiableMap(profiles);
        }catch(java.io.IOException error){ throw new IllegalStateException("Cannot read blacksmith profiles",error); }
    }
    public static List<Cost> costs(String profile,State state,Action action){
        var definition=PROFILES.get(profile); if(definition==null) return List.of();
        int scale=1<<state.tier().ordinal();
        if(action==Action.EVOLVE) return combine(List.of(new Cost(definition.regional(),32*scale),new Cost(definition.boss(),2*scale),new Cost(definition.rare(),2*scale)));
        int next=state.level()+1; var costs=new ArrayList<Cost>();
        costs.add(new Cost(definition.regional(),(2+next)*scale));
        if(next>=6) costs.add(new Cost(definition.boss(),1+state.tier().ordinal()));
        if(next>=8) costs.add(new Cost(definition.rare(),scale));
        return combine(costs);
    }
    private static List<Cost> combine(List<Cost> costs){
        var combined=new LinkedHashMap<Identifier,Integer>();
        for(var cost:costs)combined.merge(cost.item(),cost.count(),Integer::sum);
        return combined.entrySet().stream().map(entry->new Cost(entry.getKey(),entry.getValue())).toList();
    }
    public static Result execute(ServerPlayer player,Request request){
        if(!player.level().getServer().isSameThread()) return Result.WRONG_THREAD;
        var previous=REQUESTS.computeIfAbsent(player,key->new LinkedHashSet<>());
        if(!previous.add(request.nonce())) return Result.DUPLICATE_REQUEST;
        if(previous.size()>64) previous.remove(previous.iterator().next());
        var inventory=player.getInventory();
        if(request.slot()<0 || request.slot()>=inventory.getContainerSize()) return Result.INVALID_SLOT;
        var original=inventory.getItem(request.slot());
        if(!ItemStack.matches(original,request.expected())) return Result.STALE_ITEM;
        if(original.isEmpty() || original.getCount()!=1 || !supported(original)) return Result.INVALID_ITEM;
        if(!PROFILES.containsKey(request.profile())) return Result.UNKNOWN_PROFILE;
        State state;
        try { state=State.read(original); }catch(IllegalArgumentException invalid){ return Result.INVALID_ITEM; }
        if(state.revision()!=request.expectedRevision()) return Result.STALE_REVISION;
        if(!state.region().isEmpty() && !state.region().equals(request.profile())) return Result.WRONG_REGION;
        if(request.action()==Action.UPGRADE && state.level()>=10) return Result.MAX_LEVEL;
        if(request.action()==Action.EVOLVE && (state.level()!=10 || state.tier()==Tier.MYTHIC)) return Result.EVOLUTION_UNAVAILABLE;
        var plan=new LinkedHashMap<Integer,Integer>();
        var definition=PROFILES.get(request.profile());
        for(var cost:costs(request.profile(),state,request.action())){
            int remaining=cost.count();
            var accepted=substitutes(definition,cost.item());
            // Spend main-inventory materials; never touch equipped/offhand items or the upgraded slot.
            for(int slot=0;slot<36 && remaining>0;slot++){
                if(slot==request.slot()) continue;
                var stack=inventory.getItem(slot);
                if(!accepted.contains(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
                int available=stack.getCount()-plan.getOrDefault(slot,0), spend=Math.min(available,remaining);
                if(spend>0){ plan.merge(slot,spend,Integer::sum); remaining-=spend; }
            }
            if(remaining>0) return Result.INSUFFICIENT_MATERIALS;
        }
        int nextLevel=request.action()==Action.UPGRADE ? state.level()+1 : 0;
        var nextTier=request.action()==Action.UPGRADE ? state.tier() : Tier.values()[state.tier().ordinal()+1];
        var upgraded=original.copy(); // Preserve every original component, enchantment, trim, name and durability.
        apply(upgraded,new State(nextLevel,nextTier,request.profile(),state.revision()+1,request.profile()+"/"+nextTier.name().toLowerCase(Locale.ROOT)));
        var snapshots=new LinkedHashMap<Integer,ItemStack>();
        snapshots.put(request.slot(),original.copy()); plan.keySet().forEach(slot->snapshots.put(slot,inventory.getItem(slot).copy()));
        try {
            plan.forEach((slot,count)->inventory.getItem(slot).shrink(count));
            inventory.setItem(request.slot(),upgraded); inventory.setChanged();
            player.containerMenu.broadcastChanges();
            return Result.SUCCESS;
        }catch(RuntimeException error){
            snapshots.forEach(inventory::setItem); inventory.setChanged();
            Pipican.LOGGER.error("Blacksmith transaction rolled back",error); return Result.INTERNAL_ERROR;
        }
    }
    /**
     * Universal upgrade materials from the ore catalog, usable at any region's blacksmith: Titanite for the regional
     * material, Raritanium for the boss core, Eridium for the rare trophy, and Pale Ore (the final upgrade) for any.
     */
    public static Set<Identifier> substitutes(Profile definition,Identifier item){
        var accepted=new LinkedHashSet<Identifier>(); accepted.add(item);
        if(definition!=null){
            if(item.equals(definition.regional())) accepted.add(Pipican.id("titanite"));
            if(item.equals(definition.boss())) accepted.add(Pipican.id("raritanium"));
            if(item.equals(definition.rare())) accepted.add(Pipican.id("eridium"));
        }
        accepted.add(Pipican.id("pale_ingot"));
        return accepted;
    }
    public static boolean supported(ItemStack stack){
        if(stack.get(DataComponents.WEAPON)!=null || stack.get(DataComponents.TOOL)!=null) return true;
        return stack.get(DataComponents.EQUIPPABLE)!=null && stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY)
            .modifiers().stream().anyMatch(entry->entry.attribute().equals(Attributes.ARMOR));
    }
    private static void apply(ItemStack stack,State state){
        var root=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        var tag=new CompoundTag(); tag.putInt("level",state.level()); tag.putString("tier",state.tier().name());
        tag.putString("region",state.region()); tag.putLong("revision",state.revision()); tag.putString("set",state.set());
        root.put("pipican_blacksmith",tag); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
        int points=state.tier().ordinal()*10+state.level();
        var modifiers=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        var equippable=stack.get(DataComponents.EQUIPPABLE);
        boolean armor=equippable!=null && modifiers.modifiers().stream().anyMatch(entry->entry.attribute().equals(Attributes.ARMOR));
        if(armor) modifiers=modifiers.withModifierAdded(Attributes.ARMOR,new AttributeModifier(ARMOR_MOD,points*0.05,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.bySlot(equippable.slot()));
        else modifiers=modifiers.withModifierAdded(Attributes.ATTACK_DAMAGE,new AttributeModifier(WEAPON_MOD,points*0.2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,modifiers);
    }
}
