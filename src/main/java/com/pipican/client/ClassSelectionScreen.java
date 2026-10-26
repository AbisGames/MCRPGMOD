package com.pipican.client;

import com.pipican.core.ClassLookup;
import com.pipican.network.ChooseClassPayload;
import com.pipican.rpgclass.RpgClass;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Class picker shown on first join. Lists every {@code selectable} class from the synced class
 * registry on the left and shows the selected class's details on the right. Nothing here is
 * class-specific - it renders whatever the JSON says.
 */
public class ClassSelectionScreen extends Screen {

    private static final int LIST_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 10;

    private final List<Map.Entry<ResourceKey<RpgClass>, RpgClass>> classes;
    private int selected = -1;
    private int scroll;
    private Button chooseButton;
    private final List<Button> classButtons = new ArrayList<>();

    public ClassSelectionScreen(RegistryAccess access) {
        super(Component.translatable("pipican.selection.title"));
        this.classes = ClassLookup.selectableClasses(access);
    }

    private RegistryAccess access() {
        return minecraft.player.level().registryAccess();
    }

    private int visibleButtons() {
        return Math.max(1, (height - 70) / (BUTTON_HEIGHT + 2));
    }

    @Override
    protected void init() {
        classButtons.clear();
        int visible = visibleButtons();
        scroll = Math.clamp(scroll, 0, Math.max(0, classes.size() - visible));
        for (int i = scroll; i < Math.min(classes.size(), scroll + visible); i++) {
            int index = i;
            Button button = Button.builder(classes.get(i).getValue().name(), b -> select(index))
                    .bounds(PADDING, 35 + (i - scroll) * (BUTTON_HEIGHT + 2), LIST_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = index != selected;
            classButtons.add(addRenderableWidget(button));
        }
        chooseButton = addRenderableWidget(Button.builder(Component.translatable("pipican.selection.choose"), b -> confirm())
                .bounds(width - PADDING - 120, height - 30, 120, 20)
                .build());
        chooseButton.active = selected >= 0;
        addRenderableWidget(Button.builder(Component.translatable("pipican.selection.later"), b -> onClose())
                .bounds(width - PADDING - 250, height - 30, 120, 20)
                .build());
    }

    private void select(int index) {
        selected = index;
        rebuildWidgets();
    }

    private void confirm() {
        if (selected >= 0) {
            ClientPacketDistributor.sendToServer(new ChooseClassPayload(classes.get(selected).getKey()));
            onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (x < PADDING + LIST_WIDTH) {
            scroll -= (int) Math.signum(scrollY);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        g.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        if (classes.isEmpty()) {
            g.centeredText(font, Component.translatable("pipican.selection.empty"), width / 2, height / 2, 0xFFFF5555);
            return;
        }
        if (selected < 0) {
            g.text(font, Component.translatable("pipican.selection.hint"), PADDING * 2 + LIST_WIDTH, 40, 0xFFAAAAAA);
            return;
        }
        extractDetails(g, classes.get(selected).getValue(), PADDING * 2 + LIST_WIDTH, 35, width - LIST_WIDTH - PADDING * 4, mouseX, mouseY);
    }

    /** Right-hand panel: everything a player should know before picking the class. */
    private void extractDetails(GuiGraphicsExtractor g, RpgClass cls, int x, int y, int w, int mouseX, int mouseY) {
        g.fill(x - 4, y - 4, x + w + 4, height - 40, 0xA0000000);
        g.item(new ItemStack(cls.icon()), x, y);
        g.text(font, cls.name().copy().withStyle(ChatFormatting.BOLD), x + 20, y + 4, 0xFFFFD700);
        y += 22;

        y = wrapped(g, cls.description(), x, y, w, 0xFFDDDDDD) + 4;
        y = line(g, Component.translatable("pipican.selection.health", formatHalfHearts(cls.baseHealth())), x, y, 0xFFFF6666);

        if (cls.specialStat().isPresent()) {
            StatDefinition stat = ClassLookup.stats(access()).getValue(cls.specialStat().get().stat());
            Component statName = stat != null ? stat.name() : Component.literal(cls.specialStat().get().stat().identifier().toString());
            y = line(g, Component.translatable("pipican.selection.special_stat", statName), x, y, 0xFFC080FF);
            if (cls.specialStat().get().resource().isPresent()) {
                y = line(g, Component.translatable("pipican.selection.resource", cls.specialStat().get().resource().get().name()), x, y, 0xFF80C0FF);
            }
        }

        if (!cls.baseStats().isEmpty()) {
            List<Component> parts = new ArrayList<>();
            cls.baseStats().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(k -> k.identifier().toString())))
                    .forEach(e -> {
                        StatDefinition stat = ClassLookup.stats(access()).getValue(e.getKey());
                        Component name = stat != null ? stat.name() : Component.literal(e.getKey().identifier().getPath());
                        parts.add(Component.empty().append(name).append(" " + e.getValue()));
                    });
            y = wrapped(g, Component.translatable("pipican.selection.stats", joined(parts)), x, y, w, 0xFFFFFFFF);
        }

        List<Component> slots = new ArrayList<>();
        for (TrinketLayout.Group group : TrinketLayout.of(access(), cls).groups()) {
            slots.add(Component.literal(group.count() + "x ").append(TrinketLayout.name(access(), group.slot())));
        }
        if (!slots.isEmpty()) {
            y = wrapped(g, Component.translatable("pipican.selection.trinkets", joined(slots)), x, y, w, 0xFF80FF80);
        }

        if (cls.weakness().isPresent() && !cls.weakness().get().description().getString().isEmpty()) {
            y = wrapped(g, Component.translatable("pipican.selection.weakness", cls.weakness().get().description()), x, y, w, 0xFFFF8040);
        }

        if (!cls.startingKit().isEmpty()) {
            y += 4;
            g.text(font, Component.translatable("pipican.selection.kit"), x, y + 4, 0xFFFFFFFF);
            int ix = x + font.width(Component.translatable("pipican.selection.kit")) + 4;
            for (var template : cls.startingKit()) {
                ItemStack stack = template.create();
                if (ix + 16 > x + w) {
                    break;
                }
                g.item(stack, ix, y);
                g.itemDecorations(font, stack, ix, y);
                if (mouseX >= ix && mouseX < ix + 16 && mouseY >= y && mouseY < y + 16) {
                    g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
                }
                ix += 18;
            }
        }
    }

    private int line(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color);
        return y + 11;
    }

    private int wrapped(GuiGraphicsExtractor g, Component text, int x, int y, int w, int color) {
        for (FormattedCharSequence line : font.split(text, w)) {
            g.text(font, line, x, y, color);
            y += 10;
        }
        return y + 1;
    }

    private static Component joined(List<Component> parts) {
        var out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(parts.get(i));
        }
        return out;
    }

    private static String formatHalfHearts(double health) {
        double hearts = health / 2.0;
        return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
    }
}
