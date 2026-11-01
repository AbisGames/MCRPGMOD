package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.entity.Eidolon;
import com.pipican.mastery.SkillAction;
import com.pipican.mastery.SkillAction.Did.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassService;
import com.pipican.player.ClassState;
import com.pipican.player.PipicanAttachments;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Game events behind the less common {@code pipican:did} actions (see {@link com.pipican.mastery.trigger.DidTrigger}):
 * safe falls, allies' kills, assists, kill streaks, the eidolon, new chunks, weapon variety, sneaking unseen, setting mobs
 * on fire, renaming, magic hits, loot chests, thrown potions and performing. {@link MasteryEvents} has the common ones.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class TriggerEvents {

    /** Falls at least this high that end without fall damage count as safe falls. */
    static final double SAFE_FALL_HEIGHT = 6;
    /** "allies killing mobs you've hit": your hit is remembered this long (the skill narrows it with max_amount). */
    private static final long ASSIST_MEMORY_TICKS = 30 * 20;
    private static final long STREAK_TICKS = 10 * 20;
    /** "sneaking past hostile mobs": mobs this close count (skills narrow it with max_amount). */
    private static final double SNEAK_RANGE = 16;

    /** Player -> (game time of the landing, height) for falls that may still turn out to hurt. */
    private static final Map<UUID, Fall> PENDING_FALL = new ConcurrentHashMap<>();

    private record Fall(ServerPlayer player, long at, double height) {}
    /** Mob -> player -> game time of that player's last hit. */
    private static final Map<UUID, Map<UUID, Long>> HITS = new ConcurrentHashMap<>();
    private static final Map<UUID, ArrayDeque<Long>> KILL_TIMES = new ConcurrentHashMap<>();
    /** Player -> (day, weapon types killed with today, item types fought with today). */
    private static final Map<UUID, Variety> VARIETY = new ConcurrentHashMap<>();
    /** Hits on mobs that weren't burning yet: checked at the end of the tick for "setting mobs on fire". */
    private static final List<Ignition> IGNITIONS = new ArrayList<>();

    private record Ignition(ServerPlayer player, LivingEntity victim) {}

    private static final class Variety {
        long day = -1;
        final Set<String> killTypes = new HashSet<>();
        final Set<String> useTypes = new HashSet<>();

        Variety on(long today) {
            if (today != day) {
                day = today;
                killTypes.clear();
                useTypes.clear();
            }
            return this;
        }
    }

    private static final String[] WEAPON_TYPES = {"swords", "axes", "maces", "spears", "tridents", "bows", "crossbows", "staff", "shields"};
    private static final Map<String, TagKey<Item>> WEAPON_TAGS = new HashMap<>();

    static {
        for (String type : WEAPON_TYPES) {
            WEAPON_TAGS.put(type, TagKey.create(net.minecraft.core.registries.Registries.ITEM, Pipican.id("weapons/" + type)));
        }
    }

    private TriggerEvents() {}

    private static void did(ServerPlayer player, Kind kind, ItemStack item, Optional<LivingEntity> target, double amount) {
        MasteryEvents.did(player, new SkillAction.Did(kind, item, Optional.empty(), target, amount));
    }

    /** The weapon type of a stack ("swords", "bows"...), or null if it isn't a weapon. */
    public static String weaponType(ItemStack stack) {
        for (String type : WEAPON_TYPES) {
            if (stack.is(WEAPON_TAGS.get(type))) {
                return type;
            }
        }
        return null;
    }

    /** The item type for "different item types in combat": the weapon type, or the item itself, or "fists". */
    static String itemType(ItemStack stack) {
        if (stack.isEmpty()) {
            return "fists";
        }
        String weapon = weaponType(stack);
        return weapon != null ? weapon : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static long day(ServerPlayer player) {
        return player.level().getOverworldClockTime() / 24000L;
    }

    // ------------------------------------------------------------------ falls

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getDistance() >= SAFE_FALL_HEIGHT) {
            PENDING_FALL.put(player.getUUID(), new Fall(player, player.level().getGameTime(), event.getDistance()));
        }
    }

    /** Fall damage was taken: that fall wasn't safe. Called by {@link MasteryEvents}. */
    static void fellHard(ServerPlayer player) {
        PENDING_FALL.remove(player.getUUID());
    }

    // ------------------------------------------------------------------ hits, kills, assists

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player && !event.getEntity().isOnFire()) {
            IGNITIONS.add(new Ignition(player, event.getEntity()));
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getInflictedDamage() <= 0) {
            return;
        }
        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer player && victim != player) {
            HITS.computeIfAbsent(victim.getUUID(), k -> new ConcurrentHashMap<>()).put(player.getUUID(), player.level().getGameTime());
            if (event.getSource().is(Tags.DamageTypes.IS_MAGIC)) {
                did(player, Kind.MAGIC_HIT, ItemStack.EMPTY, Optional.of(victim), event.getInflictedDamage());
            }
            if (!(victim instanceof Player)) {
                ItemStack weapon = event.getSource().getWeaponItem() != null ? event.getSource().getWeaponItem() : ItemStack.EMPTY;
                Variety v = VARIETY.computeIfAbsent(player.getUUID(), k -> new Variety()).on(day(player));
                v.useTypes.add(itemType(weapon));
                did(player, Kind.VARIED_USE, weapon, Optional.of(victim), v.useTypes.size());
            }
        }
        if (source instanceof Eidolon eidolon && eidolon.getOwner() instanceof ServerPlayer owner && victim != owner) {
            did(owner, Kind.EIDOLON_HIT, ItemStack.EMPTY, Optional.of(victim), event.getInflictedDamage());
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Map<UUID, Long> hits = HITS.remove(victim.getUUID());
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || victim == killer) {
            return;
        }
        long now = killer.level().getGameTime();
        // Kill streaks.
        ArrayDeque<Long> times = KILL_TIMES.computeIfAbsent(killer.getUUID(), k -> new ArrayDeque<>());
        times.addLast(now);
        while (!times.isEmpty() && now - times.peekFirst() > STREAK_TICKS) {
            times.pollFirst();
        }
        did(killer, Kind.KILL_STREAK, ItemStack.EMPTY, Optional.of(victim), times.size());
        // Weapon variety.
        ItemStack weapon = event.getSource().getWeaponItem() != null ? event.getSource().getWeaponItem() : ItemStack.EMPTY;
        String type = weaponType(weapon);
        if (type != null) {
            Variety v = VARIETY.computeIfAbsent(killer.getUUID(), k -> new Variety()).on(day(killer));
            v.killTypes.add(type);
            did(killer, Kind.VARIED_KILL, weapon, Optional.of(victim), v.killTypes.size());
        }
        // Other players: kills inside their auras, and assists on mobs they hit.
        for (ServerPlayer other : killer.level().players()) {
            if (other == killer || !other.isAlive() || other.isSpectator() || other.distanceToSqr(killer) > 128 * 128) {
                continue;
            }
            for (ClassState.Aura aura : ClassEffectApplier.state(other).auras()) {
                if (other.distanceToSqr(killer) <= aura.radius() * aura.radius()) {
                    did(other, Kind.AURA_ALLY_KILL, ItemStack.EMPTY, Optional.of(victim), Math.sqrt(other.distanceToSqr(killer)));
                    break;
                }
            }
            Long hit = hits == null ? null : hits.get(other.getUUID());
            if (hit != null && now - hit <= ASSIST_MEMORY_TICKS) {
                did(other, Kind.ASSIST, ItemStack.EMPTY, Optional.of(victim), (now - hit) / 20.0);
            }
        }
    }

    // ------------------------------------------------------------------ fire, potions

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile().getOwner() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof LivingEntity victim
                && victim != player && !victim.isOnFire()) {
            IGNITIONS.add(new Ignition(player, victim));
        }
        if (event.getProjectile() instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion potion) {
            for (LivingEntity mob : potion.level().getEntitiesOfClass(LivingEntity.class, potion.getBoundingBox().inflate(4, 2, 4),
                    e -> e != player && !(e instanceof Player) && e.isAlive() && e.distanceToSqr(potion) <= 16)) {
                did(player, Kind.POTION_HIT, potion.getItem(), Optional.of(mob), 0);
            }
        }
    }

    /** An ability set {@code victim} on fire. */
    public static void ignited(ServerPlayer player, LivingEntity victim) {
        did(player, Kind.IGNITE, player.getMainHandItem(), Optional.of(victim), 0);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!IGNITIONS.isEmpty()) {
            List<Ignition> now = new ArrayList<>(IGNITIONS);
            IGNITIONS.clear();
            Set<LivingEntity> done = new HashSet<>();
            for (Ignition i : now) {
                if (i.victim.isOnFire() && i.victim.isAlive() && done.add(i.victim)) {
                    ignited(i.player, i.victim);
                }
            }
        }
        // Falls that didn't hurt by the next tick were safe.
        for (var it = PENDING_FALL.values().iterator(); it.hasNext(); ) {
            Fall fall = it.next();
            if (fall.player.level().getGameTime() > fall.at + 1) {
                it.remove();
                if (fall.player.isAlive()) {
                    did(fall.player, Kind.SAFE_FALL, ItemStack.EMPTY, Optional.empty(), fall.height);
                }
            }
        }
        // Forget hits on mobs that were never killed.
        if (event.getServer().getTickCount() % 600 == 0) {
            long now = event.getServer().overworld().getGameTime();
            HITS.values().forEach(m -> m.values().removeIf(t -> now - t > ASSIST_MEMORY_TICKS));
            HITS.values().removeIf(Map::isEmpty);
        }
    }

    // ------------------------------------------------------------------ per player, every second

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 != 0 || player.isSpectator() || ClassService.activeClass(player).isEmpty()) {
            return;
        }
        did(player, Kind.SECOND, ItemStack.EMPTY, Optional.empty(), 0);
        // New chunks.
        long chunk = player.chunkPosition().pack() ^ ((long) player.level().dimension().identifier().hashCode() * 0x9E3779B97F4A7C15L);
        if (player.getData(PipicanAttachments.VISITED_CHUNKS).visit(chunk)) {
            did(player, Kind.NEW_CHUNK, ItemStack.EMPTY, Optional.empty(), 0);
        }
        // Sneaking near hostile mobs that haven't noticed you.
        if (player.isShiftKeyDown()) {
            List<Mob> near = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SNEAK_RANGE),
                    m -> m instanceof Enemy && m.isAlive() && m.distanceToSqr(player) <= SNEAK_RANGE * SNEAK_RANGE);
            if (!near.isEmpty() && near.stream().noneMatch(m -> m.getTarget() == player)) {
                double closest = near.stream().mapToDouble(m -> m.distanceTo(player)).min().orElse(SNEAK_RANGE);
                did(player, Kind.SNEAK_UNSEEN, ItemStack.EMPTY, near.stream().min(java.util.Comparator.comparingDouble(m -> m.distanceTo(player))).map(m -> (LivingEntity) m), closest);
            }
        }
    }

    // ------------------------------------------------------------------ items and blocks

    @SubscribeEvent
    public static void onAnvil(AnvilCraftEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var before = event.getLeft().get(DataComponents.CUSTOM_NAME);
        var after = event.getOutput().get(DataComponents.CUSTOM_NAME);
        if (after != null && !after.equals(before)) {
            did(player, Kind.RENAME, event.getOutput(), Optional.empty(), 0);
        }
    }

    /** An armor trim was applied at a smithing table (from the smithing menu mixin). */
    public static void trimmed(Player player, ItemStack result) {
        if (player instanceof ServerPlayer sp && result.has(DataComponents.TRIM)) {
            did(sp, Kind.TRIM, result, Optional.empty(), 0);
        }
    }

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        var be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null && !player.isSecondaryUseActive()) {
            did(player, Kind.LOOT_CHEST, ItemStack.EMPTY, Optional.empty(), 0);
        }
        if (state.is(Blocks.NOTE_BLOCK)) {
            did(player, Kind.PERFORM, ItemStack.EMPTY, Optional.empty(), 0);
        } else if (be instanceof JukeboxBlockEntity jukebox && jukebox.getTheItem().isEmpty() && event.getItemStack().has(DataComponents.JUKEBOX_PLAYABLE)) {
            did(player, Kind.PERFORM, event.getItemStack(), Optional.empty(), 0);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getTarget() instanceof ContainerEntity container
                && container.getContainerLootTable() != null) {
            did(player, Kind.LOOT_CHEST, ItemStack.EMPTY, Optional.empty(), 0);
        }
    }

    @SubscribeEvent
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItemStack().is(Items.GOAT_HORN)
                && !player.getCooldowns().isOnCooldown(event.getItemStack())) {
            did(player, Kind.PERFORM, event.getItemStack(), Optional.empty(), 0);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        PENDING_FALL.remove(id);
        KILL_TIMES.remove(id);
        VARIETY.remove(id);
    }

    /** Chunks a player has been in (for "discovering new chunks"). Saved; capped so it can't grow forever. */
    public static final class VisitedChunks {
        private static final int CAP = 200_000;
        public static final com.mojang.serialization.Codec<VisitedChunks> CODEC = com.mojang.serialization.Codec.LONG_STREAM.xmap(
                s -> {
                    VisitedChunks v = new VisitedChunks();
                    s.forEach(v.chunks::add);
                    return v;
                },
                v -> java.util.Arrays.stream(v.chunks.toLongArray()));

        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet chunks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

        /** True the first time a chunk is seen. */
        public boolean visit(long chunk) {
            return chunks.size() < CAP && chunks.add(chunk);
        }
    }
}
