package com.pipican.boss;

import com.google.gson.JsonParser;
import com.pipican.Pipican;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Shared metadata for all 27 regions. Metadata alone never registers an unfinished boss. */
public final class BossCatalog {
    public record Definition(String id,String region,String geometry,String style,float scale,String signature){
        public Identifier identifier(){ return Pipican.id(id); }
        public boolean matchesBiome(Identifier biome){
            if(!biome.getNamespace().equals(Pipican.MOD_ID)) return false;
            var path=biome.getPath();
            return path.equals(region) || path.startsWith(region+"/") || path.equals("protected/"+region)
                || region.equals("umbral_chasm") && path.equals("umbral_sanctuary")
                || region.equals("aetheris_expanse") && path.equals("aetheris_sanctuary");
        }
    }
    private static final Map<String,Definition> DEFINITIONS=load();
    public static final Definition MORVATH=require("morvath_ashen_king");
    private BossCatalog(){}
    public static Collection<Definition> all(){ return DEFINITIONS.values(); }
    public static Definition require(String id){
        var definition=DEFINITIONS.get(id);
        if(definition==null) throw new IllegalArgumentException("Unknown boss: "+id);
        return definition;
    }
    private static Map<String,Definition> load(){
        var input=BossCatalog.class.getResourceAsStream("/assets/pipican/geo/bosses/boss_manifest.json");
        if(input==null) throw new IllegalStateException("Missing boss manifest");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            Map<String,Definition> result=new LinkedHashMap<>(); Set<String> regions=new HashSet<>();
            for(var element:JsonParser.parseReader(reader).getAsJsonArray()){
                var entry=element.getAsJsonObject();
                var definition=new Definition(entry.get("id").getAsString(),entry.get("region").getAsString(),
                    entry.get("geometry").getAsString(),entry.get("style").getAsString(),entry.get("scale").getAsFloat(),entry.get("signature").getAsString());
                if(result.putIfAbsent(definition.id(),definition)!=null || !regions.add(definition.region())) throw new IllegalStateException("Duplicate boss or region");
                if(!definition.identifier().toString().equals(entry.get("entity_identifier").getAsString())) throw new IllegalStateException("Boss identifier mismatch");
            }
            if(result.size()!=27) throw new IllegalStateException("Expected 27 boss definitions");
            return Collections.unmodifiableMap(result);
        }catch(IOException error){ throw new IllegalStateException("Cannot read boss manifest",error); }
    }
}
