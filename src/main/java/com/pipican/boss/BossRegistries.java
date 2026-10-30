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
    public static final DeferredHolder<EntityType<?>,EntityType<SkollvarEntity>> SKOLLVAR=ENTITIES.register(
        "skollvar_winterfang",()->EntityType.Builder.<SkollvarEntity>of(SkollvarEntity::new,MobCategory.MONSTER)
            .sized(1.5f,1.8f).clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("skollvar_winterfang"))));
    public static final DeferredHolder<EntityType<?>,EntityType<EldarothEntity>> ELDAROTH=ENTITIES.register(
        "eldaroth_treant",()->EntityType.Builder.<EldarothEntity>of(EldarothEntity::new,MobCategory.MONSTER)
            .sized(2.7f,4.1f).clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("eldaroth_treant"))));
    public static final DeferredHolder<EntityType<?>,EntityType<FallenKingEntity>> FALLEN_KING=ENTITIES.register(
        "fallen_king",()->EntityType.Builder.<FallenKingEntity>of(FallenKingEntity::new,MobCategory.MONSTER)
            .sized(1.5f,3.4f).clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("fallen_king"))));
    public static final DeferredHolder<EntityType<?>,EntityType<KragmorEntity>> KRAGMOR=ENTITIES.register(
        "kragmor_canyon_warlord",()->EntityType.Builder.<KragmorEntity>of(KragmorEntity::new,MobCategory.MONSTER)
            .sized(2.6f,3.8f).clientTrackingRange(12).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("kragmor_canyon_warlord"))));
    public static final DeferredHolder<EntityType<?>,EntityType<NoctharEntity>> NOCTHAR=ENTITIES.register(
        "nocthar_void_sovereign",()->EntityType.Builder.<NoctharEntity>of(NoctharEntity::new,MobCategory.MONSTER)
            .sized(2.2f,3.6f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("nocthar_void_sovereign"))));
    public static final DeferredHolder<EntityType<?>,EntityType<AetherionEntity>> AETHERION=ENTITIES.register(
        "aetherion_sky_guardian",()->EntityType.Builder.<AetherionEntity>of(AetherionEntity::new,MobCategory.MONSTER)
            .sized(2.0f,3.6f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("aetherion_sky_guardian"))));
    public static final DeferredHolder<EntityType<?>,EntityType<VermillionEntity>> VERMILLION=ENTITIES.register(
        "vermillion_blood_monarch",()->EntityType.Builder.<VermillionEntity>of(VermillionEntity::new,MobCategory.MONSTER)
            .sized(1.8f,3.8f).fireImmune().clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("vermillion_blood_monarch"))));
    private BossRegistries(){}
    public static void register(IEventBus bus){ ENTITIES.register(bus); bus.addListener(BossRegistries::attributes); }
    private static void attributes(EntityAttributeCreationEvent event){
        event.put(MORVATH.get(),RegionalBossEntity.attributes().build());
        event.put(FROSTBOUND.get(),FrostboundEntity.frostAttributes().build());
        event.put(SKOLLVAR.get(),SkollvarEntity.attributes().build());
        event.put(ELDAROTH.get(),EldarothEntity.attributes().build());
        event.put(FALLEN_KING.get(),FallenKingEntity.attributes().build());
        event.put(KRAGMOR.get(),KragmorEntity.attributes().build());
        event.put(NOCTHAR.get(),NoctharEntity.attributes().build());
        event.put(AETHERION.get(),AetherionEntity.attributes().build());
        event.put(VERMILLION.get(),VermillionEntity.attributes().build());
    }
}
