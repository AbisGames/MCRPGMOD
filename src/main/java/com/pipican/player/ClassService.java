package com.pipican.player;

import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanConfig;
import com.pipican.network.OpenClassSelectionPayload;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.NodeType;
import com.pipican.skill.SkillNode;
import com.pipican.skill.SkillTree;
import com.pipican.skill.TreeKind;
import com.pipican.skill.TreePoints;
import com.pipican.skill.TreeRef;
import com.pipican.stat.StatDefinition;
import com.pipican.stat.StatPoints;
import com.pipican.trinket.TrinketSlotType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * All the ways a player's class data can change. Commands, network packets and event handlers
 * call these methods - nothing else should mutate {@link PlayerClassData} directly.
 *
 * <p>Each method validates its input, mutates the data, then calls {@link #changed} which
 * re-applies bonuses and syncs to the client.
 */
public final class ClassService {

    /** Outcome of a player action, with a message to show them. */
    public record Result(boolean success, Component message) {
        public static Result ok(Component message) {
            return new Result(true, message);
        }

        public static Result fail(Component message) {
            return new Result(false, message);
        }
    }

    private ClassService() {}

    public static PlayerClassData data(ServerPlayer player) {
        return player.getData(PipicanAttachments.CLASS_DATA);
    }

    // ------------------------------------------------------------------ class choice

    /**
     * Gives the player a class. Players may only do this while they have no class; admins
     * ({@code force}) can always do it. Keeps earlier progress in that class if there is any.
     */
    public static Result chooseClass(ServerPlayer player, ResourceKey<RpgClass> key, boolean giveKit, boolean force) {
        PlayerClassData data = data(player);
        if (data.hasClass() && !force) {
            return Result.fail(Component.translatable("pipican.class.already_chosen"));
        }
        Optional<RpgClass> rpgClass = ClassLookup.rpgClass(player.level().registryAccess(), key);
        if (rpgClass.isEmpty()) {
            return Result.fail(Component.translatable("pipican.class.unknown", key.identifier().toString()));
        }
        if (!rpgClass.get().selectable() && !force) {
            return Result.fail(Component.translatable("pipican.class.not_selectable"));
        }

        data.setActiveClass(Optional.of(key));
        data.activeProgress();
        data.abilityActive().clear();
        if (giveKit) {
            for (var template : rpgClass.get().startingKit()) {
                giveOrDrop(player, template.create());
            }
        }
        changed(player);
        // Start fresh: full health, full resource if the class wants it.
        player.setHealth(player.getMaxHealth());
        boolean startFull = rpgClass.get().specialStat().flatMap(s -> s.resource()).map(r -> r.startsFull()).orElse(false);
        data.setResource(startFull ? data.resourceMax() : 0);
        player.syncData(PipicanAttachments.CLASS_DATA);
        return Result.ok(Component.translatable("pipican.class.chosen", rpgClass.get().name()));
    }

    /** Admin respec: removes the class and all class progress, returns trinkets, re-opens selection. */
    public static void resetClass(ServerPlayer player) {
        PlayerClassData data = data(player);
        data.setActiveClass(Optional.empty());
        data.allProgress().clear();
        data.abilityActive().clear();
        data.abilityCooldowns().clear();
        data.setResource(0);
        changed(player);
        promptSelection(player);
    }

    /** Opens the class-selection screen on the client if the player has no class. */
    public static void promptSelection(ServerPlayer player) {
        if (!data(player).hasClass() && !ClassLookup.selectableClasses(player.level().registryAccess()).isEmpty()
                // Only clients running the mod understand this packet (fake/test players don't).
                && player.connection.hasChannel(OpenClassSelectionPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, OpenClassSelectionPayload.INSTANCE);
        }
    }

    // ------------------------------------------------------------------ levels & xp

    /**
     * Adds kill XP: class XP (levelling up as many times as it covers) and lifetime stat XP
     * (which earns stat points, see {@link StatPoints}).
     */
    public static void addXp(ServerPlayer player, long amount) {
        PlayerClassData data = data(player);
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty() || amount <= 0) {
            return;
        }
        int pointsBefore = StatPoints.earned(data);
        data.setStatXp(saturatedAdd(data.statXp(), amount));
        int newPoints = StatPoints.earned(data) - pointsBefore;
        if (newPoints > 0) {
            player.sendSystemMessage(Component.translatable("pipican.stat.points_earned", newPoints,
                    StatPoints.available(player.level().registryAccess(), data)).withStyle(ChatFormatting.AQUA));
        }
        ClassProgress progress = data.activeProgress().orElseThrow();
        int maxLevel = rpgClass.get().progression().maxLevel();
        int startLevel = progress.level();
        long xp = progress.xp() + amount;
        while (progress.level() < maxLevel && xp >= PipicanConfig.xpToNextLevel(progress.level())) {
            xp -= PipicanConfig.xpToNextLevel(progress.level());
            progress.setLevel(progress.level() + 1);
        }
        progress.setXp(progress.level() >= maxLevel ? 0 : xp);
        if (progress.level() > startLevel) {
            player.sendSystemMessage(Component.translatable("pipican.level.up", rpgClass.get().name(), progress.level()).withStyle(ChatFormatting.GOLD));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.2f);
            changed(player);
        } else {
            player.syncData(PipicanAttachments.CLASS_DATA);
        }
    }

    public static void setLevel(ServerPlayer player, int level) {
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty()) {
            return;
        }
        ClassProgress progress = data(player).activeProgress().orElseThrow();
        progress.setLevel(Math.clamp(level, 1, rpgClass.get().progression().maxLevel()));
        progress.setXp(0);
        changed(player);
    }

    // ------------------------------------------------------------------ passive trees

    /** Unspent points in a tree: skill points for general/special, stat points for the stat tree (see {@link TreePoints}). */
    public static int availablePoints(ServerPlayer player, TreeKind kind) {
        return activeClass(player)
                .map(c -> TreePoints.available(player.level().registryAccess(), data(player), c, kind))
                .orElse(0);
    }

    /** Allocates a node: it must touch the class's start node or an owned node (Path of Exile rules). */
    public static Result allocateNode(ServerPlayer player, TreeKind kind, String nodeId) {
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty()) {
            return Result.fail(Component.translatable("pipican.class.none"));
        }
        Optional<TreeRef> ref = rpgClass.get().tree(kind);
        Optional<SkillTree> tree = ClassLookup.tree(player.level().registryAccess(), rpgClass.get(), kind);
        Optional<SkillNode> node = tree.flatMap(t -> t.node(nodeId));
        if (ref.isEmpty() || node.isEmpty()) {
            return Result.fail(Component.translatable("pipican.skill.unknown", nodeId));
        }
        ClassProgress progress = data(player).activeProgress().orElseThrow();
        SkillNode n = node.get();
        Set<String> owned = progress.nodes(kind);
        if (owned.contains(nodeId) || nodeId.equals(ref.get().start())) {
            return Result.fail(Component.translatable("pipican.skill.owned", n.name()));
        }
        if (n.type() == NodeType.START) {
            return Result.fail(Component.translatable("pipican.skill.other_start", n.name()));
        }
        if (!tree.get().isReachable(nodeId, ref.get().start(), owned)) {
            return Result.fail(Component.translatable("pipican.skill.not_connected", n.name()));
        }
        if (progress.level() < n.requiredLevel()) {
            return Result.fail(Component.translatable("pipican.skill.level_required", n.name(), n.requiredLevel()));
        }
        if (availablePoints(player, kind) < n.cost()) {
            return Result.fail(Component.translatable("pipican.skill.no_points", n.cost()));
        }
        owned.add(nodeId);
        changed(player);
        return Result.ok(Component.translatable("pipican.skill.allocated", n.name()));
    }

    /**
     * Refunds a node, giving its point back. Costs vanilla XP levels (config) and is refused if it
     * would disconnect other owned nodes from the start node.
     */
    public static Result refundNode(ServerPlayer player, TreeKind kind, String nodeId) {
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty()) {
            return Result.fail(Component.translatable("pipican.class.none"));
        }
        ClassProgress progress = data(player).activeProgress().orElseThrow();
        Set<String> owned = progress.nodes(kind);
        Optional<TreeRef> ref = rpgClass.get().tree(kind);
        Optional<SkillTree> tree = ClassLookup.tree(player.level().registryAccess(), rpgClass.get(), kind);
        if (!owned.contains(nodeId) || ref.isEmpty() || tree.isEmpty()) {
            return Result.fail(Component.translatable("pipican.skill.not_owned", nodeId));
        }
        Component name = tree.get().node(nodeId).map(SkillNode::name).orElse(Component.literal(nodeId));
        if (!tree.get().canRemove(nodeId, ref.get().start(), owned)) {
            return Result.fail(Component.translatable("pipican.skill.would_disconnect", name));
        }
        int cost = player.getAbilities().instabuild ? 0 : PipicanConfig.refundCostLevels();
        if (player.experienceLevel < cost) {
            return Result.fail(Component.translatable("pipican.skill.refund_no_xp", cost));
        }
        player.giveExperienceLevels(-cost);
        owned.remove(nodeId);
        changed(player);
        return Result.ok(Component.translatable("pipican.skill.refunded", name, cost));
    }

    /** Admin respec of one or both trees; points come back automatically (they are derived). */
    public static void resetSkills(ServerPlayer player, Optional<TreeKind> kind) {
        data(player).activeProgress().ifPresent(progress -> {
            for (TreeKind k : TreeKind.values()) {
                if (kind.isEmpty() || kind.get() == k) {
                    progress.nodes(k).clear();
                }
            }
        });
        changed(player);
    }

    // ------------------------------------------------------------------ stats (paid with stat points from kill XP)

    /** True if the player may put bought points into this stat. */
    public static boolean canRaise(RpgClass rpgClass, ResourceKey<StatDefinition> key, StatDefinition stat) {
        return !stat.exclusive() || rpgClass.isSpecialStat(key);
    }

    /** Unspent stat points, shared by direct stat raises and the stat tree. */
    public static int availableStatPoints(ServerPlayer player) {
        return StatPoints.available(player.level().registryAccess(), data(player));
    }

    /** Puts one stat point into a stat. */
    public static Result raiseStat(ServerPlayer player, ResourceKey<StatDefinition> key) {
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty()) {
            return Result.fail(Component.translatable("pipican.class.none"));
        }
        StatDefinition stat = ClassLookup.stats(player.level().registryAccess()).getValue(key);
        if (stat == null) {
            return Result.fail(Component.translatable("pipican.stat.unknown", key.identifier().toString()));
        }
        if (!canRaise(rpgClass.get(), key, stat)) {
            return Result.fail(Component.translatable("pipican.stat.exclusive", stat.name()));
        }
        PlayerClassData data = data(player);
        int current = rpgClass.get().baseStat(key) + data.boughtStat(key);
        if (current >= stat.max()) {
            return Result.fail(Component.translatable("pipican.stat.maxed", stat.name()));
        }
        if (availableStatPoints(player) < 1) {
            return Result.fail(Component.translatable("pipican.stat.no_points"));
        }
        data.boughtStats().merge(key, 1, Integer::sum);
        changed(player);
        return Result.ok(Component.translatable("pipican.stat.raised", stat.name(), current + 1, availableStatPoints(player)));
    }

    /**
     * Puts several stat points in at once (the level-up screen's "Accept"). All or nothing: if any stat can't take
     * its points, or the total is more than the player has, nothing changes.
     */
    public static Result raiseStats(ServerPlayer player, Map<ResourceKey<StatDefinition>, Integer> points) {
        Optional<RpgClass> rpgClass = activeClass(player);
        if (rpgClass.isEmpty()) {
            return Result.fail(Component.translatable("pipican.class.none"));
        }
        PlayerClassData data = data(player);
        long total = 0;
        for (Map.Entry<ResourceKey<StatDefinition>, Integer> e : points.entrySet()) {
            int amount = e.getValue();
            if (amount <= 0) {
                continue;
            }
            StatDefinition stat = ClassLookup.stats(player.level().registryAccess()).getValue(e.getKey());
            if (stat == null) {
                return Result.fail(Component.translatable("pipican.stat.unknown", e.getKey().identifier().toString()));
            }
            if (!canRaise(rpgClass.get(), e.getKey(), stat)) {
                return Result.fail(Component.translatable("pipican.stat.exclusive", stat.name()));
            }
            if ((long) rpgClass.get().baseStat(e.getKey()) + data.boughtStat(e.getKey()) + amount > stat.max()) {
                return Result.fail(Component.translatable("pipican.stat.maxed", stat.name()));
            }
            total += amount;
        }
        if (total == 0) {
            return Result.fail(Component.translatable("pipican.stat.nothing"));
        }
        if (availableStatPoints(player) < total) {
            return Result.fail(Component.translatable("pipican.stat.no_points"));
        }
        points.forEach((key, amount) -> {
            if (amount > 0) {
                data.boughtStats().merge(key, amount, Integer::sum);
            }
        });
        changed(player);
        return Result.ok(Component.translatable("pipican.stat.level_up", total, availableStatPoints(player)));
    }

    /** Admin respec of stats put in directly; the points come back automatically (they are derived). */
    public static void resetStats(ServerPlayer player) {
        data(player).boughtStats().clear();
        changed(player);
    }

    // ------------------------------------------------------------------ trinkets

    /**
     * Makes stored trinket lists match the class's slot counts. Items in slots the class no longer
     * has are returned to the player (or dropped if the inventory is full).
     */
    public static void reconcileTrinkets(ServerPlayer player) {
        PlayerClassData data = data(player);
        Map<ResourceKey<TrinketSlotType>, Integer> allowed = activeClass(player).map(RpgClass::trinketSlots).orElse(Map.of());
        for (Map.Entry<ResourceKey<TrinketSlotType>, List<ItemStack>> e : data.allTrinkets().entrySet()) {
            int count = allowed.getOrDefault(e.getKey(), 0);
            List<ItemStack> stacks = e.getValue();
            while (stacks.size() > count) {
                giveOrDrop(player, stacks.removeLast());
            }
        }
        allowed.forEach((slot, count) -> {
            List<ItemStack> stacks = data.trinkets(slot);
            while (stacks.size() < count) {
                stacks.add(ItemStack.EMPTY);
            }
        });
    }

    /** Drops all trinkets on the ground (death without keepInventory). */
    public static void dropTrinkets(ServerPlayer player) {
        for (List<ItemStack> stacks : data(player).allTrinkets().values()) {
            for (int i = 0; i < stacks.size(); i++) {
                if (!stacks.get(i).isEmpty()) {
                    player.drop(stacks.get(i), true, false);
                    stacks.set(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private static long saturatedAdd(long a, long b) {
        long sum = a + b;
        return ((a ^ sum) & (b ^ sum)) < 0 ? Long.MAX_VALUE : sum;
    }

    public static Optional<RpgClass> activeClass(ServerPlayer player) {
        return data(player).activeClass().flatMap(key -> ClassLookup.rpgClass(player.level().registryAccess(), key));
    }

    /** Call after any change: fixes trinket slots, re-applies bonuses and syncs to the client. */
    public static void changed(ServerPlayer player) {
        reconcileTrinkets(player);
        ClassEffectApplier.refresh(player);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    /** Ids of every node in a tree, for command suggestions. */
    public static List<String> nodeIds(ServerPlayer player, TreeKind kind) {
        List<String> ids = new ArrayList<>();
        activeClass(player).flatMap(c -> ClassLookup.tree(player.level().registryAccess(), c, kind))
                .ifPresent(t -> t.nodes().forEach(n -> ids.add(n.id())));
        return ids;
    }

    /** Ids of the nodes the player owns in a tree, for command suggestions. */
    public static List<String> ownedNodeIds(ServerPlayer player, TreeKind kind) {
        return data(player).activeProgress().map(p -> List.copyOf(p.nodes(kind))).orElse(List.of());
    }
}
