package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.blacksmith.BlacksmithUpgrades;
import com.pipican.blacksmith.BlacksmithUpgrades.*;
import com.pipican.boss.BossContent;
import com.pipican.worldgen.RegionalOreRegistries;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=Pipican.MOD_ID)
public final class BlacksmithTests {
    private static final Map<String,Consumer<GameTestHelper>> TESTS=Map.of("blacksmith_transaction",BlacksmithTests::transaction,
        "blacksmith_failures",BlacksmithTests::failures,"blacksmith_evolution",BlacksmithTests::evolution);
    @SubscribeEvent public static void functions(RegisterEvent event){ event.register(Registries.TEST_FUNCTION,registry->TESTS.forEach((name,fn)->registry.register(Pipican.id(name),fn))); }
    @SubscribeEvent public static void tests(RegisterGameTestsEvent event){
        Holder<TestEnvironmentDefinition<?>> env=event.registerEnvironment(Pipican.id("blacksmith_test"),new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name->event.registerTest(Pipican.id(name),new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION,Pipican.id(name)),
            new TestData<>(env,net.minecraft.resources.Identifier.withDefaultNamespace("empty"),100,0,true))));
    }
    private static ServerPlayer player(GameTestHelper helper){
        return net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"blacksmith-test"));
    }
    private static ItemStack gear(GameTestHelper helper){
        var stack=new ItemStack(Items.IRON_CHESTPLATE); stack.setDamageValue(17);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Preserved equipment"));
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putString("other_contributor","keep this metadata");
        stack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var protection=helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        stack.enchant(protection,3);
        var modifiers=stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS,modifiers.withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,
            new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("compat","kept"),2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.CHEST));
        return stack;
    }
    private static Request request(ServerPlayer player,Action action){
        var stack=player.getInventory().getItem(0);
        return new Request(UUID.randomUUID(),0,State.read(stack).revision(),stack,action,"ashlands");
    }
    private static void preserved(GameTestHelper helper,ItemStack original,ItemStack current){
        helper.assertTrue(current.getDamageValue()==17 && Objects.equals(original.get(DataComponents.CUSTOM_NAME),current.get(DataComponents.CUSTOM_NAME))
            && Objects.equals(original.get(DataComponents.ENCHANTMENTS),current.get(DataComponents.ENCHANTMENTS)),"upgrade lost damage/name/enchantments");
        helper.assertTrue(current.get(DataComponents.CUSTOM_DATA).copyTag().getStringOr("other_contributor","").equals("keep this metadata"),"upgrade overwrote contributor data");
        helper.assertTrue(current.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().stream().anyMatch(entry->entry.modifier().id().toString().equals("compat:kept") && entry.modifier().amount()==2),"upgrade lost another system's attribute modifier");
    }
    private static void transaction(GameTestHelper helper){
        var player=player(helper); var inventory=player.getInventory(); var original=gear(helper); inventory.setItem(0,original.copy());
        inventory.setItem(1,new ItemStack(RegionalOreRegistries.MATERIALS.get("cinderite").get(),10));
        var request=request(player,Action.UPGRADE);
        helper.assertTrue(BlacksmithUpgrades.execute(player,request)==Result.SUCCESS,"valid upgrade failed");
        var current=inventory.getItem(0); preserved(helper,original,current);
        helper.assertTrue(State.read(current).level()==1 && State.read(current).revision()==1 && inventory.getItem(1).getCount()==7,"wrong level/revision/material consumption");
        helper.assertTrue(BlacksmithUpgrades.execute(player,request)==Result.DUPLICATE_REQUEST && inventory.getItem(1).getCount()==7,"duplicate request spent materials twice");
        helper.assertTrue(BlacksmithUpgrades.execute(player,new Request(UUID.randomUUID(),0,0,original,Action.UPGRADE,"ashlands"))==Result.STALE_ITEM,"replayed old item snapshot accepted");
        helper.assertTrue(BlacksmithUpgrades.execute(player,new Request(UUID.randomUUID(),0,0,current,Action.UPGRADE,"ashlands"))==Result.STALE_REVISION && inventory.getItem(1).getCount()==7,"stale revision accepted");
        helper.succeed();
    }
    private static void failures(GameTestHelper helper){
        var player=player(helper); var inventory=player.getInventory(); var original=gear(helper); inventory.setItem(0,original.copy());
        helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.UPGRADE))==Result.INSUFFICIENT_MATERIALS && ItemStack.matches(inventory.getItem(0),original),"insufficient ingredients damaged or replaced equipment");
        helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.EVOLVE))==Result.EVOLUTION_UNAVAILABLE && ItemStack.matches(inventory.getItem(0),original),"early evolution changed equipment");
        inventory.setItem(0,new ItemStack(Items.STICK)); var before=inventory.getItem(0).copy();
        helper.assertTrue(BlacksmithUpgrades.execute(player,new Request(UUID.randomUUID(),0,0,before,Action.UPGRADE,"ashlands"))==Result.INVALID_ITEM && ItemStack.matches(before,inventory.getItem(0)),"invalid item was consumed");
        inventory.setItem(0,original.copy());
        helper.assertTrue(BlacksmithUpgrades.execute(player,new Request(UUID.randomUUID(),0,0,original,Action.UPGRADE,"unfinished-region"))==Result.UNKNOWN_PROFILE,"unfinished regional profile enabled");
        helper.succeed();
    }
    private static void evolution(GameTestHelper helper){
        var player=player(helper); var inventory=player.getInventory(); var original=gear(helper); inventory.setItem(0,original.copy());
        for(int slot=1;slot<=30;slot++) inventory.setItem(slot,new ItemStack(RegionalOreRegistries.MATERIALS.get("cinderite").get(),64));
        inventory.setItem(31,new ItemStack(BossContent.HEART.get(),64)); inventory.setItem(32,new ItemStack(BossContent.INFERNIUM.get(),64));
        for(var tier:Tier.values()){
            for(int level=1;level<=10;level++){
                helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.UPGRADE))==Result.SUCCESS,"upgrade failed at "+tier+" +"+level);
                var state=State.read(inventory.getItem(0));
                helper.assertTrue(state.level()==level && state.tier()==tier && state.set().equals("ashlands/"+tier.name().toLowerCase(Locale.ROOT)),"tier/set metadata mismatch");
                preserved(helper,original,inventory.getItem(0));
            }
            if(tier!=Tier.MYTHIC){
                helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.EVOLVE))==Result.SUCCESS,"valid evolution failed");
                helper.assertTrue(State.read(inventory.getItem(0)).level()==0,"evolution did not reset upgrade level");
            }
        }
        var completed=inventory.getItem(0).copy(); var materials=inventory.getItem(1).copy();
        helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.UPGRADE))==Result.MAX_LEVEL,"+10 cap bypass");
        helper.assertTrue(BlacksmithUpgrades.execute(player,request(player,Action.EVOLVE))==Result.EVOLUTION_UNAVAILABLE,"Mythic evolution cap bypass");
        helper.assertTrue(ItemStack.matches(completed,inventory.getItem(0)) && ItemStack.matches(materials,inventory.getItem(1)),"capped request consumed materials or gear");
        helper.assertTrue(State.read(completed).revision()==43,"revision reset across evolution; old requests could replay");
        helper.succeed();
    }
}
