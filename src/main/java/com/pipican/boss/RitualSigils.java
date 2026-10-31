package com.pipican.boss;

import com.google.gson.JsonParser;
import com.pipican.Pipican;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Shared definition format is ready for more bosses, without enabling unfinished encounters. */
public final class RitualSigils {
    public record Sigil(String item,String boss,int minY,int maxY){}
    private static final List<Sigil> DEFINITIONS=load();
    private RitualSigils(){}
    public static Optional<Sigil> find(ItemStack stack){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return DEFINITIONS.stream().filter(sigil->sigil.item().equals(id)).findFirst();
    }
    private static List<Sigil> load(){
        var input=RitualSigils.class.getResourceAsStream("/assets/pipican/geo/bosses/sigils.json");
        if(input==null) throw new IllegalStateException("Missing sigil definitions");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            var result=new ArrayList<Sigil>();
            for(var element:JsonParser.parseReader(reader).getAsJsonArray()){
                var object=element.getAsJsonObject(); String boss=object.get("boss").getAsString(); BossCatalog.require(boss);
                result.add(new Sigil(object.get("item").getAsString(),boss,object.get("min_y").getAsInt(),object.get("max_y").getAsInt()));
            }
            return List.copyOf(result);
        }catch(IOException error){ throw new IllegalStateException("Cannot load ritual definitions",error); }
    }
}
