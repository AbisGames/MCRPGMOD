package com.pipican.resource;

import com.pipican.Pipican;
import com.pipican.boss.BossContent;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Separate from the 27 renewable regional nodes. No rare material gates its own pickaxe. */
public final class RareResources {
    public record Deposit(String id,String region,String tool,int minY,int maxY,int chance,int size){
        public boolean matchesBiome(Holder<Biome> biome){
            if(region.equals("end")) return biome.is(BiomeTags.IS_END);
            if(region.equals("deep_ocean")){
                if(biome.is(BiomeTags.IS_DEEP_OCEAN)) return true;
                return biome.unwrapKey().map(key->Set.of("pipican:western_ocean/open_deep_ocean","pipican:western_ocean/abyssal_trench",
                    "pipican:northern_ice_sea/black_ice_trench","pipican:coral_isles/open_tropical_ocean").contains(key.identifier().toString())).orElse(false);
            }
            return biome.unwrapKey().map(key->key.identifier().getNamespace().equals(Pipican.MOD_ID)
                && (key.identifier().getPath().equals(region) || key.identifier().getPath().startsWith(region+"/"))).orElse(false);
        }
        public boolean allows(WorldGenLevel level,net.minecraft.core.BlockPos pos){
            if(pos.getY()<minY || pos.getY()>maxY) return false;
            boolean end=level.getLevel().dimension().equals(Level.END);
            if(region.equals("end")!=end) return false;
            if(!end && !level.getLevel().dimensionType().hasSkyLight()) return false;
            if(id.equals("primordial") && (long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()<1000L*1000L) return false;
            return matchesBiome(level.getBiome(pos));
        }
    }
    public static final List<Deposit> DEPOSITS=List.of(
        new Deposit("soulstone","umbral_chasm","pipican:umbrite_pickaxe",-48,-1,8,4),
        new Deposit("dragonite","end","minecraft:netherite_pickaxe",8,70,12,4),
        new Deposit("abyssium","deep_ocean","pipican:tideglass_pickaxe",-48,0,10,3),
        new Deposit("runestone","greyspine_mountains","pipican:runic_iron_pickaxe",-32,80,8,4),
        new Deposit("astralite","aetheris_expanse","pipican:skycrystal_pickaxe",256,368,8,3),
        new Deposit("sunforged","sunscorch_desert","pipican:sunglass_pickaxe",16,64,10,3),
        new Deposit("moonshard","elderwood","pipican:mooncrystal_pickaxe",-32,32,8,3),
        new Deposit("primordial","end","minecraft:netherite_pickaxe",20,60,32,2));
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(Pipican.MOD_ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Pipican.MOD_ID);
    private static final DeferredRegister<Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,Pipican.MOD_ID);
    public static final Map<String,DeferredBlock<Block>> ORES;
    public static final Map<String,DeferredItem<Item>> MATERIALS;
    public static final Map<String,DeferredItem<Item>> PICKAXES;
    static {
        Map<String,DeferredBlock<Block>> ores=new LinkedHashMap<>(); Map<String,DeferredItem<Item>> materials=new LinkedHashMap<>(),picks=new LinkedHashMap<>();
        for(var deposit:DEPOSITS){
            var ore=BLOCKS.registerSimpleBlock(deposit.id()+"_ore",properties->properties.strength(5,12).requiresCorrectToolForDrops());
            ores.put(deposit.id(),ore); ITEMS.registerSimpleBlockItem(ore);
            materials.put(deposit.id(),ITEMS.registerSimpleItem(deposit.id()));
            FEATURES.register("rare_"+deposit.id(),()->new RareOreFeature(deposit));
            if(deposit.tool().startsWith("pipican:")){
                String name=Identifier.parse(deposit.tool()).getPath();
                picks.computeIfAbsent(name,key->ITEMS.registerSimpleItem(key,properties->properties.pickaxe(ToolMaterial.IRON,1,-2.8f)));
            }
        }
        materials.put("infernium",BossContent.INFERNIUM);
        for(String name:List.of("voidsteel","celestium","bloodcrystal")) materials.put(name,ITEMS.registerSimpleItem(name,properties->properties.fireResistant()));
        ORES=Collections.unmodifiableMap(ores); MATERIALS=Collections.unmodifiableMap(materials); PICKAXES=Collections.unmodifiableMap(picks);
    }
    private RareResources(){}
    public static void register(IEventBus bus){ BLOCKS.register(bus); ITEMS.register(bus); FEATURES.register(bus); }
}
