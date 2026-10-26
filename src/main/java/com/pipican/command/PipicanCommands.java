package com.pipican.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.pipican.Pipican;
import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanConfig;
import com.pipican.core.PipicanRegistries;
import com.pipican.mastery.Skill;
import com.pipican.network.OpenSkillTreePayload;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassProgress;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.MasteryService;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
import com.pipican.trinket.TrinketMenu;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /pipican ...} commands.
 *
 * <pre>
 * Admin (gamemaster permission):
 *   /pipican class set &lt;players&gt; &lt;class&gt; [giveKit]
 *   /pipican class reset &lt;players&gt;               full respec, re-opens class selection
 *   /pipican level set &lt;players&gt; &lt;level&gt;
 *   /pipican xp add &lt;players&gt; &lt;amount&gt;
 *   /pipican tree reset &lt;players&gt; [general|special]
 *   /pipican mastery add &lt;players&gt; &lt;skill&gt; &lt;amount&gt;
 *   /pipican mastery set &lt;players&gt; &lt;skill&gt; &lt;tier 0-5&gt;
 *   /pipican mastery reset &lt;players&gt;           also resets the trees
 *   /pipican stat reset &lt;players&gt;                refunds vanilla levels
 * Everyone:
 *   /pipican class info [player]
 *   /pipican class choose                         re-opens selection if you have no class
 *   /pipican tree                                 opens the passive-tree screen
 *   /pipican tree allocate|refund &lt;general|special&gt; &lt;node&gt;
 *   /pipican mastery list [player]
 *   /pipican stat raise &lt;stat&gt;                   costs vanilla XP levels
 *   /pipican trinkets
 * </pre>
 * Stat raising is a stand-in until the character screen exists.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class PipicanCommands {

    private static final DynamicCommandExceptionType UNKNOWN_CLASS = new DynamicCommandExceptionType(id -> Component.translatable("pipican.class.unknown", String.valueOf(id)));
    private static final DynamicCommandExceptionType UNKNOWN_SKILL = new DynamicCommandExceptionType(id -> Component.translatable("pipican.mastery.unknown", String.valueOf(id)));
    private static final DynamicCommandExceptionType UNKNOWN_STAT = new DynamicCommandExceptionType(id -> Component.translatable("pipican.stat.unknown", String.valueOf(id)));

    private PipicanCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> admin(String name) {
        return Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Pipican.MOD_ID)
                .then(Commands.literal("class")
                        .then(admin("set").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("class", ResourceKeyArgument.key(PipicanRegistries.CLASS))
                                        .executes(c -> setClass(c, true))
                                        .then(Commands.argument("giveKit", BoolArgumentType.bool())
                                                .executes(c -> setClass(c, BoolArgumentType.getBool(c, "giveKit")))))))
                        .then(admin("reset").then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forEach(c, ClassService::resetClass))))
                        .then(Commands.literal("info")
                                .executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                        .then(Commands.literal("choose").executes(c -> {
                            ClassService.promptSelection(c.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("level")
                        .then(admin("set").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(c -> forEach(c, p -> ClassService.setLevel(p, IntegerArgumentType.getInteger(c, "level"))))))))
                .then(Commands.literal("xp")
                        .then(admin("add").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(c -> forEach(c, p -> ClassService.addXp(p, LongArgumentType.getLong(c, "amount"))))))))
                .then(Commands.literal("tree")
                        .executes(c -> {
                            ServerPlayer player = c.getSource().getPlayerOrException();
                            if (player.connection.hasChannel(OpenSkillTreePayload.TYPE)) {
                                PacketDistributor.sendToPlayer(player, OpenSkillTreePayload.INSTANCE);
                            }
                            return 1;
                        })
                        .then(Commands.literal("allocate")
                                .then(nodeCommand(TreeKind.GENERAL, false))
                                .then(nodeCommand(TreeKind.SPECIAL, false)))
                        .then(Commands.literal("refund")
                                .then(nodeCommand(TreeKind.GENERAL, true))
                                .then(nodeCommand(TreeKind.SPECIAL, true)))
                        .then(admin("reset").then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forEach(c, p -> ClassService.resetSkills(p, Optional.empty())))
                                .then(Commands.literal("general").executes(c -> forEach(c, p -> ClassService.resetSkills(p, Optional.of(TreeKind.GENERAL)))))
                                .then(Commands.literal("special").executes(c -> forEach(c, p -> ClassService.resetSkills(p, Optional.of(TreeKind.SPECIAL))))))))
                .then(Commands.literal("mastery")
                        .then(Commands.literal("list")
                                .executes(c -> masteryList(c.getSource(), c.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(c -> masteryList(c.getSource(), EntityArgument.getPlayer(c, "target")))))
                        .then(admin("add").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", ResourceKeyArgument.key(PipicanRegistries.SKILL))
                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                .executes(c -> withSkill(c, (p, key, skill) ->
                                                        MasteryService.addMastery(p, key, skill, DoubleArgumentType.getDouble(c, "amount"))))))))
                        .then(admin("set").then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("skill", ResourceKeyArgument.key(PipicanRegistries.SKILL))
                                        .then(Commands.argument("tier", IntegerArgumentType.integer(0, Skill.TIERS))
                                                .executes(c -> withSkill(c, (p, key, skill) ->
                                                        MasteryService.setTier(p, key, skill, IntegerArgumentType.getInteger(c, "tier"))))))))
                        .then(admin("reset").then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forEach(c, MasteryService::resetAll)))))
                .then(Commands.literal("stat")
                        .then(Commands.literal("raise").then(Commands.argument("stat", ResourceKeyArgument.key(PipicanRegistries.STAT))
                                .executes(c -> {
                                    ServerPlayer player = c.getSource().getPlayerOrException();
                                    ResourceKey<StatDefinition> stat = ResourceKeyArgument.getRegistryKey(c, "stat", PipicanRegistries.STAT, UNKNOWN_STAT);
                                    return report(c.getSource(), ClassService.raiseStat(player, stat));
                                })))
                        .then(admin("reset").then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forEach(c, ClassService::resetStats)))))
                .then(Commands.literal("trinkets").executes(c -> {
                    TrinketMenu.open(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> nodeCommand(TreeKind kind, boolean refund) {
        return Commands.literal(kind.getSerializedName()).then(Commands.argument("node", StringArgumentType.word())
                .suggests((c, builder) -> {
                    ServerPlayer player = c.getSource().getPlayer();
                    if (player == null) {
                        return builder.buildFuture();
                    }
                    return SharedSuggestionProvider.suggest(refund ? ClassService.ownedNodeIds(player, kind) : ClassService.nodeIds(player, kind), builder);
                })
                .executes(c -> {
                    ServerPlayer player = c.getSource().getPlayerOrException();
                    String node = StringArgumentType.getString(c, "node");
                    return report(c.getSource(), refund ? ClassService.refundNode(player, kind, node) : ClassService.allocateNode(player, kind, node));
                }));
    }

    @FunctionalInterface
    private interface SkillCommand {
        void apply(ServerPlayer player, ResourceKey<Skill> key, Skill skill);
    }

    private static int withSkill(CommandContext<CommandSourceStack> c, SkillCommand action) throws CommandSyntaxException {
        ResourceKey<Skill> key = ResourceKeyArgument.getRegistryKey(c, "skill", PipicanRegistries.SKILL, UNKNOWN_SKILL);
        Skill skill = ClassLookup.skills(c.getSource().getServer().registryAccess()).getValue(key);
        if (skill == null) {
            throw UNKNOWN_SKILL.create(key.identifier());
        }
        return forEach(c, p -> action.apply(p, key, skill));
    }

    /** Lists every skill the player can train, with tier and progress to the next tier. */
    private static int masteryList(CommandSourceStack source, ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        var skills = ClassLookup.skills(player.level().registryAccess()).entrySet().stream()
                .filter(e -> e.getValue().allowedFor(data.activeClass()))
                .sorted(java.util.Comparator.comparing(e -> e.getKey().identifier().toString()))
                .toList();
        if (skills.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("pipican.mastery.none"), false);
            return 0;
        }
        for (var e : skills) {
            Skill skill = e.getValue();
            double mastery = data.mastery(e.getKey());
            int tier = skill.tierFor(mastery);
            String next = tier >= Skill.TIERS ? "MAX" : formatNumber(mastery) + "/" + skill.thresholds().get(tier);
            source.sendSuccess(() -> Component.literal(" - ").append(skill.name())
                    .append(" " + MasteryService.numeral(tier) + "  (" + next + ")"), false);
        }
        return skills.size();
    }

    private static int setClass(CommandContext<CommandSourceStack> c, boolean giveKit) throws CommandSyntaxException {
        ResourceKey<RpgClass> key = ResourceKeyArgument.getRegistryKey(c, "class", PipicanRegistries.CLASS, UNKNOWN_CLASS);
        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(c, "targets")) {
            ClassService.Result result = ClassService.chooseClass(player, key, giveKit, true);
            c.getSource().sendSuccess(() -> Component.literal(player.getName().getString() + ": ").append(result.message()), true);
            count += result.success() ? 1 : 0;
        }
        return count;
    }

    private static int forEach(CommandContext<CommandSourceStack> c, Consumer<ServerPlayer> action) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        players.forEach(action);
        c.getSource().sendSuccess(() -> Component.translatable("pipican.command.done", players.size()), true);
        return players.size();
    }

    private static int report(CommandSourceStack source, ClassService.Result result) {
        if (result.success()) {
            source.sendSuccess(result::message, false);
            return 1;
        }
        source.sendFailure(result.message());
        return 0;
    }

    /** Prints a player's class sheet: class, level, points, stats, resource. */
    private static int info(CommandSourceStack source, ServerPlayer player) {
        PlayerClassData data = ClassService.data(player);
        Optional<RpgClass> rpgClass = ClassService.activeClass(player);
        if (rpgClass.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("pipican.info.no_class", player.getName()), false);
            return 0;
        }
        RpgClass cls = rpgClass.get();
        ClassProgress progress = data.activeProgress().orElseThrow();
        ClassState state = ClassEffectApplier.state(player);
        var access = player.level().registryAccess();

        source.sendSuccess(() -> Component.translatable("pipican.info.header", player.getName(), cls.name()).withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.translatable("pipican.info.level", progress.level(), cls.progression().maxLevel(),
                progress.xp(), PipicanConfig.xpToNextLevel(progress.level())), false);
        source.sendSuccess(() -> Component.translatable("pipican.info.points",
                ClassService.availablePoints(player, TreeKind.GENERAL), ClassService.availablePoints(player, TreeKind.SPECIAL)), false);
        ClassLookup.stats(access).entrySet().stream()
                .filter(e -> state.stat(e.getKey()) != 0 || ClassService.canRaise(cls, e.getKey(), e.getValue()))
                .sorted(java.util.Comparator.comparing(e -> e.getKey().identifier().toString()))
                .forEach(e -> {
                    boolean special = cls.isSpecialStat(e.getKey());
                    Component line = Component.literal(special ? " ★ " : " - ").append(e.getValue().name())
                            .append(": " + formatNumber(state.stat(e.getKey())) + " (bought " + data.boughtStat(e.getKey()) + ")");
                    source.sendSuccess(() -> line, false);
                });
        if (data.resourceMax() > 0) {
            Component name = cls.specialStat().flatMap(s -> s.resource()).map(r -> r.name()).orElse(Component.empty());
            source.sendSuccess(() -> Component.translatable("pipican.info.resource", name, (int) data.resource(), (int) data.resourceMax()), false);
        }
        source.sendSuccess(() -> Component.translatable("pipican.info.next_stat_cost", PipicanConfig.statPointCost(data.totalBoughtStats())), false);
        return 1;
    }

    private static String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
