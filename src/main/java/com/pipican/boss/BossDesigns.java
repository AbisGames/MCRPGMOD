package com.pipican.boss;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Shared design metadata. Planned entries have no entity or executable ability registration. */
public final class BossDesigns {
    private static final Set<String> REQUIRED_ANIMATIONS=Set.of("idle","walk","basic_attack","hurt","phase_transition","summoning","death");
    public record Ability(String id,String implementation,String behavior,String counterplay,int windup,int recovery,int cooldown,String animation){}
    public record Ingredient(Identifier item,int count){}
    public record Sigil(String item,boolean registered,List<Ingredient> ingredients,int minY,int maxY){}
    public record RareDrop(Identifier item,double chance){}
    public record Loot(Identifier regional,int count,String trophyItem,String plannedTrophy,int xp,Optional<RareDrop> rare){}
    public record Design(String id,String region,boolean functional,double health,double damage,Sigil sigil,List<Ability> abilities,
        String phaseTwo,String geometry,String modelFile,Map<String,String> animations,Loot loot){}
    private static final Map<String,Design> DESIGNS=load();
    private BossDesigns(){}
    public static Collection<Design> all(){ return DESIGNS.values(); }
    public static Design require(String id){
        var design=DESIGNS.get(id); if(design==null) throw new IllegalArgumentException("Unknown boss design: "+id); return design;
    }
    private static Map<String,Design> load(){
        var input=BossDesigns.class.getResourceAsStream("/assets/pipican/geo/bosses/combat_designs.json");
        if(input==null) throw new IllegalStateException("Missing combat designs");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var designs=new LinkedHashMap<String,Design>(); Set<String> sigilItems=new HashSet<>();
            for(var entry:JsonParser.parseReader(reader).getAsJsonArray()){
                var object=entry.getAsJsonObject(); String id=object.get("id").getAsString();
                var catalog=BossCatalog.require(id); String region=object.get("region").getAsString();
                boolean functional=object.get("functional").getAsBoolean();
                if(!region.equals(catalog.region()) || functional && !Set.of(BossCatalog.MORVATH.id(),"frostbound_colossus","skollvar_winterfang","eldaroth_treant","fallen_king","kragmor_canyon_warlord","nocthar_void_sovereign","aetherion_sky_guardian","vermillion_blood_monarch","longwei_jade_dragon","morgatha_swamp_queen","aurelios_sunken_emperor","lianhua_lotus_serpent","akhmar_sand_emperor").contains(id))
                    throw new IllegalStateException("Invalid/unimplemented boss binding: "+id);
                double health=object.get("health").getAsDouble(),damage=object.get("damage").getAsDouble();
                if(!Double.isFinite(health) || !Double.isFinite(damage) || health<=0 || damage<=0) throw new IllegalStateException("Invalid boss stats");
                var sigil=object.getAsJsonObject("sigil");
                var ingredients=List.of(new Ingredient(Identifier.parse(sigil.get("regional_material").getAsString()),sigil.get("regional_count").getAsInt()),
                    new Ingredient(Identifier.parse(sigil.get("dungeon_material").getAsString()),sigil.get("dungeon_count").getAsInt()),
                    new Ingredient(Identifier.parse(sigil.get("ritual_core").getAsString()),1),new Ingredient(Identifier.parse(sigil.get("catalyst").getAsString()),1));
                var sigilDefinition=new Sigil(sigil.get(functional ? "item" : "planned_item_id").getAsString(),functional,ingredients,sigil.get("min_y").getAsInt(),sigil.get("max_y").getAsInt());
                if(sigilDefinition.minY()>=sigilDefinition.maxY() || ingredients.stream().anyMatch(value->value.count()<1)
                    || ingredients.stream().mapToInt(Ingredient::count).sum()!=9 || !sigilItems.add(sigilDefinition.item()))
                    throw new IllegalStateException("Invalid or duplicate boss sigil definition: "+id);
                var abilities=new ArrayList<Ability>(); Set<String> ids=new HashSet<>();
                for(var abilityEntry:object.getAsJsonArray("abilities")){
                    var ability=abilityEntry.getAsJsonObject(); String abilityId=ability.get("id").getAsString();
                    var definition=new Ability(abilityId,ability.get("implementation").getAsString(),ability.get("behavior").getAsString(),ability.get("counterplay").getAsString(),
                        ability.get("windup_ticks").getAsInt(),ability.get("recovery_ticks").getAsInt(),ability.get("cooldown_ticks").getAsInt(),ability.get("animation").getAsString());
                    boolean implemented=definition.implementation().equals("functional") && !definition.animation().startsWith("planned:");
                    boolean explicitlyPlanned=definition.implementation().equals("planned") && definition.animation().startsWith("planned:");
                    if(!ids.add(abilityId) || definition.behavior().isBlank() || definition.counterplay().isBlank()
                        || definition.windup()<1 || definition.recovery()<1 || definition.cooldown()<1
                        || functional!=implemented || !functional && !explicitlyPlanned)
                        throw new IllegalStateException("Invalid or unimplemented boss ability binding: "+id+"/"+abilityId);
                    abilities.add(definition);
                }
                if(abilities.size()!=3) throw new IllegalStateException("Expected three abilities: "+id);
                var model=object.getAsJsonObject("model"); String geometry=model.get("geometry").getAsString(),file=model.get("file").getAsString();
                if(!geometry.equals(catalog.geometry()) || BossDesigns.class.getResource("/assets/pipican/"+file)==null) throw new IllegalStateException("Missing/mismatched boss geometry: "+id);
                var animations=new LinkedHashMap<String,String>(); model.getAsJsonObject("animation_mapping").entrySet().forEach(mapping->animations.put(mapping.getKey(),mapping.getValue().getAsString()));
                if(animations.size()!=10 || !animations.keySet().containsAll(REQUIRED_ANIMATIONS)
                    || functional && animations.values().stream().anyMatch(value->value.isBlank() || value.startsWith("planned:"))
                    || !functional && animations.values().stream().anyMatch(value->!value.startsWith("planned:")))
                    throw new IllegalStateException("Incomplete or misleading boss animation mapping: "+id);
                var rewards=object.getAsJsonObject("loot"); Optional<RareDrop> rare=Optional.empty();
                if(rewards.has("rare_drop")){
                    var drop=rewards.getAsJsonObject("rare_drop"); rare=Optional.of(new RareDrop(Identifier.parse(drop.get("item").getAsString()),drop.get("chance").getAsDouble()));
                }
                var loot=new Loot(Identifier.parse(rewards.get("guaranteed_regional_material").getAsString()),rewards.get("regional_count").getAsInt(),
                    rewards.get("trophy_item").isJsonNull() ? null : rewards.get("trophy_item").getAsString(),rewards.get("planned_trophy_name").getAsString(),rewards.get("xp").getAsInt(),rare);
                if(loot.count()<1 || loot.xp()<1 || loot.plannedTrophy().isBlank()
                    || functional!=(loot.trophyItem()!=null) || rare.filter(drop->!Double.isFinite(drop.chance()) || drop.chance()<=0 || drop.chance()>1).isPresent())
                    throw new IllegalStateException("Invalid or misleading boss loot definition: "+id);
                var phase=object.getAsJsonObject("phase_two");
                if(!phase.get("once_only").getAsBoolean() || phase.get("health_fraction").getAsDouble()!=0.5) throw new IllegalStateException("Invalid phase contract");
                var design=new Design(id,region,functional,health,damage,sigilDefinition,List.copyOf(abilities),phase.get("behavior").getAsString(),geometry,file,Collections.unmodifiableMap(animations),loot);
                if(designs.putIfAbsent(id,design)!=null) throw new IllegalStateException("Duplicate boss design");
            }
            if(designs.size()!=27) throw new IllegalStateException("Expected 27 combat designs");
            return Collections.unmodifiableMap(designs);
        }catch(IOException error){ throw new IllegalStateException("Cannot read boss combat designs",error); }
    }
}
