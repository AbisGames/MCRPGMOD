package com.pipican.client;

import com.pipican.Pipican;
import com.pipican.core.ClassLookup;
import com.pipican.network.OpenClassSelectionPayload;
import com.pipican.network.OpenSkillTreePayload;
import com.pipican.network.OpenTrinketsPayload;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.Affinity;
import com.pipican.trinket.PipicanMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
        NeoForge.EVENT_BUS.addListener(PipicanClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(PipicanClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(PipicanClient::addInventoryButton);
    }

    private static void registerPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenClassSelectionPayload.TYPE, (payload, ctx) -> openClassSelection());
        event.register(OpenSkillTreePayload.TYPE, (payload, ctx) -> Minecraft.getInstance().setScreen(new SkillTreeScreen()));
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
          event.registerEntityRenderer(com.pipican.boss.BossRegistries.AKHMAR.get(),com.pipican.client.boss.AkhmarRenderer::new);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PipicanMenus.TRINKETS.get(), TrinketScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(PipicanKeys.CATEGORY);
        event.register(PipicanKeys.TRINKETS);
        event.register(PipicanKeys.CLASS);
    }

    private static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, Pipican.id("resource_bar"), new ResourceHud());
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
        while (PipicanKeys.CLASS.consumeClick()) {
            if (!mc.player.getData(PipicanAttachments.CLASS_DATA).hasClass()) {
                openClassSelection();
            }
        }
    }

    /** A "Passive Tree" button above the survival inventory, the way into the tree screen (no keybind yet). */
    private static void addInventoryButton(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen screen) {
            event.addListener(Button.builder(Component.translatable("pipican.tree.button"), b -> Minecraft.getInstance().setScreen(new SkillTreeScreen()))
                    .bounds(screen.getGuiLeft() + screen.getImageWidth() - 60, screen.getGuiTop() - 18, 60, 16)
                    .build());
        }
    }

    /** Adds "Affinity: Proficient" (etc.) to item tooltips for the local player's class. */
    private static void onTooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || event.getItemStack().isEmpty()) {
            return;
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
