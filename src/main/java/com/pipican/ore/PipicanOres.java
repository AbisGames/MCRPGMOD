package com.pipican.ore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pipican.Pipican;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's ores: blocks, items and the creative tab, registered from {@code pipican/ores.json} (generated from
 * {@code tools/ore_builder/catalog.py}, so adding an ore needs no Java). Every ore has a tier (T1 common to T9 unique);
 * the textures are grayscale and tinted with the ore's colour ({@link #color}).
 */
public final class PipicanOres {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pipican.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Pipican.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Pipican.MOD_ID);

    /** One catalog entry: its tier, form ("metal"/"gem"), use ("set", "special", ...) and tint colour (ARGB). */
    public record Ore(String id, int tier, String form, String use, int color) {}

    public static final List<Ore> ORES = new ArrayList<>();
    /** Every block and item id (path) the ores own -> the ore. */
    private static final Map<String, Ore> BY_PATH = new HashMap<>();
    /** Blocks drawn with a tint (all of them), with their colour. */
    public static final List<Map.Entry<DeferredBlock<Block>, Integer>> TINTED = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("ores", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pipican.ores"))
            .icon(() -> TAB_ITEMS.isEmpty() ? ItemStack.EMPTY : new ItemStack(TAB_ITEMS.getFirst().get()))
            .displayItems((params, output) -> TAB_ITEMS.forEach(item -> output.accept(item.get())))
            .build());

    static {
        for (JsonElement element : load()) {
            register(element.getAsJsonObject());
        }
    }

    private PipicanOres() {}

    private static JsonArray load() {
        try (InputStream in = PipicanOres.class.getResourceAsStream("/pipican/ores.json")) {
            if (in == null) {
                throw new IllegalStateException("pipican/ores.json is missing - run tools/ore_builder/build_ores.py");
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("can't read pipican/ores.json", e);
        }
    }

    private static void register(JsonObject json) {
        Ore ore = new Ore(json.get("id").getAsString(), json.get("tier").getAsInt(), json.get("form").getAsString(),
                json.get("use").getAsString(), json.get("color").getAsInt());
        ORES.add(ore);
        int tier = ore.tier();
        int light = json.get("light").getAsInt();
        boolean gem = ore.form().equals("gem");
        Rarity rarity = tier >= 8 ? Rarity.EPIC : tier >= 6 ? Rarity.RARE : tier >= 4 ? Rarity.UNCOMMON : Rarity.COMMON;

        for (JsonElement e : json.getAsJsonArray("ores")) {
            JsonObject variant = e.getAsJsonObject();
            String id = variant.get("id").getAsString();
            String base = variant.get("base").getAsString();
            boolean deepslate = base.equals("deepslate");
            float hardness = 3.0f + 0.5f * (tier - 1) + (deepslate ? 1.5f : 0);
            Supplier<BlockBehaviour.Properties> props = () -> BlockBehaviour.Properties.of()
                    .mapColor(switch (base) {
                        case "deepslate" -> MapColor.DEEPSLATE;
                        case "nether" -> MapColor.NETHER;
                        case "end" -> MapColor.SAND;
                        default -> MapColor.STONE;
                    })
                    .strength(hardness, 3.0f + tier)
                    .requiresCorrectToolForDrops()
                    .sound(switch (base) {
                        case "deepslate" -> SoundType.DEEPSLATE;
                        case "nether" -> SoundType.NETHER_ORE;
                        default -> SoundType.STONE;
                    })
                    .lightLevel(state -> light);
            // Gem ores give experience when mined (like diamonds); metal ores when their raw ore is smelted.
            DeferredBlock<Block> block = gem
                    ? BLOCKS.registerBlock(id, p -> new DropExperienceBlock(UniformInt.of(tier, 2 + 2 * tier), p), props)
                    : BLOCKS.registerBlock(id, Block::new, props);
            blockItem(ore, id, block, rarity);
        }
        for (JsonElement e : json.getAsJsonArray("items")) {
            String id = e.getAsJsonObject().get("id").getAsString();
            DeferredItem<Item> item = ITEMS.registerSimpleItem(id, () -> itemProps(tier, rarity));
            TAB_ITEMS.add(item);
            BY_PATH.put(id, ore);
        }
        if (json.has("set")) {
            registerSet(ore, json.getAsJsonObject("set"), rarity);
        }
        for (JsonElement e : json.getAsJsonArray("storage")) {
            JsonObject storage = e.getAsJsonObject();
            String id = storage.get("id").getAsString();
            boolean raw = storage.get("kind").getAsString().equals("raw");
            DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(id, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0f, 6.0f + tier)
                    .requiresCorrectToolForDrops()
                    .sound(raw ? SoundType.STONE : gem ? SoundType.AMETHYST : SoundType.METAL)
                    .lightLevel(state -> light));
            blockItem(ore, id, block, rarity);
        }
    }

    /** Armor, weapons and tools of a set ore, with the tier's (and style's) numbers from the generator. */
    private static void registerSet(Ore ore, JsonObject set, Rarity rarity) {
        String id = ore.id();
        java.util.Set<String> traits = new java.util.HashSet<>();
        set.getAsJsonArray("traits").forEach(t -> traits.add(t.getAsString()));
        net.minecraft.tags.TagKey<Item> repair = net.minecraft.tags.TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.parse(set.get("repair").getAsString()));
        var tool = new net.minecraft.world.item.ToolMaterial(
                net.minecraft.tags.TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.parse(set.get("incorrect").getAsString())),
                set.get("tool_durability").getAsInt(), set.get("speed").getAsFloat(), set.get("damage").getAsFloat(),
                set.get("enchantability").getAsInt(), repair);
        var defense = set.getAsJsonArray("defense");
        var armor = new net.minecraft.world.item.equipment.ArmorMaterial(set.get("armor_durability").getAsInt(),
                java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET, defense.get(0).getAsInt(),
                        net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, defense.get(1).getAsInt(),
                        net.minecraft.world.item.equipment.ArmorType.LEGGINGS, defense.get(2).getAsInt(),
                        net.minecraft.world.item.equipment.ArmorType.BOOTS, defense.get(3).getAsInt(),
                        net.minecraft.world.item.equipment.ArmorType.BODY, defense.get(1).getAsInt()),
                set.get("enchantability").getAsInt(),
                switch (set.get("weight").getAsString()) {
                    case "light" -> net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN;
                    case "heavy" -> net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE;
                    default -> net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON;
                },
                set.get("toughness").getAsFloat(), set.get("knockback_resistance").getAsFloat(), repair,
                ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Pipican.id(id)));
        float swing = set.get("swing").getAsFloat();
        float dmg = set.get("damage").getAsFloat();
        for (JsonElement k : set.getAsJsonArray("kinds")) {
            String kind = k.getAsString();
            String name = id + "_" + kind;
            java.util.function.UnaryOperator<Item.Properties> traitsOf = p -> {
                p = itemProps(ore.tier(), rarity);
                if (traits.contains("unbreakable")) {
                    p = p.component(net.minecraft.core.component.DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);
                }
                return p;
            };
            DeferredItem<? extends Item> item = switch (kind) {
                case "helmet" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).humanoidArmor(armor, net.minecraft.world.item.equipment.ArmorType.HELMET));
                case "chestplate" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).humanoidArmor(armor, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
                case "leggings" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).humanoidArmor(armor, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
                case "boots" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).humanoidArmor(armor, net.minecraft.world.item.equipment.ArmorType.BOOTS));
                case "sword" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).sword(tool, 3.0f, -2.4f + swing));
                case "axe" -> ITEMS.registerItem(name, p -> new net.minecraft.world.item.AxeItem(tool, 5.5f, -3.0f + swing / 2, p), traitsOf);
                case "spear" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).spear(tool, 1.0f, 1.0f, 0.5f, 2.75f, 10.5f, 6.5f, 5.1f, 10.5f, 4.6f));
                case "pickaxe" -> ITEMS.registerItem(name, Item::new, p -> traitsOf.apply(p).pickaxe(tool, 1.0f, -2.8f));
                case "shovel" -> ITEMS.registerItem(name, p -> new net.minecraft.world.item.ShovelItem(tool, 1.5f, -3.0f, p), traitsOf);
                case "hoe" -> ITEMS.registerItem(name, p -> new net.minecraft.world.item.HoeItem(tool, -dmg, 0.0f, p), traitsOf);
                default -> throw new IllegalStateException("unknown gear kind " + kind);
            };
            TAB_ITEMS.add(item);
            BY_PATH.put(name, ore);
        }
    }

    private static void blockItem(Ore ore, String id, DeferredBlock<Block> block, Rarity rarity) {
        TINTED.add(Map.entry(block, ore.color()));
        TAB_ITEMS.add(ITEMS.registerSimpleBlockItem(id, block, () -> itemProps(ore.tier(), rarity)));
        BY_PATH.put(id, ore);
    }

    /** T6 and up survive fire and lava, like netherite. */
    private static Item.Properties itemProps(int tier, Rarity rarity) {
        Item.Properties props = new Item.Properties().rarity(rarity);
        return tier >= 6 ? props.fireResistant() : props;
    }

    /** The ore a block or item of this mod belongs to (by registry path), if any. */
    public static Optional<Ore> of(String path) {
        return Optional.ofNullable(BY_PATH.get(path));
    }
}
