package com.pipican.client;

import com.pipican.core.ClassLookup;
import com.pipican.mastery.Skill;
import com.pipican.network.NodePayload;
import com.pipican.player.ClassProgress;
import com.pipican.player.MasteryService;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.NodeType;
import com.pipican.skill.SkillNode;
import com.pipican.skill.SkillTree;
import com.pipican.skill.TreeKind;
import com.pipican.skill.TreePoints;
import com.pipican.skill.TreeRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Path-of-Exile-style passive tree screen (general and special trees), a Dark-Souls-style level-up sheet for stats
 * ({@link LevelUpPanel}) and a mastery tab.
 *
 * <ul>
 *   <li>Drag to pan, scroll to zoom (around the cursor).</li>
 *   <li>Left-click a highlighted node to allocate it, right-click an owned node to refund it.</li>
 *   <li>The search box highlights nodes whose name or description contains the text.</li>
 * </ul>
 * Everything is drawn from synced data (registries + the player's class attachment). The screen
 * only sends requests; the server validates them with the same rules ({@link SkillTree}).
 */
public class SkillTreeScreen extends Screen {

    private enum Tab { GENERAL, SPECIAL, STAT, MASTERY }

    private static final int TOP_BAR = 28;
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 2.5f;
    /** Mouse movement (px) below which a press+release counts as a click rather than a drag. */
    private static final double CLICK_SLOP = 4;

    private static final int COLOR_BG = 0xFF101014;
    private static final int COLOR_LINK_OWNED = 0xFFE0B040;
    private static final int COLOR_LINK_OPEN = 0xFF6F9F6F;
    private static final int COLOR_LINK_LOCKED = 0xFF3A3A40;
    private static final int COLOR_NODE_OWNED = 0xFFE0B040;
    private static final int COLOR_NODE_OPEN = 0xFF7FD07F;
    private static final int COLOR_NODE_LOCKED = 0xFF5A5A62;
    private static final int COLOR_START = 0xFF5FA8FF;
    private static final int COLOR_SEARCH = 0xFFFFFF55;

    private Tab tab = Tab.GENERAL;
    private float zoom = 1f;
    private double panX;
    private double panY;
    private boolean centred;
    private double pressX;
    private double pressY;
    private boolean pressedInTree;
    private EditBox search;
    private String query = "";
    private int masteryScroll;
    private LevelUpPanel levelUp;
    private Button acceptButton;
    private Button resetButton;

    public SkillTreeScreen() {
        super(Component.translatable("pipican.tree.title"));
    }

    // ------------------------------------------------------------------ data access

    private RegistryAccess access() {
        return minecraft.player.level().registryAccess();
    }

    private PlayerClassData data() {
        return minecraft.player.getData(PipicanAttachments.CLASS_DATA);
    }

    private Optional<RpgClass> rpgClass() {
        return data().activeClass().flatMap(key -> ClassLookup.rpgClass(access(), key));
    }

    private TreeKind kind() {
        return switch (tab) {
            case SPECIAL -> TreeKind.SPECIAL;
            default -> TreeKind.GENERAL;
        };
    }

    private Optional<TreeRef> treeRef() {
        return rpgClass().flatMap(c -> c.tree(kind()));
    }

    private Optional<SkillTree> tree() {
        return rpgClass().flatMap(c -> ClassLookup.tree(access(), c, kind()));
    }

    private Set<String> owned() {
        return data().activeProgress().map(p -> p.nodes(kind())).orElse(Set.of());
    }

    /** Owned nodes plus the free start node: everything that counts as "already yours". */
    private Set<String> ownedWithStart() {
        Set<String> set = new HashSet<>(owned());
        treeRef().ifPresent(ref -> set.add(ref.start()));
        return set;
    }

    // ------------------------------------------------------------------ widgets

    @Override
    protected void init() {
        int x = 8;
        for (Tab t : Tab.values()) {
            Button button = Button.builder(Component.translatable("pipican.tree.tab." + t.name().toLowerCase(Locale.ROOT)), b -> switchTab(t))
                    .bounds(x, 4, 70, 20).build();
            button.active = t != tab;
            addRenderableWidget(button);
            x += 74;
        }
        search = new EditBox(font, width - 128, 6, 120, 16, search, Component.translatable("pipican.tree.search"));
        search.setHint(Component.translatable("pipican.tree.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setResponder(text -> query = text.toLowerCase(Locale.ROOT));
        search.visible = isTreeTab();
        addRenderableWidget(search);
        if (levelUp == null) {
            levelUp = new LevelUpPanel(minecraft);
        }
        if (tab == Tab.STAT) {
            levelUp.layout(width, height, TOP_BAR);
            int bw = (levelUp.leftWidth - 18) / 2;
            int by = levelUp.bottomOfLeft;
            acceptButton = addRenderableWidget(Button.builder(Component.translatable("pipican.levelup.accept"), b -> levelUp.accept())
                    .bounds(levelUp.left + 6, by, bw, 18).build());
            resetButton = addRenderableWidget(Button.builder(Component.translatable("pipican.levelup.reset"), b -> levelUp.reset())
                    .bounds(levelUp.left + 12 + bw, by, bw, 18).build());
        }
        if (!centred) {
            centreOnStart();
        }
    }

    private boolean isTreeTab() {
        return tab == Tab.GENERAL || tab == Tab.SPECIAL;
    }

    private void switchTab(Tab t) {
        if (levelUp != null && t != tab) {
            levelUp.reset();
        }
        tab = t;
        centred = false;
        masteryScroll = 0;
        rebuildWidgets();
    }

    private void centreOnStart() {
        Optional<SkillNode> start = treeRef().flatMap(ref -> tree().flatMap(t -> t.node(ref.start())));
        zoom = 1f;
        panX = start.map(n -> -n.x()).orElse(0.0);
        panY = start.map(n -> -n.y()).orElse(0.0);
        centred = true;
    }

    // ------------------------------------------------------------------ coordinates

    private double screenX(double treeX) {
        return width / 2.0 + (treeX + panX) * zoom;
    }

    private double screenY(double treeY) {
        return (height + TOP_BAR) / 2.0 + (treeY + panY) * zoom;
    }

    private Optional<SkillNode> nodeAt(double mx, double my) {
        if (my < TOP_BAR) {
            return Optional.empty();
        }
        return tree().flatMap(t -> t.nodes().stream()
                .filter(n -> Math.abs(screenX(n.x()) - mx) <= n.type().size() * zoom / 2
                        && Math.abs(screenY(n.y()) - my) <= n.type().size() * zoom / 2)
                .findFirst());
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (tab == Tab.STAT) {
            return levelUp.mouseClicked(event.x(), event.y(), event.hasShiftDown());
        }
        pressX = event.x();
        pressY = event.y();
        pressedInTree = isTreeTab() && event.y() >= TOP_BAR;
        return pressedInTree;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (pressedInTree && event.button() == 0) {
            panX += dx / zoom;
            panY += dy / zoom;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasTree = pressedInTree;
        pressedInTree = false;
        if (wasTree && Math.hypot(event.x() - pressX, event.y() - pressY) < CLICK_SLOP) {
            nodeAt(event.x(), event.y()).ifPresent(node -> {
                boolean refund = event.button() == 1;
                if (refund ? owned().contains(node.id()) : canAllocate(node)) {
                    ClientPacketDistributor.sendToServer(new NodePayload(kind(), node.id(), refund));
                }
            });
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (tab == Tab.MASTERY) {
            masteryScroll = Math.max(0, masteryScroll - (int) Math.signum(scrollY) * 12);
            return true;
        }
        if (tab == Tab.STAT) {
            return false;
        }
        float old = zoom;
        zoom = Math.clamp(zoom * (scrollY > 0 ? 1.15f : 1 / 1.15f), MIN_ZOOM, MAX_ZOOM);
        // Keep the tree point under the cursor fixed while zooming.
        double cx = width / 2.0;
        double cy = (height + TOP_BAR) / 2.0;
        panX += (x - cx) / zoom - (x - cx) / old;
        panY += (y - cy) / zoom - (y - cy) / old;
        return true;
    }

    // ------------------------------------------------------------------ node state

    private boolean canAllocate(SkillNode node) {
        Optional<TreeRef> ref = treeRef();
        Optional<SkillTree> tree = tree();
        if (ref.isEmpty() || tree.isEmpty() || node.type() == NodeType.START) {
            return false;
        }
        Set<String> owned = owned();
        int level = data().activeProgress().map(ClassProgress::level).orElse(1);
        return !owned.contains(node.id())
                && tree.get().isReachable(node.id(), ref.get().start(), owned)
                && level >= node.requiredLevel()
                && availablePoints() >= node.cost();
    }

    private int availablePoints() {
        return rpgClass().map(c -> TreePoints.available(access(), data(), c, kind())).orElse(0);
    }

    private boolean matchesSearch(SkillNode node) {
        return !query.isBlank() && (node.name().getString().toLowerCase(Locale.ROOT).contains(query)
                || node.description().getString().toLowerCase(Locale.ROOT).contains(query));
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fill(0, 0, width, height, COLOR_BG);
        // Drawn under the widgets so the Accept / Reset buttons sit on top of the sheet.
        if (tab == Tab.STAT && rpgClass().isPresent()) {
            extractLevelUp(g, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (rpgClass().isEmpty()) {
            g.centeredText(font, Component.translatable("pipican.class.none"), width / 2, height / 2, 0xFFFF5555);
            return;
        }
        if (tab == Tab.MASTERY) {
            extractMastery(g, mouseX, mouseY);
        } else if (tab == Tab.STAT) {
            // Drawn in extractBackground, under the buttons.
        } else if (tree().isEmpty() || treeRef().isEmpty()) {
            g.centeredText(font, Component.translatable("pipican.tree.none"), width / 2, height / 2, 0xFFAAAAAA);
        } else {
            g.enableScissor(0, TOP_BAR, width, height);
            extractTree(g, tree().get(), treeRef().get());
            g.disableScissor();
            nodeAt(mouseX, mouseY).ifPresent(node -> g.setComponentTooltipForNextFrame(font, tooltip(node), mouseX, mouseY));
        }
        extractHeader(g);
    }

    private void extractHeader(GuiGraphicsExtractor g) {
        g.fill(0, TOP_BAR - 1, width, TOP_BAR, 0xFF404048);
        if (!isTreeTab()) {
            return;
        }
        // Points / level sit just under the tab bar so they never collide with the search box.
        int level = data().activeProgress().map(ClassProgress::level).orElse(1);
        Component info = Component.translatable("pipican.tree.points", availablePoints()).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("   ").append(Component.translatable("pipican.tree.level", level)).withStyle(ChatFormatting.GRAY));
        g.fill(4, TOP_BAR + 2, 12 + font.width(info), TOP_BAR + 14, 0xC0000000);
        g.text(font, info, 8, TOP_BAR + 4, 0xFFFFFFFF);
        Component help = Component.translatable("pipican.tree.help");
        g.fill(4, height - 14, 12 + font.width(help), height - 2, 0xC0000000);
        g.text(font, help, 8, height - 12, 0xFF808080);
    }

    private void extractTree(GuiGraphicsExtractor g, SkillTree tree, TreeRef ref) {
        Set<String> mine = ownedWithStart();
        // Links first, so nodes are drawn on top.
        for (SkillNode node : tree.nodes()) {
            for (String otherId : tree.neighbours(node.id())) {
                if (otherId.compareTo(node.id()) < 0) {
                    continue; // each link once
                }
                SkillNode other = tree.node(otherId).orElseThrow();
                boolean a = mine.contains(node.id());
                boolean b = mine.contains(otherId);
                int color = a && b ? COLOR_LINK_OWNED : (a || b) ? COLOR_LINK_OPEN : COLOR_LINK_LOCKED;
                line(g, screenX(node.x()), screenY(node.y()), screenX(other.x()), screenY(other.y()), Math.max(1, Math.round(3 * zoom)), color);
            }
        }
        for (SkillNode node : tree.nodes()) {
            boolean isStart = node.id().equals(ref.start());
            boolean owned = mine.contains(node.id());
            int color = isStart ? COLOR_START
                    : owned ? COLOR_NODE_OWNED
                    : canAllocate(node) ? COLOR_NODE_OPEN
                    : COLOR_NODE_LOCKED;
            extractNode(g, node, color, matchesSearch(node), !owned && !isStart);
        }
    }

    private void extractNode(GuiGraphicsExtractor g, SkillNode node, int frameColor, boolean highlighted, boolean dim) {
        float size = node.type().size() * zoom;
        float x = (float) screenX(node.x());
        float y = (float) screenY(node.y());
        if (x < -size || y < TOP_BAR - size || x > width + size || y > height + size) {
            return;
        }
        boolean diamond = node.type() == NodeType.KEYSTONE || node.type() == NodeType.START;
        int frame = node.type() == NodeType.SMALL ? 2 : 3;

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        if (diamond) {
            g.pose().rotate((float) Math.toRadians(45));
            size /= 1.25f;
        }
        int half = Math.max(2, Math.round(size / 2));
        if (highlighted) {
            g.fill(-half - 3, -half - 3, half + 3, half + 3, COLOR_SEARCH);
        }
        g.fill(-half, -half, half, half, frameColor);
        g.fill(-half + frame, -half + frame, half - frame, half - frame, node.type() == NodeType.KEYSTONE ? 0xFF2A1416 : 0xFF18181E);
        g.pose().popMatrix();

        // Icon, scaled to the node size.
        float iconScale = node.type().size() * zoom / 26f;
        if (iconScale > 0.25f) {
            g.pose().pushMatrix();
            g.pose().translate(x - 8 * iconScale, y - 8 * iconScale);
            g.pose().scale(iconScale, iconScale);
            g.item(new ItemStack(node.icon()), 0, 0);
            g.pose().popMatrix();
            if (dim) {
                g.fill(Math.round(x - 8 * iconScale), Math.round(y - 8 * iconScale), Math.round(x + 8 * iconScale), Math.round(y + 8 * iconScale), 0x80000000);
            }
        }
    }

    /** A straight line of the given thickness, drawn as a rotated rectangle. */
    private static void line(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int thickness, int color) {
        double length = Math.hypot(x2 - x1, y2 - y1);
        if (length < 1) {
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate((float) x1, (float) y1);
        g.pose().rotate((float) Math.atan2(y2 - y1, x2 - x1));
        int half = Math.max(1, thickness / 2);
        g.fill(0, -half, (int) Math.round(length), thickness - half, color);
        g.pose().popMatrix();
    }

    private List<Component> tooltip(SkillNode node) {
        List<Component> lines = new ArrayList<>();
        ChatFormatting nameColor = switch (node.type()) {
            case SMALL -> ChatFormatting.WHITE;
            case NOTABLE -> ChatFormatting.YELLOW;
            case KEYSTONE -> ChatFormatting.LIGHT_PURPLE;
            case START -> ChatFormatting.AQUA;
        };
        lines.add(node.name().copy().withStyle(nameColor));
        lines.add(Component.translatable("pipican.node_type." + node.type().getSerializedName()).withStyle(ChatFormatting.DARK_GRAY));
        if (!node.description().getString().isEmpty()) {
            lines.add(node.description().copy().withStyle(ChatFormatting.GRAY));
        }
        boolean isStart = treeRef().map(r -> r.start().equals(node.id())).orElse(false);
        if (isStart || node.type() == NodeType.START) {
            return lines;
        }
        int level = data().activeProgress().map(ClassProgress::level).orElse(1);
        if (node.requiredLevel() > level) {
            lines.add(Component.translatable("pipican.tree.requires_level", node.requiredLevel()).withStyle(ChatFormatting.RED));
        }
        if (owned().contains(node.id())) {
            lines.add(Component.translatable("pipican.tree.hint_refund").withStyle(ChatFormatting.DARK_AQUA));
        } else if (canAllocate(node)) {
            lines.add(Component.translatable("pipican.tree.hint_allocate", node.cost()).withStyle(ChatFormatting.GREEN));
        } else if (!tree().map(t -> t.isReachable(node.id(), treeRef().get().start(), owned())).orElse(false)) {
            lines.add(Component.translatable("pipican.tree.not_connected").withStyle(ChatFormatting.DARK_GRAY));
        } else if (availablePoints() < node.cost()) {
            lines.add(Component.translatable("pipican.tree.no_points").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    // ------------------------------------------------------------------ stats tab

    private void extractLevelUp(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        levelUp.render(g, font, width, height, mouseX, mouseY);
        // The stat list can grow (new stat bonus, pack reload): keep Accept/Reset under it.
        int by = levelUp.bottomOfLeft;
        if (acceptButton != null && acceptButton.getY() != by) {
            acceptButton.setY(by);
            resetButton.setY(by);
        }
        if (acceptButton != null) {
            acceptButton.active = levelUp.hasPending();
            resetButton.active = levelUp.hasPending();
        }
    }

    // ------------------------------------------------------------------ mastery tab

    private void extractMastery(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        PlayerClassData data = data();
        List<Map.Entry<ResourceKey<Skill>, Skill>> skills = ClassLookup.skills(access()).entrySet().stream()
                .filter(e -> e.getValue().allowedFor(data.activeClass()))
                .sorted(java.util.Comparator.comparing(e -> e.getKey().identifier().toString()))
                .map(e -> Map.entry(e.getKey(), e.getValue()))
                .toList();
        if (skills.isEmpty()) {
            g.centeredText(font, Component.translatable("pipican.mastery.none"), width / 2, height / 2, 0xFFAAAAAA);
            return;
        }
        int rowHeight = 30;
        int left = Math.max(8, width / 2 - 170);
        int barWidth = 200;
        g.enableScissor(0, TOP_BAR, width, height);
        int y = TOP_BAR + 8 - masteryScroll;
        for (Map.Entry<ResourceKey<Skill>, Skill> e : skills) {
            Skill skill = e.getValue();
            double mastery = data.mastery(e.getKey());
            int tier = skill.tierFor(mastery);
            g.fill(left - 4, y - 2, left + 340, y + rowHeight - 4, 0x40FFFFFF);
            g.item(new ItemStack(skill.icon()), left, y + 3);
            g.text(font, skill.name().copy().withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  " + MasteryService.numeral(tier)).withStyle(ChatFormatting.GOLD)), left + 22, y, 0xFFFFFFFF);

            // Progress bar towards the next tier.
            double from = tier == 0 ? 0 : skill.thresholds().get(tier - 1);
            double to = tier >= Skill.TIERS ? from : skill.thresholds().get(tier);
            double fraction = tier >= Skill.TIERS ? 1 : (mastery - from) / Math.max(1, to - from);
            int bx = left + 22;
            int by = y + 12;
            g.fill(bx, by, bx + barWidth, by + 6, 0xFF303038);
            g.fill(bx, by, bx + (int) (barWidth * Math.clamp(fraction, 0, 1)), by + 6, tier >= Skill.TIERS ? 0xFFE0B040 : 0xFF4FA0E0);
            String progress = tier >= Skill.TIERS ? "MAX" : (int) mastery + " / " + (int) to;
            g.text(font, progress, bx + barWidth + 6, by - 1, 0xFFBBBBBB);

            if (mouseX >= left - 4 && mouseX < left + 340 && mouseY >= y - 2 && mouseY < y + rowHeight - 4 && mouseY >= TOP_BAR) {
                g.setComponentTooltipForNextFrame(font, masteryTooltip(skill, tier), mouseX, mouseY);
            }
            y += rowHeight;
        }
        g.disableScissor();
    }

    private List<Component> masteryTooltip(Skill skill, int tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(skill.name().copy().withStyle(ChatFormatting.WHITE));
        if (!skill.description().getString().isEmpty()) {
            lines.add(skill.description().copy().withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("pipican.mastery.points_to",
                Component.translatable("pipican.tree." + skill.pointsTo().getSerializedName())).withStyle(ChatFormatting.DARK_AQUA));
        for (int t = 0; t < Skill.TIERS; t++) {
            Component desc = t < skill.tiers().size() ? skill.tiers().get(t).description() : Component.empty();
            ChatFormatting color = t < tier ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY;
            lines.add(Component.literal(MasteryService.numeral(t + 1) + " (" + skill.thresholds().get(t) + "): ").append(desc).withStyle(color));
        }
        return lines;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
