package com.pipican.client;

import com.pipican.Pipican;
import com.pipican.core.ClassLookup;
import com.pipican.entity.PipicanEntities;
import com.pipican.network.OpenClassSelectionPayload;
import com.pipican.network.OpenSkillTreePayload;
import com.pipican.network.OpenTrinketsPayload;
import com.pipican.network.UseAbilityPayload;
import com.pipican.network.OpenBlacksmithPayload;
import com.pipican.network.RequestOpenBlacksmithPayload;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.Affinity;
import com.pipican.trinket.PipicanMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Client-only entry point: screens, HUD, keys, tooltips and client packet handlers. */
@Mod(value = Pipican.MOD_ID, dist = Dist.CLIENT)
public final class PipicanClient {

    public PipicanClient(IEventBus modBus) {
        modBus.addListener(PipicanClient::registerPayloadHandlers);
        modBus.addListener(PipicanClient::registerScreens);
        modBus.addListener(PipicanClient::registerBossRenderers);
        modBus.addListener(PipicanClient::registerKeys);
        modBus.addListener(PipicanClient::registerHud);
        modBus.addListener(PipicanClient::registerRenderers);
        modBus.addListener(PipicanClient::registerBlockTints);
        NeoForge.EVENT_BUS.addListener(PipicanClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(MovementClient::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(MovementClient::onNameTag);
        NeoForge.EVENT_BUS.addListener(PipicanClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(PipicanClient::addInventoryButton);
    }

    /** Ore blocks are grayscale; each is tinted with its ore's colour (tint index 0). */
    private static void registerBlockTints(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        for (var entry : com.pipican.ore.PipicanOres.TINTED) {
            int color = entry.getValue();
            event.register(java.util.List.of(state -> color), entry.getKey().get());
        }
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenClassSelectionPayload.TYPE, (payload, ctx) -> openClassSelection());
        event.register(OpenSkillTreePayload.TYPE, (payload, ctx) -> Minecraft.getInstance().setScreen(new SkillTreeScreen()));
        event.register(OpenBlacksmithPayload.TYPE, (payload, ctx) -> Minecraft.getInstance().setScreen(new BlacksmithScreen()));
    }

    private static void registerBossRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.MORVATH.get(),com.pipican.client.boss.RegionalBossRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.FROSTBOUND.get(),com.pipican.client.boss.FrostboundRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.SKOLLVAR.get(),com.pipican.client.boss.SkollvarRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.ELDAROTH.get(),com.pipican.client.boss.EldarothRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.FALLEN_KING.get(),com.pipican.client.boss.FallenKingRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.KRAGMOR.get(),com.pipican.client.boss.KragmorRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.NOCTHAR.get(),com.pipican.client.boss.NoctharRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.AETHERION.get(),com.pipican.client.boss.AetherionRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.VERMILLION.get(),com.pipican.client.boss.VermillionRenderer::new);
        event.registerEntityRenderer(com.pipican.boss.BossRegistries.LONGWEI.get(),com.pipican.client.boss.LongweiRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.MORGATHA.get(),com.pipican.client.boss.MorgathaRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.AURELIOS.get(),com.pipican.client.boss.AureliosRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.LIANHUA.get(),com.pipican.client.boss.LianhuaRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.AKHMAR.get(),com.pipican.client.boss.AkhmarRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.NETHARIS.get(),com.pipican.client.boss.NetharisRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.ZAHIRA.get(),com.pipican.client.boss.ZahiraRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.XALTEK.get(),com.pipican.client.boss.XaltekRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.CORALLITH.get(),com.pipican.client.boss.CorallithRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.AZHAR.get(),com.pipican.client.boss.AzharRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.NHALKORR.get(),com.pipican.client.boss.NhalkorrRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.HROTHGAR.get(),com.pipican.client.boss.HrothgarRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.VALTHOR.get(),com.pipican.client.boss.ValthorRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.TENGRI.get(),com.pipican.client.boss.TengriRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.THALASSOR.get(),com.pipican.client.boss.ThalassorRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.DURMAGON.get(),com.pipican.client.boss.DurmagonRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.SYLTHERA.get(),com.pipican.client.boss.SyltheraRenderer::new);
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.LUMYNARA.get(),com.pipican.client.boss.LumynaraRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PipicanMenus.TRINKETS.get(), TrinketScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(PipicanKeys.CATEGORY);
        event.register(PipicanKeys.TRINKETS);
        event.register(PipicanKeys.CLASS);
        for (var key : PipicanKeys.ABILITIES) {
            event.register(key);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The eidolon reuses the iron golem model and texture; its SCALE attribute makes it bigger.
        event.registerEntityRenderer(PipicanEntities.EIDOLON.get(), IronGolemRenderer::new);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Pipican.id("resource_bar"), new ResourceHud());
        event.registerAbove(VanillaGuiLayers.HOTBAR, Pipican.id("abilities"), new AbilityHud());
    }

    private static void openClassSelection() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new ClassSelectionScreen(mc.player.level().registryAccess()));
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (PipicanKeys.TRINKETS.consumeClick()) {
            ClientPacketDistributor.sendToServer(OpenTrinketsPayload.INSTANCE);
        }
        for (int i = 0; i < PipicanKeys.ABILITIES.length; i++) {
            while (PipicanKeys.ABILITIES[i].consumeClick()) {
                ClientPacketDistributor.sendToServer(new UseAbilityPayload(i));
            }
        }
        while (PipicanKeys.CLASS.consumeClick()) {
            if (!mc.player.getData(PipicanAttachments.CLASS_DATA).hasClass()) {
                openClassSelection();
            }
        }
    }

    /** A "Passive Tree" button above the survival inventory, the way into the tree screen (no keybind yet). */
    private static void addInventoryButton(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(Button.builder(Component.translatable("blacksmith.pipican.button"), b ->
                            ClientPacketDistributor.sendToServer(RequestOpenBlacksmithPayload.INSTANCE))
                    .bounds(screen.getLeftPos() + 4, screen.getTopPos() - 18, 72, 16)
                    .build());
            event.addListener(Button.builder(Component.translatable("pipican.tree.button"), b -> Minecraft.getInstance().setScreen(new SkillTreeScreen()))
                    .bounds(screen.getLeftPos() + screen.getImageWidth() - 60, screen.getTopPos() - 18, 60, 16)
                    .build());
        }
    }

    /** Adds "Affinity: Proficient" (etc.) and stat requirements ("Needs 15 Strength") to item tooltips. */
    private static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || event.getItemStack().isEmpty()) {
            return;
        }
        // Ores: their tier and what they're for.
        var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (itemId.getNamespace().equals(com.pipican.Pipican.MOD_ID)) {
            com.pipican.ore.PipicanOres.of(itemId.getPath()).ifPresent(ore -> {
                event.getToolTip().add(Component.translatable("pipican.ore.tier." + ore.tier()).withStyle(net.minecraft.ChatFormatting.GOLD));
                // An item with its own description (Mercury) uses it; otherwise the ore's.
                String own = "pipican.ore." + itemId.getPath() + ".note";
                event.getToolTip().add(Component.translatable(net.minecraft.client.resources.language.I18n.exists(own) ? own : "pipican.ore." + ore.id() + ".note")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                if (com.pipican.ore.SpecialOreEvents.materialOf(itemId.getPath()).isPresent()) {
                    for (String use : new String[] {"pipican.coating." + ore.id() + ".slot", "pipican.coating." + ore.id(), "pipican.focus." + ore.id()}) {
                        if (net.minecraft.client.resources.language.I18n.exists(use)) {
                            event.getToolTip().add(Component.translatable(use).withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
                        }
                    }
                }
            });
        }
        // Coated gear: which ore, and what it does.
        com.pipican.ore.SpecialOreEvents.coating(event.getItemStack()).ifPresent(ore -> {
            event.getToolTip().add(Component.translatable("pipican.coating.tooltip",
                    new net.minecraft.world.item.ItemStack(com.pipican.ore.PipicanOres.material(ore).get()).getHoverName()).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
            event.getToolTip().add(Component.translatable("pipican.coating." + ore).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        });
        for (var stat : ClassLookup.stats(event.getEntity().level().registryAccess())) {
            for (var req : stat.gear()) {
                if (event.getItemStack().is(req.items())) {
                    event.getToolTip().add(Component.translatable("pipican.stat.requires", req.min(), stat.name())
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        }
        PlayerClassData data = event.getEntity().getData(PipicanAttachments.CLASS_DATA);
        data.activeClass()
                .flatMap(key -> ClassLookup.rpgClass(event.getEntity().level().registryAccess(), key))
                .ifPresent(cls -> {
                    Affinity affinity = cls.affinities().resolve(event.getItemStack());
                    if (affinity != Affinity.NEUTRAL) {
                        event.getToolTip().add(Component.translatable("pipican.affinity.tooltip",
                                Component.translatable(affinity.translationKey()).withStyle(affinity.color())));
                    }
                });
    }
}
