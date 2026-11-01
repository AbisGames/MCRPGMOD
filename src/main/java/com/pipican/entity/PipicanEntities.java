package com.pipican.entity;

import com.pipican.Pipican;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own entity types. */
public final class PipicanEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER = DeferredRegister.create(Registries.ENTITY_TYPE, Pipican.MOD_ID);

    /** The Engineer's construct. Iron golem sized; its SCALE attribute makes it ~3.5 blocks tall. */
    public static final DeferredHolder<EntityType<?>, EntityType<Eidolon>> EIDOLON = REGISTER.register("eidolon",
            id -> EntityType.Builder.of(Eidolon::new, MobCategory.MISC).sized(1.4f, 2.7f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** The Witch's steerable green fireball (Witch Fire). */
    public static final DeferredHolder<EntityType<?>, EntityType<WitchFireball>> WITCH_FIREBALL = REGISTER.register("witch_fireball",
            id -> EntityType.Builder.<WitchFireball>of(WitchFireball::new, MobCategory.MISC).sized(0.3125f, 0.3125f).clientTrackingRange(4).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private PipicanEntities() {}

    /** Mod-bus listener. */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EIDOLON.get(), Eidolon.createEidolonAttributes().build());
    }
}
