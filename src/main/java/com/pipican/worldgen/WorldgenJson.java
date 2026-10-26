package com.pipican.worldgen;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Resource-loader for the canonical authored world files included in this mod jar. */
public final class WorldgenJson {
    private WorldgenJson() {}
    public static JsonObject read(String filename) {
        String path="/data/pipican/worldgen/"+filename;
        try(InputStream stream=WorldgenJson.class.getResourceAsStream(path)){
            if(stream==null)throw new IllegalStateException("Missing worldgen resource: "+path);
            try(Reader reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }catch(IOException e){throw new IllegalStateException("Unable to load "+path,e);}
    }
}
