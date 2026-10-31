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
