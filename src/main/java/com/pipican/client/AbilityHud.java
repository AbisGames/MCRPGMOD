package com.pipican.client;

import com.pipican.ability.Ability;
import com.pipican.ability.AbilitySlots;
import com.pipican.core.ClassLookup;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * The four ability slots, right of the hotbar: icon, key, a dark overlay that shrinks as the
 * cooldown runs out, and a gold frame (with time left) while a state or toggle is active.
 * Everything is read from the synced class data, so it matches what the server allows.
 */
public final class AbilityHud implements GuiLayer {

    private static final int SIZE = 20;

    @Override
    public void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) {
            return;
        }
        PlayerClassData data = mc.player.getData(PipicanAttachments.CLASS_DATA);
        var access = mc.player.level().registryAccess();
        List<ResourceKey<Ability>> slots = AbilitySlots.slots(access, data);
        if (slots.isEmpty()) {
            return;
        }
        long now = mc.player.level().getGameTime();
        // Right of the hotbar, leaving room for the vanilla off-hand slot / attack indicator.
        int x = g.guiWidth() / 2 + 91 + 26;
        int y = g.guiHeight() - SIZE - 2;
        for (int i = 0; i < slots.size(); i++) {
            Ability ability = ClassLookup.abilities(access).getValue(slots.get(i));
            int sx = x + i * (SIZE + 2);
            var active = data.abilityActiveUntil(slots.get(i));
            g.fill(sx - 1, y - 1, sx + SIZE + 1, y + SIZE + 1, active.isPresent() ? 0xFFFFC000 : 0xFF000000);
            g.fill(sx, y, sx + SIZE, y + SIZE, 0xC0202020);
            g.item(new ItemStack(ability.icon()), sx + 2, y + 2);

            long left = data.abilityReadyAt(slots.get(i)) - now;
            if (left > 0 && ability.cooldownTicks() > 0) {
                int h = (int) Math.ceil(SIZE * Math.min(1.0, left / (double) ability.cooldownTicks()));
                g.fill(sx, y + SIZE - h, sx + SIZE, y + SIZE, 0xA0000000);
                String secs = String.valueOf((left + 19) / 20);
                g.text(mc.font, secs, sx + SIZE - mc.font.width(secs), y + SIZE - 8, 0xFFFFFFFF, true);
            } else if (data.resource() < ability.cost()) {
                g.fill(sx, y, sx + SIZE, y + SIZE, 0x803050A0);
            }
            if (active.isPresent() && active.get() != Long.MAX_VALUE) {
                String secs = String.valueOf(Math.max(0, (active.get() - now + 19) / 20));
                g.text(mc.font, secs, sx + 1, y + 1, 0xFFFFC000, true);
            }
            String key = PipicanKeys.ABILITIES[i].getTranslatedKeyMessage().getString();
            g.text(mc.font, key.length() > 2 ? key.substring(0, 2) : key, sx + 1, y - 9, 0xFFAAAAAA, true);
        }
    }
}
