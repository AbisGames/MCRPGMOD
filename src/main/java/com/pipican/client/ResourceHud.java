package com.pipican.client;

import com.pipican.core.ClassLookup;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.rpgclass.SpecialStat;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * HUD bar for the special-stat resource (mana, rage...). Drawn just left of the hotbar, away from vanilla bars,
 * coloured with the resource's JSON {@code color}. Hidden for classes without a resource.
 */
public final class ResourceHud implements GuiLayer {

    private static final int WIDTH = 60;
    private static final int HEIGHT = 5;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        PlayerClassData data = mc.player.getData(PipicanAttachments.CLASS_DATA);
        if (data.resourceMax() <= 0 || data.activeClass().isEmpty()) {
            return;
        }
        Optional<SpecialStat.Resource> resource = ClassLookup.rpgClass(mc.player.level().registryAccess(), data.activeClass().get())
                .flatMap(RpgClass::specialStat).flatMap(SpecialStat::resource);
        if (resource.isEmpty()) {
            return;
        }

        int hotbarLeft = g.guiWidth() / 2 - 91;
        int x = Math.max(2, hotbarLeft - 6 - WIDTH);
        int y = g.guiHeight() - 10;
        double fraction = Math.clamp(data.resource() / data.resourceMax(), 0, 1);
        g.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF000000);
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF303030);
        g.fill(x, y, x + (int) Math.round(WIDTH * fraction), y + HEIGHT, resource.get().color() | 0xFF000000);

        Component label = Component.literal((int) data.resource() + "/" + (int) data.resourceMax());
        if (mc.options.keyShift.isDown()) {
            label = Component.empty().append(resource.get().name()).append(" ").append(label);
        }
        g.text(mc.font, label, x, y - 11, 0xFFFFFFFF, true);
    }
}
