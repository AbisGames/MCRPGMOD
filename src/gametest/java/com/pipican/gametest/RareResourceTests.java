package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.resource.*;
import com.pipican.worldgen.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class RareResourceTests {
    private static final Map<String,Consumer<GameTestHelper>> TESTS=Map.of(
        "rare_resource_catalog",RareResourceTests::catalog,
        "rare_mining_enforcement",RareResourceTests::mining,
        "rare_feature_safety",RareResourceTests::safety,
        "rare_native_generation",RareResourceTests::nativeGeneration);
    @SubscribeEvent public static void functions(RegisterEvent event){ event.register(Registries.TEST_FUNCTION,registry->TESTS.forEach((name,fn)->registry.register(Pipican.id(name),fn))); }
    @SubscribeEvent public static void tests(RegisterGameTestsEvent event){
        Holder<TestEnvironmentDefinition<?>> env=event.registerEnvironment(Pipican.id("rare_resource_test"),new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name->{
            if(name.equals("rare_native_generation") && !Boolean.getBoolean("pipican.rareGenerationTests")) return;
            event.registerTest(Pipican.id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,Pipican.id(name)),
                new TestData<>(env,net.minecraft.resources.Identifier.withDefaultNamespace("empty"),2400,0,true)));
        });
    }
    private static void catalog(GameTestHelper helper){
        helper.assertTrue(RareResources.MATERIALS.size()==12 && RareResources.ORES.size()==8 && RareResources.PICKAXES.size()==6,"rare resource registration counts");
        helper.assertTrue(RegionalOreRegistries.NODES.size()==27,"regional ore registry changed");
        var level=helper.getLevel();
        for(var deposit:RareResources.DEPOSITS){
            helper.assertTrue(level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(ResourceKey.create(Registries.PLACED_FEATURE,Pipican.id("rare_"+deposit.id()))).isPresent(),"missing placed rare feature");
            var state=RareResources.ORES.get(deposit.id()).get().defaultBlockState();
            var tool=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(deposit.tool())));
            helper.assertTrue(tool.isCorrectToolForDrops(state),"required pickaxe cannot harvest "+deposit.id());
            var drops=Block.getDrops(state,level,helper.absolutePos(new BlockPos(2,1,2)),null,null,tool);
            helper.assertTrue(drops.size()==1 && drops.getFirst().is(RareResources.MATERIALS.get(deposit.id()).get()),"rare loot mismatch: "+deposit.id());
            helper.assertTrue(Block.getDrops(state,level,helper.absolutePos(new BlockPos(2,1,2)),null,null,ItemStack.EMPTY).isEmpty(),"explosion/empty-tool bypass");
            if(deposit.tool().startsWith("pipican:")){
                var recipe=level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,net.minecraft.resources.Identifier.parse(deposit.tool()))).orElseThrow();
                var shaped=(net.minecraft.world.item.crafting.ShapedRecipe)recipe.value();
                var material=RegionalOreRegistries.MATERIALS.get(net.minecraft.resources.Identifier.parse(deposit.tool()).getPath().replace("_pickaxe","")).get();
                var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,List.of(new ItemStack(material),new ItemStack(material),new ItemStack(material),ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY,ItemStack.EMPTY,new ItemStack(Items.STICK),ItemStack.EMPTY));
                helper.assertTrue(shaped.matches(input,level) && shaped.assemble(input).is(tool.getItem()),"pickaxe progression cycle or invalid recipe");
            }
        }
        helper.succeed();
    }
    private static void mining(GameTestHelper helper){
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(1600,20,1600));
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.snapTo(pos.getX(),pos.getY()+1,pos.getZ());
        for(var deposit:RareResources.DEPOSITS){
            level.setBlock(pos,RareResources.ORES.get(deposit.id()).get().defaultBlockState(),2);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            helper.assertTrue(!player.gameMode.destroyBlock(pos) && level.getBlockState(pos).is(RareResources.ORES.get(deposit.id()).get()),"wrong pickaxe broke "+deposit.id());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(deposit.tool()))));
            helper.assertTrue(player.gameMode.destroyBlock(pos) && level.getBlockState(pos).isAir(),"correct pickaxe was rejected: "+deposit.id());
        }
        helper.succeed();
    }
    private static void safety(GameTestHelper helper){
        var level=helper.getLevel(); var pos=helper.absolutePos(new BlockPos(1800,100,1800));
        var protection=PlayerWorldgenProtection.get(level);
        var biome=level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(ResourceKey.create(Registries.BIOME,Pipican.id("greyspine_mountains/jagged_peaks")));
        for(int x=-8;x<=8;x+=8) for(int z=-8;z<=8;z+=8) level.getChunkAt(pos.offset(x,0,z));
        helper.assertTrue(net.minecraft.server.commands.FillBiomeCommand.fill(level,pos.offset(-8,-8,-8),pos.offset(8,8,8),biome).left().isPresent(),"rare fixture biome fill failed");
        var deposit=RareResources.DEPOSITS.stream().filter(d->d.id().equals("runestone")).findFirst().orElseThrow();
        var feature=new RareOreFeature(deposit);
        java.util.function.BooleanSupplier place=()->feature.place(new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(0),pos,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE));
        level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);
        helper.assertTrue(place.getAsBoolean() && level.getBlockState(pos).is(RareResources.ORES.get("runestone").get()),"positive rare feature fixture failed");
        level.setBlock(pos,Blocks.STONE.defaultBlockState(),2); protection.protect(pos);
        place.getAsBoolean(); helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE),"rare generation overwrote player provenance");
        for(var block:List.of(Blocks.CHEST,Blocks.GOLD_BLOCK)){
            level.setBlock(pos,block.defaultBlockState(),2); place.getAsBoolean(); helper.assertTrue(level.getBlockState(pos).is(block),"rare generation overwrote artificial/inventory block");
        }
        helper.succeed();
    }
    private static void nativeGeneration(GameTestHelper helper){ RareGenerationTests.verify(helper); helper.succeed(); }
}
