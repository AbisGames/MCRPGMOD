package com.pipican.boss;

import com.pipican.Pipican;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Only implemented bosses are registered. The other 26 geometry files remain prototypes. */
public final class BossRegistries {
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,Pipican.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<RegionalBossEntity>> MORVATH=ENTITIES.register(
        "morvath_ashen_king",()->EntityType.Builder.<RegionalBossEntity>of(RegionalBossEntity::new,MobCategory.MONSTER)
            .sized(1.7f,3.7f).fireImmune().clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("morvath_ashen_king"))));
    public static final DeferredHolder<EntityType<?>,EntityType<FrostboundEntity>> FROSTBOUND=ENTITIES.register(
        "frostbound_colossus",()->EntityType.Builder.<FrostboundEntity>of(FrostboundEntity::new,MobCategory.MONSTER)
            .sized(2.4f,3.6f).clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("frostbound_colossus"))));
    private BossRegistries(){}
    public static void register(IEventBus bus){ ENTITIES.register(bus); bus.addListener(BossRegistries::attributes); }
    private static void attributes(EntityAttributeCreationEvent event){
        event.put(MORVATH.get(),RegionalBossEntity.attributes().build());
        event.put(FROSTBOUND.get(),FrostboundEntity.frostAttributes().build());
    }
}
