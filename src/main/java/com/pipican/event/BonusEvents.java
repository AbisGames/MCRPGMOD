package com.pipican.event;

import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import com.pipican.Pipican;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Applies {@code pipican:bonus} kinds: XP, potion durations, hunger, natural regen, trade prices, draw/reload speed, knockback, extra drops. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class BonusEvents {

    /** Fractions of XP / use ticks carried over so small bonuses still add up. */
    private static final Map<UUID, Double> XP_REMAINDER = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> USE_REMAINDER = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> REGEN_REMAINDER = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> LAST_EXHAUSTION = new ConcurrentHashMap<>();
    /** Price changes made to traders that don't reset their own prices (wandering traders), undone on close. */
    private static final Map<UUID, Map<MerchantOffer, Integer>> TRADE_CHANGES = new ConcurrentHashMap<>();

    private BonusEvents() {}

    @SubscribeEvent
    static void onXp(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0 || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double mult = ClassEffectApplier.state(player).bonus(Kind.EXPERIENCE);
        if (mult == 1) {
            return;
        }
        double total = event.getAmount() * mult + XP_REMAINDER.getOrDefault(player.getUUID(), 0.0);
        int whole = (int) Math.floor(total);
        XP_REMAINDER.put(player.getUUID(), total - whole);
        event.setAmount(whole);
    }

    @SubscribeEvent
    static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect.isInfiniteDuration() || effect.getEffect().value().isInstantenous() || effect.duration <= 0) {
            return;
        }
        double mult = 1;
        if (event.getEffectSource() instanceof ServerPlayer source && source != event.getEntity()) {
            ClassState state = ClassEffectApplier.state(source);
            mult *= state.bonus(Kind.APPLIED_DURATION);
            if (!effect.getEffect().value().isBeneficial()) {
                mult *= state.bonus(Kind.APPLIED_HARMFUL_DURATION);
            }
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            ClassState state = ClassEffectApplier.state(player);
            mult *= state.bonus(Kind.POTION_DURATION)
                    * state.bonus(effect.getEffect().value().isBeneficial() ? Kind.BENEFICIAL_DURATION : Kind.HARMFUL_DURATION);
        }
        if (mult != 1) {
            effect.duration = Math.max(1, (int) Math.round(effect.duration * mult));
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodData food = player.getFoodData();
        naturalRegen(player, food);
        float now = food.exhaustionLevel;
        Float last = LAST_EXHAUSTION.put(player.getUUID(), now);
        double mult = ClassEffectApplier.state(player).bonus(Kind.HUNGER);
        if (last == null || mult == 1 || player.getAbilities().invulnerable) {
            return;
        }
        // Every 4 exhaustion the food tick takes 4 off, so a drop means it wrapped.
        float gained = now >= last ? now - last : now + 4 - last;
        if (gained <= 0 || gained > 4) {
            return;
        }
        float adjusted = Math.max(0, now + (float) (gained * (mult - 1)));
        food.exhaustionLevel = adjusted;
        LAST_EXHAUSTION.put(player.getUUID(), adjusted);
    }

    /** Natural regen heals when the food tick timer reaches its threshold: move the timer faster or slower. */
    private static void naturalRegen(ServerPlayer player, FoodData food) {
        double extra = ClassEffectApplier.state(player).bonus(Kind.NATURAL_REGEN) - 1;
        if (extra == 0 || food.getFoodLevel() < 18 || player.getHealth() >= player.getMaxHealth()) {
            return;
        }
        double total = extra + REGEN_REMAINDER.getOrDefault(player.getUUID(), 0.0);
        int whole = (int) total;
        REGEN_REMAINDER.put(player.getUUID(), total - whole);
        if (whole != 0) {
            food.tickTimer = Math.max(0, food.tickTimer + whole);
        }
    }

    @SubscribeEvent
    static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double mult = ClassEffectApplier.state(player).bonus(Kind.DETECTION_RANGE);
            if (mult != 1) {
                event.modifyVisibility(mult);
            }
        }
    }

    /** Knockback from a player's hit (the mob was hurt by them this tick). */
    @SubscribeEvent
    static void onKnockback(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.getLastHurtByMob() instanceof ServerPlayer player && target.getLastHurtByMobTimestamp() == target.tickCount) {
            double mult = ClassEffectApplier.state(player).bonus(Kind.KNOCKBACK);
            if (mult != 1) {
                event.setStrength((float) (event.getStrength() * mult));
            }
        }
    }

    /** Extra mob loot: a chance to drop one more of a random item. */
    @SubscribeEvent
    static void onMobDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getDrops().isEmpty()) {
            return;
        }
        if (player.getRandom().nextDouble() < ClassEffectApplier.state(player).chance(Kind.EXTRA_LOOT)) {
            ItemEntity pick = event.getDrops().stream().toList().get(player.getRandom().nextInt(event.getDrops().size()));
            ItemStack one = pick.getItem().copyWithCount(1);
            event.getDrops().add(new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), one));
        }
    }

    /** Extra ore / crop drops: a chance that everything the block dropped drops twice. */
    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || event.getDrops().isEmpty()) {
            return;
        }
        Kind kind = event.getState().is(Tags.Blocks.ORES) ? Kind.EXTRA_ORE : event.getState().is(BlockTags.CROPS) ? Kind.EXTRA_CROP : null;
        if (kind == null || player.getRandom().nextDouble() >= ClassEffectApplier.state(player).chance(kind)) {
            return;
        }
        // Mature crops only: a planted seed shouldn't come back doubled.
        if (kind == Kind.EXTRA_CROP && event.getState().getBlock() instanceof CropBlock crop && !crop.isMaxAge(event.getState())) {
            return;
        }
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            extra.add(new ItemEntity(drop.level(), drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
        }
        event.getDrops().addAll(extra);
    }

    @SubscribeEvent
    static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack item = event.getItem();
        Kind kind = item.getItem() instanceof CrossbowItem ? Kind.RELOAD_SPEED
                : item.getItem() instanceof BowItem || item.getItem() instanceof TridentItem ? Kind.DRAW_SPEED : null;
        if (kind == null) {
            return;
        }
        double extra = ClassEffectApplier.state(player).bonus(kind) - 1;
        if (extra == 0) {
            return;
        }
        // Faster: skip extra use ticks; slower: give some back.
        double total = extra + USE_REMAINDER.getOrDefault(player.getUUID(), 0.0);
        int whole = (int) total;
        USE_REMAINDER.put(player.getUUID(), total - whole);
        if (whole != 0) {
            event.setDuration(Math.max(1, event.getDuration() - whole));
        }
    }

    @SubscribeEvent
    static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof MerchantMenu menu) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double change = ClassEffectApplier.state(player).bonus(Kind.TRADE_PRICE) - 1;
        if (change == 0) {
            return;
        }
        Map<MerchantOffer, Integer> changes = new HashMap<>();
        for (MerchantOffer offer : menu.getOffers()) {
            int diff = (int) Math.round(offer.getBaseCostA().getCount() * change);
            if (diff != 0) {
                offer.addToSpecialPriceDiff(diff);
                changes.put(offer, diff);
            }
        }
        // Villagers reset their special prices when trading stops; other traders keep them, so undo ours on close.
        if (!(menu.trader instanceof Villager)) {
            TRADE_CHANGES.put(player.getUUID(), changes);
        }
    }

    @SubscribeEvent
    static void onContainerClose(PlayerContainerEvent.Close event) {
        Map<MerchantOffer, Integer> changes = TRADE_CHANGES.remove(event.getEntity().getUUID());
        if (changes != null) {
            changes.forEach((offer, diff) -> offer.addToSpecialPriceDiff(-diff));
        }
    }
}
