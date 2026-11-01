package com.pipican.soul;

import com.pipican.Pipican;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Tamashigane soul gear: the Soul Blade and four pieces of soul armor (the plain black kimono). Unbreakable. */
public final class SoulItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pipican.MOD_ID);
    public static final TagKey<Item> GEAR = TagKey.create(Registries.ITEM, Pipican.id("soul/gear"));

    /** Like netherite to start; the stages add the rest (see {@link Soul#bladeBonus}). */
    private static final ToolMaterial STEEL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2031, 9.0f, 4.0f, 15, GEAR);
    /** A plain kimono: 2/5/4/2; the stages add armor and toughness on the wearer. */
    private static final ArmorMaterial ROBE = new ArmorMaterial(37, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5,
            ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 2, ArmorType.BODY, 5), 15, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0f, 0.0f, GEAR,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Pipican.id("soul_robe")));

    public static final DeferredItem<Item> BLADE = ITEMS.registerItem("soul_blade", SoulBladeItem::new, p -> base(p).sword(STEEL, 3.0f, -2.4f));
    public static final DeferredItem<Item> HELMET = ITEMS.registerItem("soul_helmet", Item::new, p -> base(p).humanoidArmor(ROBE, ArmorType.HELMET));
    public static final DeferredItem<Item> CHESTPLATE = ITEMS.registerItem("soul_chestplate", Item::new, p -> base(p).humanoidArmor(ROBE, ArmorType.CHESTPLATE));
    public static final DeferredItem<Item> LEGGINGS = ITEMS.registerItem("soul_leggings", Item::new, p -> base(p).humanoidArmor(ROBE, ArmorType.LEGGINGS));
    public static final DeferredItem<Item> BOOTS = ITEMS.registerItem("soul_boots", Item::new, p -> base(p).humanoidArmor(ROBE, ArmorType.BOOTS));

    public static List<DeferredItem<Item>> armor() {
        return List.of(HELMET, CHESTPLATE, LEGGINGS, BOOTS);
    }

    private static Item.Properties base(Item.Properties p) {
        return p.rarity(Rarity.EPIC).fireResistant().component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    private SoulItems() {}

    /** The blade: use = the path's ability, sneak + use = trial / Bankai / Absolute Form (in {@link Soul#use}). */
    public static final class SoulBladeItem extends Item {
        public SoulBladeItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                Soul.use(sp);
            }
            return InteractionResult.SUCCESS;
        }
    }
}
