package com.pipican.gametest;

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
        double clumsyArmor = PipicanConfig.tier(Affinity.CLUMSY).armor();
        assertNear(helper, 3 + 6 * (clumsyArmor - 1), modifier(player, Attributes.ARMOR, "class/add_value"), "clumsy heavy armor");
        double clumsyMove = PipicanConfig.tier(Affinity.CLUMSY).movePenaltyPerArmorPiece();
        assertNear(helper, clumsyMove - 1, modifier(player, Attributes.MOVEMENT_SPEED, "class/add_multiplied_total"), "clumsy armor slows");
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

        ClassService.setLevel(player, 999);
        helper.assertTrue(ClassService.data(player).activeProgress().orElseThrow().level() == 10, "clamped to max_level");
        helper.succeed();
    }

    /** pipican:resource changes the bar; pipican:conditional effects switch on and off as conditions flip. */
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
            assertNear(helper, 2, eidolon.getAttribute(Attributes.ARMOR).getModifier(Pipican.id("eidolon/add_value")).amount(), "eidolon upgrade applied");
            eidolon.discard();
            helper.succeed();
        });
    }

    /** Every ability the mod ships runs without errors, with a zombie in front of the player to aim at. */
    private static void shippedAbilities(GameTestHelper helper) {
        var registry = ClassLookup.abilities(helper.getLevel().registryAccess());
        int count = 0;
        for (var entry : registry.entrySet()) {
            if (!entry.getKey().identifier().getNamespace().equals(Pipican.MOD_ID)) {
                continue;
            }
            @SuppressWarnings("removal")
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            String owner = entry.getKey().identifier().getPath().split("/")[0];
            ClassService.chooseClass(player, ResourceKey.create(PipicanRegistries.CLASS, Pipican.id(owner)), false, true);
            var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, 1, 1, 3);
            player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2, 0, 0);
            try {
                AbilityService.force(player, entry.getKey());
            } catch (RuntimeException e) {
                throw new IllegalStateException("ability " + entry.getKey().identifier() + " failed: " + e, e);
            }
            boolean deals = entry.getValue().actions().stream().anyMatch(a -> a instanceof com.pipican.ability.action.DamageAction d && d.amount() > 0);
            helper.assertTrue(!deals || zombie.getHealth() < zombie.getMaxHealth(),
                    entry.getKey().identifier() + " should damage the zombie in front of the player");
            AbilityService.reset(player);
            // Remove everything the ability spawned (eidolons, summons, decoys) so it can't get in the next one's way.
            for (var e : helper.getLevel().getEntities((net.minecraft.world.entity.Entity) null, player.getBoundingBox().inflate(48),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                e.discard();
            }
            player.stopRiding();
            count++;
        }
        helper.assertTrue(count == 107, "expected 107 shipped abilities, ran " + count);
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
            for (TreeKind kind : TreeKind.values()) {
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
}
