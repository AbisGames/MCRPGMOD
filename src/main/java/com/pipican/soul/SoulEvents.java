package com.pipican.soul;

import com.pipican.Pipican;
import com.pipican.ore.PipicanOres;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The Tamashigane events: the vein, forging, soul absorption, the blade's hits and its owner's wounds. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class SoulEvents {

    private SoulEvents() {}

    // ------------------------------------------------------------------ the vein: once per soul, then it closes again

    static boolean isVein(BlockState state) {
        return state.is(PipicanOres.block("tamashigane_ore").get()) || state.is(PipicanOres.block("deepslate_tamashigane_ore").get());
    }

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        BlockState state = event.getState();
        if (!isVein(state) || !(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var pos = event.getPos();
        if (Soul.get(player).mined()) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            player.sendSystemMessage(Component.translatable("pipican.soul.mined_once").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
        if (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos)) {
            return;   // no piece taken - and the vein comes back below
        }
        Soul.set(player, Soul.get(player).withMined());
        player.sendSystemMessage(Component.translatable("pipican.soul.mined").withStyle(ChatFormatting.DARK_PURPLE));
        // The vein re-forms right after.
        SoulMinions.later(level, 2, () -> {
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, state);
            }
        });
    }

    // ------------------------------------------------------------------ forging

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getItemStack().is(PipicanOres.item("tamashigane_ingot").get())) {
            Soul.offerPaths(player);
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ souls

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        if (dead instanceof ServerPlayer owner && Soul.get(owner).forged() && SoulAbilities.saveFromDeath(owner)) {
            event.setCanceled(true);
            return;
        }
        int value = Soul.soulValue(dead);
        var killerEntity = event.getSource().getEntity();
        ServerPlayer killer = killerEntity instanceof ServerPlayer sp ? sp : null;
        if (killer == null && killerEntity != null) {
            // A summon's kill is its owner's.
            for (ServerPlayer p : level.players()) {
                if (SoulMinions.summonOf(killerEntity, p)) {
                    killer = p;
                }
            }
        }
        if (killer != null && Soul.get(killer).forged()) {
            SoulAbilities.onKill(killer, dead);
        }
        if (value <= 0) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            if (p == dead || p.distanceTo(dead) > Soul.RANGE || !Soul.get(p).forged() || !Soul.carriesBlade(p)) {
                continue;
            }
            Soul.addSouls(p, p == killer ? value : value * 0.5);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, dead.getX(), dead.getY() + dead.getBbHeight() / 2, dead.getZ(),
                    Math.min(30, 3 + value), 0.3, 0.4, 0.3, 0.03);
        }
    }

    // ------------------------------------------------------------------ the blade's hits, its owner's wounds

    @SubscribeEvent
    static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getSource().getDirectEntity() == attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            if (weapon.is(SoulItems.BLADE.get())) {
                if (!Soul.owns(attacker, weapon)) {
                    // Bound to another soul: it won't cut for you.
                    event.setCanceled(true);
                    attacker.sendOverlayMessage(Component.translatable("pipican.soul.not_owner").withStyle(ChatFormatting.RED));
                    return;
                }
                event.setAmount(SoulAbilities.onHit(attacker, victim, event.getAmount()));
            }
        }
        if (victim instanceof ServerPlayer owner && Soul.get(owner).forged()) {
            SoulAbilities.onHurt(owner, event);
        }
    }

    // ------------------------------------------------------------------ every second

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        // Soul armor bound to another soul won't stay on you.
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = player.getItemBySlot(slot);
            if (Soul.isGear(worn) && !Soul.owns(player, worn)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(worn)) {
                    player.drop(worn, false);
                }
                player.sendOverlayMessage(Component.translatable("pipican.soul.not_owner").withStyle(ChatFormatting.RED));
            }
        }
        SoulData data = Soul.get(player);
        if (!data.forged()) {
            SoulSpring.hint(player);
            return;
        }
        Soul.restampAll(player);
        SoulAbilities.tick(player);
    }

    /** Before stage 4 the blade won't take a name. */
    @SubscribeEvent
    static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        if (!left.is(SoulItems.BLADE.get()) || event.getName() == null || event.getName().isBlank()) {
            return;
        }
        Player player = event.getPlayer();
        if (player != null && Soul.owns(player, left) && Soul.get(player).stage() < 4
                && !event.getName().equals(left.getHoverName().getString())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("pipican.soul.no_name_yet").withStyle(ChatFormatting.GRAY));
        }
    }
}
