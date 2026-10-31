package com.pipican.client;

import com.pipican.blacksmith.BlacksmithUpgrades;
import com.pipican.blacksmith.BlacksmithUpgrades.Action;
import com.pipican.blacksmith.BlacksmithUpgrades.Cost;
import com.pipican.blacksmith.BlacksmithUpgrades.State;
import com.pipican.network.BlacksmithUpgradePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Inventory-driven blacksmith UI; all mutations are submitted to the authoritative server backend. */
public final class BlacksmithScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;
    private final List<String> profiles;
    private int slot;
    private int profileIndex;
    private Action action = Action.UPGRADE;
    private String feedback = "";

    public BlacksmithScreen() {
        super(Component.translatable("blacksmith.pipican.title"));
        profiles = new ArrayList<>(BlacksmithUpgrades.profiles());
        profiles.sort(String::compareTo);
        if (!profiles.isEmpty()) profileIndex = Math.max(0, profiles.indexOf("ashlands"));
        slot = nextSupported(-1, 1);
    }

    private String profile() {
        return profiles.isEmpty() ? "" : profiles.get(Math.floorMod(profileIndex, profiles.size()));
    }

    private String selectedProfile() {
        State current = state();
        return current != null && !current.region().isEmpty() ? current.region() : profile();
    }

    private ItemStack selected() {
        if (minecraft == null || minecraft.player == null || slot < 0 || slot >= 36) return ItemStack.EMPTY;
        return minecraft.player.getInventory().getItem(slot);
    }

    private int nextSupported(int from, int direction) {
        if (minecraft == null || minecraft.player == null) return -1;
        for (int step = 1; step <= 36; step++) {
            int candidate = Math.floorMod(from + direction * step, 36);
            if (BlacksmithUpgrades.supported(minecraft.player.getInventory().getItem(candidate))) return candidate;
        }
        return -1;
    }

    private State state() {
        try {
            return selected().isEmpty() ? null : State.read(selected());
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF171923);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 2, 0xFFC59A48);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFD36A);

        ItemStack item = selected();
        State current = state();
        if (item.isEmpty()) {
            graphics.text(font, Component.translatable("blacksmith.pipican.no_equipment"), left + 14, top + 34, 0xFFFF7777);
        } else {
            graphics.item(item, left + 14, top + 32);
            graphics.itemDecorations(font, item, left + 14, top + 32);
            graphics.text(font, Component.translatable("blacksmith.pipican.selected", slot + 1, item.getHoverName()), left + 36, top + 36, 0xFFFFFFFF);
            if (current != null) {
                graphics.text(font, Component.translatable("blacksmith.pipican.current",
                        Component.translatable("blacksmith.pipican.tier." + current.tier().name().toLowerCase(Locale.ROOT)), current.level()),
                        left + 36, top + 49, 0xFFBBBBBB);
                if (!current.region().isEmpty()) {
                    graphics.text(font, Component.translatable("blacksmith.pipican.bound", regionName(current.region())), left + 36, top + 61, 0xFF9AC8FF);
                }
            }
        }
        button(graphics, left + 236, top + 34, 24, 18, "<", mouseX, mouseY);
        button(graphics, left + 264, top + 34, 24, 18, ">", mouseX, mouseY);

        boolean profileLocked = current != null && !current.region().isEmpty();
        if (!profileLocked) button(graphics, left + 14, top + 78, 24, 18, "<", mouseX, mouseY);
        graphics.text(font, Component.translatable(profileLocked ? "blacksmith.pipican.bound" : "blacksmith.pipican.region",
                regionName(selectedProfile())), left + 44, top + 83, 0xFF9FE7FF);
        if (!profileLocked) button(graphics, left + 264, top + 78, 24, 18, ">", mouseX, mouseY);
        button(graphics, left + 14, top + 101, PANEL_WIDTH - 28, 18,
                Component.translatable("blacksmith.pipican.action." + action.name().toLowerCase(Locale.ROOT)).getString(), mouseX, mouseY);

        int y = top + 128;
        graphics.text(font, Component.translatable("blacksmith.pipican.costs"), left + 14, y, 0xFFFFD36A);
        y += 13;
        boolean transitionAvailable = current != null && (action == Action.UPGRADE ? current.level() < 10
                : current.level() == 10 && current.tier() != BlacksmithUpgrades.Tier.MYTHIC);
        List<Cost> costs = !transitionAvailable || selectedProfile().isEmpty() ? List.of()
                : BlacksmithUpgrades.costs(selectedProfile(), current, action);
        if (costs.isEmpty()) {
            graphics.text(font, Component.translatable("blacksmith.pipican.no_cost"), left + 14, y, 0xFFAAAAAA);
        } else {
            int x = left + 14;
            for (Cost cost : costs) {
                var materialHolder = BuiltInRegistries.ITEM.get(cost.item()).orElse(null);
                if (materialHolder == null) continue;
                var material = new ItemStack(materialHolder, cost.count());
                graphics.item(material, x, y);
                graphics.itemDecorations(font, material, x, y);
                graphics.text(font, Component.translatable("blacksmith.pipican.cost_item", cost.count(), material.getHoverName()), x + 20, y + 4, 0xFFE0E0E0);
                x += Math.min(190, font.width(Component.translatable("blacksmith.pipican.cost_item", cost.count(), material.getHoverName()).getString()) + 35);
                if (x > left + PANEL_WIDTH - 80) {
                    y += 19;
                    x = left + 14;
                }
            }
        }
        int points = current == null ? 0 : current.tier().ordinal() * 10 + current.level();
        int resulting = !transitionAvailable ? points : action == Action.UPGRADE ? points + 1 : points + 10;
        boolean armor = isArmor(item);
        double amount = resulting * (armor ? 0.05 : 0.2);
        graphics.text(font, Component.translatable(armor ? "blacksmith.pipican.result_armor" : "blacksmith.pipican.result_damage", amount),
                left + 14, top + 176, 0xFFB8F5B0);
        if (!feedback.isEmpty()) graphics.text(font, Component.literal(feedback), left + 14, top + 190, 0xFFFFE39A);

        button(graphics, left + 14, top + PANEL_HEIGHT - 25, 188, 18,
                Component.translatable("blacksmith.pipican.submit").getString(), mouseX, mouseY);
        button(graphics, left + PANEL_WIDTH - 82, top + PANEL_HEIGHT - 25, 68, 18,
                Component.translatable("gui.cancel").getString(), mouseX, mouseY);
    }

    private Component regionName(String id) {
        return Component.translatable("blacksmith.pipican.region." + id);
    }

    private boolean isArmor(ItemStack stack) {
        var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        return equippable != null && stack.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).modifiers().stream()
                .anyMatch(entry -> entry.attribute().equals(Attributes.ARMOR));
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        int color = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h ? 0xFF66522F : 0xFF393B48;
        graphics.fill(x, y, x + w, y + h, color);
        graphics.centeredText(font, Component.literal(label), x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        if (inside(event, left + 236, top + 34, 24, 18)) slot = nextSupported(slot, -1);
        else if (inside(event, left + 264, top + 34, 24, 18)) slot = nextSupported(slot, 1);
        else if (inside(event, left + 14, top + 78, 24, 18) && !profileLocked()) profileIndex = Math.floorMod(profileIndex - 1, profiles.size());
        else if (inside(event, left + 264, top + 78, 24, 18) && !profileLocked()) profileIndex = Math.floorMod(profileIndex + 1, profiles.size());
        else if (inside(event, left + 14, top + 101, PANEL_WIDTH - 28, 18)) action = action == Action.UPGRADE ? Action.EVOLVE : Action.UPGRADE;
        else if (inside(event, left + 14, top + PANEL_HEIGHT - 25, 188, 18)) submit();
        else if (inside(event, left + PANEL_WIDTH - 82, top + PANEL_HEIGHT - 25, 68, 18)) onClose();
        else return super.mouseClicked(event, doubleClick);
        return true;
    }

    private static boolean inside(MouseButtonEvent e, int x, int y, int w, int h) {
        return e.x() >= x && e.x() < x + w && e.y() >= y && e.y() < y + h;
    }

    private void submit() {
        ItemStack stack = selected();
        State current = state();
        if (stack.isEmpty() || current == null || profile().isEmpty()) {
            feedback = Component.translatable("blacksmith.pipican.no_equipment").getString();
            return;
        }
        ClientPacketDistributor.sendToServer(new BlacksmithUpgradePayload(UUID.randomUUID(), slot, current.revision(), stack, action, selectedProfile()));
        feedback = Component.translatable("blacksmith.pipican.sent").getString();
    }

    private boolean profileLocked() {
        State current = state();
        return current != null && !current.region().isEmpty();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
