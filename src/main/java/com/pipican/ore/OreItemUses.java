package com.pipican.ore;

import com.pipican.Pipican;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Right-click uses of the phase 4 ores:
 * <ul>
 *   <li>Element Zero: a gravity well 8 blocks ahead - creatures within 6 blocks are pulled in and lifted (used up)</li>
 *   <li>Dilithium: fast travel to your respawn point, or the world spawn (used up)</li>
 *   <li>SAM: opens your ender chest wherever you are</li>
 *   <li>Philosopher's Stone: transmutes the ore material in your off hand into one a tier higher, at the cost of 2 max
 *       health, for good (the stone stays)</li>
 *   <li>Chaos Emeralds: with all seven, a 30-second super form - flight, Strength III, Resistance III, Speed II
 *       (10-minute cooldown)</li>
 * </ul>
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class OreItemUses {

    private static final Identifier PHILOSOPHER = Pipican.id("philosophers_price");
    private static final String PRICE = "pipican_philosophers_price";
    private static final Map<UUID, Long> SUPER_FORM = new ConcurrentHashMap<>();

    private OreItemUses() {}

    private static boolean is(ItemStack stack, String material) {
        return stack.is(PipicanOres.material(material).get());
    }

    @SubscribeEvent
    static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean used;
        if (is(stack, "element_zero")) {
            used = gravityWell(player, level);
            if (used) {
                stack.consume(1, player);
            }
        } else if (is(stack, "dilithium")) {
            used = recall(player, level);
            if (used) {
                stack.consume(1, player);
            }
        } else if (is(stack, "sam")) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, p.getEnderChestInventory()),
                    Component.translatable("container.enderchest")));
            used = true;
        } else if (is(stack, "philosophers_stone")) {
            used = transmute(player, level) != null;
        } else if (is(stack, "chaos_emerald")) {
            used = superForm(player);
        } else {
            return;
        }
        if (used) {
            player.getCooldowns().addCooldown(stack, is(stack, "chaos_emerald") ? 20 * 600 : 40);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean gravityWell(ServerPlayer player, ServerLevel level) {
        var hit = player.pick(8, 1, false);
        Vec3 center = hit.getLocation();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(6),
                e -> e != player && e.isAlive())) {
            Vec3 pull = center.subtract(e.position());
            e.setDeltaMovement(pull.normalize().scale(Math.min(1.2, pull.length() * 0.35)).add(0, 0.3, 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0), player);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 60, 2, 2, 2, 0.1);
        return true;
    }

    private static boolean recall(ServerPlayer player, ServerLevel level) {
        var config = player.getRespawnConfig();
        ServerLevel target = config != null ? level.getServer().getLevel(config.respawnData().dimension()) : null;
        net.minecraft.core.BlockPos pos;
        if (target != null) {
            pos = config.respawnData().pos();
        } else {
            target = level.getServer().overworld();
            pos = target.getRespawnData().pos();
        }
        if (target == null) {
            return false;
        }
        player.teleportTo(target, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        player.sendOverlayMessage(Component.translatable("pipican.ore.dilithium").withStyle(ChatFormatting.AQUA));
        return true;
    }

    /** Transmutes the off-hand ore material one tier up; returns the new item, or null if there's nothing to change (public for tests). */
    public static Item transmute(ServerPlayer player, ServerLevel level) {
        ItemStack held = player.getOffhandItem();
        var key = BuiltInRegistries.ITEM.getKey(held.getItem());
        var ore = SpecialOreEvents.materialOf(key.getPath()).flatMap(PipicanOres::byId);
        if (held.isEmpty() || !key.getNamespace().equals(Pipican.MOD_ID) || ore.isEmpty() || ore.get().tier() >= 7) {
            player.sendOverlayMessage(Component.translatable("pipican.ore.philosopher.nothing"));
            return null;
        }
        List<PipicanOres.Ore> higher = PipicanOres.ORES.stream()
                .filter(o -> o.tier() == ore.get().tier() + 1 && !o.use().equals("limited") && !o.use().equals("relic") && !o.use().equals("alloy"))
                .toList();
        if (higher.isEmpty()) {
            return null;
        }
        Item result = PipicanOres.material(higher.get(level.getRandom().nextInt(higher.size())).id()).get();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(result, held.getCount()));
        // The price: 2 max health, for good (kept through death).
        var persisted = player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        int price = persisted.getIntOr(PRICE, 0) + 1;
        persisted.putInt(PRICE, price);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        applyPrice(player);
        player.sendOverlayMessage(Component.translatable("pipican.ore.philosopher.done", price * 2).withStyle(ChatFormatting.DARK_RED));
        return result;
    }

    private static void applyPrice(ServerPlayer player) {
        int price = player.getPersistentData().getCompoundOrEmpty(Player.PERSISTED_NBT_TAG).getIntOr(PRICE, 0);
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        var current = health.getModifier(PHILOSOPHER);
        if (price == 0 ? current == null : current != null && current.amount() == -2.0 * price) {
            return;
        }
        health.removeModifier(PHILOSOPHER);
        if (price > 0) {
            health.addPermanentModifier(new AttributeModifier(PHILOSOPHER, -2.0 * price, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static boolean superForm(ServerPlayer player) {
        int emeralds = 0;
        for (ItemStack s : player.getInventory()) {
            if (is(s, "chaos_emerald")) {
                emeralds += s.getCount();
            }
        }
        if (emeralds < 7) {
            player.sendOverlayMessage(Component.translatable("pipican.ore.chaos.need", emeralds));
            return false;
        }
        SUPER_FORM.put(player.getUUID(), player.level().getGameTime() + 20 * 30);
        for (var effect : List.of(MobEffects.STRENGTH, MobEffects.RESISTANCE)) {
            player.addEffect(new MobEffectInstance(effect, 20 * 30, 2));
        }
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 30, 1));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 30, 0));
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        player.sendSystemMessage(Component.translatable("pipican.ore.chaos.super").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        return true;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        applyPrice(player);
        Long end = SUPER_FORM.get(player.getUUID());
        if (end != null && player.level().getGameTime() >= end) {
            SUPER_FORM.remove(player.getUUID());
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }
    }
}
