package com.pipican.gametest;

import com.pipican.Pipican;
import com.pipican.client.SkillTreeScreen;
import com.pipican.core.PipicanRegistries;
import com.pipican.player.ClassService;
import com.pipican.stat.StatPoints;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opt-in visual check of the level-up sheet: with {@code PIPICAN_LEVELUP_QA=1} the client makes a flat test world,
 * picks a class, opens the Stats tab with a few points planned and screenshots it at several window sizes and GUI
 * scales (into {@code screenshots/}), then quits. This source set is never shipped.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID, value = Dist.CLIENT)
public final class LevelUpScreenQa {

    /** {window width, window height, gui scale} */
    private static final List<int[]> SHOTS = List.of(
            new int[] {854, 480, 2}, new int[] {854, 480, 1}, new int[] {1280, 720, 3}, new int[] {1280, 720, 2},
            new int[] {1920, 1080, 4}, new int[] {1920, 1080, 3}, new int[] {640, 480, 2});

    private static boolean created;
    private static int menuTicks;
    private static int ticks;
    private static int shot;

    private LevelUpScreenQa() {}

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!"1".equals(System.getenv("PIPICAN_LEVELUP_QA"))) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // Any menu (first launch shows the accessibility onboarding instead of the title screen).
        if (!created && mc.level == null && mc.getOverlay() == null && mc.screen != null && ++menuTicks > 40) {
            created = true;
            LevelSettings settings = new LevelSettings("levelup_qa", GameType.SURVIVAL,
                    new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("levelup_qa_" + System.currentTimeMillis(), settings,
                    WorldOptions.testWorldWithRandomSeed(), WorldPresets::createFlatWorldDimensions, new TitleScreen());
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) {
            return;
        }
        ticks++;
        String cls = System.getenv().getOrDefault("PIPICAN_LEVELUP_QA_CLASS", "warrior");
        if (ticks == 40) {
            var server = mc.getSingleplayerServer();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                ClassService.chooseClass(player, ResourceKey.create(PipicanRegistries.CLASS, Pipican.id(cls)), true, true);
                ClassService.addXp(player, StatPoints.threshold(12));
            });
        }
        if (ticks == 100) {
            SkillTreeScreen screen = new SkillTreeScreen();
            mc.setScreen(screen);
            try {
                Class<?> tabClass = Class.forName("com.pipican.client.SkillTreeScreen$Tab");
                var stat = tabClass.getEnumConstants()[2];
                var switchTab = SkillTreeScreen.class.getDeclaredMethod("switchTab", tabClass);
                switchTab.setAccessible(true);
                switchTab.invoke(screen, stat);
                var levelUpField = SkillTreeScreen.class.getDeclaredField("levelUp");
                levelUpField.setAccessible(true);
                Object panel = levelUpField.get(screen);
                var pendingField = panel.getClass().getDeclaredField("pending");
                pendingField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<Object, Integer> pending = (Map<Object, Integer>) pendingField.get(panel);
                pending.put(ResourceKey.create(PipicanRegistries.STAT, Pipican.id("strength")), 3);
                pending.put(ResourceKey.create(PipicanRegistries.STAT, Pipican.id("vitality")), 2);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        // Every 20 ticks: resize for the next shot, then grab it 10 ticks later.
        if (ticks >= 110 && shot < SHOTS.size()) {
            int phase = (ticks - 110) % 20;
            int[] s = SHOTS.get(shot);
            if (phase == 0) {
                mc.getWindow().setWindowed(s[0], s[1]);
                mc.options.guiScale().set(s[2]);
                mc.resizeGui();
            } else if (phase == 10) {
                String name = "levelup_" + cls + "_" + s[0] + "x" + s[1] + "_gui" + s[2] + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, msg -> Pipican.LOGGER.info("QA: {}", msg.getString()));
                shot++;
            }
        }
        if (shot >= SHOTS.size() && ticks > 110 + SHOTS.size() * 20 + 20) {
            mc.stop();
        }
    }
}
