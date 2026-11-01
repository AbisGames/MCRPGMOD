package com.pipican.client;

import com.pipican.core.ClassLookup;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.Scaling;
import com.pipican.effect.type.BonusEffect;
import com.pipican.effect.type.CriticalEffect;
import com.pipican.effect.type.DamageMultiplierEffect;
import com.pipican.player.ClassProgress;
import com.pipican.player.PipicanAttachments;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.rpgclass.SpecialStat;
import com.pipican.stat.StatDefinition;
import com.pipican.stat.StatPoints;
import com.pipican.effect.AttributeBonus;
import com.pipican.network.RaiseStatPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Dark-Souls-style level-up sheet (the passive screen's Stats tab).
 *
 * <p>Left: the class, stat points and every stat the class can train, each with its class scaling grade
 * (from {@code stat_growth}: S, A, B, C, D, E). Click {@code +}/{@code -} to plan points (shift: 5 at a time);
 * nothing is spent until "Accept", which sends them all at once. Middle and right: what the planned points change,
 * old value then new value, blue when it goes up, like Dark Souls' level-up preview.
 *
 * <p>The preview uses each stat's {@code per_point} and {@code effects} times the class's growth, which is how the
 * server applies them; anything the server adds on top (passives that scale with a stat) shows up after Accept.
 */
final class LevelUpPanel {

    /** Shipped stats in Dark Souls order; other stats follow by name. */
    private static final List<String> CORE_ORDER = List.of(
            "pipican:vitality", "pipican:endurance", "pipican:strength", "pipican:dexterity",
            "pipican:agility", "pipican:intelligence", "pipican:wisdom", "pipican:luck");

    private static final int ROW = 12;
    private static final int COLOR_PANEL = 0xE0141210;
    private static final int COLOR_BORDER = 0xFF7A6640;
    private static final int COLOR_TITLE = 0xFFE8D8A8;
    private static final int COLOR_LABEL = 0xFFC8C0B0;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_UP = 0xFF5FA8FF;
    private static final int COLOR_DOWN = 0xFFFF6060;
    private static final int COLOR_DIM = 0xFF707070;

    /** The player's stat totals as the server last computed them (bonuses included). */
    private static Map<ResourceKey<StatDefinition>, Double> serverTotals = Map.of();

    static void setTotals(Map<ResourceKey<StatDefinition>, Double> totals) {
        serverTotals = Map.copyOf(totals);
    }

    private final Minecraft minecraft;
    private final Map<ResourceKey<StatDefinition>, Integer> pending = new HashMap<>();
    /** Clickable boxes from the last frame, in sheet coordinates: {x, y, width, statIndex, delta or action, height}. */
    private final List<int[]> hitBoxes = new ArrayList<>();
    private List<ResourceKey<StatDefinition>> shownStats = List.of();

    LevelUpPanel(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    // ------------------------------------------------------------------ data

    private RegistryAccess access() {
        return minecraft.player.level().registryAccess();
    }

    private PlayerClassData data() {
        return minecraft.player.getData(PipicanAttachments.CLASS_DATA);
    }

    private Optional<RpgClass> rpgClass() {
        return data().activeClass().flatMap(key -> ClassLookup.rpgClass(access(), key));
    }

    private StatDefinition stat(ResourceKey<StatDefinition> key) {
        return ClassLookup.stats(access()).getValue(key);
    }

    private static boolean canRaise(RpgClass cls, ResourceKey<StatDefinition> key, StatDefinition stat) {
        return !stat.exclusive() || cls.isSpecialStat(key);
    }

    /** Points the player has put in (base + bought), the part capped by {@code max}. */
    private int trained(RpgClass cls, ResourceKey<StatDefinition> key) {
        return cls.baseStat(key) + data().boughtStat(key);
    }

    /** Total points in a stat right now, bonuses included when the server has told us. */
    private double total(RpgClass cls, ResourceKey<StatDefinition> key) {
        Double server = serverTotals.get(key);
        return server != null ? server : trained(cls, key);
    }

    int pendingTotal() {
        return pending.values().stream().mapToInt(Integer::intValue).sum();
    }

    private int available() {
        return StatPoints.available(access(), data());
    }

    boolean hasPending() {
        return pendingTotal() > 0;
    }

    void reset() {
        pending.clear();
    }

    void accept() {
        if (hasPending()) {
            ClientPacketDistributor.sendToServer(new RaiseStatPayload(new HashMap<>(pending)));
            pending.clear();
        }
    }

    private List<ResourceKey<StatDefinition>> statsToShow(RpgClass cls) {
        List<ResourceKey<StatDefinition>> list = new ArrayList<>();
        for (var e : ClassLookup.stats(access()).entrySet()) {
            ResourceKey<StatDefinition> key = e.getKey();
            if (canRaise(cls, key, e.getValue()) || total(cls, key) != 0) {
                list.add(key);
            }
        }
        list.sort(Comparator.<ResourceKey<StatDefinition>>comparingInt(k -> {
                    int i = CORE_ORDER.indexOf(k.identifier().toString());
                    if (i >= 0) {
                        return i;
                    }
                    return cls.isSpecialStat(k) ? CORE_ORDER.size() : CORE_ORDER.size() + 1;
                })
                .thenComparing(k -> k.identifier().toString()));
        return list;
    }

    // ------------------------------------------------------------------ scaling grades

    /** Dark-Souls-style letter for a class's growth in a stat. */
    static String grade(double growth) {
        if (growth >= 1.4) return "S";
        if (growth >= 1.25) return "A";
        if (growth >= 1.1) return "B";
        if (growth >= 0.9) return "C";
        if (growth >= 0.75) return "D";
        return "E";
    }

    private static int gradeColor(String grade) {
        return switch (grade) {
            case "S" -> 0xFFFFC040;
            case "A" -> 0xFFE0D060;
            case "B" -> 0xFF80E080;
            case "C" -> 0xFFC8C8C8;
            case "D" -> 0xFFC09070;
            default -> 0xFFB06060;
        };
    }

    // ------------------------------------------------------------------ input

    private static final int HIT_ACCEPT = 2;
    private static final int HIT_RESET = 3;

    /** Returns true if the click hit a +/-, Accept or Reset box. Coordinates are screen coordinates. */
    boolean mouseClicked(double screenX, double screenY, boolean shift) {
        Optional<RpgClass> cls = rpgClass();
        if (cls.isEmpty() || scale <= 0) {
            return false;
        }
        double mx = (screenX - originX) / scale;
        double my = (screenY - originY) / scale;
        int[] box = boxAt(mx, my);
        if (box == null) {
            return false;
        }
        if (box[4] == HIT_ACCEPT) {
            accept();
        } else if (box[4] == HIT_RESET) {
            reset();
        } else {
            ResourceKey<StatDefinition> key = shownStats.get(box[3]);
            int steps = shift ? 5 : 1;
            for (int i = 0; i < steps; i++) {
                if (box[4] > 0) {
                    StatDefinition stat = stat(key);
                    int planned = pending.getOrDefault(key, 0);
                    if (stat == null || pendingTotal() >= available() || trained(cls.get(), key) + planned >= stat.max()) {
                        break;
                    }
                    pending.merge(key, 1, Integer::sum);
                } else {
                    if (pending.getOrDefault(key, 0) <= 0) {
                        break;
                    }
                    pending.merge(key, -1, Integer::sum);
                }
            }
            pending.values().removeIf(v -> v <= 0);
        }
        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    private int[] boxAt(double mx, double my) {
        for (int[] box : hitBoxes) {
            if (mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[5]) {
                return box;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ rendering

    private static final int GAP = 5;        // between panels
    private static final int PAD = 6;        // inside a panel
    private static final int HEADER = 20;    // panel title strip
    private static final int BTN = 11;       // +/- box size
    private static final int ACTION_H = 14;  // Accept / Reset height
    private static final int MAX_LABEL = 100;

    /** Where the sheet is drawn and how much it is shrunk to fit; mouse input is mapped back through these. */
    private float originX;
    private float originY;
    private float scale = 1;

    /** One "label  old > new" line in the middle or right panel. */
    private record Line(Component label, String now, Change change) {}

    /**
     * Draws the sheet. Everything is measured first, laid out at its natural size, then scaled down (never up) so the
     * whole sheet fits between the tab bar and the bottom of the screen at any GUI scale.
     */
    void render(GuiGraphicsExtractor g, Font font, int screenWidth, int screenHeight, int topBar, int mouseX, int mouseY) {
        Optional<RpgClass> clsOpt = rpgClass();
        if (clsOpt.isEmpty()) {
            return;
        }
        RpgClass cls = clsOpt.get();
        shownStats = statsToShow(cls);
        // Drop plans the player can no longer afford (points spent elsewhere, data changed...).
        while (pendingTotal() > available() && !pending.isEmpty()) {
            ResourceKey<StatDefinition> any = pending.keySet().iterator().next();
            pending.merge(any, -1, Integer::sum);
            pending.values().removeIf(v -> v <= 0);
        }

        // ---- gather the content
        int level = data().activeProgress().map(ClassProgress::level).orElse(1);
        int avail = available();
        int remaining = avail - pendingTotal();
        long toNext = StatPoints.xpToNextPoint(data());
        List<Line> info = List.of(
                new Line(Component.translatable("pipican.levelup.level"), String.valueOf(level), null),
                new Line(Component.translatable("pipican.levelup.points"), String.valueOf(avail),
                        hasPending() ? new Change(String.valueOf(remaining), COLOR_DOWN) : null),
                new Line(Component.translatable("pipican.levelup.next_point"), toNext == Long.MAX_VALUE ? "-" : toNext + " XP", null));

        List<Line> attrLines = new ArrayList<>();
        Player player = minecraft.player;
        for (var e : attributePreview(cls).entrySet()) {
            double now = player.getAttributeValue(e.getKey());
            attrLines.add(new Line(Component.translatable(e.getKey().value().getDescriptionId()), fmt(now), changeOf(now, now + e.getValue()[0])));
        }
        Optional<SpecialStat> special = cls.specialStat();
        if (special.isPresent() && special.get().resource().isPresent()) {
            SpecialStat.Resource res = special.get().resource().get();
            double points = total(cls, special.get().stat());
            int planned = pending.getOrDefault(special.get().stat(), 0);
            double nowMax = data().resourceMax();
            double afterMax = nowMax + (res.max(points + planned) - res.max(points));
            attrLines.add(new Line(Component.translatable("pipican.levelup.resource_max", res.name()), fmt(nowMax), changeOf(nowMax, afterMax)));
            double nowRegen = res.regenPerSecond(points);
            attrLines.add(new Line(Component.translatable("pipican.levelup.resource_regen", res.name()), fmt(nowRegen),
                    changeOf(nowRegen, res.regenPerSecond(points + planned))));
        }
        List<Line> bonusLines = new ArrayList<>();
        for (BonusLine b : bonusPreview(cls)) {
            bonusLines.add(new Line(b.label(), pct(b.now()), changeOf(b.now(), b.after(), LevelUpPanel::pct)));
        }

        // ---- measure
        int statNameW = 0;
        int statValueW = font.width("00");
        for (ResourceKey<StatDefinition> key : shownStats) {
            StatDefinition stat = stat(key);
            if (stat != null) {
                statNameW = Math.max(statNameW, font.width(stat.name()));
                double now = total(cls, key);
                statValueW = Math.max(statValueW, font.width(fmtInt(now + pending.getOrDefault(key, 0))));
                statValueW = Math.max(statValueW, font.width(fmtInt(now)));
            }
        }
        int infoLabelW = info.stream().mapToInt(l -> font.width(l.label())).max().orElse(0);
        int infoValueW = valueWidth(font, info);
        int gradeW = font.width("S");
        int arrowW = font.width(" > ");
        int statLabelW = Math.min(MAX_LABEL, statNameW);
        // stat row: [bar 2][4][name][6][grade][6][value][ > ][value][6][-][2][+]
        int statRowW = 6 + statLabelW + 6 + gradeW + 6 + statValueW + arrowW + statValueW + 6 + BTN + 2 + BTN;
        int infoRowW = Math.min(MAX_LABEL, infoLabelW) + 8 + infoValueW + arrowW + infoValueW;
        Component title = Component.translatable("pipican.levelup.title").withStyle(ChatFormatting.BOLD);
        int titleW = 22 + Math.max(font.width(title), font.width(cls.name())) + 12;
        int actionMinW = 2 * (Math.max(font.width(Component.translatable("pipican.levelup.accept")),
                font.width(Component.translatable("pipican.levelup.reset"))) + 12) + 6;
        int leftW = 2 * PAD + Math.max(Math.max(statRowW, infoRowW), Math.max(titleW, actionMinW));

        Component attrTitle = Component.translatable("pipican.levelup.attributes");
        Component bonusTitle = Component.translatable("pipican.levelup.bonuses");
        int midW = 2 * PAD + Math.max(font.width(attrTitle), lineWidth(font, attrLines, arrowW));
        List<Line> bonusShown = bonusLines.isEmpty()
                ? List.of(new Line(Component.translatable("pipican.levelup.no_bonuses"), "", null)) : bonusLines;
        int rightW = 2 * PAD + Math.max(font.width(bonusTitle), lineWidth(font, bonusShown, arrowW));

        int leftH = HEADER + 4 + info.size() * ROW + 6 + shownStats.size() * ROW + 4 + ACTION_H + PAD;
        int midH = HEADER + 4 + attrLines.size() * ROW + PAD;
        int rightH = HEADER + 4 + bonusShown.size() * ROW + PAD;
        int sheetH = Math.max(leftH, Math.max(midH, rightH));
        int sheetW = leftW + GAP + midW + GAP + rightW;

        // ---- fit to the screen
        int availW = screenWidth - 8;
        int availH = screenHeight - topBar - 8;
        scale = Math.min(1f, Math.min(availW / (float) sheetW, availH / (float) sheetH));
        originX = (screenWidth - sheetW * scale) / 2f;
        originY = topBar + 4 + Math.max(0, (availH - sheetH * scale) / 2f);
        int mx = (int) Math.floor((mouseX - originX) / scale);
        int my = (int) Math.floor((mouseY - originY) / scale);

        hitBoxes.clear();
        List<Component> tooltip = null;
        g.pose().pushMatrix();
        g.pose().translate(originX, originY);
        g.pose().scale(scale, scale);

        // ---- left: class, points, stats, Accept / Reset
        panel(g, 0, 0, leftW, sheetH);
        g.item(new ItemStack(cls.icon()), PAD - 2, 2);
        g.text(font, clip(font, title, leftW - 22 - 18), 20, 3, COLOR_TITLE);
        g.text(font, clip(font, cls.name(), leftW - 22 - 18), 20, 11, COLOR_LABEL);
        // "?" with the help text as its tooltip, instead of a line under the sheet.
        int qx = leftW - PAD - font.width("?");
        g.text(font, "?", qx, 6, COLOR_TITLE);
        if (mx >= qx - 2 && mx < leftW - 2 && my >= 2 && my < 16) {
            tooltip = List.of(Component.translatable("pipican.levelup.help").withStyle(ChatFormatting.GRAY));
        }
        int y = HEADER + 4;
        for (Line l : info) {
            line(g, font, PAD, leftW - 2 * PAD, y, l, infoValueW, arrowW);
            y += ROW;
        }
        g.fill(PAD, y + 1, leftW - PAD, y + 2, 0x807A6640);
        y += 6;

        int plusX = leftW - PAD - BTN;
        int minusX = plusX - 2 - BTN;
        int newX = minusX - 6 - statValueW;
        int oldX = newX - arrowW - statValueW;
        int gradeX = oldX - 6 - gradeW;
        int nameX = PAD + 6;
        int nameMax = gradeX - 6 - nameX;
        for (int i = 0; i < shownStats.size(); i++) {
            ResourceKey<StatDefinition> key = shownStats.get(i);
            StatDefinition stat = stat(key);
            if (stat == null) {
                y += ROW;
                continue;
            }
            boolean raisable = canRaise(cls, key, stat);
            int planned = pending.getOrDefault(key, 0);
            boolean hover = mx >= 2 && mx < leftW - 2 && my >= y - 2 && my < y + ROW - 2;
            if (hover) {
                g.fill(2, y - 2, leftW - 2, y + ROW - 2, 0x30FFFFFF);
            }
            g.fill(PAD, y, PAD + 2, y + 8, stat.color() == 0xFFFFFFFF ? COLOR_BORDER : stat.color());
            Component name = cls.isSpecialStat(key) ? stat.name().copy().withStyle(ChatFormatting.LIGHT_PURPLE) : stat.name();
            g.text(font, clip(font, name, nameMax), nameX, y, raisable ? COLOR_LABEL : COLOR_DIM);
            String grade = grade(cls.growth(key));
            g.text(font, grade, gradeX, y, gradeColor(grade));
            double now = total(cls, key);
            rightText(g, font, fmtInt(now), oldX + statValueW, y, COLOR_VALUE);
            g.text(font, " > ", oldX + statValueW, y, COLOR_DIM);
            rightText(g, font, fmtInt(now + planned), newX + statValueW, y, planned > 0 ? COLOR_UP : COLOR_DIM);
            if (raisable) {
                boolean canAdd = remaining > 0 && trained(cls, key) + planned < stat.max();
                smallButton(g, font, minusX, y - 2, "-", planned > 0, i, -1, mx, my);
                smallButton(g, font, plusX, y - 2, "+", canAdd, i, 1, mx, my);
            }
            if (hover && boxAt(mx, my) == null) {
                tooltip = statTooltip(cls, key, stat, now);
            }
            y += ROW;
        }
        int actionY = sheetH - PAD - ACTION_H;
        int actionW = (leftW - 2 * PAD - 6) / 2;
        actionButton(g, font, PAD, actionY, actionW, Component.translatable("pipican.levelup.accept"), hasPending(), HIT_ACCEPT, mx, my);
        actionButton(g, font, PAD + actionW + 6, actionY, actionW, Component.translatable("pipican.levelup.reset"), hasPending(), HIT_RESET, mx, my);

        // ---- middle: attributes
        int midX = leftW + GAP;
        panel(g, midX, 0, midW, sheetH);
        g.text(font, attrTitle, midX + PAD, 6, COLOR_TITLE);
        int attrValueW = valueWidth(font, attrLines);
        y = HEADER + 4;
        for (Line l : attrLines) {
            line(g, font, midX + PAD, midW - 2 * PAD, y, l, attrValueW, arrowW);
            y += ROW;
        }

        // ---- right: stat bonuses
        int rightX = midX + midW + GAP;
        panel(g, rightX, 0, rightW, sheetH);
        g.text(font, bonusTitle, rightX + PAD, 6, COLOR_TITLE);
        int bonusValueW = valueWidth(font, bonusLines);
        y = HEADER + 4;
        if (bonusLines.isEmpty()) {
            g.text(font, bonusShown.getFirst().label(), rightX + PAD, y, COLOR_DIM);
        }
        for (Line l : bonusLines) {
            line(g, font, rightX + PAD, rightW - 2 * PAD, y, l, bonusValueW, arrowW);
            y += ROW;
        }
        g.pose().popMatrix();

        if (tooltip != null) {
            g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private record Change(String text, int color) {}

    private record BonusLine(Component label, double now, double after) {}

    private static Change changeOf(double now, double after) {
        return changeOf(now, after, LevelUpPanel::fmt);
    }

    private static Change changeOf(double now, double after, java.util.function.DoubleFunction<String> format) {
        if (Math.abs(after - now) < 1e-9) {
            return null;
        }
        return new Change(format.apply(after), after > now ? COLOR_UP : COLOR_DOWN);
    }

    /** Widest value (old or new) in these lines. */
    private static int valueWidth(Font font, List<Line> lines) {
        int w = font.width("0");
        for (Line l : lines) {
            w = Math.max(w, font.width(l.now()));
            if (l.change() != null) {
                w = Math.max(w, font.width(l.change().text()));
            }
        }
        return w;
    }

    /** Natural width of a column of lines: label, gap, old value, " > ", new value. */
    private static int lineWidth(Font font, List<Line> lines, int arrowW) {
        int label = 0;
        for (Line l : lines) {
            label = Math.min(MAX_LABEL, Math.max(label, font.width(l.label())));
        }
        int value = valueWidth(font, lines);
        boolean values = lines.stream().anyMatch(l -> !l.now().isEmpty());
        return values ? label + 8 + value + arrowW + value : label;
    }

    /** Label on the left, "old > new" right-aligned, like Dark Souls' level-up columns. */
    private static void line(GuiGraphicsExtractor g, Font font, int x, int width, int y, Line l, int valueW, int arrowW) {
        int newRight = x + width;
        int oldRight = newRight - valueW - arrowW;
        g.text(font, clip(font, l.label(), oldRight - valueW - 8 - x), x, y, COLOR_LABEL);
        rightText(g, font, l.now(), oldRight, y, COLOR_VALUE);
        g.text(font, " > ", oldRight, y, COLOR_DIM);
        if (l.change() != null) {
            rightText(g, font, l.change().text(), newRight, y, l.change().color());
        } else {
            rightText(g, font, l.now(), newRight, y, COLOR_DIM);
        }
    }

    private static void rightText(GuiGraphicsExtractor g, Font font, String text, int right, int y, int color) {
        g.text(font, text, right - font.width(text), y, color);
    }

    private void smallButton(GuiGraphicsExtractor g, Font font, int x, int y, String label, boolean active, int index, int delta, int mx, int my) {
        boolean hover = active && mx >= x && mx < x + BTN && my >= y && my < y + BTN;
        g.fill(x, y, x + BTN, y + BTN, active ? (hover ? 0xFFB09050 : COLOR_BORDER) : 0xFF3A3A3A);
        g.fill(x + 1, y + 1, x + BTN - 1, y + BTN - 1, hover ? 0xFF3A3020 : 0xFF1C1A16);
        g.text(font, label, x + (BTN - font.width(label) + 1) / 2, y + 2, active ? COLOR_TITLE : COLOR_DIM);
        if (active) {
            hitBoxes.add(new int[] {x, y, BTN, index, delta, BTN});
        }
    }

    private void actionButton(GuiGraphicsExtractor g, Font font, int x, int y, int w, Component label, boolean active, int code, int mx, int my) {
        boolean hover = active && mx >= x && mx < x + w && my >= y && my < y + ACTION_H;
        g.fill(x, y, x + w, y + ACTION_H, active ? (hover ? 0xFFD0B070 : COLOR_BORDER) : 0xFF3A3A3A);
        g.fill(x + 1, y + 1, x + w - 1, y + ACTION_H - 1, hover ? 0xFF3A3020 : 0xFF1C1A16);
        Component text = clip(font, label, w - 4);
        g.text(font, text, x + (w - font.width(text)) / 2, y + 3, active ? COLOR_TITLE : COLOR_DIM);
        if (active) {
            hitBoxes.add(new int[] {x, y, w, -1, code, ACTION_H});
        }
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COLOR_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL);
        g.fill(x + 3, y + HEADER - 1, x + w - 3, y + HEADER, 0x807A6640);
    }

    private static Component clip(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String s = text.getString();
        while (!s.isEmpty() && font.width(Component.literal(s + "..").withStyle(text.getStyle())) > width) {
            s = s.substring(0, s.length() - 1);
        }
        return Component.literal(s + "..").withStyle(text.getStyle());
    }

    // ------------------------------------------------------------------ preview maths

    /** Attribute -> {change from planned points}, for every attribute a shown stat's {@code per_point} touches. */
    private Map<Holder<Attribute>, double[]> attributePreview(RpgClass cls) {
        Map<Holder<Attribute>, double[]> out = new LinkedHashMap<>();
        Player player = minecraft.player;
        for (ResourceKey<StatDefinition> key : shownStats) {
            StatDefinition stat = stat(key);
            if (stat == null) {
                continue;
            }
            int planned = pending.getOrDefault(key, 0);
            double grown = planned * cls.growth(key);
            for (AttributeBonus b : stat.perPoint()) {
                if (player.getAttribute(b.attribute()) == null) {
                    continue;
                }
                double delta = switch (b.operation()) {
                    case ADD_VALUE -> b.amount() * grown;
                    case ADD_MULTIPLIED_BASE -> player.getAttributeBaseValue(b.attribute()) * b.amount() * grown;
                    case ADD_MULTIPLIED_TOTAL -> player.getAttributeValue(b.attribute()) * b.amount() * grown;
                };
                out.computeIfAbsent(b.attribute(), a -> new double[1])[0] += delta;
            }
        }
        return out;
    }

    /** Effects inside stat files that grow with {@code "points"}: magic damage, healing, crit chance... */
    private List<BonusLine> bonusPreview(RpgClass cls) {
        List<BonusLine> out = new ArrayList<>();
        for (ResourceKey<StatDefinition> key : shownStats) {
            StatDefinition stat = stat(key);
            if (stat == null) {
                continue;
            }
            double growth = cls.growth(key);
            double now = total(cls, key) * growth;
            double after = (total(cls, key) + pending.getOrDefault(key, 0)) * growth;
            for (ClassEffect effect : stat.effects()) {
                double per = perPoint(effect);
                if (per == 0) {
                    continue;
                }
                out.add(new BonusLine(bonusLabel(key, stat, effect), per * now, per * after));
            }
        }
        return out;
    }

    /** How much one (grown) point adds to the effect, from its {@code "points"} scaling. */
    private static double perPoint(ClassEffect effect) {
        List<Scaling> scaling = switch (effect) {
            case BonusEffect b -> b.scaling();
            case CriticalEffect c -> c.chanceScaling();
            case DamageMultiplierEffect d -> d.scaling();
            default -> List.of();
        };
        double per = 0;
        for (Scaling s : scaling) {
            if (s.source().equals("points")) {
                per += s.per();
            }
        }
        return per;
    }

    private static Component bonusLabel(ResourceKey<StatDefinition> key, StatDefinition stat, ClassEffect effect) {
        String langKey = "pipican.levelup.bonus." + key.identifier().getNamespace() + "." + key.identifier().getPath();
        if (net.minecraft.client.resources.language.I18n.exists(langKey)) {
            return Component.translatable(langKey);
        }
        if (effect instanceof BonusEffect b) {
            return Component.literal(b.kind().getSerializedName().replace('_', ' '));
        }
        return stat.name();
    }

    private List<Component> statTooltip(RpgClass cls, ResourceKey<StatDefinition> key, StatDefinition stat, double total) {
        List<Component> lines = new ArrayList<>();
        lines.add(stat.name().copy().withStyle(ChatFormatting.GOLD));
        if (!stat.description().getString().isEmpty()) {
            lines.add(stat.description().copy().withStyle(ChatFormatting.GRAY));
        }
        double growth = cls.growth(key);
        lines.add(Component.translatable("pipican.levelup.tip.scaling", grade(growth), String.format(Locale.ROOT, "%.2f", growth))
                .withStyle(ChatFormatting.AQUA));
        for (AttributeBonus b : stat.perPoint()) {
            double amount = b.amount() * growth;
            String value = b.operation() == AttributeModifier.Operation.ADD_VALUE
                    ? String.format(Locale.ROOT, "%+.3f", amount)
                    : String.format(Locale.ROOT, "%+.2f%%", amount * 100);
            lines.add(Component.translatable("pipican.levelup.tip.per_point", value,
                    Component.translatable(b.attribute().value().getDescriptionId())).withStyle(ChatFormatting.DARK_AQUA));
        }
        for (ClassEffect effect : stat.effects()) {
            double per = perPoint(effect);
            if (per != 0) {
                lines.add(Component.translatable("pipican.levelup.tip.per_point", String.format(Locale.ROOT, "%+.2f%%", per * growth * 100),
                        bonusLabel(key, stat, effect)).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        int base = cls.baseStat(key);
        int bought = data().boughtStat(key);
        double bonus = total - base - bought;
        lines.add(Component.translatable("pipican.levelup.tip.breakdown", base, bought, fmtInt(bonus)).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("pipican.levelup.tip.max", stat.max()).withStyle(ChatFormatting.DARK_GRAY));
        if (!canRaise(cls, key, stat)) {
            lines.add(Component.translatable("pipican.stat.exclusive", stat.name()).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private static String fmtInt(double v) {
        return Math.abs(v - Math.round(v)) < 1e-6 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-6 && Math.abs(v) >= 1) {
            return String.valueOf(Math.round(v));
        }
        return Math.abs(v) < 1 ? String.format(Locale.ROOT, "%.3f", v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private static String pct(double v) {
        return String.format(Locale.ROOT, "%+.1f%%", v * 100);
    }
}
