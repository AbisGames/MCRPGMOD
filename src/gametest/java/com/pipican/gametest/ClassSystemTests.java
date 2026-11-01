package com.pipican.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import com.pipican.ability.FavoredWeapon;
import com.pipican.ability.Auras;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.LivingEntity;
import com.pipican.effect.VictimState;
import com.pipican.ability.Bleeds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.pipican.condition.type.StillCondition;
import com.pipican.effect.type.AttributeEffect;
import com.pipican.ability.action.BoostAction;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.Mounts;
import com.pipican.ability.Movement;
import com.pipican.ability.Boosts;
import com.pipican.effect.type.ReactionEffect;
import com.pipican.ability.Reactions;
import com.pipican.ability.Combat;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import com.pipican.effect.type.BonusEffect;
import com.pipican.Pipican;
import com.pipican.ability.Ability;
import com.pipican.ability.AbilityService;
import com.pipican.ability.AbilitySlots;
import com.pipican.core.ClassLookup;
import com.pipican.core.PipicanConfig;
import com.pipican.core.PipicanRegistries;
import com.pipican.mastery.MoveMode;
import com.pipican.mastery.Skill;
import com.pipican.mastery.SkillAction;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.MasteryService;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.Affinity;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.NodeType;
import com.pipican.skill.SkillTree;
import com.pipican.skill.SkillTrees;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
import com.pipican.stat.StatPoints;
import com.pipican.trinket.TrinketSlotType;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GameTests for the class system, using the hidden {@code pipican_test:tester} class from
 * src/gametest/resources. Run with {@code gradlew runGameTestServer}, or in a dev client with
 * {@code /test runall}.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class ClassSystemTests {

    private static final ResourceKey<RpgClass> TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "tester"));
    private static final ResourceKey<RpgClass> CONDITION_TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "condition_tester"));
    private static final ResourceKey<RpgClass> ABILITY_TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "ability_tester"));
    private static final ResourceKey<RpgClass> MECHANICS_TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "mechanics_tester"));
    private static final ResourceKey<RpgClass> STAT_TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "stat_tester"));
    private static final ResourceKey<RpgClass> BONUS_TESTER = ResourceKey.create(PipicanRegistries.CLASS, Identifier.fromNamespaceAndPath("pipican_test", "bonus_tester"));
    private static final ResourceKey<StatDefinition> MIGHT = ResourceKey.create(PipicanRegistries.STAT, Identifier.fromNamespaceAndPath("pipican_test", "might"));
    private static final ResourceKey<StatDefinition> FOCUS = ResourceKey.create(PipicanRegistries.STAT, Identifier.fromNamespaceAndPath("pipican_test", "focus"));
    private static final ResourceKey<Skill> BLADE = ResourceKey.create(PipicanRegistries.SKILL, Identifier.fromNamespaceAndPath("pipican_test", "blade"));
    private static final ResourceKey<Skill> RUNNER = ResourceKey.create(PipicanRegistries.SKILL, Identifier.fromNamespaceAndPath("pipican_test", "runner"));
    private static final ResourceKey<TrinketSlotType> RING = ResourceKey.create(PipicanRegistries.TRINKET_SLOT, Pipican.id("ring"));
    private static final ResourceKey<TrinketSlotType> AMULET = ResourceKey.create(PipicanRegistries.TRINKET_SLOT, Pipican.id("amulet"));

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("choose_class", ClassSystemTests::chooseClass);
        TESTS.put("skill_tree", ClassSystemTests::skillTree);
        TESTS.put("stat_tree", ClassSystemTests::statTree);
        TESTS.put("mastery", ClassSystemTests::mastery);
        TESTS.put("affinity_and_damage", ClassSystemTests::affinityAndDamage);
        TESTS.put("stats_and_levels", ClassSystemTests::statsAndLevels);
        TESTS.put("trinkets", ClassSystemTests::trinkets);
        TESTS.put("shipped_classes", ClassSystemTests::shippedClasses);
        TESTS.put("conditions_and_resource", ClassSystemTests::conditionsAndResource);
        TESTS.put("abilities", ClassSystemTests::abilities);
        TESTS.put("shipped_abilities", ClassSystemTests::shippedAbilities);
        TESTS.put("ability_mechanics", ClassSystemTests::abilityMechanics);
        TESTS.put("bonuses", ClassSystemTests::bonuses);
        TESTS.put("class_mechanics", ClassSystemTests::classMechanics);
        TESTS.put("stat_effects", ClassSystemTests::statEffects);
        TESTS.put("ores", ClassSystemTests::ores);
        TESTS.put("gear_sets", ClassSystemTests::gearSets);
        TESTS.put("special_ores", ClassSystemTests::specialOres);
        TESTS.put("ore_mechanics", ClassSystemTests::oreMechanics);
        TESTS.put("allomancy", ClassSystemTests::allomancy);
        TESTS.put("tamashigane", ClassSystemTests::tamashigane);
        TESTS.put("soul_paths_play", ClassSystemTests::soulPathsPlay);
        TESTS.put("skill_triggers", ClassSystemTests::skillTriggers);
        TESTS.put("ability_specs", ClassSystemTests::abilitySpecs);
    }

    private ClassSystemTests() {}

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach((name, fn) -> helper.register(Pipican.id(name), fn)));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(Pipican.id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS.keySet()) {
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, Pipican.id(name));
            event.registerTest(Pipican.id(name), new FunctionGameTestInstance(fn, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        }
    }

    // ------------------------------------------------------------------ helpers

    private static ServerPlayer testerPlayer(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ClassService.Result result = ClassService.chooseClass(player, TESTER, true, true);
        helper.assertTrue(result.success(), "chooseClass failed: " + result.message().getString());
        return player;
    }

    /** Removes a mock player so its class (auras...) can't reach players in other tests. */
    private static void dismiss(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
    }

    private static void assertNear(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 1.0e-4, what + ": expected " + expected + " but was " + actual);
    }

    // ------------------------------------------------------------------ tests

    /** Base health, stat-driven attributes, passives, special passives scaling, resource, kit. */
    private static void chooseClass(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        assertNear(helper, 30, player.getMaxHealth(), "max health from base_health");
        assertNear(helper, 30, player.getHealth(), "healed to full on choose");
        assertNear(helper, 1 + 4 * 0.5, player.getAttributeValue(Attributes.ATTACK_DAMAGE), "attack damage from 4 might");
        assertNear(helper, 3, player.getAttributeValue(Attributes.ARMOR), "armor passive");
        assertNear(helper, 5 * 0.5, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness scaling with special stat");
        PlayerClassData data = ClassService.data(player);
        assertNear(helper, 10 + 2 * 5, data.resourceMax(), "resource max");
        assertNear(helper, data.resourceMax(), data.resource(), "resource starts full");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.STONE_SWORD)), "starting kit given");
        helper.assertTrue(!ClassService.chooseClass(player, TESTER, false, false).success(), "players cannot re-pick a class");
        helper.succeed();
    }

    /** Path of Exile rules: adjacency, level gates, start nodes, refunds that would disconnect, merged files. */
    private static void skillTree(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        SkillTree tree = SkillTrees.get(helper.getLevel().registryAccess(), Identifier.fromNamespaceAndPath("pipican_test", "main")).orElseThrow();
        helper.assertTrue(tree.node("far").isPresent() && tree.node("tough").isPresent(), "tree files are merged");
        helper.assertTrue(tree.neighbours("tough").contains("brave"), "links are two-way");

        helper.assertTrue(!ClassService.allocateNode(player, TreeKind.GENERAL, "tough").success(), "no points without mastery");
        MasteryService.setTier(player, BLADE, skill(helper, BLADE), 3);
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.GENERAL) == 3, "one point per mastery tier");

        helper.assertTrue(!ClassService.allocateNode(player, TreeKind.GENERAL, "brave").success(), "brave not connected yet");
        helper.assertTrue(ClassService.allocateNode(player, TreeKind.GENERAL, "tough").success(), "tough touches the start");
        assertNear(helper, 32, player.getMaxHealth(), "tough gives +2 health");
        helper.assertTrue(!ClassService.allocateNode(player, TreeKind.GENERAL, "brave").success(), "brave needs level 3");
        ClassService.setLevel(player, 3);
        helper.assertTrue(ClassService.allocateNode(player, TreeKind.GENERAL, "brave").success(), "brave");
        assertNear(helper, 1.5, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), helper.getLevel().damageSources().generic()), "brave damage bonus");
        helper.assertTrue(!ClassService.allocateNode(player, TreeKind.GENERAL, "start_b").success(), "other class's start can't be bought");

        helper.assertTrue(!ClassService.refundNode(player, TreeKind.GENERAL, "tough").success(), "refunding tough would strand brave");
        helper.assertTrue(ClassService.refundNode(player, TreeKind.GENERAL, "brave").success(), "brave is a leaf");
        helper.assertTrue(ClassService.refundNode(player, TreeKind.GENERAL, "tough").success(), "tough is now a leaf");
        assertNear(helper, 30, player.getMaxHealth(), "refund removes bonus");
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.GENERAL) == 3, "refund gives points back");

        MasteryService.setTier(player, RUNNER, skill(helper, RUNNER), 1);
        helper.assertTrue(ClassService.allocateNode(player, TreeKind.SPECIAL, "zeal").success(), "special point from runner");
        assertNear(helper, 6, ClassEffectApplier.state(player).stat(FOCUS), "zeal adds a focus point");
        assertNear(helper, 3, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness follows focus");
        assertNear(helper, 22, ClassService.data(player).resourceMax(), "resource follows focus");
        helper.succeed();
    }

    /** The stat tree is paid with stat points (shared with raising stats), not with mastery. */
    private static void statTree(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        MasteryService.setTier(player, BLADE, skill(helper, BLADE), 5);
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.STAT) == 0, "mastery gives no stat-tree points");
        helper.assertTrue(!ClassService.allocateNode(player, TreeKind.STAT, "sturdy").success(), "no stat points yet");

        ClassService.addXp(player, StatPoints.threshold(2));
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.STAT) == 2, "stat tree uses stat points");
        helper.assertTrue(ClassService.allocateNode(player, TreeKind.STAT, "sturdy").success(), "sturdy costs 2");
        assertNear(helper, 34, player.getMaxHealth(), "sturdy gives +4 health");
        helper.assertTrue(ClassService.availableStatPoints(player) == 0, "the node spent the shared stat points");
        helper.assertTrue(!ClassService.raiseStat(player, FOCUS).success(), "nothing left to raise stats with");
        MasteryService.resetAll(player);
        assertNear(helper, 34, player.getMaxHealth(), "mastery reset keeps the stat tree");

        helper.assertTrue(ClassService.refundNode(player, TreeKind.STAT, "sturdy").success(), "refund");
        helper.assertTrue(ClassService.availableStatPoints(player) == 2, "refund gives the stat points back");
        helper.succeed();
    }

    /** Triggers, cooldown, affinity-scaled gain, continuous movement, tier effects. */
    private static void mastery(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        PlayerClassData data = ClassService.data(player);
        double masteryMult = PipicanConfig.tier(Affinity.MASTERY).mastery();

        MasteryService.onAction(player, new SkillAction.MeleeHit(new ItemStack(Items.STONE_SWORD)));
        assertNear(helper, masteryMult, data.mastery(BLADE), "sword hit, scaled by mastery affinity");
        MasteryService.onAction(player, new SkillAction.MeleeHit(new ItemStack(Items.STONE_SWORD)));
        assertNear(helper, masteryMult, data.mastery(BLADE), "second hit in the same tick is on cooldown");
        MasteryService.onAction(player, new SkillAction.MeleeHit(new ItemStack(Items.STICK)));
        assertNear(helper, masteryMult, data.mastery(BLADE), "non-sword doesn't count");

        MasteryService.onAction(player, new SkillAction.Moved(MoveMode.SPRINT, 2.5));
        MasteryService.onAction(player, new SkillAction.Moved(MoveMode.SPRINT, 1.0));
        MasteryService.onAction(player, new SkillAction.Moved(MoveMode.WALK, 50));
        assertNear(helper, 3.5, data.mastery(RUNNER), "sprinting counts per block, no cooldown, walking ignored");
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.SPECIAL) == 3, "runner tier 3 = 3 special points");

        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        var sources = helper.getLevel().damageSources();
        assertNear(helper, 1.1, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), sources.playerAttack(player)), "blade tier 1 bonus with a sword");
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        assertNear(helper, 1.0, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), sources.playerAttack(player)), "no bonus bare-handed");
        helper.succeed();
    }

    private static Skill skill(GameTestHelper helper, ResourceKey<Skill> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(PipicanRegistries.SKILL).getValue(key);
    }

    /** Weakness multipliers and equipment affinity tiers. */
    private static void affinityAndDamage(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        ClassState state = ClassEffectApplier.state(player);
        var sources = helper.getLevel().damageSources();
        assertNear(helper, 2, ClassState.multiplier(state.damageTaken(), sources.onFire()), "fire weakness");
        assertNear(helper, 1, ClassState.multiplier(state.damageTaken(), sources.generic()), "weakness only for fire");

        RpgClass cls = state.rpgClass().orElseThrow();
        helper.assertTrue(cls.affinities().resolve(new ItemStack(Items.IRON_SWORD)) == Affinity.FORBIDDEN, "specific rule beats tag");
        helper.assertTrue(cls.affinities().resolve(new ItemStack(Items.DIAMOND_SWORD)) == Affinity.MASTERY, "sword tag");
        helper.assertTrue(cls.affinities().resolve(new ItemStack(Items.DIRT)) == Affinity.NEUTRAL, "default tier");

        // Vanilla applies an item's own modifiers on the next entity tick, so check the modifier we add.
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        ClassEffectApplier.refresh(player);
        double forbiddenSpeed = PipicanConfig.tier(Affinity.FORBIDDEN).attackSpeed();
        assertNear(helper, forbiddenSpeed - 1, modifier(player, Attributes.ATTACK_SPEED, "class/add_multiplied_total"), "forbidden weapon attack speed");

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        ClassEffectApplier.refresh(player);
        // Heavy armor is clumsy for this class, and an iron chestplate needs 3 Strength it doesn't have: one tier worse.
        double forbiddenArmor = PipicanConfig.tier(Affinity.FORBIDDEN).armor();
        assertNear(helper, 3 + 6 * (forbiddenArmor - 1), modifier(player, Attributes.ARMOR, "class/add_value"), "clumsy heavy armor without the strength");
        double forbiddenMove = PipicanConfig.tier(Affinity.FORBIDDEN).movePenaltyPerArmorPiece();
        assertNear(helper, forbiddenMove - 1, modifier(player, Attributes.MOVEMENT_SPEED, "class/add_multiplied_total"), "it slows like forbidden armor");
        helper.succeed();
    }

    private static double modifier(ServerPlayer player, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, String id) {
        var mod = player.getAttribute(attribute).getModifier(Pipican.id(id));
        return mod == null ? 0 : mod.amount();
    }

    /** Exclusive stats, stat points from kill XP, raising the special stat, class XP and levelling. */
    private static void statsAndLevels(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        helper.assertTrue(StatPoints.threshold(1) == 100 && StatPoints.threshold(2) == 250 && StatPoints.threshold(3) == 450, "default stat point curve");
        helper.assertTrue(StatPoints.earnedForXp(249) == 1 && StatPoints.earnedForXp(250) == 2, "points earned at thresholds");
        helper.assertTrue(StatPoints.threshold(Long.MAX_VALUE / 2) == Long.MAX_VALUE, "curve saturates instead of overflowing");

        helper.assertTrue(!ClassService.raiseStat(player, FOCUS).success(), "no stat points without kill XP");
        player.giveExperienceLevels(30);
        helper.assertTrue(!ClassService.raiseStat(player, FOCUS).success(), "vanilla XP does not buy stats");

        ClassService.addXp(player, PipicanConfig.xpToNextLevel(1) + 1);
        helper.assertTrue(ClassService.data(player).activeProgress().orElseThrow().level() == 2, "level up from class XP");
        helper.assertTrue(player.experienceLevel == 30, "kill XP does not touch vanilla XP");
        helper.assertTrue(ClassService.availableStatPoints(player) == 1, "the same kill XP earns a stat point");

        helper.assertTrue(!ClassService.raiseStat(player, MIGHT).success(), "exclusive non-special stat can't be raised");
        helper.assertTrue(ClassService.raiseStat(player, FOCUS).success(), "special stat can be raised");
        assertNear(helper, 6, ClassEffectApplier.state(player).stat(FOCUS), "raised focus point");
        helper.assertTrue(ClassService.availableStatPoints(player) == 0, "raising spends the point");
        ClassService.resetStats(player);
        helper.assertTrue(ClassService.availableStatPoints(player) == 1, "stat reset gives the point back");

        // Level-up screen's Accept: several points at once, all or nothing.
        ClassService.addXp(player, StatPoints.threshold(3) - ClassService.data(player).statXp());
        helper.assertTrue(ClassService.availableStatPoints(player) == 3, "three stat points to plan");
        helper.assertTrue(!ClassService.raiseStats(player, Map.of(FOCUS, 4)).success(), "can't accept more than you have");
        helper.assertTrue(!ClassService.raiseStats(player, Map.of(FOCUS, 1, MIGHT, 1)).success(), "one bad stat cancels the batch");
        helper.assertTrue(ClassService.availableStatPoints(player) == 3, "a refused batch spends nothing");
        helper.assertTrue(!ClassService.raiseStats(player, Map.of()).success(), "empty batch is refused");
        helper.assertTrue(ClassService.raiseStats(player, Map.of(FOCUS, 3)).success(), "batch raise");
        assertNear(helper, 8, ClassEffectApplier.state(player).stat(FOCUS), "batch put three points into focus");
        helper.assertTrue(ClassService.availableStatPoints(player) == 0, "batch spent all three");
        ClassService.resetStats(player);

        ClassService.setLevel(player, 999);
        helper.assertTrue(ClassService.data(player).activeProgress().orElseThrow().level() == 10, "clamped to max_level");
        helper.succeed();
    }

    /** pipican:resource changes the bar; pipican:conditional effects switch on and off as conditions flip. */
    /** pipican:execute, pipican:aura, low_health + shrug, start reactions, favored weapon. */
    /** Phase 5: flakes and vials; a Misting burns one metal at a time, a Mistborn (Lerasium) every metal; Aluminum wipes. */
    private static void allomancy(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (String recipe : List.of("allomancy/iron_flakes", "allomancy/steel_flakes", "allomancy/vial_of_pewter", "allomancy/nickel_flakes")) {
            helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Pipican.id(recipe))).isPresent(), "recipe " + recipe);
        }
        var vial = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Pipican.id("vial_of_tin"));
        helper.assertTrue(new ItemStack(vial).has(net.minecraft.core.component.DataComponents.CONSUMABLE), "vials can be drunk");
        helper.assertTrue(com.pipican.ore.PipicanOres.action("vial_of_tin").startsWith("burn:tin:"), "a vial burns its metal");
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        com.pipican.ore.Allomancy.drink(player, "tin", 60);
        com.pipican.ore.Allomancy.drink(player, "pewter", 60);
        helper.assertTrue(!com.pipican.ore.Allomancy.burning(player, "tin") && com.pipican.ore.Allomancy.burning(player, "pewter"),
                "a Misting burns one metal at a time");
        helper.assertTrue(!com.pipican.ore.Allomancy.isMistborn(player), "not Mistborn yet");
        com.pipican.ore.Allomancy.becomeMistborn(player);
        com.pipican.ore.Allomancy.drink(player, "tin", 60);
        helper.assertTrue(com.pipican.ore.Allomancy.burning(player, "tin") && com.pipican.ore.Allomancy.burning(player, "pewter"),
                "a Mistborn burns several at once");
        com.pipican.ore.Allomancy.drink(player, "aluminum", 1);
        helper.assertTrue(!com.pipican.ore.Allomancy.burning(player, "tin") && !com.pipican.ore.Allomancy.burning(player, "pewter"), "aluminum wipes them");
        helper.succeed();
    }

    /** Phase 6: Tamashigane - the vein, forging, souls, stages, trials, Bankai and the paths' abilities. */
    private static void tamashigane(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(com.pipican.soul.Soul.PATHS.size() == 15, "15 paths");
        helper.assertTrue(com.pipican.soul.Soul.threshold(10) == 50000 && com.pipican.soul.Soul.threshold(5) == 1500, "stage thresholds");
        // Every path has its names and descriptions.
        try (var in = com.pipican.soul.Soul.class.getResourceAsStream("/assets/pipican/lang/en_us.json")) {
            var lang = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (String path : com.pipican.soul.Soul.PATHS.keySet()) {
                for (String key : List.of("", ".s3", ".s5", ".s7", ".s10", ".armor", ".aura")) {
                    helper.assertTrue(lang.has("pipican.soul.path." + path + key), "lang for " + path + key);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        var blade = com.pipican.soul.SoulItems.BLADE.get();
        helper.assertTrue(new ItemStack(blade).is(net.minecraft.tags.TagKey.create(Registries.ITEM, Pipican.id("traits/soulbound"))), "soul gear is soulbound");

        // The spring: one ore on a shrine.
        BlockPos spring = com.pipican.soul.SoulSpring.build(level, helper.absolutePos(new BlockPos(6, 3, 6)));
        var ore = level.getBlockState(spring);
        helper.assertTrue(ore.is(com.pipican.ore.PipicanOres.block("deepslate_tamashigane_ore").get()), "the spring holds the vein");

        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        @SuppressWarnings("removal")
        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // A soul that already took its piece is refused by the vein.
        com.pipican.soul.Soul.set(player, com.pipican.soul.SoulData.NONE.withMined());
        var refused = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.block.BreakBlockEvent(level, spring, ore, player));
        helper.assertTrue(refused.isCanceled(), "the vein refuses a second piece (creative " + player.isCreative() + ", vein " + com.pipican.soul.SoulEvents.class.getSimpleName() + ")");

        // Forging: the ingot becomes a bound blade and armor, on a path, once.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.pipican.ore.PipicanOres.item("tamashigane_ingot").get()));
        helper.assertTrue(!com.pipican.soul.Soul.forge(player, "no_such_path"), "unknown path");
        helper.assertTrue(com.pipican.soul.Soul.forge(player, "black_crescent"), "forged");
        helper.assertTrue(!com.pipican.soul.Soul.forge(player, "ash_sovereign"), "only once");
        helper.assertTrue(com.pipican.soul.Soul.get(player).stage() == 1 && com.pipican.soul.Soul.carriesBlade(player), "stage 1, blade in hand");
        ItemStack ownBlade = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(blade)) {
                ownBlade = player.getInventory().getItem(i);
            }
        }
        helper.assertTrue(com.pipican.soul.Soul.owns(player, ownBlade) && !com.pipican.soul.Soul.owns(other, ownBlade), "bound to its owner alone");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ownBlade.copy());
        player.getInventory().removeItem(ownBlade);

        // What a death is worth.
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        var golem = helper.spawn(net.minecraft.world.entity.EntityType.IRON_GOLEM, new BlockPos(1, 2, 1));
        helper.assertTrue(com.pipican.soul.Soul.soulValue(zombie) == 1 && com.pipican.soul.Soul.soulValue(golem) == 0
                && com.pipican.soul.Soul.soulValue(other) == 25, "soul values");
        golem.discard();
        // Killing near the blade feeds it.
        player.snapTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 4.5)));
        zombie.hurtServer(level, level.damageSources().playerAttack(player), 1000);
        helper.assertTrue(com.pipican.soul.Soul.get(player).souls() == 1, "a kill gives its soul, got " + com.pipican.soul.Soul.get(player).souls());

        // Stages: 100 -> 2, 300 -> 3, then 1500 waits for the Shikai trial.
        com.pipican.soul.Soul.addSouls(player, 299);
        helper.assertTrue(com.pipican.soul.Soul.get(player).stage() == 3, "stage 3 at 300 souls");
        com.pipican.soul.Soul.addSouls(player, 1200);
        helper.assertTrue(com.pipican.soul.Soul.get(player).stage() == 4 && com.pipican.soul.Soul.pendingTrial(com.pipican.soul.Soul.get(player)) == 5,
                "the Shikai trial waits");
        helper.assertTrue(player.getMainHandItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME), "the blade whispered its name");
        var spirit = com.pipican.soul.SoulMinions.startTrial(player, 5);
        helper.assertTrue(spirit != null, "the spirit appears");
        spirit.hurtServer(level, level.damageSources().playerAttack(other), 1000);
        helper.assertTrue(spirit.isAlive(), "only its owner can hurt the spirit");
        spirit.hurtServer(level, level.damageSources().playerAttack(player), 1000);
        helper.assertTrue(com.pipican.soul.Soul.get(player).stage() == 5, "trial won: Shikai");

        // The stage-3/Shikai ability: a crescent cuts the zombie in front.
        var target = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 7));
        target.setNoAi(true);
        var eye = player.getEyePosition();
        var to = target.getBoundingBox().getCenter().subtract(eye);
        player.setYRot((float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90));
        player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
        float before = target.getHealth();
        com.pipican.soul.Soul.use(player);
        helper.assertTrue(target.getHealth() < before, "the Shikai crescent hits");
        helper.assertTrue(com.pipican.soul.Soul.get(player).abilityReady() > level.getGameTime(), "then it recovers");

        // Bankai: stage 7, 60 s, 10 min to recover; the Absolute Form once a day.
        com.pipican.soul.Soul.set(player, com.pipican.soul.Soul.get(player).withSouls(50000).withStage(10));
        com.pipican.soul.Soul.releaseBankai(player);
        var d = com.pipican.soul.Soul.get(player);
        helper.assertTrue(com.pipican.soul.Soul.bankai(player) && d.bankaiEnd() - level.getGameTime() == 90 * 20, "Bankai Mastery: 90 s");
        player.setShiftKeyDown(true);
        com.pipican.soul.Soul.use(player);
        long day = com.pipican.soul.Soul.get(player).ultimateDay();
        helper.assertTrue(day == level.getDefaultClockTime() / 24000L && com.pipican.soul.SoulAbilities.on(player, "beast"), "the Absolute Form");
        player.setShiftKeyDown(false);

        // Every path's abilities run (none throws), at every form.
        for (String path : com.pipican.soul.Soul.PATHS.keySet()) {
            com.pipican.soul.Soul.set(player, com.pipican.soul.Soul.get(player).withPath(path).withStage(10).withBankai(0, 0).withAbilityReady(0).withUltimateDay(-1));
            var ctx = com.pipican.soul.SoulAbilities.ctx(player);
            com.pipican.soul.SoulAbilities.whisper(ctx);
            com.pipican.soul.SoulAbilities.shikai(ctx);
            com.pipican.soul.Soul.releaseBankai(player);
            ctx = com.pipican.soul.SoulAbilities.ctx(player);
            com.pipican.soul.SoulAbilities.bankaiAbility(ctx);
            com.pipican.soul.SoulAbilities.ultimate(ctx);
            com.pipican.soul.SoulAbilities.tick(player);
        }
        com.pipican.soul.Soul.reset(player);
        helper.assertTrue(!com.pipican.soul.Soul.get(player).forged(), "reset");
        helper.succeed();
    }

    /**
     * Phase 6, every path at every form against real targets: three zombies (500 health) in a line ahead of the
     * player. Logs what each use did (SOULCHECK lines) and fails if a form does nothing observable.
     */
    private static void soulPathsPlay(GameTestHelper helper) {
        var level = helper.getLevel();
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(com.pipican.ore.PipicanOres.item("tamashigane_ingot").get()));
        com.pipican.soul.Soul.forge(player, "black_crescent");
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(com.pipican.soul.SoulItems.BLADE.get())) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, player.getInventory().getItem(i).copy());
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        List<String> silent = new java.util.ArrayList<>();
        for (String path : com.pipican.soul.Soul.PATHS.keySet()) {
            for (String form : List.of("s3", "shikai", "bankai", "ultimate")) {
                com.pipican.soul.SoulAbilities.clear(player);
                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                var start = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 1.5));
                player.snapTo(start.x, start.y, start.z, 0, 0);   // looking south (+z)
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                int stage = form.equals("s3") ? 4 : form.equals("shikai") ? 6 : 10;
                com.pipican.soul.Soul.set(player, com.pipican.soul.Soul.get(player).withPath(path).withStage(stage).withBankai(0, 0)
                        .withAbilityReady(0).withUltimateDay(-1));
                List<net.minecraft.world.entity.monster.zombie.Zombie> targets = new java.util.ArrayList<>();
                for (int z : new int[] {4, 6, 9}) {
                    var t = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, z));
                    t.setNoAi(true);
                    t.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);
                    t.setHealth(500);
                    t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 9999, 0, true, false));
                    t.setTarget(player);
                    targets.add(t);
                }
                var eye = player.getEyePosition();
                var to = targets.getFirst().getBoundingBox().getCenter().subtract(eye);
                player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
                var effectsBefore = player.getActiveEffects().size();
                var box = player.getBoundingBox().inflate(20);
                int mobsBefore = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box).size();
                if (!form.equals("s3") && !form.equals("shikai")) {
                    com.pipican.soul.Soul.releaseBankai(player);
                }
                int effectsAfterRelease = player.getActiveEffects().size();
                var ctx = com.pipican.soul.SoulAbilities.ctx(player);
                int cooldown = 0;
                switch (form) {
                    case "s3" -> cooldown = com.pipican.soul.SoulAbilities.whisper(ctx);
                    case "shikai" -> cooldown = com.pipican.soul.SoulAbilities.shikai(ctx);
                    case "bankai" -> cooldown = com.pipican.soul.SoulAbilities.bankaiAbility(ctx);
                    default -> {
                        // Bound foes / poison shards come from the Bankai ability first.
                        com.pipican.soul.SoulAbilities.bankaiAbility(ctx);
                        targets.forEach(t -> t.setHealth(500));
                        com.pipican.soul.SoulAbilities.ultimate(ctx);
                    }
                }
                StringBuilder what = new StringBuilder();
                float dealt = 0;
                int hit = 0;
                int debuffed = 0;
                for (var t : targets) {
                    float d = 500 - (t.isAlive() ? t.getHealth() : 0);
                    dealt += d;
                    if (d > 0) {
                        hit++;
                    }
                    if (t.getActiveEffects().size() > 1 || t.getRemainingFireTicks() > 0 || t.getTicksFrozen() > 0) {
                        debuffed++;
                        for (var eff : t.getActiveEffects()) {
                            if (!eff.getEffect().is(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE)) {
                                what.append(eff.getEffect().getRegisteredName().replace("minecraft:", "")).append(eff.getAmplifier() + 1).append(' ');
                            }
                        }
                        if (t.getRemainingFireTicks() > 0) {
                            what.append("burning ");
                        }
                        if (t.getTicksFrozen() > 0) {
                            what.append("frozen ");
                        }
                        what.append("| ");
                    }
                    if (t.getTarget() != player) {
                        what.append("lost-target ");
                    }
                }
                int playerEffects = player.getActiveEffects().size() - effectsBefore;
                int summons = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box).size() - mobsBefore - (int) targets.stream().filter(t -> !t.isAlive()).count() * 0;
                boolean moved = player.position().distanceTo(start) > 0.5 || player.getDeltaMovement().lengthSqr() > 0.1;
                long states = java.util.stream.Stream.of("fire_blade", "rage", "twin", "lifesteal", "poison_blade", "reach", "next_hit", "second_dance",
                        "beast", "cheat", "white_blade", "unlimited", "limitless", "first_beast", "death_marks", "adaptive", "serpent_arms", "immortal")
                        .filter(st -> com.pipican.soul.SoulAbilities.on(player, st)).count();
                boolean clouds = !level.getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class, box).isEmpty();
                what.insert(0, String.format("dealt=%.1f hit=%d debuffed=%d playerEffects=%d summons=%d moved=%s states=%d cloud=%s cd=%.1fs dmgBase=%.1f",
                        dealt, hit, debuffed, playerEffects, summons, moved, states, clouds, cooldown / 20.0, ctx.dmg()) + " targets: ");
                System.out.println("SOULCHECK " + path + " " + form + " " + what);
                boolean anything = dealt > 0 || debuffed > 0 || playerEffects > 0 || summons > 0 || moved || states > 0 || clouds
                        || (form.equals("bankai") && effectsAfterRelease > effectsBefore);
                if (!anything) {
                    silent.add(path + "/" + form);
                }
                targets.forEach(net.minecraft.world.entity.Entity::discard);
                for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box, e -> e instanceof net.minecraft.world.entity.Mob
                        || e instanceof net.minecraft.world.entity.AreaEffectCloud)) {
                    e.discard();
                }
            }
        }
        helper.assertTrue(silent.isEmpty(), "forms that did nothing: " + silent);
        // The Black Coffin closes 2 seconds later.
        com.pipican.soul.SoulAbilities.clear(player);
        com.pipican.soul.Soul.set(player, com.pipican.soul.Soul.get(player).withPath("perfect_lie").withStage(7).withBankai(0, 0).withAbilityReady(0));
        var coffined = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 4));
        coffined.setNoAi(true);
        coffined.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);
        coffined.setHealth(500);
        var start = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 1.5));
        player.snapTo(start.x, start.y, start.z, 0, 0);
        var to = coffined.getBoundingBox().getCenter().subtract(player.getEyePosition());
        player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z))));
        com.pipican.soul.Soul.releaseBankai(player);
        com.pipican.soul.SoulAbilities.bankaiAbility(com.pipican.soul.SoulAbilities.ctx(player));
        helper.assertTrue(coffined.getHealth() == 500, "the coffin hasn't closed yet");
        helper.runAfterDelay(45, () -> {
            System.out.println("SOULCHECK perfect_lie coffin-closed dealt=" + (500 - coffined.getHealth()));
            helper.assertTrue(coffined.getHealth() < 500, "the black coffin closes and crushes");
            helper.succeed();
        });
    }

    /** Phase 4: alloys, blacksmith substitutes, meteors, spreading ores, Quantanium, transmutation, mutation. */
    private static void oreMechanics(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        java.util.function.Function<String, net.minecraft.world.item.Item> item = path ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Pipican.id(path));
        java.util.function.Function<String, net.minecraft.world.level.block.Block> block = path ->
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Pipican.id(path));
        for (String recipe : List.of("special/manyullyn_ingot_from_cobalt_ingot", "special/daedric_ingot_from_ebony_ingot", "valyrian_steel_sword",
                "special/tnt_from_refined_quantanium")) {
            helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Pipican.id(recipe))).isPresent(), "recipe " + recipe);
        }
        // Universal upgrade materials stand in at any blacksmith.
        var profile = new com.pipican.blacksmith.BlacksmithUpgrades.Profile("x", Pipican.id("frostcrystal"), Pipican.id("x_boss_core"), Pipican.id("x_trophy"));
        helper.assertTrue(com.pipican.blacksmith.BlacksmithUpgrades.substitutes(profile, Pipican.id("frostcrystal")).contains(Pipican.id("titanite"))
                && com.pipican.blacksmith.BlacksmithUpgrades.substitutes(profile, Pipican.id("x_boss_core")).contains(Pipican.id("raritanium"))
                && com.pipican.blacksmith.BlacksmithUpgrades.substitutes(profile, Pipican.id("x_trophy")).contains(Pipican.id("eridium")), "substitutes");
        // A meteor leaves Meteoric Iron under the impact.
        BlockPos ground = helper.absolutePos(new BlockPos(4, 8, 4));
        for (BlockPos p : BlockPos.betweenClosed(ground.offset(-4, -7, -4), ground.offset(4, -1, 4))) {
            level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        }
        com.pipican.ore.OreWorldEvents.meteor(level, ground);
        int iron = 0;
        for (BlockPos p : BlockPos.betweenClosed(ground.offset(-4, -7, -4), ground.offset(4, 0, 4))) {
            if (level.getBlockState(p).is(block.apply("meteoric_iron_ore"))) {
                iron++;
            }
        }
        helper.assertTrue(iron > 0, "the meteor left meteoric iron");
        // Tiberium spreads into the stone around it.
        BlockPos tib = helper.absolutePos(new BlockPos(1, 2, 1));
        for (BlockPos p : BlockPos.betweenClosed(tib.offset(-1, -1, -1), tib.offset(1, 1, 1))) {
            level.setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        }
        var tiberium = block.apply("tiberium_ore").defaultBlockState();
        level.setBlock(tib, tiberium, 2);
        boolean spread = false;
        for (int i = 0; i < 400 && !spread; i++) {
            spread = ((com.pipican.ore.SpreadingOreBlock) tiberium.getBlock()).spread(tiberium, level, tib, level.getRandom());
        }
        helper.assertTrue(spread, "tiberium spreads");
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        // Quantanium: stamped when carried; harmless until its fuse runs out.
        player.getInventory().setItem(3, new ItemStack(item.apply("quantanium"), 2));
        helper.assertTrue(!com.pipican.ore.OreWorldEvents.quantanium(player, level), "fresh quantanium is calm");
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, player.getInventory().getItem(3),
                tag -> tag.putLong("pipican_unstable_since", level.getGameTime() - com.pipican.ore.OreWorldEvents.QUANTANIUM_FUSE));
        var stamped = player.getInventory().getItem(3).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        boolean exploded = com.pipican.ore.OreWorldEvents.quantanium(player, level);
        helper.assertTrue(exploded && player.getInventory().getItem(3).isEmpty(), "old quantanium explodes: " + exploded + " " + stamped
                + " now=" + level.getGameTime() + " left=" + player.getInventory().getItem(3));
        // The Philosopher's Stone: an off-hand T1 material becomes a T2 one; 2 max health, for good.
        double health = player.getAttributeValue(Attributes.MAX_HEALTH);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(item.apply("electrum_ingot"), 3));
        var result = com.pipican.ore.OreItemUses.transmute(player, level);
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result);
        helper.assertTrue(result != null && player.getOffhandItem().getCount() == 3
                && com.pipican.ore.PipicanOres.of(key.getPath()).map(o -> o.tier() == 2).orElse(false), "transmuted to a T2 material: " + key);
        assertNear(helper, health - 2, player.getAttributeValue(Attributes.MAX_HEALTH), "the stone's price");
        // Warpstone mutation: half again the health.
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        com.pipican.ore.OreWorldEvents.mutate(zombie);
        assertNear(helper, 30, zombie.getMaxHealth(), "a mutated zombie");
        helper.succeed();
    }

    /** Phase 3: brewing, fuel, consumables, anvil coatings, special blocks, trades and off-hand focuses. */
    private static void specialOres(GameTestHelper helper) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        java.util.function.Function<String, net.minecraft.world.item.Item> item = path ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Pipican.id(path));
        java.util.function.Function<net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>, ItemStack> potion = p ->
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, p);
        var brewing = level.potionBrewing();
        // Awkward + Frostcrystal = Slowness; Amber lengthens any potion, Mercury strengthens it.
        ItemStack slow = brewing.mix(new ItemStack(item.apply("frostcrystal")), potion.apply(net.minecraft.world.item.alchemy.Potions.AWKWARD));
        helper.assertTrue(slow.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(net.minecraft.world.item.alchemy.Potions.SLOWNESS), "frostcrystal brews slowness");
        ItemStack longSwift = brewing.mix(new ItemStack(item.apply("amber")), potion.apply(net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        helper.assertTrue(longSwift.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(net.minecraft.world.item.alchemy.Potions.LONG_SWIFTNESS), "amber lengthens");
        ItemStack strongHeal = brewing.mix(new ItemStack(item.apply("mercury")), potion.apply(net.minecraft.world.item.alchemy.Potions.HEALING));
        helper.assertTrue(strongHeal.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS).is(net.minecraft.world.item.alchemy.Potions.STRONG_HEALING), "mercury strengthens");
        // Fuel, consumables, the unbreakable block.
        helper.assertTrue(level.fuelValues().burnDuration(new ItemStack(item.apply("vespene"))) == 6400, "vespene burns 6400 ticks");
        helper.assertTrue(new ItemStack(item.apply("eco")).has(net.minecraft.core.component.DataComponents.CONSUMABLE), "eco can be used up");
        helper.assertTrue(!new ItemStack(item.apply("morkite")).has(net.minecraft.core.component.DataComponents.CONSUMABLE), "morkite can't");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Pipican.id("katchin_block")).defaultBlockState()
                .getDestroySpeed(level, BlockPos.ZERO) < 0, "katchin blocks can't be broken");
        // Anvil coating: an iron sword and a Silversheen ingot make a silver-coated sword; it can't be coated twice.
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var anvil = new net.neoforged.neoforge.event.AnvilUpdateEvent(new ItemStack(Items.IRON_SWORD), new ItemStack(item.apply("silversheen_ingot")),
                null, ItemStack.EMPTY, 0, 0, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(anvil);
        helper.assertTrue(com.pipican.ore.SpecialOreEvents.coating(anvil.getOutput()).filter("silversheen"::equals).isPresent(), "silversheen coats a sword");
        var again = new net.neoforged.neoforge.event.AnvilUpdateEvent(anvil.getOutput(), new ItemStack(item.apply("bloodstone")), null, ItemStack.EMPTY, 0, 0, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(again);
        helper.assertTrue(again.getOutput().isEmpty(), "one coating per item");
        var helmetOnSword = new net.neoforged.neoforge.event.AnvilUpdateEvent(new ItemStack(Items.IRON_SWORD), new ItemStack(item.apply("kyanite")), null, ItemStack.EMPTY, 0, 0, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(helmetOnSword);
        helper.assertTrue(helmetOnSword.getOutput().isEmpty(), "kyanite only coats helmets");
        // Trades and off-hand focuses are registered.
        helper.assertTrue(access.lookupOrThrow(Registries.VILLAGER_TRADE).get(ResourceKey.create(Registries.VILLAGER_TRADE, Pipican.id("trade/morkite_emerald"))).isPresent(), "morkite trade");
        helper.assertTrue(com.pipican.core.ClassLookup.gearSets(access).containsKey(Pipican.id("focus_aquamarine")), "aquamarine focus");
        helper.succeed();
    }

    /** Gear sets: tier/style stats, mining levels, traits, recipes, stat requirements and set bonuses. */
    private static void gearSets(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = helper.getLevel().registryAccess();
        java.util.function.Function<String, net.minecraft.world.item.Item> item = path ->
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Pipican.id(path));
        java.util.function.Function<String, net.minecraft.world.level.block.state.BlockState> ore = path ->
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Pipican.id(path)).defaultBlockState();
        // Mining levels: T3 tools mine T4 but not T5; T5 tools mine T6 but not T7; Adamantine mines anything.
        ItemStack cobalt = new ItemStack(item.apply("cobalt_pickaxe"));
        helper.assertTrue(cobalt.isCorrectToolForDrops(ore.apply("iridium_ore")) && !cobalt.isCorrectToolForDrops(ore.apply("mithril_ore")), "T3 pickaxe: up to T4");
        ItemStack mithril = new ItemStack(item.apply("mithril_pickaxe"));
        helper.assertTrue(mithril.isCorrectToolForDrops(ore.apply("orichalcum_ore")) && !mithril.isCorrectToolForDrops(ore.apply("adamantine_ore")), "T5 pickaxe: up to T6");
        helper.assertTrue(new ItemStack(item.apply("adamantine_pickaxe")).isCorrectToolForDrops(ore.apply("unobtanium_ore")), "Adamantine mines everything");
        helper.assertTrue(new ItemStack(item.apply("darksteel_sword")).has(net.minecraft.core.component.DataComponents.UNBREAKABLE), "Darksteel never breaks");
        helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Pipican.id("crimtane_chestplate"))).isPresent(), "crimtane chestplate recipe");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Pipican.id("valyrian_steel_helmet")).isEmpty()
                && item.apply("valyrian_steel_sword") != net.minecraft.world.item.Items.AIR, "Valyrian steel: weapons only");
        // Mithril (T5, heavy style) needs 9 Strength.
        helper.assertTrue(ClassEffectApplier.missedRequirements(access, new ItemStack(item.apply("mithril_chestplate")), k -> 8) == 1
                && ClassEffectApplier.missedRequirements(access, new ItemStack(item.apply("mithril_chestplate")), k -> 9) == 0, "mithril needs 9 strength");
        // Set bonuses: the full Crimtane set gives +2 max health; its sword adds the life-steal reaction.
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, TESTER, false, true).success(), "choose tester");
        double before = player.getAttributeValue(Attributes.MAX_HEALTH);
        int reactions = ClassEffectApplier.state(player).reactions().size();
        for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS)) {
            player.setItemSlot(slot, new ItemStack(item.apply("crimtane_" + switch (slot) {
                case HEAD -> "helmet"; case CHEST -> "chestplate"; default -> "leggings"; })));
        }
        ClassEffectApplier.refresh(player);
        assertNear(helper, before, player.getAttributeValue(Attributes.MAX_HEALTH), "three pieces: no set bonus yet");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(item.apply("crimtane_boots")));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item.apply("crimtane_sword")));
        ClassEffectApplier.refresh(player);
        assertNear(helper, before + 2, player.getAttributeValue(Attributes.MAX_HEALTH), "full set: +2 max health");
        helper.assertTrue(ClassEffectApplier.state(player).reactions().size() == reactions + 1, "the sword adds its life steal");
        helper.succeed();
    }

    /** Ores: registered from ores.json, mining levels by tier, worldgen features, smelting and drops. */
    private static void ores(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(com.pipican.ore.PipicanOres.ORES.size() == 169, "169 ores: " + com.pipican.ore.PipicanOres.ORES.size());
        java.util.function.Function<String, net.minecraft.world.level.block.Block> block = path ->
                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Pipican.id(path));
        for (String path : List.of("electrum_ore", "deepslate_electrum_ore", "nether_hellstone_ore", "end_dragonite_ore", "mithril_block", "raw_mithril_block")) {
            helper.assertTrue(block.apply(path) != net.minecraft.world.level.block.Blocks.AIR, path + " is registered");
        }
        // Mining level by tier: T1 a stone pickaxe, T4 diamond, T5 netherite, T6+ none of vanilla's.
        helper.assertTrue(block.apply("electrum_ore").defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL), "T1 needs stone");
        helper.assertTrue(block.apply("iridium_ore").defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL), "T4 needs diamond");
        helper.assertTrue(block.apply("mithril_ore").defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
                && !block.apply("mithril_ore").defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL), "T5 needs netherite");
        helper.assertTrue(block.apply("deepslate_adamantine_ore").defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL), "T7 beyond netherite");
        // Worldgen: T1-T7 ores spawn; T8/T9 don't (they come from limited sources).
        var placed = access.lookupOrThrow(Registries.PLACED_FEATURE);
        helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, Pipican.id("ore_electrum"))).isPresent(), "electrum spawns");
        helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, Pipican.id("ore_adamantine"))).isPresent(), "adamantine spawns");
        helper.assertTrue(placed.get(ResourceKey.create(Registries.PLACED_FEATURE, Pipican.id("ore_unobtanium"))).isEmpty(), "unobtanium doesn't");
        var modifiers = access.lookupOrThrow(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        helper.assertTrue(modifiers.get(ResourceKey.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS, Pipican.id("ore_hellstone"))).isPresent(),
                "hellstone is added to its biomes");
        // Smelting and drops.
        helper.assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Pipican.id("electrum_ingot_from_smelting_raw_electrum"))).isPresent(),
                "raw electrum smelts");
        var loot = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Pipican.id("blocks/electrum_ore")));
        helper.assertTrue(loot != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "electrum ore has drops");
        // A vein really places: fill a stone cube, run the configured feature in it, count the ore.
        BlockPos center = helper.absolutePos(new BlockPos(3, 4, 3));
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
            helper.getLevel().setBlock(p, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
        }
        var feature = access.lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, Pipican.id("ore_electrum")));
        int placedOres = 0;
        for (int attempt = 0; attempt < 8 && placedOres == 0; attempt++) {
            feature.value().place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), helper.getLevel().getRandom(), center);
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
                if (helper.getLevel().getBlockState(p).is(block.apply("electrum_ore"))) {
                    placedOres++;
                }
            }
        }
        helper.assertTrue(placedOres > 0, "an electrum vein replaces stone");
        helper.succeed();
    }

    /** Stats' own effects ("points" scaling), a class's stat_growth, and stat requirements on gear. */
    private static void statEffects(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, STAT_TESTER, false, true).success(), "choose stat tester");
        ClassState state = ClassEffectApplier.state(player);
        // Intelligence: +0.5% magic damage per point; 10 points count double for this class.
        assertNear(helper, 1.1, ClassState.multiplier(state.damageDealt(), helper.getLevel().damageSources().magic()), "magic damage from intelligence");
        assertNear(helper, 1, ClassState.multiplier(state.damageDealt(), helper.getLevel().damageSources().generic()), "not other damage");
        // Luck: +0.2% crit chance per point.
        assertNear(helper, 0.04, state.criticals().stream().mapToDouble(ClassState.Critical::chance).sum(), "crit chance from luck");
        // Grip 0 of the 5 a stone sword needs: one requirement missed; an iron sword has none.
        var access = helper.getLevel().registryAccess();
        helper.assertTrue(ClassEffectApplier.missedRequirements(access, new ItemStack(Items.STONE_SWORD), state::stat) == 1, "stone sword needs grip");
        helper.assertTrue(ClassEffectApplier.missedRequirements(access, new ItemStack(Items.WOODEN_SWORD), state::stat) == 0, "a wooden sword needs nothing");
        // The shipped requirements: an iron sword needs 3 Strength (this class has none).
        helper.assertTrue(ClassEffectApplier.missedRequirements(access, new ItemStack(Items.IRON_SWORD), state::stat) == 1, "iron sword needs strength");
        helper.assertTrue(ClassEffectApplier.capped(player, new ItemStack(Items.STONE_SWORD), com.pipican.rpgclass.Affinity.NEUTRAL, k -> 0.0)
                == com.pipican.rpgclass.Affinity.CLUMSY, "a missed requirement is one tier worse");
        helper.succeed();
    }

    private static void classMechanics(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        @SuppressWarnings("removal")
        ServerPlayer ally = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, MECHANICS_TESTER, false, true).success(), "choose mechanics tester");
        helper.assertTrue(ClassService.chooseClass(ally, TESTER, false, true).success(), "ally picks a class");
        player.setGameMode(GameType.SURVIVAL);
        ally.setGameMode(GameType.SURVIVAL);
        ServerLevel level = helper.getLevel();

        // Aura: the ally nearby takes half damage; the aura's owner doesn't (include_self false).
        Auras.tick(ally);
        assertNear(helper, 0.25, ClassState.multiplier(ClassEffectApplier.state(ally).damageTaken(), level.damageSources().magic()),
                "ally inside the aura: -50% at +50% strength = -75%");
        assertNear(helper, 1, ClassState.multiplier(ClassEffectApplier.state(player).damageTaken(), level.damageSources().magic()), "aura owner not affected");

        // Execute: a hit that leaves the mob below half health kills it.
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setHealth(9);
        zombie.hurtServer(level, level.damageSources().playerAttack(player), 1);
        helper.assertTrue(!zombie.isAlive(), "executed below 50% health");

        // Cleave: a hit also hits mobs next to the target for 50%.
        Zombie first = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        Zombie second = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 3));
        com.pipican.ability.Strikes.after(player, first, 8);
        helper.assertTrue(second.getHealth() < second.getMaxHealth() - 3.5 && second.getHealth() > second.getMaxHealth() - 4.5,
                "cleaved for half (4, minus a little armor): " + second.getHealth());
        first.discard();
        second.discard();

        // Low health: the reaction reports the hit as shrugged off. (Mock players can't take real damage:
        // they count as invulnerable until a client has loaded.)
        helper.assertTrue(Reactions.fire(player, zombie, ItemStack.EMPTY, ReactionEffect.Event.LOW_HEALTH), "low-health hit shrugged off");
        helper.assertTrue(!Reactions.fire(player, zombie, ItemStack.EMPTY, ReactionEffect.Event.HURT), "other events aren't cancelled");

        // Uses per rest (premonitions): 2 + 1 from an upgrade, then none until sleeping.
        Zombie attacker = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        for (int n = 0; n < 3; n++) {
            helper.assertTrue(Reactions.fire(player, attacker, ItemStack.EMPTY, ReactionEffect.Event.ATTACKED), "premonition " + (n + 1));
        }
        helper.assertTrue(!Reactions.fire(player, attacker, ItemStack.EMPTY, ReactionEffect.Event.ATTACKED), "out of premonitions");
        Reactions.rest(player);
        helper.assertTrue(Reactions.fire(player, attacker, ItemStack.EMPTY, ReactionEffect.Event.ATTACKED), "refilled by sleeping");

        // Damage over time (Pain curse).
        new com.pipican.ability.action.DotAction(new com.pipican.ability.Targets(com.pipican.ability.Targets.Kind.VICTIM, 6, 16, 360, Optional.empty(), false), 2, 4)
                .run(new AbilityContext(player, Optional.empty(), attacker.position(), Optional.of(attacker), 0));
        helper.assertTrue(Bleeds.isBleeding(attacker), "a dot is running on the mob");
        attacker.discard();

        // Start reactions: entering the conditional (sneaking) fires once.
        helper.assertTrue(!player.hasEffect(MobEffects.GLOWING), "not glowing yet");
        player.setShiftKeyDown(true);
        ClassEffectApplier.recheckConditions(player);
        helper.assertTrue(player.hasEffect(MobEffects.GLOWING), "start reaction fired");
        player.setShiftKeyDown(false);
        ClassEffectApplier.recheckConditions(player);
        helper.assertTrue(player.hasEffect(MobEffects.SPEED), "end reaction fired when sneaking stopped");

        // Death: cheat_death saves you once per rest, then again after sleeping.
        player.setHealth(0.5f);
        helper.assertTrue(Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.DEATH), "saved from death");
        assertNear(helper, 4, player.getHealth(), "left at 4 health");
        helper.assertTrue(!Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.DEATH), "only once per rest");
        Reactions.rest(player);
        helper.assertTrue(Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.DEATH), "again after resting");

        // Ability cooldowns -50%.
        Ability mend = ClassLookup.abilities(level.registryAccess()).getValue(ability("mend"));
        helper.assertTrue(AbilityService.cooldown(player, mend) == Math.round(mend.cooldownTicks() * 0.5) - 20,
                "half cooldown, then 1s off from the ability upgrade: " + AbilityService.cooldown(player, mend));
        assertNear(helper, mend.cost() * 0.5, AbilityService.cost(player, mend), "ability upgrade halves the cost");

        // Class item rules: bound weapon (hotbar slot 1), sigil weapon (renamed), carrying a book.
        player.getInventory().setSelectedSlot(0);
        player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
        var bound = new com.pipican.condition.type.ItemRuleCondition(com.pipican.condition.type.ItemRuleCondition.Rule.BOUND_WEAPON, 1);
        var sigil = new com.pipican.condition.type.ItemRuleCondition(com.pipican.condition.type.ItemRuleCondition.Rule.SIGIL_WEAPON, 1);
        helper.assertTrue(bound.test(player), "weapon in slot 1 is bound");
        helper.assertTrue(!sigil.test(player), "not renamed yet");
        player.getMainHandItem().set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Rune"));
        helper.assertTrue(sigil.test(player), "a renamed weapon is sigil-marked");
        player.getInventory().setSelectedSlot(1);
        player.getInventory().setItem(1, new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(!bound.test(player), "slot 2 isn't the bound weapon");
        var books = new com.pipican.condition.type.CarryingCondition(net.minecraft.core.HolderSet.direct(Items.BOOK.builtInRegistryHolder()));
        helper.assertTrue(!books.test(player), "no book yet");
        player.getInventory().setItem(20, new ItemStack(Items.BOOK));
        helper.assertTrue(books.test(player), "a book in the backpack counts");
        player.getInventory().setSelectedSlot(0);

        // Durability -100%: wear is refunded.
        ItemStack worn = new ItemStack(Items.IRON_SWORD);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, worn);
        com.pipican.event.BonusEvents.durability(player);
        worn.setDamageValue(5);
        com.pipican.event.BonusEvents.durability(player);
        helper.assertTrue(worn.getDamageValue() == 0, "durability loss refunded: " + worn.getDamageValue());

        // Tamed animals: +2 armor near you, half damage taken.
        net.minecraft.world.entity.animal.wolf.Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(1, 2, 1));
        double armor = wolf.getArmorValue();
        wolf.tame(player);
        player.setPos(wolf.position());
        com.pipican.ability.Pets.tick(player);
        helper.assertTrue(wolf.getArmorValue() == armor + 2, "pet armor +2: " + wolf.getArmorValue());
        float before = wolf.getHealth();
        wolf.hurtServer(level, level.damageSources().magic(), 4);
        assertNear(helper, before - 2, wolf.getHealth(), "pet takes half damage");
        wolf.discard();

        // Victim effects and distance: only glowing mobs within 6 blocks take double damage.
        Zombie glowing = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        assertNear(helper, 1, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), level.damageSources().playerAttack(player), glowing, player), "not glowing");
        glowing.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.GLOWING, 100));
        assertNear(helper, 2, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), level.damageSources().playerAttack(player), glowing, player), "glowing and close");
        glowing.discard();

        // Applied effects: poison you give lasts 2s longer, is Poison II and brings Slowness.
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 2));
        pig.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 100, 0), player);
        var poison = pig.getEffect(MobEffects.POISON);
        helper.assertTrue(poison != null && poison.getAmplifier() == 1 && poison.getDuration() >= 139, "poison upgraded: " + poison);
        helper.assertTrue(pig.hasEffect(MobEffects.SLOWNESS), "poison brings slowness");
        pig.discard();

        // Safe food: no harmful effects while eating.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH));
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.HUNGER, 100));
        helper.assertTrue(!player.hasEffect(MobEffects.HUNGER), "no hunger from food");
        player.stopUsingItem();

        // Armor pierce 100%: a zombie's armor doesn't soften the hit.
        Zombie armored = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        armored.hurtServer(level, level.damageSources().playerAttack(player), 4);
        assertNear(helper, armored.getMaxHealth() - 4, armored.getHealth(), "armor ignored");
        armored.discard();

        // Chain damage hits the closest other hostile; kills drop experience.
        Zombie hitOne = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        Zombie nearOne = helper.spawn(EntityType.ZOMBIE, new BlockPos(4, 2, 1));
        float hitBefore = hitOne.getHealth();
        float nearBefore = nearOne.getHealth();
        new com.pipican.ability.action.DamageAction(new com.pipican.ability.Targets(com.pipican.ability.Targets.Kind.NEAR_VICTIM, 4, 16, 360, Optional.empty(), false),
                2, com.pipican.ability.action.DamageAction.Kind.MAGIC, 0, 0, List.of())
                .run(new AbilityContext(player, Optional.empty(), hitOne.position(), Optional.of(hitOne), 0));
        helper.assertTrue(hitOne.getHealth() == hitBefore && nearOne.getHealth() < nearBefore, "chained to the neighbour only: " + hitOne.getHealth() + " / " + nearOne.getHealth());
        Reactions.fire(player, hitOne, ItemStack.EMPTY, ReactionEffect.Event.KILL);
        helper.assertTrue(!level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, hitOne.getBoundingBox().inflate(2)).isEmpty(), "kill dropped experience");
        level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, hitOne.getBoundingBox().inflate(2)).forEach(net.minecraft.world.entity.Entity::discard);
        hitOne.discard();
        nearOne.discard();

        // Effect durations: Luck lasts twice as long on you.
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.LUCK, 100));
        helper.assertTrue(player.getEffect(MobEffects.LUCK).getDuration() == 200, "luck doubled: " + player.getEffect(MobEffects.LUCK).getDuration());

        // Ignored by skeletons until you hurt one. (Mock players are never valid targets, so ask the event directly.)
        var skeleton = helper.spawn(EntityType.SKELETON, new BlockPos(5, 2, 5));
        var mobTarget = net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent.LivingTargetType.MOB_TARGET;
        helper.assertTrue(net.neoforged.neoforge.common.CommonHooks.onLivingChangeTarget(skeleton, player, mobTarget).isCanceled(), "skeleton ignores you");
        player.setLastHurtMob(skeleton);
        helper.assertTrue(!net.neoforged.neoforge.common.CommonHooks.onLivingChangeTarget(skeleton, player, mobTarget).isCanceled(), "it fights back once attacked");
        skeleton.discard();

        // From-behind reactions: only when the victim faces away.
        Zombie facing = helper.spawn(EntityType.ZOMBIE, new BlockPos(1, 2, 4));
        player.setPos(facing.getX(), facing.getY(), facing.getZ() - 2); // the player stands north of it
        facing.setYHeadRot(180); // facing north, toward the player
        Reactions.fire(player, facing, ItemStack.EMPTY, ReactionEffect.Event.HIT);
        helper.assertTrue(!facing.hasEffect(MobEffects.UNLUCK), "not from behind");
        facing.setYHeadRot(0); // facing south, away from the player
        Reactions.fire(player, facing, ItemStack.EMPTY, ReactionEffect.Event.HIT);
        helper.assertTrue(facing.hasEffect(MobEffects.UNLUCK), "hit from behind");
        facing.discard();

        // Anvil costs: 10 levels -> half (5) -> one fewer (4).
        var anvil = new net.neoforged.neoforge.event.AnvilUpdateEvent(new ItemStack(Items.IRON_SWORD), new ItemStack(Items.IRON_INGOT), null,
                new ItemStack(Items.IRON_SWORD), 10, 1, player);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(anvil);
        helper.assertTrue(anvil.getXpCost() == 4, "anvil cost 4: " + anvil.getXpCost());

        // Drunk potions last twice as long.
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, new net.minecraft.world.item.alchemy.PotionContents(net.minecraft.world.item.alchemy.Potions.SWIFTNESS));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, potion);
        player.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.HASTE, 100));
        player.stopUsingItem();
        helper.assertTrue(player.getEffect(MobEffects.HASTE).getDuration() == 200, "drunk effect doubled: " + player.getEffect(MobEffects.HASTE).getDuration());

        // Minimum melee damage 4: a 1-damage punch deals 4 (armor pierce 100% keeps it exact).
        Zombie punched = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 3));
        punched.hurtServer(level, level.damageSources().playerAttack(player), 1);
        assertNear(helper, punched.getMaxHealth() - 4, punched.getHealth(), "punch raised to 4");
        punched.discard();

        // Eat reactions only for the listed food.
        Reactions.fire(player, null, new ItemStack(Items.BREAD), ReactionEffect.Event.EAT);
        helper.assertTrue(!player.hasEffect(MobEffects.DOLPHINS_GRACE), "bread doesn't count");
        Reactions.fire(player, null, new ItemStack(Items.COOKED_BEEF), ReactionEffect.Event.EAT);
        helper.assertTrue(player.hasEffect(MobEffects.DOLPHINS_GRACE), "cooked beef does");

        // Reaction upgrades: the named reaction lasts 3s longer and also gives Water Breathing.
        Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.INSTRUMENT);
        helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION) != null && player.getEffect(MobEffects.NIGHT_VISION).getDuration() == 100,
                "upgraded duration 5s: " + player.getEffect(MobEffects.NIGHT_VISION));
        helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING), "upgrade's extra action ran");
        helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 1, "upgrade raised the effect to level II");

        // Skill triggers: named reactions and the new actions.
        var reacted = new com.pipican.mastery.SkillAction.Reacted("Trickery");
        helper.assertTrue(new com.pipican.mastery.trigger.ReactionTrigger("trickery").uses(reacted) == 1, "reaction trigger matches by name");
        helper.assertTrue(new com.pipican.mastery.trigger.ReactionTrigger("zeal").uses(reacted) == 0, "and only that name");
        var healOther = new com.pipican.mastery.trigger.DidTrigger(com.pipican.mastery.SkillAction.Did.Kind.HEAL_OTHER, Optional.empty(), Optional.empty());
        helper.assertTrue(healOther.uses(com.pipican.mastery.SkillAction.Did.of(com.pipican.mastery.SkillAction.Did.Kind.HEAL_OTHER)) == 1, "heal_other trigger");

        // Spell ward 100%: harmful effects from others never land; your own still do.
        Zombie caster = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 100), caster);
        helper.assertTrue(!player.hasEffect(MobEffects.WEAKNESS), "warded against a mob's weakness");
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 100));
        helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "no source: not warded");
        player.removeEffect(MobEffects.WEAKNESS);
        caster.discard();

        // "except": half (drowning) damage to everything that isn't a husk.
        var husk = helper.spawn(EntityType.HUSK, new BlockPos(4, 2, 4));
        var notHusk = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 2));
        var hit = level.damageSources().drown();
        assertNear(helper, 1, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), hit, husk, player), "husks are excepted");
        assertNear(helper, 0.5, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), hit, notHusk, player), "everything else -50%");
        husk.discard();
        notHusk.discard();

        // Melee-only "attacked" reactions (Stay the Hand).
        Zombie swinger = helper.spawn(EntityType.ZOMBIE, new BlockPos(3, 2, 2));
        helper.assertTrue(Reactions.fire(player, swinger, ItemStack.EMPTY, ReactionEffect.Event.ATTACKED_MELEE), "melee hit cancelled");
        swinger.discard();

        // Reaction strength: a +2 armor boost upgraded by 50% gives +3.
        double armorBefore = player.getAttributeValue(Attributes.ARMOR);
        Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.LIGHTNING);
        assertNear(helper, armorBefore + 3, player.getAttributeValue(Attributes.ARMOR), "boost +50% stronger");
        Boosts.clear(player);

        // Attack reset: the next swing counts as fully charged.
        player.resetAttackStrengthTicker();
        helper.assertTrue(player.getAttackStrengthScale(0) < 1, "swing just used");
        new com.pipican.ability.action.ResetAttackAction().run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        assertNear(helper, 1, player.getAttackStrengthScale(0), "attack cooldown reset");

        // Right-click reactions: only the listed mob, only with the listed item.
        var sheep = helper.spawn(EntityType.SHEEP, new BlockPos(2, 2, 4));
        Reactions.fire(player, sheep, new ItemStack(Items.STICK), ReactionEffect.Event.INTERACT);
        helper.assertTrue(!sheep.hasEffect(MobEffects.GLOWING), "wrong item");
        Reactions.fire(player, sheep, new ItemStack(Items.SHEARS), ReactionEffect.Event.INTERACT);
        helper.assertTrue(sheep.hasEffect(MobEffects.GLOWING), "right-clicked with shears");
        sheep.discard();

        // Craft bonus 100%: crafting ladders gives a second batch.
        int ladders = player.getInventory().countItem(Items.LADDER);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(
                player, new ItemStack(Items.LADDER, 3), new net.minecraft.world.SimpleContainer(1)));
        helper.assertTrue(player.getInventory().countItem(Items.LADDER) == ladders + 3, "extra batch of ladders: " + player.getInventory().countItem(Items.LADDER));

        // "Unarmed": an empty main hand counts as holding air.
        var unarmed = new com.pipican.condition.type.HoldingCondition(net.minecraft.core.HolderSet.direct(Items.AIR.builtInRegistryHolder()),
                com.pipican.condition.type.HoldingCondition.Hand.MAIN, Optional.empty());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(unarmed.test(player), "empty hand is unarmed");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        helper.assertTrue(!unarmed.test(player), "holding a stick isn't");

        // Favored weapon: chosen on waking from the main hand.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        AbilityService.onWake(player);
        helper.assertTrue(FavoredWeapon.holding(player), "swords are favored");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        helper.assertTrue(!FavoredWeapon.holding(player), "an axe isn't");

        // Servants: an ally control with max 1 takes the first mob only.
        var command = com.pipican.ability.action.ControlAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"target\": \"victim\", \"mode\": \"ally\", \"seconds\": 30, \"max\": 1}")).getOrThrow();
        Zombie servant = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 2, 5));
        Zombie spare = helper.spawn(EntityType.ZOMBIE, new BlockPos(6, 2, 5));
        command.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.of(servant), 0));
        command.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.of(spare), 0));
        helper.assertTrue(com.pipican.ability.MobControl.isAllyOf(servant, player), "first mob serves");
        helper.assertTrue(!com.pipican.ability.MobControl.isAllyOf(spare, player), "cap of one servant");

        // Victim states: armored (armor or a raised shield), pet_target (one of your tamed animals attacks it).
        var shielded = com.pipican.effect.VictimState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"armored\": true}")).getOrThrow();
        var hunted = com.pipican.effect.VictimState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"pet_target\": true}")).getOrThrow();
        helper.assertTrue(!shielded.test(player, spare), "bare zombie isn't armored");
        spare.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        helper.assertTrue(shielded.test(player, spare), "zombie in a chestplate is");
        helper.assertTrue(!hunted.test(player, spare), "no pet on it yet");
        var hunter = helper.spawn(EntityType.WOLF, new BlockPos(6, 2, 6));
        hunter.tame(player);
        hunter.setTarget(spare);
        helper.assertTrue(hunted.test(player, spare), "your wolf is attacking it");

        // Nearby pets of one kind: a stray cat doesn't count, your own does.
        var nearCat = com.pipican.condition.type.NearbyCondition.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"entities\": \"pets\", \"entity\": \"minecraft:cat\", \"radius\": 8}")).getOrThrow();
        var cat = helper.spawn(EntityType.CAT, new BlockPos(1, 2, 1));
        cat.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
        helper.assertTrue(!nearCat.test(player), "stray cat");
        cat.tame(player);
        helper.assertTrue(nearCat.test(player), "your cat");
        helper.assertTrue(!com.pipican.condition.type.NearbyCondition.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"entities\": \"pets\", \"entity\": \"minecraft:parrot\"}")).getOrThrow().test(player), "no parrot");
        cat.discard();
        hunter.discard();

        // Mark strength upgrades: a +30% mark at +100% strength gives +60%.
        var mark = com.pipican.ability.action.MarkAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"target\": \"victim\", \"multiplier\": 1.3, \"next_hit\": true, \"glow\": false}")).getOrThrow();
        mark.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.of(spare), 0)
                .withMod(new ClassState.ReactionMod(0, 0, 0, 0, java.util.List.of(), 1.0)));
        assertNear(helper, 1.6, com.pipican.ability.Marks.multiplier(player, spare, level.damageSources().playerAttack(player)), "stronger mark");
        assertNear(helper, 1, com.pipican.ability.Marks.multiplier(player, spare, level.damageSources().playerAttack(player)), "next-hit mark used up");

        // Near home: within 64 blocks of the respawn point.
        var home = new com.pipican.condition.type.StateCondition(com.pipican.condition.type.StateCondition.State.NEAR_HOME);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(net.minecraft.world.level.storage.LevelData.RespawnData.of(level.dimension(),
                player.blockPosition().offset(200, 0, 0), 0, 0), true), false);
        helper.assertTrue(!home.test(player), "200 blocks from the bed");
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(net.minecraft.world.level.storage.LevelData.RespawnData.of(level.dimension(),
                player.blockPosition().offset(20, 0, 0), 0, 0), true), false);
        helper.assertTrue(home.test(player), "20 blocks from the bed");

        // Quarry: an arrow hit makes the mob your prey - marked by you and taking +50% from you; melee doesn't pick.
        Zombie prey = helper.spawn(EntityType.ZOMBIE, new BlockPos(7, 2, 7));
        com.pipican.ability.Quarries.onHit(player, prey, false, true);
        helper.assertTrue(!com.pipican.ability.Quarries.has(player, "test prey"), "a melee hit doesn't pick prey");
        com.pipican.ability.Quarries.onHit(player, prey, true, false);
        helper.assertTrue(com.pipican.ability.Quarries.has(player, "test prey"), "an arrow hit does");
        helper.assertTrue(com.pipican.ability.Marks.isMarkedBy(prey, player), "prey counts as marked");
        assertNear(helper, 1.5, com.pipican.ability.Quarries.multiplier(player, prey), "prey takes +50%");
        assertNear(helper, 1, com.pipican.ability.Quarries.multiplier(player, spare), "other mobs don't");
        com.pipican.ability.Quarries.onHit(player, spare, true, false);
        helper.assertTrue(!com.pipican.ability.Marks.isMarkedBy(prey, player) && com.pipican.ability.Marks.isMarkedBy(spare, player), "new prey replaces the old");

        // Reaction duration upgrades: "after-effects last half as long" turns a 10s effect into 5s.
        var effect = com.pipican.ability.action.EffectAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"target\": \"self\", \"effects\": [{\"effect\": \"minecraft:haste\", \"seconds\": 10}]}")).getOrThrow();
        player.removeEffect(MobEffects.HASTE);
        effect.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0)
                .withMod(new ClassState.ReactionMod(0, 0, 0, 0, java.util.List.of(), 0, java.util.Map.of(), 0, 0, -0.5)));
        var haste = player.getEffect(MobEffects.HASTE);
        helper.assertTrue(haste != null && haste.getDuration() == 100, "half as long: " + (haste == null ? "none" : haste.getDuration()));
        player.removeEffect(MobEffects.HASTE);

        // Stored charges: an empower with charges 1 keeps at most one waiting.
        var gadget = com.pipican.ability.action.EmpowerAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"multiplier\": 1.37, \"seconds\": 60, \"charges\": 1}")).getOrThrow();
        gadget.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        gadget.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        helper.assertTrue(com.pipican.ability.Empowers.count(player, 1.37, 0) == 1, "one gadget charge stored: " + com.pipican.ability.Empowers.count(player, 1.37, 0));
        com.pipican.ability.Empowers.forget(player);

        // A heal that needs someone to heal fizzles when no one is there (a reaction then keeps its cooldown).
        var succor = com.pipican.ability.action.HealAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"target\": \"players\", \"radius\": 0.5, \"amount\": 6, \"require_target\": true}")).getOrThrow();
        var lonely = new AbilityContext(player, Optional.empty(), player.position().add(0, 50, 0), Optional.empty(), 0);
        succor.run(lonely);
        helper.assertTrue(lonely.fizzled(), "no one to heal: fizzled");

        // Focus schools: one slot; fire picked, so fire damage is +50% and a second pick is refused.
        helper.assertTrue(com.pipican.ability.Focus.choose(player, "fire").success(), "fire picked");
        helper.assertTrue(!com.pipican.ability.Focus.choose(player, "water").success(), "only one slot");
        assertNear(helper, 1.5, com.pipican.ability.Focus.damage(player, level.damageSources().onFire(), spare), "fire focus");
        assertNear(helper, 1, com.pipican.ability.Focus.damage(player, level.damageSources().cactus(), spare), "no school");

        // Hobbles: "if_effect" only takes on a mob with that effect; a root also stops jumping.
        var lethargy = com.pipican.ability.action.HobbleAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"seconds\": 5, \"if_effect\": \"minecraft:slowness\"}")).getOrThrow();
        spare.removeEffect(MobEffects.SLOWNESS);
        lethargy.run(new AbilityContext(player, Optional.empty(), spare.position(), Optional.of(spare), 0));
        helper.assertTrue(!com.pipican.ability.action.HobbleAction.noJump(spare), "not slowed: may still jump");
        spare.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
        lethargy.run(new AbilityContext(player, Optional.empty(), spare.position(), Optional.of(spare), 0));
        helper.assertTrue(com.pipican.ability.action.HobbleAction.noJump(spare) && !com.pipican.ability.action.HobbleAction.rooted(spare), "slowed: can't jump");
        spare.removeEffect(MobEffects.SLOWNESS);

        // Poison on undead becomes Wither.
        spare.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 80, 0), player);
        helper.assertTrue(!spare.hasEffect(MobEffects.POISON) && spare.hasEffect(MobEffects.WITHER), "zombie poison became wither");
        spare.removeEffect(MobEffects.WITHER);

        // A capped mark ("max": 1): a second mob can't be marked while the first one is.
        var curse = com.pipican.ability.action.MarkAction.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,
                com.google.gson.JsonParser.parseString("{\"target\": \"victim\", \"seconds\": 5, \"multiplier\": 1.0, \"glow\": false, \"max\": 1}")).getOrThrow();
        var cursedOne = new AbilityContext(player, Optional.empty(), spare.position(), Optional.of(spare), 0);
        curse.run(cursedOne);
        var cursedTwo = new AbilityContext(player, Optional.empty(), prey.position(), Optional.of(prey), 0);
        curse.run(cursedTwo);
        helper.assertTrue(!cursedOne.fizzled() && cursedTwo.fizzled(), "one curse at a time");

        // A temple that follows you: you're always in it, and so is anything within its 6 blocks.
        helper.assertTrue(new com.pipican.condition.type.TempleCondition(Optional.empty()).test(player), "inside a moving temple");
        helper.assertTrue(com.pipican.ability.Temples.contains(player, player.position().add(5, 0, 0))
                && !com.pipican.ability.Temples.contains(player, player.position().add(7, 0, 0)), "6 blocks wide");

        // Trackless: the mixed-in pressure plate count skips a player with no_plates (but not one without tripwire immunity).
        helper.assertTrue(com.pipican.ability.Trackless.ignoresPlates(player) && !com.pipican.ability.Trackless.ignoresTripwires(player), "plates only");
        var plate = player.getBoundingBox().inflate(0.5);
        try {
            var count = net.minecraft.world.level.block.BasePressurePlateBlock.class.getDeclaredMethod("getEntityCount",
                    net.minecraft.world.level.Level.class, net.minecraft.world.phys.AABB.class, Class.class);
            count.setAccessible(true);
            int counted = (int) count.invoke(null, level, plate, net.minecraft.world.entity.player.Player.class);
            helper.assertTrue(counted == 0, "pressure plate doesn't count the player: " + counted);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }

    /** pipican:bonus (XP, potion durations), dodge and immunities. */
    private static void bonuses(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, BONUS_TESTER, false, true).success(), "choose bonus tester");
        ClassState state = ClassEffectApplier.state(player);
        assertNear(helper, 0.1, state.dodgeChance(), "dodge chance");
        assertNear(helper, 1, state.bonus(BonusEffect.Kind.POTION_DURATION), "amounts of one kind add up");

        int before = player.totalExperience;
        player.giveExperiencePoints(10);
        player.giveExperiencePoints(1);
        helper.assertTrue(player.totalExperience - before == 16, "+50% XP with remainders carried: " + (player.totalExperience - before));

        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        helper.assertTrue(player.getEffect(MobEffects.POISON).getDuration() == 100, "harmful effects last half as long");
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200));
        helper.assertTrue(player.getEffect(MobEffects.SPEED).getDuration() == 200, "beneficial effects unchanged");
        helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200)), "immune to nausea");

        PlayerClassData data = ClassService.data(player);
        assertNear(helper, 0, data.resource(), "fill-type bar starts empty");
        Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.HIT);
        assertNear(helper, 3, data.resource(), "a hit fills 2, +50% resource gain");
        helper.assertTrue(!Combat.inCombat(player), "not in combat yet");
        Combat.mark(player);
        helper.assertTrue(Combat.inCombat(player), "in combat after a hit");
        Ability mend = ClassLookup.abilities(player.level().registryAccess()).getValue(ability("mend"));
        assertNear(helper, 2.5, AbilityService.cost(player, mend), "abilities cost half");

        // Named reaction chances: 0% base + 100% from reaction_chance
        double before2 = data.resource();
        Reactions.fire(player, null, ItemStack.EMPTY, ReactionEffect.Event.KILL);
        assertNear(helper, before2 + 6, data.resource(), "chance-0 reaction fires once its chance is raised (4 +50%)");

        // Bleeding: effects add up
        ClassState.Bleed bleed = ClassEffectApplier.state(player).bleed();
        assertNear(helper, 5, bleed.seconds(), "bleed seconds add up");
        helper.assertTrue(bleed.maxStacks() == 2, "highest max_stacks counts");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        helper.assertTrue(!Bleeds.isBleeding(zombie), "not bleeding yet");
        Bleeds.onHit(player, zombie, bleed, false);
        helper.assertTrue(!Bleeds.isBleeding(zombie), "normal hits don't bleed");
        Bleeds.onHit(player, zombie, bleed, true);
        helper.assertTrue(Bleeds.isBleeding(zombie), "crits make mobs bleed");
        helper.assertTrue(new VictimState(Optional.empty(), false, false, false, false, true, false, false, Optional.empty(), Optional.empty(), false, false, false, false, false, false).test(player, zombie), "bleeding victim state");
        zombie.discard();

        // Skill triggers: pipican:any of filtered / did triggers
        ResourceKey<Skill> survivor = ResourceKey.create(PipicanRegistries.SKILL, Identifier.fromNamespaceAndPath("pipican_test", "survivor"));
        MasteryService.onAction(player, new SkillAction.Did(SkillAction.Did.Kind.HURT, ItemStack.EMPTY, Optional.empty(), Optional.empty(), 3));
        assertNear(helper, 0, data.mastery(survivor), "small hits don't count (min_amount 5)");
        MasteryService.onAction(player, new SkillAction.Did(SkillAction.Did.Kind.HURT, ItemStack.EMPTY, Optional.empty(), Optional.empty(), 6));
        assertNear(helper, 1, data.mastery(survivor), "a big hit counts");
        Zombie undead = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 2));
        MasteryService.onAction(player, new SkillAction.Kill(ItemStack.EMPTY, Optional.of(cow)));
        assertNear(helper, 1, data.mastery(survivor), "killing a cow isn't killing undead");
        MasteryService.onAction(player, new SkillAction.Kill(ItemStack.EMPTY, Optional.of(undead)));
        assertNear(helper, 2, data.mastery(survivor), "killing undead counts");
        MasteryService.onAction(player, new SkillAction.Did(SkillAction.Did.Kind.MINE, ItemStack.EMPTY, Optional.of(Blocks.STONE.defaultBlockState()), Optional.empty(), 0));
        MasteryService.onAction(player, new SkillAction.Did(SkillAction.Did.Kind.MINE, ItemStack.EMPTY, Optional.of(Blocks.OAK_LOG.defaultBlockState()), Optional.empty(), 0));
        assertNear(helper, 3, data.mastery(survivor), "mining stone counts, logs don't");
        undead.discard();
        cow.discard();

        // Healing received -50% (natural regen is left alone).
        player.setHealth(10);
        player.heal(4);
        assertNear(helper, 12, player.getHealth(), "half healing received");
        player.setHealth(player.getMaxHealth());

        // Scaling with missing health refreshes as health changes.
        player.setHealth(player.getMaxHealth());
        ClassEffectApplier.recheckConditions(player);
        assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "full health: no bonus");
        player.setHealth(player.getMaxHealth() - 6);
        ClassEffectApplier.recheckConditions(player);
        assertNear(helper, 3, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "3 missing hearts: +3 toughness");
        player.setHealth(player.getMaxHealth());
        ClassEffectApplier.recheckConditions(player);

        // Timed boosts from reactions
        double armor = player.getAttributeValue(Attributes.ARMOR);
        new BoostAction(5, List.of(new AttributeEffect(Attributes.ARMOR, 3, AttributeModifier.Operation.ADD_VALUE, List.of())), 1, false, 0)
                .run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        assertNear(helper, armor + 3, player.getAttributeValue(Attributes.ARMOR), "boost gives +3 armor");
        Boosts.clear(player);
        assertNear(helper, armor, player.getAttributeValue(Attributes.ARMOR), "boost removed");
        var stacking = new BoostAction(5, List.of(new AttributeEffect(Attributes.ARMOR, 1, AttributeModifier.Operation.ADD_VALUE, List.of())), 3, false, 0);
        for (int n = 0; n < 4; n++) {
            stacking.run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        }
        assertNear(helper, armor + 3, player.getAttributeValue(Attributes.ARMOR), "boost stacks up to 3");
        Boosts.clear(player);

        // Standing still / moving
        Movement.tick(player);
        helper.assertTrue(new StillCondition(0).test(player), "a player that hasn't moved is still");
        player.teleportTo(player.getX() + 2, player.getY(), player.getZ());
        Movement.tick(player);
        helper.assertTrue(!new StillCondition(0).test(player), "just moved");

        // Mount upgrades
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));
        helper.assertTrue(player.startRiding(horse, true, true), "mount the horse");
        Mounts.tick(player);
        assertNear(helper, 4, horse.getAttributeValue(Attributes.ARMOR), "mount gets +4 armor");
        player.stopRiding();
        Mounts.tick(player);
        assertNear(helper, 0, horse.getAttributeValue(Attributes.ARMOR), "upgrade removed after dismounting");
        helper.succeed();
    }

    private static void conditionsAndResource(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, CONDITION_TESTER, false, true).success(), "choose condition tester");
        ClassState state = ClassEffectApplier.state(player);
        assertNear(helper, (10 + 5) * 1.5, state.resourceMax(), "resource max: (base + flat) * (1 + percent)");
        assertNear(helper, (2 + 1) / 20.0, state.resourceRegenPerTick(), "resource regen: base + flat");

        assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR), "no armor at full health");
        assertNear(helper, 1, player.getAttributeValue(Attributes.LUCK), "not on fire -> luck");
        helper.assertTrue(!ClassEffectApplier.recheckConditions(player), "nothing changed yet");

        player.setHealth(player.getMaxHealth() * 0.25f);
        helper.assertTrue(ClassEffectApplier.recheckConditions(player), "health condition flipped");
        assertNear(helper, 5, player.getAttributeValue(Attributes.ARMOR), "armor below half health");

        player.setShiftKeyDown(true);
        helper.assertTrue(ClassEffectApplier.recheckConditions(player), "sneaking flipped");
        assertNear(helper, 2, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness while sneaking");
        assertNear(helper, 0, ClassEffectApplier.state(player).resourceRegenPerTick(), "-100% regen while sneaking");

        player.setHealth(player.getMaxHealth());
        player.setShiftKeyDown(false);
        helper.assertTrue(ClassEffectApplier.recheckConditions(player), "both flipped back");
        assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR), "armor gone at full health");
        assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness gone");
        helper.succeed();
    }

    private static ResourceKey<Ability> ability(String name) {
        return ResourceKey.create(PipicanRegistries.ABILITY, Identifier.fromNamespaceAndPath("pipican_test", name));
    }

    /** Costs, cooldowns, timed states, toggles that drain, node grants, auto abilities, slots. */
    private static void abilities(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, ABILITY_TESTER, false, true).success(), "choose ability tester");
        PlayerClassData data = ClassService.data(player);
        var access = helper.getLevel().registryAccess();
        assertNear(helper, 20, data.resource(), "resource starts full");
        helper.assertTrue(AbilitySlots.slots(access, data).subList(0, 3).equals(List.of(ability("mend"), ability("bulwark"), ability("soar"))),
                "auto abilities stay off the keys: " + AbilitySlots.slots(access, data));

        // Instant: cost and cooldown.
        player.setHealth(10);
        helper.assertTrue(AbilityService.use(player, ability("mend")).success(), "mend");
        assertNear(helper, 14, player.getHealth(), "mend heals 4");
        assertNear(helper, 15, data.resource(), "mend costs 5");
        helper.assertTrue(!AbilityService.use(player, ability("mend")).success(), "mend on cooldown");
        helper.assertTrue(!AbilityService.use(player, ability("granted")).success(), "node ability not granted yet");

        // Timed state: effects and ability_active while it runs.
        helper.assertTrue(AbilityService.use(player, ability("bulwark")).success(), "bulwark");
        assertNear(helper, 4, player.getAttributeValue(Attributes.ARMOR), "bulwark armor while active");
        assertNear(helper, 1, player.getAttributeValue(Attributes.LUCK), "ability_active condition");
        helper.assertTrue(!AbilityService.use(player, ability("bulwark")).success(), "already active");

        // Toggle: flight while on, drains the bar, stops itself when empty.
        helper.assertTrue(AbilityService.use(player, ability("soar")).success(), "soar on");
        // (Mock players are in creative, where flight is left alone, so check the computed state.)
        helper.assertTrue(ClassEffectApplier.state(player).flight(), "flight while soaring");
        helper.assertTrue(AbilityService.use(player, ability("soar")).success(), "soar off");
        helper.assertTrue(!ClassEffectApplier.state(player).flight(), "no flight after soaring");

        // Node grant.
        helper.assertTrue(ClassService.allocateNode(player, TreeKind.GENERAL, "a_grant").success(), "allocate granting node");
        helper.assertTrue(AbilitySlots.granted(access, data).contains(ability("granted")), "node grants its ability");

        // Auto: starts by itself below half health.
        player.setHealth(5);
        player.tickCount = 9;
        AbilityService.tick(player);
        player.tickCount = 10;
        AbilityService.tick(player);
        assertNear(helper, 4, player.getAbsorptionAmount(), "last stand fired by itself");

        helper.runAfterDelay(41, () -> {
            player.tickCount = 41;
            AbilityService.tick(player);
            assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR), "bulwark ended");
            helper.assertTrue(!data.abilityActive().containsKey(ability("bulwark")), "bulwark no longer active");
            helper.succeed();
        });
    }

    /** Marks, next-hit buffs, hit shields, cancel + refund, per-rest uses, reactions, mob control, eidolon, Optimization. */
    private static void abilityMechanics(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(ClassService.chooseClass(player, ABILITY_TESTER, false, true).success(), "choose ability tester");
        PlayerClassData data = ClassService.data(player);
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 1, 3);
        zombie.setHealth(zombie.getMaxHealth());
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2, 0, 0);
        var sources = helper.getLevel().damageSources();

        // Cancel + refund: nothing in the crosshair within 4 blocks... after moving away.
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 10, 0, 0);
        double before = data.resource();
        helper.assertTrue(!AbilityService.use(player, ability("needs_target")).success(), "no target -> cancelled");
        assertNear(helper, before, data.resource(), "cancelled use is refunded");
        helper.assertTrue(data.abilityReadyAt(ability("needs_target")) == 0, "cancelled use starts no cooldown");
        player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2, 0, 0);

        // Mark (x2, next hit only) and empower (+5 next hit) stack on the next hit, then are used up.
        helper.assertTrue(AbilityService.use(player, ability("brand")).success(), "brand");
        helper.assertTrue(AbilityService.use(player, ability("smite")).success(), "smite");
        float hp = zombie.getHealth();
        // Magic damage ignores the zombie's armor, so the numbers stay exact.
        zombie.hurtServer(helper.getLevel(), sources.indirectMagic(player, player), 2);
        assertNear(helper, hp - (2 * 2 + 5), zombie.getHealth(), "marked + empowered hit");
        hp = zombie.getHealth();
        zombie.invulnerableTime = 0;
        zombie.hurtServer(helper.getLevel(), sources.indirectMagic(player, player), 2);
        assertNear(helper, hp - 2, zombie.getHealth(), "mark and empower used up");

        // Hit shield: the next hit on the player misses.
        player.setHealth(20);
        helper.assertTrue(AbilityService.use(player, ability("guard")).success(), "guard");
        player.hurtServer(helper.getLevel(), sources.generic(), 6);
        assertNear(helper, 20, player.getHealth(), "guarded hit missed");

        // Per-rest uses come back after sleeping.
        helper.assertTrue(AbilityService.use(player, ability("rested")).success(), "rested");
        helper.assertTrue(!AbilityService.use(player, ability("rested")).success(), "only once per rest");
        AbilityService.onWake(player);
        helper.assertTrue(AbilityService.use(player, ability("rested")).success(), "usable again after sleeping");

        // Mob control: a pacified zombie can't target the player.
        helper.assertTrue(AbilityService.use(player, ability("calm")).success(), "calm");
        zombie.setTarget(player);
        helper.assertTrue(zombie.getTarget() != player, "pacified mob doesn't target you");

        // Reaction: a kill gives Speed.
        zombie.hurtServer(helper.getLevel(), sources.playerAttack(player), 1000);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "on-kill reaction");

        // Eidolon: built, owned, upgraded by the owner's class; calling it again doesn't build a second one.
        helper.assertTrue(AbilityService.use(player, ability("build")).success(), "build eidolon");
        var eidolon = com.pipican.entity.Eidolon.of(player).orElseThrow();
        helper.assertTrue(eidolon.isOwnedBy(player), "eidolon owned");
        helper.assertTrue(!AbilityService.use(player, ability("build")).success(), "second use calls it instead");
        helper.assertTrue(!eidolon.canAttack(player), "eidolon never attacks players");

        // Optimization: owed after waking, chat command applies it.
        AbilityService.onWake(player);
        var raise = ResourceKey.create(PipicanRegistries.STAT, Pipican.id("strength"));
        var lower = ResourceKey.create(PipicanRegistries.STAT, Pipican.id("luck"));
        helper.assertTrue(com.pipican.ability.Optimize.choose(player, raise, lower).success(), "optimize");
        assertNear(helper, 3, ClassEffectApplier.state(player).stat(raise), "raised stat");
        assertNear(helper, -3, ClassEffectApplier.state(player).stat(lower), "lowered stat");
        helper.assertTrue(!com.pipican.ability.Optimize.choose(player, raise, lower).success(), "only once per sleep");

        helper.runAfterDelay(45, () -> {
            eidolon.refreshUpgrades();
            var upgrade = eidolon.getAttribute(Attributes.ARMOR).getModifier(Pipican.id("eidolon/add_value"));
            helper.assertTrue(upgrade != null, "eidolon upgrade applied");
            assertNear(helper, 2, upgrade.amount(), "eidolon upgrade amount");
            eidolon.discard();
            helper.succeed();
        });
    }

    /** Every ability the mod ships runs without errors, with a zombie in front of the player to aim at. */
    private static void shippedAbilities(GameTestHelper helper) {
        var registry = ClassLookup.abilities(helper.getLevel().registryAccess());
        int count = 0;
        // Whatever was here before the test (other tests' mobs) stays; everything the abilities make is removed.
        var existing = new java.util.HashSet<>(helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null,
                helper.getBounds().inflate(64), e -> true));
        for (var entry : registry.entrySet()) {
            if (!entry.getKey().identifier().getNamespace().equals(Pipican.MOD_ID)) {
                continue;
            }
            @SuppressWarnings("removal")
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            String owner = entry.getKey().identifier().getPath().split("/")[0];
            ClassService.chooseClass(player, ResourceKey.create(PipicanRegistries.CLASS, Pipican.id(owner)), false, true);
            var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 1, 3);
            zombie.setBaby(false); // a baby zombie is below eye level, so "look" abilities would miss it
            player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2, 0, 0);
            // Finishers need an afflicted target (Coup de Grace); Rune Blast needs a sigil weapon in hand.
            zombie.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 200));
            if (owner.equals("runesmith")) {
                ItemStack sigil = new ItemStack(Items.IRON_SWORD);
                sigil.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Rune"));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sigil);
            }
            var area = player.getBoundingBox().inflate(48);
            try {
                AbilityService.force(player, entry.getKey());
            } catch (RuntimeException e) {
                throw new IllegalStateException("ability " + entry.getKey().identifier() + " failed: " + e, e);
            }
            boolean deals = entry.getValue().actions().stream().anyMatch(a -> a instanceof com.pipican.ability.action.DamageAction d && d.amount() > 0);
            helper.assertTrue(!deals || zombie.getHealth() < zombie.getMaxHealth(),
                    entry.getKey().identifier() + " should damage the zombie in front of the player (zombie " + zombie.getHealth() + "/" + zombie.getMaxHealth()
                            + " at " + zombie.position() + ", baby " + zombie.isBaby() + ", armor " + zombie.getArmorValue() + ", player at " + player.position()
                            + ", vehicle " + zombie.getVehicle() + ", passengers " + zombie.getPassengers() + ")");
            AbilityService.reset(player);
            // Remove everything the ability spawned (eidolons, summons, decoys) so it can't get in the next one's way.
            // (Only what appeared during this ability: other tests' mobs nearby are left alone.)
            var bounds = helper.getBounds().inflate(12);
            for (var e : helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null, area,
                    e -> !(e instanceof net.minecraft.world.entity.player.Player) && !existing.contains(e)
                            && (bounds.contains(e.position()) || com.pipican.ability.Summons.isSummonOf(e, player)
                                || e instanceof net.minecraft.world.entity.OwnableEntity o && o.getOwner() == player))) {
                e.discard();
            }
            player.stopRiding();
            dismiss(helper, player);
            count++;
        }
        helper.assertTrue(count == 114, "expected 114 shipped abilities, ran " + count);
        helper.succeed();
    }

    /** Every class the mod ships (built from class_specs/) can be chosen, and its three trees resolve to their start nodes. */
    private static void shippedClasses(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        int count = 0;
        for (var entry : ClassLookup.classes(access).entrySet()) {
            ResourceKey<RpgClass> key = entry.getKey();
            if (!key.identifier().getNamespace().equals(Pipican.MOD_ID)) {
                continue;
            }
            count++;
            RpgClass cls = entry.getValue();
            String id = key.identifier().toString();
            // Shipped classes spend stat points in the level-up screen, not a stat tree.
            helper.assertTrue(cls.tree(TreeKind.STAT).isEmpty(), id + " still has a stat tree");
            for (TreeKind kind : List.of(TreeKind.GENERAL, TreeKind.SPECIAL)) {
                var ref = cls.tree(kind);
                helper.assertTrue(ref.isPresent(), id + " has no " + kind.getSerializedName() + " tree");
                var tree = ClassLookup.tree(access, cls, kind);
                helper.assertTrue(tree.isPresent(), id + ": " + kind.getSerializedName() + " tree " + ref.get().tree() + " doesn't exist");
                var start = tree.get().node(ref.get().start());
                helper.assertTrue(start.isPresent() && start.get().type() == NodeType.START,
                        id + ": start node " + ref.get().start() + " missing from " + ref.get().tree());
            }
            @SuppressWarnings("removal")
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            ClassService.Result result = ClassService.chooseClass(player, key, true, true);
            helper.assertTrue(result.success(), id + ": " + result.message().getString());
            dismiss(helper, player);
        }
        helper.assertTrue(count == 84, "expected 84 shipped classes, found " + count);
        helper.succeed();
    }

    /** Slot counts per class, and items returned when slots disappear. */
    private static void trinkets(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        PlayerClassData data = ClassService.data(player);
        helper.assertTrue(data.trinkets(RING).size() == 2, "2 ring slots");
        helper.assertTrue(data.trinkets(AMULET).size() == 1, "1 amulet slot");

        data.trinkets(RING).set(1, new ItemStack(Items.GOLD_NUGGET));
        ClassService.resetClass(player);
        helper.assertTrue(data.trinkets(RING).isEmpty(), "no slots without a class");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.GOLD_NUGGET)), "trinket returned to inventory");
        helper.succeed();
    }

    // ------------------------------------------------------------------ skill triggers driven by real game events

    private static ServerPlayer classPlayer(GameTestHelper helper, String classId, BlockPos pos) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ClassService.Result result = ClassService.chooseClass(player, ResourceKey.create(PipicanRegistries.CLASS, Pipican.id(classId)), true, true);
        helper.assertTrue(result.success(), "chooseClass " + classId + " failed: " + result.message().getString());
        BlockPos at = helper.absolutePos(pos);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static double mastery(ServerPlayer player, String skill) {
        return ClassService.data(player).mastery(ResourceKey.create(PipicanRegistries.SKILL, Pipican.id(skill)));
    }

    private static Zombie victim(GameTestHelper helper, int x, int z) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(x, 2, z));
        zombie.setNoAi(true);
        return zombie;
    }

    private static void killWith(GameTestHelper helper, ServerPlayer player, LivingEntity mob, ItemStack weapon) {
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
        mob.invulnerableTime = 0;
        mob.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000);
    }

    /**
     * The less common skill triggers, through the real events: kill streaks, assists, kills inside an aura, weapon
     * variety, Looting kills, empowered hits, setting mobs on fire, renaming at an anvil, safe falls and new chunks.
     */
    private static void skillTriggers(GameTestHelper helper) {
        // Death Dealer: only the 3rd kill within 10s counts.
        ServerPlayer reaper = classPlayer(helper, "death_dealer", new BlockPos(1, 2, 1));
        killWith(helper, reaper, victim(helper, 2, 2), new ItemStack(Items.IRON_AXE));
        killWith(helper, reaper, victim(helper, 2, 3), new ItemStack(Items.IRON_AXE));
        helper.assertTrue(mastery(reaper, "death_dealer_reaping") == 0, "two kills aren't a streak");
        killWith(helper, reaper, victim(helper, 2, 4), new ItemStack(Items.IRON_AXE));
        helper.assertTrue(mastery(reaper, "death_dealer_reaping") > 0, "the third kill within 10s trains Reaping");
        dismiss(helper, reaper);

        // Conqueror: an ally killing a mob the conqueror hit; Bard: an ally killing inside the Song.
        ServerPlayer general = classPlayer(helper, "conqueror", new BlockPos(1, 2, 1));
        ServerPlayer bard = classPlayer(helper, "bard", new BlockPos(1, 2, 2));
        ServerPlayer ally = classPlayer(helper, "warrior", new BlockPos(2, 2, 2));
        // The Song is off while no other player is near (the bard's weakness): rebuild now that the ally is here.
        com.pipican.player.ClassEffectApplier.refresh(bard);
        Zombie marked = victim(helper, 3, 3);
        marked.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(general), 1);
        killWith(helper, ally, marked, ItemStack.EMPTY);
        helper.assertTrue(mastery(general, "conqueror_generalship") > 0, "an ally finishing the conqueror's mob trains Generalship");
        helper.assertTrue(mastery(bard, "bard_repertoire") > 0, "an ally's kill inside the Song trains Repertoire");
        double before = mastery(general, "conqueror_generalship");
        killWith(helper, ally, victim(helper, 3, 4), ItemStack.EMPTY);
        helper.assertTrue(mastery(general, "conqueror_generalship") == before, "mobs the conqueror never hit don't count");
        dismiss(helper, general);
        dismiss(helper, bard);
        dismiss(helper, ally);

        // Fighter: kills with 3 different weapon types in one day.
        ServerPlayer fighter = classPlayer(helper, "fighter", new BlockPos(1, 2, 1));
        killWith(helper, fighter, victim(helper, 2, 2), new ItemStack(Items.IRON_SWORD));
        killWith(helper, fighter, victim(helper, 2, 3), new ItemStack(Items.IRON_SWORD));
        killWith(helper, fighter, victim(helper, 2, 4), new ItemStack(Items.IRON_AXE));
        helper.assertTrue(mastery(fighter, "fighter_drill") == 0, "two weapon types aren't enough");
        killWith(helper, fighter, victim(helper, 3, 2), new ItemStack(Items.MACE));
        helper.assertTrue(mastery(fighter, "fighter_drill") > 0, "a third weapon type trains Drill");
        dismiss(helper, fighter);

        // Thief: kills with a Looting weapon count, plain ones don't.
        ServerPlayer thief = classPlayer(helper, "thief", new BlockPos(1, 2, 1));
        killWith(helper, thief, victim(helper, 2, 2), new ItemStack(Items.IRON_SWORD));
        helper.assertTrue(mastery(thief, "thief_thievery") == 0, "no Looting, no Thievery");
        ItemStack looting = new ItemStack(Items.IRON_SWORD);
        looting.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), 1);
        killWith(helper, thief, victim(helper, 2, 3), looting);
        helper.assertTrue(mastery(thief, "thief_thievery") > 0, "a Looting kill trains Thievery");
        dismiss(helper, thief);

        // Mage Knight: hits that use up an empowered hit.
        ServerPlayer knight = classPlayer(helper, "mage_knight", new BlockPos(1, 2, 1));
        Zombie dummy = victim(helper, 2, 2);
        dummy.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(knight), 1);
        helper.assertTrue(mastery(knight, "mage_knight_war_magic") == 0, "plain hits don't train War Magic");
        com.pipican.ability.Empowers.add(knight, 200, 1.2, 0, List.of(), Optional.empty(), 3);
        dummy.invulnerableTime = 0;
        dummy.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(knight), 1);
        helper.assertTrue(mastery(knight, "mage_knight_war_magic") > 0, "an Escalating Violence hit trains War Magic");
        dismiss(helper, knight);

        // Runesmith: renaming a weapon at an anvil.
        ServerPlayer runesmith = classPlayer(helper, "runesmith", new BlockPos(1, 2, 1));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        ItemStack named = sword.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Oathkeeper"));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.AnvilCraftEvent.Post(
                null, runesmith, sword, ItemStack.EMPTY, named));
        helper.assertTrue(mastery(runesmith, "runesmith_runecarving") > 0, "renaming a weapon trains Runecarving");
        dismiss(helper, runesmith);

        // Pyromancer: setting a mob on fire (checked at the end of the tick).
        ServerPlayer pyro = classPlayer(helper, "pyromancer", new BlockPos(1, 2, 1));
        Zombie kindling = victim(helper, 2, 2);
        kindling.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(pyro), 1);
        kindling.igniteForSeconds(4);
        // Aeromancer: a long fall that doesn't hurt.
        ServerPlayer aero = classPlayer(helper, "aeromancer", new BlockPos(1, 2, 3));
        net.neoforged.neoforge.common.CommonHooks.onLivingFall(aero, 10, 0);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(mastery(pyro, "pyromancer_firestarting") > 0, "setting a mob on fire trains Firestarting");
            helper.assertTrue(mastery(aero, "aeromancer_windcalling") > 0, "a safe long fall trains Windcalling");
            dismiss(helper, pyro);
            dismiss(helper, aero);
            helper.succeed();
        });
    }

    // ------------------------------------------------------------------ abilities built to their spec text

    private static ResourceKey<com.pipican.ability.Ability> shipped(String id) {
        return ResourceKey.create(PipicanRegistries.ABILITY, Pipican.id(id));
    }

    /** Each new mechanic does what its ability's text says. */
    private static void abilitySpecs(GameTestHelper helper) {
        var level = helper.getLevel();

        // Time Stop: frozen in place, then thawed.
        ServerPlayer chrono = classPlayer(helper, "chronomancer", new BlockPos(1, 2, 1));
        Zombie frozen = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        com.pipican.ability.AbilityService.force(chrono, shipped("chronomancer/time_stop"));
        helper.assertTrue(frozen.isNoAi() && com.pipican.ability.Freezes.isFrozen(frozen), "Time Stop freezes mobs within 6 blocks");
        dismiss(helper, chrono);

        // Hex: sneaking picks the next curse (free), using it casts the picked one.
        ServerPlayer hexer = classPlayer(helper, "hexer", new BlockPos(1, 2, 4));
        Zombie hexed = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 2, 7);
        hexed.setBaby(false);
        hexer.snapTo(hexed.getX(), hexed.getY(), hexed.getZ() - 2, 0, 0);
        hexer.setShiftKeyDown(true);
        var pick = com.pipican.ability.AbilityService.force(hexer, shipped("hexer/hex"));
        helper.assertTrue(pick.isCancelled() && pick.cancelReason().getString().contains("Lethargy"), "sneak + use picks the next curse: " + pick.cancelReason().getString());
        hexer.setShiftKeyDown(false);
        com.pipican.ability.AbilityService.force(hexer, shipped("hexer/hex"));
        helper.assertTrue(hexed.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "the picked curse (Lethargy) is cast");
        dismiss(helper, hexer);

        // Shared Recovery: you and the nearest player.
        ServerPlayer priest = classPlayer(helper, "priest", new BlockPos(4, 2, 1));
        ServerPlayer friend = classPlayer(helper, "warrior", new BlockPos(5, 2, 1));
        ServerPlayer far = classPlayer(helper, "warrior", new BlockPos(7, 2, 7));
        priest.setHealth(4);
        friend.setHealth(4);
        far.setHealth(4);
        com.pipican.ability.AbilityService.force(priest, shipped("priest/shared_recovery"));
        helper.assertTrue(priest.getHealth() > 4 && friend.getHealth() > 4, "Shared Recovery heals you and the nearest player");
        helper.assertTrue(far.getHealth() == 4, "...and only one other player");
        dismiss(helper, priest);
        dismiss(helper, friend);
        dismiss(helper, far);

        // Escape: cobwebs around you are torn.
        ServerPlayer thief = classPlayer(helper, "thief", new BlockPos(6, 2, 4));
        BlockPos web = helper.absolutePos(new BlockPos(6, 2, 4));
        level.setBlockAndUpdate(web, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState());
        com.pipican.ability.AbilityService.force(thief, shipped("thief/escape"));
        helper.assertTrue(!level.getBlockState(web).is(net.minecraft.world.level.block.Blocks.COBWEB), "Escape breaks the cobweb you're stuck in");
        dismiss(helper, thief);

        // Store Spell: the off-hand potion goes into the sword, and each hit casts it.
        ServerPlayer artificer = classPlayer(helper, "artificer", new BlockPos(1, 2, 1));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        artificer.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        artificer.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                Items.POTION, net.minecraft.world.item.alchemy.Potions.WEAKNESS));
        com.pipican.ability.AbilityService.force(artificer, shipped("artificer/store_spell"));
        helper.assertTrue(com.pipican.ability.StoredSpells.has(artificer.getMainHandItem()), "Store Spell loads the potion into the weapon");
        helper.assertTrue(artificer.getOffhandItem().is(Items.GLASS_BOTTLE), "the potion is used up");
        Zombie stung = victim(helper, 2, 1);
        stung.hurtServer(level, level.damageSources().playerAttack(artificer), 1);
        helper.assertTrue(stung.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "a hit with the loaded weapon casts the stored potion");
        dismiss(helper, artificer);

        // Animate Object: the block becomes a construct, and comes back when it ends.
        ServerPlayer techno = classPlayer(helper, "technomancer", new BlockPos(1, 2, 1));
        BlockPos anvil = helper.absolutePos(new BlockPos(1, 2, 3));
        level.setBlockAndUpdate(anvil, net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState());
        techno.snapTo(anvil.getX() + 0.5, anvil.getY(), anvil.getZ() - 1.5, 0, 0);
        techno.setXRot(20);
        var animated = com.pipican.ability.AbilityService.force(techno, shipped("technomancer/animate_object"));
        helper.assertTrue(!animated.isCancelled(), "Animate Object found the block: " + (animated.isCancelled() ? animated.cancelReason().getString() : ""));
        helper.assertTrue(level.getBlockState(anvil).isAir(), "the block left its spot");
        var construct = level.getEntitiesOfClass(net.minecraft.world.entity.animal.golem.IronGolem.class, new net.minecraft.world.phys.AABB(anvil).inflate(2),
                g -> com.pipican.ability.Summons.isSummonOf(g, techno)).stream().findFirst();
        helper.assertTrue(construct.isPresent() && construct.get().isInvisible() && !construct.get().getPassengers().isEmpty(), "a construct wearing the block took its place");
        construct.get().discard();
        helper.assertTrue(level.getBlockState(construct.get().blockPosition()).is(net.minecraft.world.level.block.Blocks.IRON_BLOCK), "the block comes back when the construct ends");
        level.setBlockAndUpdate(construct.get().blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        dismiss(helper, techno);

        // Recall Weapon: the bound weapon comes back to slot 1, in hand.
        ServerPlayer binder = classPlayer(helper, "spellbinder", new BlockPos(1, 2, 1));
        ItemStack blade = new ItemStack(Items.DIAMOND_SWORD);
        binder.getInventory().setItem(0, blade.copy());
        com.pipican.ability.SpecialItems.trackBound(binder);
        binder.getInventory().setItem(0, ItemStack.EMPTY);
        binder.getInventory().setItem(20, blade.copy());
        binder.getInventory().setSelectedSlot(3);
        com.pipican.ability.AbilityService.force(binder, shipped("spellbinder/recall_weapon"));
        helper.assertTrue(binder.getInventory().getSelectedSlot() == 0 && binder.getMainHandItem().is(Items.DIAMOND_SWORD), "Recall Weapon puts the bound weapon in your hand");
        dismiss(helper, binder);

        // Earth Glide: stone at your feet has no collision for you while it lasts.
        ServerPlayer geo = classPlayer(helper, "geomancer", new BlockPos(1, 2, 1));
        geo.setData(com.pipican.player.PipicanAttachments.MOVE_FLAGS, new com.pipican.player.PipicanAttachments.MoveFlags(List.of("phase_stone")));
        BlockPos wall = helper.absolutePos(new BlockPos(2, 2, 1));
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var shape = level.getBlockState(wall).getCollisionShape(level, wall, net.minecraft.world.phys.shapes.CollisionContext.of(geo));
        helper.assertTrue(shape.isEmpty(), "phasing players pass through stone");
        BlockPos floor = wall.below();
        helper.assertTrue(!level.getBlockState(floor).getCollisionShape(level, floor, net.minecraft.world.phys.shapes.CollisionContext.of(geo)).isEmpty()
                || !level.getBlockState(floor).is(com.pipican.ability.Phasing.PASSABLE), "the floor below holds unless you sneak");
        level.setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        dismiss(helper, geo);

        // Raise Dead: the next kill rises as a servant, then the state ends.
        ServerPlayer necro = classPlayer(helper, "necromancer", new BlockPos(1, 2, 1));
        com.pipican.ability.AbilityService.force(necro, shipped("necromancer/raise_dead"));
        killWith(helper, necro, victim(helper, 3, 3), new ItemStack(Items.IRON_SWORD));
        var servants = level.getEntitiesOfClass(Zombie.class, helper.getBounds().inflate(4), z -> com.pipican.ability.Summons.isSummonOf(z, necro));
        helper.assertTrue(servants.size() == 1, "the killed zombie rises as one servant zombie, got " + servants.size());
        helper.assertTrue(!ClassService.data(necro).abilityActive().containsKey(shipped("necromancer/raise_dead")), "Raise Dead ends after raising one");
        servants.forEach(net.minecraft.world.entity.Entity::discard);
        dismiss(helper, necro);

        // Witch Fire: a fireball appears while steering and goes out when it stops.
        ServerPlayer witch = classPlayer(helper, "witch", new BlockPos(1, 2, 1));
        var steer = new com.pipican.ability.action.SteerAction(0.6, 3, 1, false);
        steer.run(com.pipican.ability.AbilityContext.of(witch, shipped("witch/witch_fire"), 0));
        helper.assertTrue(!level.getEntitiesOfClass(com.pipican.entity.WitchFireball.class, witch.getBoundingBox().inflate(4)).isEmpty(), "Witch Fire conjures a fireball");
        new com.pipican.ability.action.SteerAction(0.6, 3, 1, true).run(com.pipican.ability.AbilityContext.of(witch, shipped("witch/witch_fire"), 0));
        helper.assertTrue(level.getEntitiesOfClass(com.pipican.entity.WitchFireball.class, witch.getBoundingBox().inflate(4), e -> e.isAlive()).isEmpty(), "...and puts it out");
        dismiss(helper, witch);

        // Consecrate Ground: the temple stands where you used it.
        ServerPlayer templar = classPlayer(helper, "templar", new BlockPos(3, 2, 3));
        com.pipican.ability.AbilityService.force(templar, shipped("templar/consecrate_ground"));
        helper.assertTrue(com.pipican.ability.Temples.center(templar).map(c -> c.distanceTo(templar.position()) < 0.1).orElse(false), "Consecrate Ground raises a temple at your feet");
        dismiss(helper, templar);

        helper.runAfterDelay(70, () -> {
            helper.assertTrue(!frozen.isNoAi() && !com.pipican.ability.Freezes.isFrozen(frozen), "frozen mobs thaw after 3s");
            helper.succeed();
        });
    }
}
