package com.pipican.event;

import net.minecraft.world.entity.EquipmentSlot;
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
    /** Each player's worn and held stacks and their damage last tick, to see durability being used. */
    private record Worn(ItemStack stack, int damage) {}
    private static final EquipmentSlot[] GEAR = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<UUID, Worn[]> WORN = new ConcurrentHashMap<>();
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
        // drunk_potion_strength: a potion you drink gives a lower level (level II at -30% is 1.4 levels: level I).
        if (event.getEntity() instanceof ServerPlayer drinker && drinker.isUsingItem()
                && drinker.getUseItem().has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
            double k = ClassEffectApplier.state(drinker).bonus(Kind.DRUNK_POTION_STRENGTH);
            if (k != 1) {
                effect.amplifier = Math.max(0, (int) Math.round((effect.amplifier + 1) * k) - 1);
            }
        }
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
            mult *= state.effectDurations().getOrDefault(effect.getEffect(), 1.0);
            if (player.isUsingItem() && player.getUseItem().has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
                mult *= state.bonus(Kind.DRUNK_POTION_DURATION);
            }
        }
        if (mult != 1) {
            effect.duration = Math.max(1, (int) Math.round(effect.duration * mult));
        }
    }

    /** {@code drunk_potion_strength} also weakens Instant Health from a potion you drink. */
    @SubscribeEvent
    static void onDrunkHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isUsingItem()
                && player.getUseItem().has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)) {
            double k = ClassEffectApplier.state(player).bonus(Kind.DRUNK_POTION_STRENGTH);
            if (k != 1) {
                event.setAmount((float) Math.max(0, event.getAmount() * k));
            }
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        durability(player);
        if (player.isSprinting() && ClassEffectApplier.state(player).chance(Kind.NO_SPRINT) > 0) {
            player.setSprinting(false);
        }
        if (player.isBlocking() && ClassEffectApplier.state(player).chance(Kind.NO_SHIELD) > 0) {
            player.stopUsingItem();
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

    /**
     * Durability bonuses: when a worn or held item lost durability since last tick, each point lost is refunded with
     * the chance the bonus saves (or, for faster wear, extra points are taken), like Unbreaking.
     */
    public static void durability(ServerPlayer player) {
        ClassState state = ClassEffectApplier.state(player);
        Worn[] last = WORN.computeIfAbsent(player.getUUID(), k -> new Worn[GEAR.length]);
        boolean any = state.bonuses().containsKey(Kind.DURABILITY) || state.bonuses().containsKey(Kind.ARMOR_DURABILITY)
                || state.bonuses().containsKey(Kind.WEAPON_DURABILITY);
        for (int i = 0; i < GEAR.length; i++) {
            ItemStack stack = player.getItemBySlot(GEAR[i]);
            Worn before = last[i];
            if (any && !player.getAbilities().instabuild && before != null && before.stack == stack && stack.isDamageableItem()
                    && stack.getDamageValue() > before.damage) {
                double mult = state.bonus(Kind.DURABILITY);
                if (GEAR[i].getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                    mult *= state.bonus(Kind.ARMOR_DURABILITY);
                } else if (GEAR[i] == EquipmentSlot.MAINHAND) {
                    mult *= state.bonus(Kind.WEAPON_DURABILITY);
                }
                int lost = stack.getDamageValue() - before.damage;
                int change = 0;
                for (int p = 0; p < lost; p++) {
                    if (mult < 1 && player.getRandom().nextDouble() >= mult) {
                        change--;
                    }
                }
                if (mult > 1) {
                    double extra = lost * (mult - 1);
                    change += (int) extra + (player.getRandom().nextDouble() < extra - (int) extra ? 1 : 0);
                }
                if (change != 0) {
                    stack.setDamageValue(Math.clamp(stack.getDamageValue() + change, 0, stack.getMaxDamage() - 1));
                }
            }
            // Unbreakable: worn armor keeps at least armor_floor of its durability.
            double floor = state.chance(Kind.ARMOR_FLOOR);
            if (floor > 0 && GEAR[i].getType() == EquipmentSlot.Type.HUMANOID_ARMOR && stack.isDamageableItem()) {
                int most = (int) Math.floor(stack.getMaxDamage() * (1 - Math.min(floor, 1)));
                if (stack.getDamageValue() > most) {
                    stack.setDamageValue(most);
                }
            }
            last[i] = new Worn(stack, stack.getDamageValue());
        }
        // Mend: the main-hand item repairs itself.
        double repair = state.chance(Kind.WEAPON_REPAIR);
        ItemStack hand = player.getMainHandItem();
        if (repair > 0 && player.tickCount % 200 == 0 && hand.isDamageableItem() && hand.getDamageValue() > 0) {
            hand.setDamageValue(Math.max(0, hand.getDamageValue() - (int) Math.max(1, Math.round(repair))));
            last[0] = new Worn(hand, hand.getDamageValue());
        }
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

    /** Healing bonuses: everything but natural regeneration from a full food bar. */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOW)
    static void onHeal(net.neoforged.neoforge.event.entity.living.LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double mult = ClassEffectApplier.state(player).bonus(Kind.HEALING_RECEIVED);
        if (mult == 1 || naturalRegenNow(player)) {
            return;
        }
        event.setAmount((float) (event.getAmount() * mult));
    }

    /** The food tick heals once its timer is full: at 80 ticks (fed) or 10 (saturated, full food bar). */
    private static boolean naturalRegenNow(ServerPlayer player) {
        FoodData food = player.getFoodData();
        return food.getFoodLevel() >= 18 && (food.tickTimer >= 80 || food.getSaturationLevel() > 0 && food.getFoodLevel() >= 20 && food.tickTimer >= 10);
    }

    /** Goat horns and note blocks: {@code instrument} reactions. */
    @SubscribeEvent
    static void onUseItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItemStack().is(net.minecraft.world.item.Items.GOAT_HORN)
                && !player.getCooldowns().isOnCooldown(event.getItemStack())) {
            com.pipican.ability.Reactions.fire(player, null, event.getItemStack(), com.pipican.effect.type.ReactionEffect.Event.INSTRUMENT);
        }
    }

    @SubscribeEvent
    static void onUseBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.NOTE_BLOCK)) {
            com.pipican.ability.Reactions.fire(player, null, ItemStack.EMPTY, com.pipican.effect.type.ReactionEffect.Event.INSTRUMENT);
        }
    }

    @SubscribeEvent
    static void onLightning(net.neoforged.neoforge.event.entity.EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            com.pipican.ability.Reactions.fire(player, null, ItemStack.EMPTY, com.pipican.effect.type.ReactionEffect.Event.LIGHTNING);
        }
    }

    @SubscribeEvent
    static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double mult = ClassEffectApplier.state(player).bonus(Kind.DETECTION_RANGE);
            if (event.getLookingEntity() instanceof net.minecraft.world.entity.monster.illager.AbstractIllager) {
                mult *= ClassEffectApplier.state(player).bonus(Kind.ILLAGER_DETECTION);
            }
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
    static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().has(net.minecraft.core.component.DataComponents.FOOD)
                && ClassEffectApplier.state(player).chance(Kind.NO_EATING_IN_COMBAT) > 0 && com.pipican.ability.Combat.inCombat(player)) {
            event.setCanceled(true);
        }
    }

    /** The player whose kill is generating loot right now (between the death and the drops events). */
    private static @org.jspecify.annotations.Nullable ServerPlayer looter;

    private static net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity looted;

    /** Looting levels the player adds to this kill: {@code looting}, plus {@code execute_looting} for an executed mob. */
    private static double extraLooting(ServerPlayer player, net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity victim) {
        ClassState state = ClassEffectApplier.state(player);
        double extra = state.chance(Kind.LOOTING);
        if (victim != null && victim.getPersistentData().getBooleanOr(ClassEvents.EXECUTED, false)) {
            extra += state.chance(Kind.EXECUTE_LOOTING);
        }
        return extra;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    static void onDeathForLoot(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        looter = !event.isCanceled() && event.getSource().getEntity() instanceof ServerPlayer p && extraLooting(p, event.getEntity()) >= 1 ? p : null;
        looted = event.getEntity();
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    static void onDropsForLoot(LivingDropsEvent event) {
        looter = null;
        looted = null;
    }

    @SubscribeEvent
    static void onServerTickEnd(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        looter = null;
    }

    @SubscribeEvent
    static void onEnchantmentLevel(net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent event) {
        ServerPlayer player = looter;
        if (player == null || !event.isTargetting(net.minecraft.world.item.enchantment.Enchantments.LOOTING)) {
            return;
        }
        event.getHolder(net.minecraft.world.item.enchantment.Enchantments.LOOTING).ifPresent(looting -> {
            int extra = (int) Math.floor(extraLooting(player, looted) + 1e-9);
            event.getEnchantments().set(looting, event.getEnchantments().getLevel(looting) + extra);
        });
    }

    @SubscribeEvent
    static void onAnvilUpdate(net.neoforged.neoforge.event.AnvilUpdateEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.getVanillaResult().output().isEmpty()) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        int levels = (int) Math.floor(state.chance(Kind.ANVIL_LEVELS) + 1e-9);
        double mult = state.bonus(Kind.ANVIL_COST);
        int cost = event.getVanillaResult().xpCost();
        int changed = Math.max(1, (int) Math.round(cost * mult) - levels);
        if (changed != cost) {
            event.setXpCost(changed);
        }
    }

    /** Experience level at the end of the last tick, to see what an enchant cost. */
    private static final java.util.Map<java.util.UUID, Integer> LEVEL_BEFORE = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    static void onLevelSnapshot(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu) {
            LEVEL_BEFORE.put(player.getUUID(), player.experienceLevel);
        }
    }

    /** {@code bookshelf_bonus}: the table counts more bookshelves for the player using it. */
    @SubscribeEvent
    static void onEnchantLevels(net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) || event.getEnchantLevel() <= 0) {
            return;
        }
        ServerPlayer user = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(event.getPos()).inflate(8),
                p -> p.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu).stream().findFirst().orElse(null);
        if (user == null) {
            return;
        }
        double better = ClassEffectApplier.state(user).chance(Kind.BETTER_ROLL);
        if (better > 0 && user.getRandom().nextDouble() < better) {
            event.setEnchantLevel(Math.min(30, event.getEnchantLevel() + Math.max(1, (int) Math.round(event.getEnchantLevel() * 0.2))));
        }
        int extra = (int) Math.floor(ClassEffectApplier.state(user).chance(Kind.BOOKSHELF_BONUS) + 1e-9);
        int power = Math.min(15, event.getPower() + extra) - event.getPower();
        if (power <= 0) {
            return;
        }
        // Vanilla: each bookshelf adds about one level to the top option, 2/3 and 1/3 to the others; the top is at least 2x shelves.
        double[] share = {1 / 3.0, 2 / 3.0, 1};
        int row = Math.clamp(event.getEnchantRow(), 0, 2);
        int boosted = event.getEnchantLevel() + (int) Math.round(power * share[row]);
        if (row == 2) {
            boosted = Math.max(boosted, (event.getPower() + power) * 2);
        }
        event.setEnchantLevel(Math.min(30, boosted));
    }

    @SubscribeEvent
    static void onEnchant(net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        int levels = (int) Math.floor(state.chance(Kind.ENCHANT_LEVELS) + 1e-9);
        int lapis = (int) Math.floor(state.chance(Kind.ENCHANT_LAPIS) + 1e-9);
        // The levels just spent tell which option was picked (1-3; the lapis cost is the same).
        int spent = Math.max(0, LEVEL_BEFORE.getOrDefault(player.getUUID(), player.experienceLevel) - player.experienceLevel);
        if (spent == 3) {
            lapis += (int) Math.floor(state.chance(Kind.ENCHANT_LAPIS_TOP) + 1e-9);
        }
        if (spent > 0 && player.getRandom().nextDouble() < state.chance(Kind.ENCHANT_LAPIS_KEEP)) {
            lapis += spent;
        }
        if (levels != 0) {
            // Negative: enchanting costs that many more levels (never below 0).
            player.giveExperienceLevels(Math.max(levels, -player.experienceLevel));
        }
        // Persistent Enchantment: a chance of one more enchantment that fits the item.
        double extra = state.chance(Kind.EXTRA_ENCHANT);
        if (extra > 0 && player.getRandom().nextDouble() < extra) {
            ItemStack item = event.getEnchantedItem();
            var current = item.getEnchantments();
            var options = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).listElements()
                    .filter(e -> e.value().canEnchant(item) && current.getLevel(e) == 0
                            && current.keySet().stream().allMatch(have -> net.minecraft.world.item.enchantment.Enchantment.areCompatible(have, e)))
                    .toList();
            if (!options.isEmpty()) {
                var pick = options.get(player.getRandom().nextInt(options.size()));
                item.enchant(pick, 1 + player.getRandom().nextInt(pick.value().getMaxLevel()));
            }
        }
        // Fired before the lapis is taken: topping the stack up makes the enchant cost that much less.
        if (lapis > 0 && player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu menu) {
            ItemStack currency = menu.getSlot(1).getItem();
            if (!currency.isEmpty()) {
                currency.grow(lapis);
            }
        }
    }

    /** The anvil a player with {@code anvil_safe} is using, so its damage can be undone. */
    private static final Map<UUID, net.minecraft.world.level.block.state.BlockState> ANVILS = new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    static void onAnvilPre(net.neoforged.neoforge.event.entity.player.AnvilCraftEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && ClassEffectApplier.state(player).chance(Kind.ANVIL_SAFE) > 0) {
            event.getMenu().access.execute((level, pos) -> ANVILS.put(player.getUUID(), level.getBlockState(pos)));
        }
    }

    @SubscribeEvent
    static void onAnvilPost(net.neoforged.neoforge.event.entity.player.AnvilCraftEvent.Post event) {
        var before = ANVILS.remove(event.getEntity().getUUID());
        if (before != null) {
            event.getMenu().access.execute((level, pos) -> {
                if (level.getBlockState(pos) != before) {
                    level.setBlock(pos, before, 2);
                }
            });
        }
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
        // Silver Tongue: a villager you've never traded with gives the first trade at a discount.
        double first = ClassEffectApplier.state(player).chance(Kind.FIRST_TRADE_DISCOUNT);
        if (first > 0 && menu.trader instanceof Villager villager && !tradedWith(player, villager)) {
            Map<MerchantOffer, Integer> discount = new HashMap<>();
            for (MerchantOffer offer : menu.getOffers()) {
                int diff = -(int) Math.round(offer.getBaseCostA().getCount() * Math.min(1, first));
                if (diff != 0) {
                    offer.addToSpecialPriceDiff(diff);
                    discount.put(offer, diff);
                }
            }
            FIRST_TRADE.put(player.getUUID(), discount);
        }
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

    /** Discounts given for a first trade, taken back once that trade is made. */
    private static final Map<UUID, Map<MerchantOffer, Integer>> FIRST_TRADE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String TRADED = "pipican_traded_with";

    private static boolean tradedWith(ServerPlayer player, Villager villager) {
        var list = player.getPersistentData().getListOrEmpty(TRADED);
        String id = villager.getUUID().toString();
        for (int n = 0; n < list.size(); n++) {
            if (id.equals(list.getStringOr(n, ""))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    static void onTrade(net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }
        Map<MerchantOffer, Integer> discount = FIRST_TRADE.remove(player.getUUID());
        if (discount != null) {
            discount.forEach((offer, diff) -> offer.addToSpecialPriceDiff(-diff));
            if (player.containerMenu instanceof MerchantMenu menu) {
                player.sendMerchantOffers(menu.containerId, menu.getOffers(), villager.getVillagerData().level(), villager.getVillagerXp(),
                        villager.showProgressBar(), villager.canRestock());
            }
        }
        if (!tradedWith(player, villager)) {
            var list = player.getPersistentData().getListOrEmpty(TRADED);
            list.add(net.minecraft.nbt.StringTag.valueOf(villager.getUUID().toString()));
            player.getPersistentData().put(TRADED, list);
        }
    }

    @SubscribeEvent
    static void onContainerClose(PlayerContainerEvent.Close event) {
        FIRST_TRADE.remove(event.getEntity().getUUID());
        Map<MerchantOffer, Integer> changes = TRADE_CHANGES.remove(event.getEntity().getUUID());
        if (changes != null) {
            changes.forEach((offer, diff) -> offer.addToSpecialPriceDiff(-diff));
        }
    }
}
