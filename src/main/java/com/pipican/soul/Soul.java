package com.pipican.soul;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pipican.Pipican;
import com.pipican.player.PipicanAttachments;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;

/**
 * Tamashigane, soul steel (phase 6; data: tools/ore_builder/soul_paths.py -> pipican/soul_paths.json). Each player may
 * take one piece from the one vein ({@link SoulSpring}), and forge it - choosing one of 15 paths, for good - into a
 * Soul Blade and soul armor bound to them. The blade absorbs the souls of creatures that die within 16 blocks (100% of
 * your own kills, 50% of others') and grows through 10 stages; stages 5 (Shikai) and 7 (Bankai) are earned by defeating
 * the blade's spirit. What each path does is in {@link SoulAbilities}; the events in {@link SoulEvents}.
 */
public final class Soul {

    public static final String OWNER = "pipican_soul_owner";
    public static final String OWNER_NAME = "pipican_soul_owner_name";
    public static final int RANGE = 16;
    public static final int MAX_STAGE = 10;

    /** Souls needed for stage 1..10. */
    public static final long[] STAGES = new long[MAX_STAGE];
    /** Souls by kind: common, strong, elite, mini_boss, player, region_boss, world_boss. */
    public static final Map<String, Integer> SOULS = new HashMap<>();
    /** Path id -> colour (RGB), in the document's order. */
    public static final Map<String, Integer> PATHS = new LinkedHashMap<>();

    static final TagKey<EntityType<?>> SOULLESS = TagKey.create(Registries.ENTITY_TYPE, Pipican.id("soul/soulless"));
    static final TagKey<EntityType<?>> MINI_BOSSES = TagKey.create(Registries.ENTITY_TYPE, Pipican.id("soul/mini_bosses"));
    static final TagKey<EntityType<?>> WORLD_BOSSES = TagKey.create(Registries.ENTITY_TYPE, Pipican.id("soul/world_bosses"));

    /** Half souls not yet worth a whole one, per player (not saved). */
    private static final Map<UUID, Double> FRACTION = new HashMap<>();

    static {
        try (InputStream in = Soul.class.getResourceAsStream("/pipican/soul_paths.json")) {
            if (in == null) {
                throw new IllegalStateException("pipican/soul_paths.json is missing - run tools/ore_builder/build_ores.py");
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            var stages = json.getAsJsonArray("stages");
            for (int i = 0; i < MAX_STAGE; i++) {
                STAGES[i] = stages.get(i).getAsLong();
            }
            json.getAsJsonObject("souls").entrySet().forEach(e -> SOULS.put(e.getKey(), e.getValue().getAsInt()));
            for (JsonElement e : json.getAsJsonArray("paths")) {
                PATHS.put(e.getAsJsonObject().get("id").getAsString(), e.getAsJsonObject().get("color").getAsInt());
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("can't read pipican/soul_paths.json", e);
        }
    }

    private Soul() {}

    // ------------------------------------------------------------------ data

    public static SoulData get(Player player) {
        return player.getData(PipicanAttachments.SOUL);
    }

    public static void set(Player player, SoulData data) {
        player.setData(PipicanAttachments.SOUL, data);
    }

    public static long now(Player player) {
        return player.level().getGameTime();
    }

    public static boolean bankai(Player player) {
        return get(player).bankaiEnd() > now(player);
    }

    /** Souls needed for {@code stage} (1-10), or Long.MAX_VALUE past the last. */
    public static long threshold(int stage) {
        return stage < 1 ? 0 : stage > MAX_STAGE ? Long.MAX_VALUE : STAGES[stage - 1];
    }

    /** The trial stage (5 or 7) the player's souls reached but has not yet won, or 0. */
    public static int pendingTrial(SoulData data) {
        int next = data.stage() + 1;
        return data.forged() && (next == 5 || next == 7) && data.souls() >= threshold(next) ? next : 0;
    }

    // ------------------------------------------------------------------ the gear

    public static boolean isGear(ItemStack stack) {
        return stack.is(SoulItems.GEAR);
    }

    public static Optional<UUID> owner(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(OWNER)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(data.copyTag().getStringOr(OWNER, "")));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static boolean owns(Player player, ItemStack stack) {
        return isGear(stack) && owner(stack).filter(player.getUUID()::equals).isPresent();
    }

    /** Holding your own Soul Blade in the main hand. */
    public static boolean holdsBlade(Player player) {
        ItemStack hand = player.getMainHandItem();
        return hand.is(SoulItems.BLADE.get()) && owns(player, hand);
    }

    /** Your own Soul Blade anywhere on you. */
    public static boolean carriesBlade(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(SoulItems.BLADE.get()) && owns(player, s)) {
                return true;
            }
        }
        return false;
    }

    /** How many pieces of your own soul armor you wear. */
    public static int piecesWorn(Player player) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (owns(player, player.getItemBySlot(slot))) {
                n++;
            }
        }
        return n;
    }

    /** Writes the owner and the blade's state onto a piece of soul gear: colour from stage 3, glint from Shikai. */
    public static void stamp(ItemStack stack, ServerPlayer player) {
        SoulData data = get(player);
        // Only touch the stack when something changed (a changed stack re-equips worn armor).
        CustomData had = stack.get(DataComponents.CUSTOM_DATA);
        var old = had == null ? null : had.copyTag();
        boolean same = old != null && old.getStringOr(OWNER, "").equals(player.getUUID().toString())
                && old.getStringOr("pipican_soul_path", "").equals(data.path()) && old.getIntOr("pipican_soul_stage", -1) == data.stage()
                && old.getLongOr("pipican_soul_souls", -1L) == data.souls();
        if (!same) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putString(OWNER, player.getUUID().toString());
                tag.putString(OWNER_NAME, player.getGameProfile().name());
                tag.putString("pipican_soul_path", data.path());
                tag.putInt("pipican_soul_stage", data.stage());
                tag.putLong("pipican_soul_souls", data.souls());
            });
        }
        int color = PATHS.getOrDefault(data.path(), 0);
        if (data.stage() >= 3 && color != 0) {
            if (!new DyedItemColor(color).equals(stack.get(DataComponents.DYED_COLOR))) {
                stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color));
            }
        } else if (stack.has(DataComponents.DYED_COLOR)) {
            stack.remove(DataComponents.DYED_COLOR);
        }
        boolean glint = data.stage() >= 8 || (data.stage() >= 5 && stack.is(SoulItems.BLADE.get())) || (data.stage() >= 7 && bankai(player));
        if (glint && !Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else if (!glint && stack.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) {
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
        // Stage 4: the blade whispers its name, if it has none yet.
        if (stack.is(SoulItems.BLADE.get()) && data.stage() >= 4 && !stack.has(DataComponents.CUSTOM_NAME)) {
            Component name = whisperedName(data.path(), player.getUUID());
            stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(Style.EMPTY.withItalic(false).withColor(color)));
            player.sendSystemMessage(Component.translatable("pipican.soul.whisper", name).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        }
    }

    private static final String[] NAME_A = {"Zan", "Hyō", "Ka", "Getsu", "Kage", "Rai", "Ten", "Kō", "Yami", "Sen", "Ketsu", "Ryū"};
    private static final String[] NAME_B = {"getsu", "rin", "maru", "giri", "kaze", "mon", "ka", "jin", "tora", "gami", "hime", "zashi"};

    /** The name the blade whispers: made from its owner's soul (the same each time). */
    static Component whisperedName(String path, UUID owner) {
        long h = owner.getMostSignificantBits() ^ owner.getLeastSignificantBits() ^ path.hashCode();
        int a = (int) Math.floorMod(h, NAME_A.length);
        int b = (int) Math.floorMod(h >> 8, NAME_B.length);
        return Component.literal(NAME_A[a] + NAME_B[b]);
    }

    // ------------------------------------------------------------------ forging

    /** Shows the 15 paths as clickable lines (using a Tamashigane ingot). */
    public static void offerPaths(ServerPlayer player) {
        if (get(player).forged()) {
            player.sendSystemMessage(Component.translatable("pipican.soul.already_forged").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("pipican.soul.choose").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (var path : PATHS.entrySet()) {
            String id = path.getKey();
            MutableComponent line = Component.literal(" - ").append(Component.translatable("pipican.soul.path." + id)
                    .withStyle(Style.EMPTY.withColor(Math.max(path.getValue(), 0x555555)).withBold(true)))
                    .append(Component.literal(": ").append(Component.translatable("pipican.soul.path." + id + ".s3")).withStyle(ChatFormatting.GRAY));
            line.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("/pipican soul forge " + id))
                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("pipican.soul.choose_hover").append("\n")
                            .append(Component.translatable("pipican.soul.path." + id + ".s5")).append("\n")
                            .append(Component.translatable("pipican.soul.path." + id + ".s7")).append("\n")
                            .append(Component.translatable("pipican.soul.path." + id + ".s10")))));
            player.sendSystemMessage(line);
        }
    }

    /** Forges the held Tamashigane ingot on {@code path}: the blade and armor, bound to the player. False if it can't. */
    public static boolean forge(ServerPlayer player, String path) {
        if (!PATHS.containsKey(path)) {
            player.sendSystemMessage(Component.translatable("pipican.soul.unknown_path").withStyle(ChatFormatting.RED));
            return false;
        }
        if (get(player).forged()) {
            player.sendSystemMessage(Component.translatable("pipican.soul.already_forged").withStyle(ChatFormatting.GRAY));
            return false;
        }
        var ingot = com.pipican.ore.PipicanOres.item("tamashigane_ingot").get();
        ItemStack held = player.getMainHandItem().is(ingot) ? player.getMainHandItem() : player.getOffhandItem().is(ingot) ? player.getOffhandItem() : ItemStack.EMPTY;
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable("pipican.soul.need_ingot").withStyle(ChatFormatting.RED));
            return false;
        }
        held.shrink(1);
        set(player, get(player).withPath(path).withMined());
        List<ItemStack> gear = new ArrayList<>();
        gear.add(new ItemStack(SoulItems.BLADE.get()));
        SoulItems.armor().forEach(a -> gear.add(new ItemStack(a.get())));
        for (ItemStack stack : gear) {
            stamp(stack, player);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        player.sendSystemMessage(Component.translatable("pipican.soul.forged", Component.translatable("pipican.soul.path." + path))
                .withStyle(ChatFormatting.GOLD));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ souls and stages

    /** What a death is worth: 0 for the soulless (golems, slimes, constructs, the blade's own spirits and summons). */
    public static int soulValue(LivingEntity e) {
        if (e instanceof Player) {
            return SOULS.getOrDefault("player", 25);
        }
        if (e.is(SOULLESS) || e instanceof AbstractGolem || e instanceof Slime || SoulMinions.isSummoned(e)) {
            return 0;
        }
        if (e.is(WORLD_BOSSES)) {
            return SOULS.getOrDefault("world_boss", 1000);
        }
        if (e instanceof com.pipican.boss.RegionalBossEntity) {
            return SOULS.getOrDefault("region_boss", 250);
        }
        if (e.is(MINI_BOSSES)) {
            return SOULS.getOrDefault("mini_boss", 50);
        }
        float hp = e.getMaxHealth();
        return hp >= 60 ? SOULS.getOrDefault("elite", 10) : hp >= 30 ? SOULS.getOrDefault("strong", 3) : SOULS.getOrDefault("common", 1);
    }

    /** A boss for abilities that would kill outright. */
    public static boolean isBoss(LivingEntity e) {
        return e.is(WORLD_BOSSES) || e.is(MINI_BOSSES) || e instanceof com.pipican.boss.RegionalBossEntity || e.getMaxHealth() >= 150;
    }

    /** Adds souls (fractions carry over), then advances stages - up to a trial, which waits for the spirit to fall. */
    public static void addSouls(ServerPlayer player, double amount) {
        SoulData data = get(player);
        if (!data.forged() || amount <= 0) {
            return;
        }
        double total = FRACTION.getOrDefault(player.getUUID(), 0.0) + amount;
        long whole = (long) Math.floor(total);
        FRACTION.put(player.getUUID(), total - whole);
        if (whole <= 0) {
            return;
        }
        set(player, data.withSouls(data.souls() + whole));
        player.sendOverlayMessage(Component.translatable("pipican.soul.souls", whole, get(player).souls()).withStyle(ChatFormatting.DARK_AQUA));
        advance(player);
    }

    /** Climbs every stage the souls allow, stopping at a trial (5, 7) not yet won. */
    public static void advance(ServerPlayer player) {
        boolean announced = false;
        while (true) {
            SoulData data = get(player);
            int next = data.stage() + 1;
            if (next > MAX_STAGE || data.souls() < threshold(next)) {
                break;
            }
            if (next == 5 || next == 7) {
                if (!announced) {
                    player.sendSystemMessage(Component.translatable("pipican.soul.trial_ready").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                break;
            }
            reach(player, next);
            announced = true;
        }
        restampAll(player);
    }

    static void reach(ServerPlayer player, int stage) {
        set(player, get(player).withStage(stage));
        player.sendSystemMessage(Component.translatable("pipican.soul.stage_up", stage, Component.translatable("pipican.soul.stage." + stage))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("pipican.soul.stage." + stage + ".desc").withStyle(ChatFormatting.GRAY));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    /** The spirit fell: the trial's stage is won (public for tests). */
    public static void winTrial(ServerPlayer player) {
        int trial = pendingTrial(get(player));
        if (trial == 0) {
            return;
        }
        player.sendSystemMessage(Component.translatable("pipican.soul.trial_won").withStyle(ChatFormatting.GOLD));
        reach(player, trial);
        advance(player);
    }

    /** Re-stamps every piece of the player's own gear (stage, colour, name). */
    public static void restampAll(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (owns(player, s)) {
                stamp(s, player);
            }
        }
    }

    // ------------------------------------------------------------------ using the blade

    /** Right-click with the blade: the ability; sneaking: the trial, Bankai, or the Absolute Form. */
    public static void use(ServerPlayer player) {
        ItemStack blade = player.getMainHandItem();
        if (!owns(player, blade)) {
            player.sendOverlayMessage(Component.translatable("pipican.soul.not_owner").withStyle(ChatFormatting.RED));
            return;
        }
        SoulData data = get(player);
        long now = now(player);
        ServerLevel level = (ServerLevel) player.level();
        if (player.isShiftKeyDown()) {
            if (pendingTrial(data) != 0) {
                SoulMinions.startTrial(player, pendingTrial(data));
                return;
            }
            if (data.stage() >= 10 && bankai(player)) {
                long day = level.getDefaultClockTime() / 24000L;
                if (data.ultimateDay() == day) {
                    player.sendOverlayMessage(Component.translatable("pipican.soul.ultimate_used").withStyle(ChatFormatting.GRAY));
                    return;
                }
                set(player, data.withUltimateDay(day));
                player.sendSystemMessage(Component.translatable("pipican.soul.ultimate").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                SoulAbilities.ultimate(SoulAbilities.ctx(player));
                return;
            }
            if (data.stage() >= 7 && !bankai(player)) {
                if (data.bankaiReady() > now) {
                    player.sendOverlayMessage(Component.translatable("pipican.soul.bankai_wait", (data.bankaiReady() - now) / 20).withStyle(ChatFormatting.GRAY));
                    return;
                }
                releaseBankai(player);
                return;
            }
        }
        if (data.stage() < 3) {
            player.sendOverlayMessage(Component.translatable("pipican.soul.locked").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (data.abilityReady() > now) {
            player.sendOverlayMessage(Component.translatable("pipican.soul.cooldown", (data.abilityReady() - now + 19) / 20).withStyle(ChatFormatting.GRAY));
            return;
        }
        var ctx = SoulAbilities.ctx(player);
        int cooldown;
        if (ctx.bankai()) {
            cooldown = SoulAbilities.bankaiAbility(ctx);
        } else if (data.stage() >= 5) {
            cooldown = SoulAbilities.shikai(ctx);
        } else {
            cooldown = SoulAbilities.whisper(ctx);
        }
        set(player, get(player).withAbilityReady(now + cooldown));
    }

    /** Releases Bankai: 60 s and 10 min to recover; from stage 8, 90 s and 5 min (public for tests). */
    public static void releaseBankai(ServerPlayer player) {
        SoulData data = get(player);
        long now = now(player);
        int seconds = data.stage() >= 8 ? 90 : 60;
        int recover = data.stage() >= 8 ? 5 * 60 : 10 * 60;
        set(player, data.withBankai(now + seconds * 20L, now + (seconds + recover) * 20L));
        player.sendSystemMessage(Component.translatable("pipican.soul.bankai").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
        SoulAbilities.bankaiStart(SoulAbilities.ctx(player));
        restampAll(player);
    }

    /** Extra melee damage of the blade in its owner's hand: +6% per stage above 1, +30% during Bankai. */
    public static float bladeBonus(ServerPlayer player, float amount) {
        SoulData data = get(player);
        float mult = 1 + 0.06f * Math.max(0, data.stage() - 1);
        if (bankai(player)) {
            mult += 0.3f;
        }
        return amount * mult;
    }

    /** {@code /pipican soul}: the player's blade in a line or two. */
    public static void info(ServerPlayer player) {
        SoulData data = get(player);
        if (!data.forged()) {
            player.sendSystemMessage(Component.translatable("pipican.soul.info_none").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("pipican.soul.info", Component.translatable("pipican.soul.path." + data.path()),
                data.stage(), Component.translatable("pipican.soul.stage." + data.stage()), data.souls()).withStyle(ChatFormatting.AQUA));
        if (data.stage() < MAX_STAGE) {
            player.sendSystemMessage(Component.translatable("pipican.soul.info_next", threshold(data.stage() + 1)).withStyle(ChatFormatting.GRAY));
        }
        if (pendingTrial(data) != 0) {
            player.sendSystemMessage(Component.translatable("pipican.soul.trial_ready").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Forgets everything (admin). The gear in the world stays bound to a soul that no longer has it. */
    public static void reset(ServerPlayer player) {
        set(player, SoulData.NONE);
        FRACTION.remove(player.getUUID());
        SoulAbilities.clear(player);
    }
}
