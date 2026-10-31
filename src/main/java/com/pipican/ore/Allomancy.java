package com.pipican.ore;

import com.pipican.Pipican;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Mistborn Allomancy (phase 5): drinking a vial burns its metal for a while (data: tools/ore_builder/mistborn.py).
 * A player burns one metal at a time - a new vial replaces the old burn - unless they ate Lerasium and are Mistborn,
 * who burn any number at once. Aluminum wipes every burn and effect; Duralumin flares everything burning at triple
 * strength for 5 seconds and then burns it out. Harmonium explodes on contact with water.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class Allomancy {

    private static final String MISTBORN = "pipican_mistborn";
    private static final TagKey<Item> METAL_ITEMS = TagKey.create(Registries.ITEM, Pipican.id("allomancy/metal_items"));
    /** Player -> metal -> game time its burn ends. */
    private static final Map<UUID, Map<String, Long>> BURNING = new ConcurrentHashMap<>();
    /** Player -> game time a Duralumin flare ends. */
    private static final Map<UUID, Long> FLARE = new ConcurrentHashMap<>();
    /** Mob -> the Malatium burner who read its past. */
    private static final Map<UUID, UUID> READ = new ConcurrentHashMap<>();

    private Allomancy() {}

    public static boolean isMistborn(Player player) {
        return player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG).getBooleanOr(MISTBORN, false);
    }

    public static void becomeMistborn(ServerPlayer player) {
        var persisted = player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(MISTBORN, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        player.sendSystemMessage(Component.translatable("pipican.allomancy.mistborn").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    public static boolean burning(Player player, String metal) {
        Long end = BURNING.getOrDefault(player.getUUID(), Map.of()).get(metal);
        return end != null && end > player.level().getGameTime();
    }

    /** How strongly a metal burns right now: 3 during a Duralumin flare, else 1. */
    private static int strength(Player player) {
        Long flare = FLARE.get(player.getUUID());
        return flare != null && flare > player.level().getGameTime() ? 3 : 1;
    }

    /** Drinking a vial of {@code metal} (public for tests). */
    public static void drink(ServerPlayer player, String metal, int seconds) {
        long now = player.level().getGameTime();
        Map<String, Long> burns = BURNING.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        switch (metal) {
            case "aluminum" -> {
                // Wipes every burn and every effect.
                burns.clear();
                FLARE.remove(player.getUUID());
                player.removeAllEffects();
                player.sendOverlayMessage(Component.translatable("pipican.allomancy.aluminum"));
                return;
            }
            case "duralumin" -> {
                // Everything burning flares at triple strength for 5 seconds, then burns out.
                if (burns.isEmpty()) {
                    return;
                }
                FLARE.put(player.getUUID(), now + 100);
                burns.replaceAll((m, end) -> Math.min(end, now + 100));
                player.sendOverlayMessage(Component.translatable("pipican.allomancy.duralumin").withStyle(ChatFormatting.RED));
                return;
            }
            default -> { }
        }
        if (!isMistborn(player)) {
            burns.keySet().removeIf(m -> !m.equals(metal));   // a Misting burns one metal at a time
        }
        burns.merge(metal, now + seconds * 20L, (a, b) -> Math.max(a, now) + seconds * 20L);
    }

    // ------------------------------------------------------------------ every second

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Map<String, Long> burns = BURNING.get(player.getUUID());
        if (burns == null || burns.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        burns.values().removeIf(end -> end <= now);
        // Iron pulls every few ticks so it feels like a pull; everything else once a second.
        if (burns.containsKey("iron")) {
            ironPull(player, level);
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        int power = strength(player);
        for (String metal : burns.keySet()) {
            burn(player, level, metal, power);
        }
        Map<String, Long> shown = new LinkedHashMap<>(burns);
        if (!shown.isEmpty()) {
            StringBuilder text = new StringBuilder();
            shown.forEach((m, end) -> text.append(text.isEmpty() ? "" : ", ").append(Component.translatable("item.pipican." + m + "_flakes").getString()
                    .replace(" Flakes", "").replace(" Pulu", "")).append(' ').append((end - now) / 20).append('s'));
            player.sendOverlayMessage(Component.translatable("pipican.allomancy.burning", text.toString()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static void burn(ServerPlayer player, ServerLevel level, String metal, int power) {
        int amp = power - 1;
        switch (metal) {
            case "tin" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false));
                for (LivingEntity e : near(player, 24 * power, LivingEntity.class)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
                }
            }
            case "pewter" -> {
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, amp, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, amp, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, 30, 0, true, false));
            }
            case "zinc" -> {
                var mobs = near(player, 12 * power, Mob.class).stream().filter(m -> m instanceof Enemy).toList();
                for (Mob mob : mobs) {
                    mobs.stream().filter(o -> o != mob).min(java.util.Comparator.comparingDouble(o -> o.distanceToSqr(mob))).ifPresent(mob::setTarget);
                }
            }
            case "brass" -> near(player, 12 * power, Mob.class).forEach(mob -> {
                if (mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            });
            case "cadmium" -> near(player, 6 * power, LivingEntity.class).forEach(e -> {
                if (!(e instanceof Player p && p == player)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 2, true, false));
                    e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30, 1, true, false));
                }
            });
            case "bendalloy" -> {
                for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(4 * power))) {
                    p.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, 1, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.HASTE, 30, 1, true, false));
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, amp, true, false));
                }
            }
            case "gold" -> player.getLastDeathLocation().ifPresentOrElse(death -> {
                BlockPos pos = death.pos();
                player.sendSystemMessage(Component.translatable("pipican.allomancy.gold", pos.getX(), pos.getY(), pos.getZ(),
                        death.dimension().identifier().toString()), true);
            }, () -> player.sendSystemMessage(Component.translatable("pipican.allomancy.gold.none"), true));
            case "bronze" -> {
                int burners = 0;
                for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(48))) {
                    if (other != player && !burning(other, "copper") && !BURNING.getOrDefault(other.getUUID(), Map.of()).isEmpty()) {
                        burners++;
                    }
                }
                if (burners > 0) {
                    player.sendSystemMessage(Component.translatable("pipican.allomancy.bronze", burners).withStyle(ChatFormatting.GOLD), true);
                }
            }
            default -> { }
        }
    }

    private static <T extends LivingEntity> java.util.List<T> near(ServerPlayer player, double radius, Class<T> type) {
        return player.level().getEntitiesOfClass(type, player.getBoundingBox().inflate(radius), e -> e != player && e.isAlive());
    }

    /** Iron: metal items and creatures wearing or holding metal drift toward you. */
    private static void ironPull(ServerPlayer player, ServerLevel level) {
        double r = 12 * strength(player);
        Vec3 to = player.position().add(0, 1, 0);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(r), i -> i.getItem().is(METAL_ITEMS))) {
            item.setDeltaMovement(item.getDeltaMovement().add(to.subtract(item.position()).normalize().scale(0.08)));
        }
        if (player.tickCount % 10 == 0) {
            for (Mob mob : near(player, r / 2, Mob.class)) {
                if (wearsMetal(mob)) {
                    mob.setDeltaMovement(mob.getDeltaMovement().add(to.subtract(mob.position()).normalize().scale(0.25)));
                    mob.hurtMarked = true;
                }
            }
        }
    }

    private static boolean wearsMetal(LivingEntity e) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (e.getItemBySlot(slot).is(METAL_ITEMS)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ combat

    @SubscribeEvent
    static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // Electrum and Atium: glimpses of the future - dodge.
        if (victim instanceof Player player && event.getSource().getEntity() != null) {
            float dodge = burning(player, "atium") ? 0.85f : burning(player, "electrum") ? 0.25f * strength(player) : 0;
            if (dodge > 0 && player.getRandom().nextFloat() < Math.min(0.95f, dodge)) {
                event.setCanceled(true);
                return;
            }
        }
        if (!(event.getSource().getEntity() instanceof Player attacker)) {
            return;
        }
        // Steel: your arrows fly harder.
        if (event.getSource().getDirectEntity() instanceof AbstractArrow && burning(attacker, "steel")) {
            event.setAmount(event.getAmount() * (1 + 0.3f * strength(attacker)));
        }
        if (event.getSource().getDirectEntity() != attacker) {
            return;
        }
        // Chromium: the touch wipes the target.
        if (burning(attacker, "chromium")) {
            victim.getActiveEffects().stream().filter(e -> e.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect).toList()
                    .forEach(victim::removeEffect);
            if (victim instanceof Player p) {
                BURNING.remove(p.getUUID());
            }
        }
        // Malatium: read the target's past - you hit it 20% harder from then on.
        if (burning(attacker, "malatium") && READ.put(victim.getUUID(), attacker.getUUID()) == null && attacker instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("pipican.allomancy.malatium", victim.getName(),
                    (int) victim.getHealth(), (int) victim.getMaxHealth(), (int) victim.getArmorValue()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        if (attacker.getUUID().equals(READ.get(victim.getUUID()))) {
            event.setAmount(event.getAmount() * 1.2f);
        }
    }

    /** Brass soothes, Trellium hides: creatures can't pick you as a target. */
    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) {
            return;
        }
        if (burning(player, "brass") && event.getEntity().distanceToSqr(player) <= 144
                || burning(player, "trellium") && event.getEntity().distanceToSqr(player) > 16
                || burning(player, "copper") && event.getEntity().distanceToSqr(player) > 64) {
            event.setCanceled(true);
        }
    }

    /** Nicrosil: touch an ally to flare them - Strength and Speed, and their burns last 30 seconds longer. */
    @SubscribeEvent
    static void onTouch(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof ServerPlayer ally) || !burning(player, "nicrosil")) {
            return;
        }
        ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0));
        ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 0));
        BURNING.getOrDefault(ally.getUUID(), new HashMap<>()).replaceAll((m, end) -> end + 600);
        event.setCanceled(true);
    }

    /** Harmonium (raw or refined) explodes when it touches water. */
    @SubscribeEvent
    static void onItemTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity item && item.isInWater() && item.level() instanceof ServerLevel level) {
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem().getItem());
            if (key.getNamespace().equals(Pipican.MOD_ID) && key.getPath().contains("harmonium")) {
                item.discard();
                level.explode(null, item.getX(), item.getY(), item.getZ(), 3.0f + item.getItem().getCount() * 0.1f, Level.ExplosionInteraction.TNT);
            }
        }
    }
}
