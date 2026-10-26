package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.core.PipicanConfig;
import com.pipican.core.PipicanRegistries;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.PlayerClassData;
import com.pipican.rpgclass.Affinity;
import com.pipican.rpgclass.RpgClass;
import com.pipican.skill.TreeKind;
import com.pipican.stat.StatDefinition;
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
    private static final ResourceKey<StatDefinition> MIGHT = ResourceKey.create(PipicanRegistries.STAT, Identifier.fromNamespaceAndPath("pipican_test", "might"));
    private static final ResourceKey<StatDefinition> FOCUS = ResourceKey.create(PipicanRegistries.STAT, Identifier.fromNamespaceAndPath("pipican_test", "focus"));
    private static final ResourceKey<TrinketSlotType> RING = ResourceKey.create(PipicanRegistries.TRINKET_SLOT, Pipican.id("ring"));
    private static final ResourceKey<TrinketSlotType> AMULET = ResourceKey.create(PipicanRegistries.TRINKET_SLOT, Pipican.id("amulet"));

    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("choose_class", ClassSystemTests::chooseClass);
        TESTS.put("skill_tree", ClassSystemTests::skillTree);
        TESTS.put("affinity_and_damage", ClassSystemTests::affinityAndDamage);
        TESTS.put("stats_and_levels", ClassSystemTests::statsAndLevels);
        TESTS.put("trinkets", ClassSystemTests::trinkets);
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

    /** Points per level, ranks, requirements, level gates, stat effects from the special tree. */
    private static void skillTree(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        helper.assertTrue(!ClassService.unlockNode(player, TreeKind.GENERAL, "tough").success(), "no points at level 1");

        ClassService.setLevel(player, 3);
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.GENERAL) == 4, "2 general points per level-up");
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.SPECIAL) == 2, "1 special point per level-up");
        helper.assertTrue(!ClassService.unlockNode(player, TreeKind.GENERAL, "brave").success(), "requires tough first");

        helper.assertTrue(ClassService.unlockNode(player, TreeKind.GENERAL, "tough").success(), "tough rank 1");
        helper.assertTrue(ClassService.unlockNode(player, TreeKind.GENERAL, "tough").success(), "tough rank 2");
        helper.assertTrue(!ClassService.unlockNode(player, TreeKind.GENERAL, "tough").success(), "tough is max rank 2");
        assertNear(helper, 34, player.getMaxHealth(), "2 ranks of +2 health");

        helper.assertTrue(ClassService.unlockNode(player, TreeKind.GENERAL, "brave").success(), "brave");
        assertNear(helper, 1.5, ClassState.multiplier(ClassEffectApplier.state(player).damageDealt(), helper.getLevel().damageSources().generic()), "brave damage bonus");

        helper.assertTrue(ClassService.unlockNode(player, TreeKind.SPECIAL, "zeal").success(), "zeal");
        assertNear(helper, 6, ClassEffectApplier.state(player).stat(FOCUS), "zeal adds a focus point");
        assertNear(helper, 3, player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness follows focus");
        assertNear(helper, 22, ClassService.data(player).resourceMax(), "resource follows focus");

        ClassService.resetSkills(player, java.util.Optional.empty());
        assertNear(helper, 30, player.getMaxHealth(), "skill reset removes bonuses");
        helper.assertTrue(ClassService.availablePoints(player, TreeKind.GENERAL) == 4, "skill reset refunds points");
        helper.succeed();
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

    /** Exclusive stats, buying the special stat, class XP and levelling. */
    private static void statsAndLevels(GameTestHelper helper) {
        ServerPlayer player = testerPlayer(helper);
        helper.assertTrue(!ClassService.raiseStat(player, MIGHT).success(), "exclusive non-special stat can't be bought");
        helper.assertTrue(ClassService.raiseStat(player, FOCUS).success(), "special stat can be bought");
        assertNear(helper, 6, ClassEffectApplier.state(player).stat(FOCUS), "bought focus point");

        ClassService.addXp(player, PipicanConfig.xpToNextLevel(1) + 1);
        helper.assertTrue(ClassService.data(player).activeProgress().orElseThrow().level() == 2, "level up from class XP");
        helper.assertTrue(player.experienceLevel == 0, "class XP does not touch vanilla XP");

        ClassService.setLevel(player, 999);
        helper.assertTrue(ClassService.data(player).activeProgress().orElseThrow().level() == 10, "clamped to max_level");
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
