package com.pipican.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import com.pipican.ability.FavoredWeapon;
import com.pipican.ability.Auras;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
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
    /** pipican:execute, pipican:aura, low_health + shrug, start reactions, favored weapon. */
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
        helper.assertTrue(AbilityService.cooldown(player, mend) == Math.round(mend.cooldownTicks() * 0.5), "half cooldown");

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
                2, com.pipican.ability.action.DamageAction.Kind.MAGIC, 0, 0)
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

        // "Unarmed": an empty main hand counts as holding air.
        var unarmed = new com.pipican.condition.type.HoldingCondition(net.minecraft.core.HolderSet.direct(Items.AIR.builtInRegistryHolder()),
                com.pipican.condition.type.HoldingCondition.Hand.MAIN);
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
        helper.assertTrue(new VictimState(Optional.empty(), false, false, false, false, true, false, false, Optional.empty(), Optional.empty(), false, false, false).test(player, zombie), "bleeding victim state");
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
        new BoostAction(5, List.of(new AttributeEffect(Attributes.ARMOR, 3, AttributeModifier.Operation.ADD_VALUE, List.of())))
                .run(new AbilityContext(player, Optional.empty(), player.position(), Optional.empty(), 0));
        assertNear(helper, armor + 3, player.getAttributeValue(Attributes.ARMOR), "boost gives +3 armor");
        Boosts.clear(player);
        assertNear(helper, armor, player.getAttributeValue(Attributes.ARMOR), "boost removed");

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
            player.snapTo(zombie.getX(), zombie.getY(), zombie.getZ() - 2, 0, 0);
            var area = player.getBoundingBox().inflate(48);
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
}
