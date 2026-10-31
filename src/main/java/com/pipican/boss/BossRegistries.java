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
    public static final DeferredHolder<EntityType<?>,EntityType<LongweiEntity>> LONGWEI=ENTITIES.register(
        "longwei_jade_dragon",()->EntityType.Builder.<LongweiEntity>of(LongweiEntity::new,MobCategory.MONSTER)
            .sized(3.0f,2.6f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("longwei_jade_dragon"))));
    public static final DeferredHolder<EntityType<?>,EntityType<MorgathaEntity>> MORGATHA=ENTITIES.register(
        "morgatha_swamp_queen",()->EntityType.Builder.<MorgathaEntity>of(MorgathaEntity::new,MobCategory.MONSTER)
            .sized(1.9f,3.4f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("morgatha_swamp_queen"))));
    public static final DeferredHolder<EntityType<?>,EntityType<AureliosEntity>> AURELIOS=ENTITIES.register(
        "aurelios_sunken_emperor",()->EntityType.Builder.<AureliosEntity>of(AureliosEntity::new,MobCategory.MONSTER)
            .sized(1.8f,3.5f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("aurelios_sunken_emperor"))));
    public static final DeferredHolder<EntityType<?>,EntityType<LianhuaEntity>> LIANHUA=ENTITIES.register(
        "lianhua_lotus_serpent",()->EntityType.Builder.<LianhuaEntity>of(LianhuaEntity::new,MobCategory.MONSTER)
            .sized(2.6f,2.8f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("lianhua_lotus_serpent"))));
    public static final DeferredHolder<EntityType<?>,EntityType<AkhmarEntity>> AKHMAR=ENTITIES.register(
        "akhmar_sand_emperor",()->EntityType.Builder.<AkhmarEntity>of(AkhmarEntity::new,MobCategory.MONSTER)
            .sized(1.9f,3.6f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("akhmar_sand_emperor"))));
    public static final DeferredHolder<EntityType<?>,EntityType<NetharisEntity>> NETHARIS=ENTITIES.register(
        "netharis_river_guardian",()->EntityType.Builder.<NetharisEntity>of(NetharisEntity::new,MobCategory.MONSTER)
            .sized(2.4f,3.4f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("netharis_river_guardian"))));
    public static final DeferredHolder<EntityType<?>,EntityType<ZahiraEntity>> ZAHIRA=ENTITIES.register(
        "zahira_crimson_corsair",()->EntityType.Builder.<ZahiraEntity>of(ZahiraEntity::new,MobCategory.MONSTER)
            .sized(1.7f,3.3f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("zahira_crimson_corsair"))));
    public static final DeferredHolder<EntityType<?>,EntityType<XaltekEntity>> XALTEK=ENTITIES.register(
        "xaltek_emerald_devourer",()->EntityType.Builder.<XaltekEntity>of(XaltekEntity::new,MobCategory.MONSTER)
            .sized(2.1f,3.1f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("xaltek_emerald_devourer"))));
    public static final DeferredHolder<EntityType<?>,EntityType<CorallithEntity>> CORALLITH=ENTITIES.register(
        "corallith_reef_colossus",()->EntityType.Builder.<CorallithEntity>of(CorallithEntity::new,MobCategory.MONSTER)
            .sized(2.8f,3.7f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("corallith_reef_colossus"))));
    public static final DeferredHolder<EntityType<?>,EntityType<AzharEntity>> AZHAR=ENTITIES.register(
        "azhar_golden_behemoth",()->EntityType.Builder.<AzharEntity>of(AzharEntity::new,MobCategory.MONSTER)
            .sized(2.4f,3.4f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("azhar_golden_behemoth"))));
    public static final DeferredHolder<EntityType<?>,EntityType<NhalkorrEntity>> NHALKORR=ENTITIES.register(
        "nhalkorr_leviathan",()->EntityType.Builder.<NhalkorrEntity>of(NhalkorrEntity::new,MobCategory.MONSTER)
            .sized(3.4f,2.8f).clientTrackingRange(16).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("nhalkorr_leviathan"))));
    public static final DeferredHolder<EntityType<?>,EntityType<HrothgarEntity>> HROTHGAR=ENTITIES.register(
        "hrothgar_drowned_jarl",()->EntityType.Builder.<HrothgarEntity>of(HrothgarEntity::new,MobCategory.MONSTER)
            .sized(1.9f,3.6f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("hrothgar_drowned_jarl"))));
    public static final DeferredHolder<EntityType<?>,EntityType<ValthorEntity>> VALTHOR=ENTITIES.register(
        "valthor_stormbringer",()->EntityType.Builder.<ValthorEntity>of(ValthorEntity::new,MobCategory.MONSTER)
            .sized(2.1f,3.8f).clientTrackingRange(15).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("valthor_stormbringer"))));
    public static final DeferredHolder<EntityType<?>,EntityType<TengriEntity>> TENGRI=ENTITIES.register(
        "tengri_champion",()->EntityType.Builder.<TengriEntity>of(TengriEntity::new,MobCategory.MONSTER)
            .sized(1.8f,3.4f).clientTrackingRange(14).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Pipican.id("tengri_champion"))));
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
        event.put(LONGWEI.get(),LongweiEntity.attributes().build());
        event.put(MORGATHA.get(),MorgathaEntity.attributes().build());
        event.put(AURELIOS.get(),AureliosEntity.attributes().build());
        event.put(LIANHUA.get(),LianhuaEntity.attributes().build());
        event.put(AKHMAR.get(),AkhmarEntity.attributes().build());
        event.put(NETHARIS.get(),NetharisEntity.attributes().build());
        event.put(ZAHIRA.get(),ZahiraEntity.attributes().build());
        event.put(XALTEK.get(),XaltekEntity.attributes().build());
        event.put(CORALLITH.get(),CorallithEntity.attributes().build());
        event.put(AZHAR.get(),AzharEntity.attributes().build());
        event.put(NHALKORR.get(),NhalkorrEntity.attributes().build());
        event.put(HROTHGAR.get(),HrothgarEntity.attributes().build());
        event.put(VALTHOR.get(),ValthorEntity.attributes().build());
        event.put(TENGRI.get(),TengriEntity.attributes().build());
    }
}
