package com.pipican.client;

import com.pipican.trinket.TrinketLayout;
import com.pipican.trinket.TrinketMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen for {@link TrinketMenu}. The top part is drawn procedurally (labels + slot frames) because
 * its height depends on the class; the bottom reuses the vanilla inventory texture.
 */
public class TrinketScreen extends AbstractContainerScreen<TrinketMenu> {

    private static final Identifier INVENTORY = Identifier.withDefaultNamespace("textures/gui/container/inventory.png");
    /** The player-inventory part of the vanilla texture: from just above its first slot row to the bottom. */
    private static final int INV_TEXTURE_TOP = 83;
    private static final int INV_TEXTURE_HEIGHT = 166 - INV_TEXTURE_TOP;

    public TrinketScreen(TrinketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.inventoryTop - 1 + INV_TEXTURE_HEIGHT);
        this.inventoryLabelY = menu.inventoryTop - 11;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;
        int topHeight = menu.inventoryTop - 1;
        // Top panel (trinket rows + "Inventory" label) in vanilla GUI colours; the texture below has its own border.
        g.fill(x, y, x + imageWidth, y + topHeight, 0xFFC6C6C6);
        g.fill(x, y, x + imageWidth, y + 1, 0xFF555555);
        g.fill(x, y, x + 1, y + topHeight, 0xFF555555);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + topHeight, 0xFF555555);
        // Bottom: vanilla player inventory.
        g.blit(RenderPipelines.GUI_TEXTURED, INVENTORY, x, y + topHeight, 0.0F, INV_TEXTURE_TOP, imageWidth, INV_TEXTURE_HEIGHT, 256, 256);
        // Slot frames.
        for (Slot slot : menu.slots) {
            if (slot instanceof TrinketMenu.TrinketSlot) {
                g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, 0xFF373737);
                g.fill(x + slot.x, y + slot.y, x + slot.x + 17, y + slot.y + 17, 0xFFFFFFFF);
                g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, 0xFF8B8B8B);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        super.extractLabels(g, xm, ym);
        var access = minecraft.player.level().registryAccess();
        int row = 0;
        for (TrinketLayout.Group group : menu.layout().groups()) {
            g.text(font, TrinketLayout.name(access, group.slot()), 8, TrinketMenu.TOP + row * TrinketMenu.ROW_HEIGHT + 5, 0xFF404040, false);
            row += TrinketMenu.rowsFor(group);
        }
    }
}
