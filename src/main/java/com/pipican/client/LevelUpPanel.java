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

    private static final int ROW = 13;
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
    /** Clickable +/- boxes from the last frame: {x, y, size, statIndex, delta}. */
    private final List<int[]> hitBoxes = new ArrayList<>();
    private List<ResourceKey<StatDefinition>> shownStats = List.of();
    int left;
    int top;
    int leftWidth;
    int bottomOfLeft;

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

    /** Returns true if the click hit a +/- box. */
    boolean mouseClicked(double mx, double my, boolean shift) {
        Optional<RpgClass> cls = rpgClass();
        if (cls.isEmpty()) {
            return false;
        }
        for (int[] box : hitBoxes) {
            if (mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[2]) {
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
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ layout

    /** Lays the panel out for a screen of this size; call before adding the Accept/Reset buttons. */
    void layout(int screenWidth, int screenHeight, int topBar) {
        int total = Math.min(screenWidth - 16, 560);
        left = (screenWidth - total) / 2;
        top = topBar + 8;
        leftWidth = Math.max(170, total * 36 / 100);
        Optional<RpgClass> cls = minecraft.player == null ? Optional.empty() : rpgClass();
        shownStats = cls.map(this::statsToShow).orElse(List.of());
        bottomOfLeft = top + 22 + 3 * ROW + 6 + shownStats.size() * ROW + 6;
    }

    // ------------------------------------------------------------------ rendering

    void render(GuiGraphicsExtractor g, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        Optional<RpgClass> clsOpt = rpgClass();
        if (clsOpt.isEmpty()) {
            return;
        }
        RpgClass cls = clsOpt.get();
        shownStats = statsToShow(cls);
        hitBoxes.clear();
        // Drop plans the player can no longer afford (points spent elsewhere, data changed...).
        while (pendingTotal() > available() && !pending.isEmpty()) {
            ResourceKey<StatDefinition> any = pending.keySet().iterator().next();
            pending.merge(any, -1, Integer::sum);
            pending.values().removeIf(v -> v <= 0);
        }

        int total = Math.min(screenWidth - 16, 560);
        int midLeft = left + leftWidth + 6;
        int restWidth = total - leftWidth - 6;
        int midWidth = (restWidth - 6) / 2;
        int rightLeft = midLeft + midWidth + 6;
        int rightWidth = restWidth - midWidth - 6;

        // ---- left: class, points, stats
        int bottom = Math.max(bottomOfLeft + 26, top + 22 + 3 * ROW + 6 + shownStats.size() * ROW + 32);
        panel(g, left, top, leftWidth, bottom - top);
        g.item(new ItemStack(cls.icon()), left + 6, top + 4);
        g.text(font, Component.translatable("pipican.levelup.title").withStyle(ChatFormatting.BOLD), left + 26, top + 4, COLOR_TITLE);
        g.text(font, cls.name(), left + 26, top + 13, COLOR_LABEL);

        int y = top + 26;
        int level = data().activeProgress().map(ClassProgress::level).orElse(1);
        row(g, font, left, leftWidth, y, Component.translatable("pipican.levelup.level"), String.valueOf(level), null);
        y += ROW;
        int avail = available();
        int left2 = avail - pendingTotal();
        row(g, font, left, leftWidth, y, Component.translatable("pipican.levelup.points"), String.valueOf(avail),
                hasPending() ? new Change(String.valueOf(left2), COLOR_DOWN) : null);
        y += ROW;
        long toNext = StatPoints.xpToNextPoint(data());
        row(g, font, left, leftWidth, y, Component.translatable("pipican.levelup.next_point"),
                toNext == Long.MAX_VALUE ? "-" : toNext + " XP", null);
        y += ROW;
        g.fill(left + 6, y + 2, left + leftWidth - 6, y + 3, COLOR_BORDER);
        y += 6;

        int valueX = left + leftWidth - 74;
        int gradeX = valueX - 16;
        for (int i = 0; i < shownStats.size(); i++) {
            ResourceKey<StatDefinition> key = shownStats.get(i);
            StatDefinition stat = stat(key);
            if (stat == null) {
                continue;
            }
            boolean raisable = canRaise(cls, key, stat);
            int planned = pending.getOrDefault(key, 0);
            boolean hover = mouseX >= left + 2 && mouseX < left + leftWidth - 2 && mouseY >= y - 1 && mouseY < y + ROW - 1;
            if (hover) {
                g.fill(left + 3, y - 1, left + leftWidth - 3, y + ROW - 2, 0x30FFFFFF);
            }
            g.fill(left + 6, y + 1, left + 8, y + 9, stat.color() == 0xFFFFFFFF ? COLOR_BORDER : stat.color());
            Component name = stat.name().copy();
            if (cls.isSpecialStat(key)) {
                name = name.copy().withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            g.text(font, clip(font, name, gradeX - left - 16), left + 12, y + 1, raisable ? COLOR_LABEL : COLOR_DIM);
            String grade = grade(cls.growth(key));
            g.text(font, grade, gradeX, y + 1, gradeColor(grade));

            double now = total(cls, key);
            g.text(font, fmtInt(now), valueX, y + 1, COLOR_VALUE);
            if (planned > 0) {
                g.text(font, ">", valueX + 20, y + 1, COLOR_DIM);
                g.text(font, fmtInt(now + planned), valueX + 28, y + 1, COLOR_UP);
            }
            if (raisable) {
                boolean canAdd = left2 > 0 && trained(cls, key) + planned < stat.max();
                smallButton(g, font, left + leftWidth - 30, y, "-", planned > 0, i, -1, mouseX, mouseY);
                smallButton(g, font, left + leftWidth - 17, y, "+", canAdd, i, 1, mouseX, mouseY);
            }
            if (hover && !overBox(mouseX, mouseY)) {
                g.setComponentTooltipForNextFrame(font, statTooltip(cls, key, stat, now), mouseX, mouseY);
            }
            y += ROW;
        }
        bottomOfLeft = y + 4;

        // ---- middle: attributes
        Map<Holder<Attribute>, double[]> attrs = attributePreview(cls);
        int midBottom = Math.max(bottom, top + 26 + (attrs.size() + 2) * ROW);
        panel(g, midLeft, top, midWidth, midBottom - top);
        g.text(font, Component.translatable("pipican.levelup.attributes"), midLeft + 6, top + 6, COLOR_TITLE);
        y = top + 22;
        Player player = minecraft.player;
        for (var e : attrs.entrySet()) {
            double now = player.getAttributeValue(e.getKey());
            double after = now + e.getValue()[0];
            Component label = Component.translatable(e.getKey().value().getDescriptionId());
            row(g, font, midLeft, midWidth, y, clip(font, label, midWidth - 80), fmt(now), changeOf(now, after));
            y += ROW;
        }
        // The class resource (stamina, mana...) grows with the special stat.
        Optional<SpecialStat> special = cls.specialStat();
        if (special.isPresent() && special.get().resource().isPresent()) {
            SpecialStat.Resource res = special.get().resource().get();
            double points = total(cls, special.get().stat());
            int planned = pending.getOrDefault(special.get().stat(), 0);
            double nowMax = data().resourceMax();
            double afterMax = nowMax + (res.max(points + planned) - res.max(points));
            row(g, font, midLeft, midWidth, y, clip(font, Component.translatable("pipican.levelup.resource_max", res.name()), midWidth - 80),
                    fmt(nowMax), changeOf(nowMax, afterMax));
            y += ROW;
            double nowRegen = res.regenPerSecond(points);
            row(g, font, midLeft, midWidth, y, clip(font, Component.translatable("pipican.levelup.resource_regen", res.name()), midWidth - 80),
                    fmt(nowRegen), changeOf(nowRegen, res.regenPerSecond(points + planned)));
        }

        // ---- right: bonuses that grow with stats
        List<BonusLine> bonuses = bonusPreview(cls);
        int rightBottom = Math.max(midBottom, top + 26 + bonuses.size() * ROW);
        panel(g, rightLeft, top, rightWidth, rightBottom - top);
        g.text(font, Component.translatable("pipican.levelup.bonuses"), rightLeft + 6, top + 6, COLOR_TITLE);
        y = top + 22;
        for (BonusLine b : bonuses) {
            row(g, font, rightLeft, rightWidth, y, clip(font, b.label, rightWidth - 80), pct(b.now), changeOf(b.now, b.after, LevelUpPanel::pct));
            y += ROW;
        }
        if (bonuses.isEmpty()) {
            g.text(font, Component.translatable("pipican.levelup.no_bonuses"), rightLeft + 6, y, COLOR_DIM);
        }

        Component help = Component.translatable("pipican.levelup.help");
        g.text(font, help, left, Math.max(rightBottom, bottom) + 6, COLOR_DIM);
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

    /** Label on the left, value then "> new value" right-aligned, like Dark Souls' level-up columns. */
    private static void row(GuiGraphicsExtractor g, Font font, int x, int width, int y, Component label, String value, Change change) {
        g.text(font, label, x + 6, y + 1, COLOR_LABEL);
        int newX = x + width - 6 - 34;
        int oldX = newX - 44;
        g.text(font, value, oldX + 34 - font.width(value), y + 1, COLOR_VALUE);
        g.text(font, ">", oldX + 37, y + 1, COLOR_DIM);
        if (change != null) {
            g.text(font, change.text(), newX + 34 - font.width(change.text()), y + 1, change.color());
        } else {
            g.text(font, value, newX + 34 - font.width(value), y + 1, COLOR_DIM);
        }
    }

    private void smallButton(GuiGraphicsExtractor g, Font font, int x, int y, String label, boolean active, int index, int delta, int mouseX, int mouseY) {
        int size = 11;
        boolean hover = active && mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
        g.fill(x, y, x + size, y + size, active ? (hover ? 0xFFB09050 : COLOR_BORDER) : 0xFF3A3A3A);
        g.fill(x + 1, y + 1, x + size - 1, y + size - 1, hover ? 0xFF3A3020 : 0xFF1C1A16);
        g.text(font, label, x + (size - font.width(label)) / 2 + 1, y + 2, active ? COLOR_TITLE : COLOR_DIM);
        if (active) {
            hitBoxes.add(new int[] {x, y, size, index, delta});
        }
    }

    private boolean overBox(int mx, int my) {
        for (int[] box : hitBoxes) {
            if (mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[2]) {
                return true;
            }
        }
        return false;
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COLOR_BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, COLOR_PANEL);
        g.fill(x + 3, y + 17, x + w - 3, y + 18, 0x807A6640);
    }

    private static Component clip(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String s = text.getString();
        while (!s.isEmpty() && font.width(s + "..") > width) {
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
