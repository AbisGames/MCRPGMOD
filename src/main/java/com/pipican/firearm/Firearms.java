package com.pipican.firearm;

import com.pipican.Pipican;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Firearms (the Gunslinger's gear category): the pistol, the six-shooter (a pistol modified at a smithing table) and the
 * rifle, all firing bullets. They load like crossbows, so every crossbow mechanic (reload speed, Quick Draw, the
 * cylinder, accuracy, ricochet...) applies to them; on top of that plain pistols and rifles can misfire.
 */
public final class Firearms {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pipican.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Pipican.MOD_ID);

    /** {@code #pipican:weapons/firearms}: pistols, six-shooters, rifles. */
    public static final TagKey<Item> FIREARMS = TagKey.create(Registries.ITEM, Pipican.id("weapons/firearms"));
    /** {@code #pipican:bullets}: what firearms load. */
    public static final TagKey<Item> BULLETS = TagKey.create(Registries.ITEM, Pipican.id("bullets"));

    //                                                                         damage, speed, reload x, misfire, durability
    public static final DeferredItem<FirearmItem> PISTOL = ITEMS.registerItem("pistol",
            p -> new FirearmItem(p, new FirearmItem.Stats(7, 4.0f, 0.8, 0.05)), p -> gun(p, 350));
    public static final DeferredItem<FirearmItem> SIX_SHOOTER = ITEMS.registerItem("six_shooter",
            p -> new FirearmItem(p, new FirearmItem.Stats(7, 4.0f, 0.8, 0.0)), p -> gun(p, 500).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<FirearmItem> RIFLE = ITEMS.registerItem("rifle",
            p -> new FirearmItem(p, new FirearmItem.Stats(12, 5.5f, 1.6, 0.05)), p -> gun(p, 465));
    public static final DeferredItem<BulletItem> BULLET = ITEMS.registerItem("bullet", BulletItem::new, p -> p);

    public static final DeferredHolder<EntityType<?>, EntityType<Bullet>> BULLET_ENTITY = ENTITIES.register("bullet",
            id -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC).noLootTable().sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private static Item.Properties gun(Item.Properties p, int durability) {
        return p.durability(durability).component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
    }

    private Firearms() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(Firearms::addToTabs);
    }

    private static void addToTabs(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.COMBAT) {
            event.accept(PISTOL.get());
            event.accept(SIX_SHOOTER.get());
            event.accept(RIFLE.get());
            event.accept(BULLET.get());
        }
    }
}
