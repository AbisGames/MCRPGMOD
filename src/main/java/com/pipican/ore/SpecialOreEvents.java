package com.pipican.ore;

import com.pipican.Pipican;
import com.pipican.player.ClassService;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What the special-use ores do (phase 3; data from tools/ore_builder/specials.py via pipican/ores.json):
 * <ul>
 *   <li>brewing: awkward potion + ore -> potion; Amber makes any potion long, Mercury strong</li>
 *   <li>consumables: the item's effects, plus Mirestone (cures poison/wither), Lacrima (refills 30% of the class
 *       resource), Evolution Stone / Mega Stone (+5 / +10 class levels)</li>
 *   <li>coatings: at an anvil, gear + one ore -> coated gear (stored in custom data as {@code pipican_coating})</li>
 *   <li>Kryptonite in your inventory weakens bosses and large creatures within 8 blocks - and you; Octiron points to the
 *       nearest rare (T4+) ore within 48 blocks; Energon / Sakuradite heal and empower an iron golem; Verdantite works
 *       like strong bone meal</li>
 *   <li>blocks: Sunstone keeps undead from targeting players within 8 blocks; Sekkiseki stops teleports within 8;
 *       Kairoseki stops abilities within 6 ({@link #suppressesAbilities})</li>
 * </ul>
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class SpecialOreEvents {

    public static final String COATING = "pipican_coating";

    private SpecialOreEvents() {}

    // ------------------------------------------------------------------ brewing

    @SubscribeEvent
    static void onBrewing(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        for (PipicanOres.Ore ore : PipicanOres.ORES) {
            var special = PipicanOres.special(ore.id());
            if (special.has("brew")) {
                BuiltInRegistries.POTION.get(Identifier.withDefaultNamespace(special.get("brew").getAsString()))
                        .ifPresent(potion -> builder.addMix(Potions.AWKWARD, PipicanOres.item(ore.id()).get(), potion));
            }
            if (special.has("brew_extend") || special.has("amplifier_item")) {
                boolean extend = special.has("brew_extend");
                var ingredient = extend ? PipicanOres.item(ore.id()).get() : PipicanOres.item(special.get("amplifier_item").getAsString()).get();
                // Every potion with a long (Amber) or strong (Mercury) version, like redstone and glowstone.
                for (var potion : BuiltInRegistries.POTION.listElements().toList()) {
                    String name = potion.key().identifier().getPath();
                    if (name.startsWith("long_") || name.startsWith("strong_")) {
                        continue;
                    }
                    Optional<? extends net.minecraft.core.Holder<Potion>> stronger = BuiltInRegistries.POTION.get(
                            potion.key().identifier().withPath((extend ? "long_" : "strong_") + name));
                    stronger.ifPresent(to -> builder.addMix(potion, ingredient, to));
                }
            }
        }
    }

    // ------------------------------------------------------------------ consumables

    @SubscribeEvent
    static void onConsumed(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var key = BuiltInRegistries.ITEM.getKey(event.getItem().getItem());
        if (!key.getNamespace().equals(Pipican.MOD_ID)) {
            return;
        }
        var special = PipicanOres.special(key.getPath());
        if (!special.has("consume")) {
            return;
        }
        switch (special.getAsJsonObject("consume").get("action").getAsString()) {
            case "cure_poison" -> {
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
            }
            case "refill_resource" -> {
                var data = ClassService.data(player);
                if (data.resourceMax() > 0) {
                    data.setResource(Math.min(data.resourceMax(), data.resource() + data.resourceMax() * 0.3));
                    ClassService.changed(player);
                }
            }
            case "class_levels_5" -> classLevels(player, 5);
            case "class_levels_10" -> classLevels(player, 10);
            default -> { }
        }
    }

    private static void classLevels(ServerPlayer player, int levels) {
        ClassService.data(player).activeProgress().ifPresent(progress -> {
            ClassService.setLevel(player, progress.level() + levels);
            player.sendSystemMessage(Component.translatable("pipican.ore.class_levels", levels).withStyle(ChatFormatting.GOLD));
        });
    }

    // ------------------------------------------------------------------ coatings

    /** The ore whose material (its gem, or a metal's ingot) this item is. */
    public static Optional<String> materialOf(String path) {
        return PipicanOres.of(path).map(PipicanOres.Ore::id).filter(id -> path.equals(id) || path.equals(id + "_ingot"));
    }

    /** The coating on a piece of gear (an ore id), if any. */
    public static Optional<String> coating(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? Optional.empty() : data.copyTag().getString(COATING);
    }

    @SubscribeEvent
    static void onAnvil(AnvilUpdateEvent event) {
        ItemStack gear = event.getLeft();
        var key = BuiltInRegistries.ITEM.getKey(event.getRight().getItem());
        if (gear.isEmpty() || !key.getNamespace().equals(Pipican.MOD_ID) || coating(gear).isPresent()) {
            return;
        }
        Optional<String> ore = materialOf(key.getPath());
        var special = ore.map(PipicanOres::special).orElse(new com.google.gson.JsonObject());
        if (!special.has("coating")) {
            return;
        }
        boolean fits = switch (special.get("coating").getAsString()) {
            case "weapon" -> gear.is(ItemTags.SWORDS) || gear.is(ItemTags.AXES) || gear.is(ItemTags.SPEARS) || gear.is(ItemTags.MACE_ENCHANTABLE)
                    || gear.is(ItemTags.TRIDENT_ENCHANTABLE);
            case "helmet" -> gear.is(ItemTags.HEAD_ARMOR);
            case "armor" -> gear.is(ItemTags.HEAD_ARMOR) || gear.is(ItemTags.CHEST_ARMOR) || gear.is(ItemTags.LEG_ARMOR) || gear.is(ItemTags.FOOT_ARMOR);
            case "shield" -> gear.getItem() instanceof net.minecraft.world.item.ShieldItem;
            case "tool" -> gear.is(ItemTags.PICKAXES) || gear.is(ItemTags.AXES) || gear.is(ItemTags.SHOVELS) || gear.is(ItemTags.HOES);
            default -> false;
        };
        if (!fits) {
            return;
        }
        ItemStack coated = gear.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, coated, tag -> tag.putString(COATING, ore.get()));
        event.setOutput(coated);
        event.setXpCost(5);
        event.setMaterialCost(1);
    }

    @SubscribeEvent
    static void onHit(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        // Cortosis on a raised shield: half the magic damage.
        if (victim.isBlocking() && coating(victim.getUseItem()).filter("cortosis"::equals).isPresent()
                && event.getSource().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Pipican.id("is_magic")))) {
            event.setAmount(event.getAmount() * 0.5f);
        }
        if (!(event.getSource().getEntity() instanceof Player attacker) || event.getSource().getDirectEntity() != attacker) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        Optional<String> coat = coating(weapon);
        if (coat.isEmpty()) {
            return;
        }
        var rnd = attacker.getRandom();
        switch (coat.get()) {
            case "silversheen" -> {
                if (victim.is(EntityTypeTags.UNDEAD) || isSpirit(victim)) {
                    event.setAmount(event.getAmount() * 1.3f);
                }
            }
            case "dragonglass" -> {
                if (victim.is(EntityTypeTags.UNDEAD) || isIceCreature(victim)) {
                    event.setAmount(event.getAmount() * 1.5f);
                }
                if (attacker.level() instanceof ServerLevel && rnd.nextFloat() < 0.5f) {
                    weapon.hurtAndBreak(1, attacker, net.minecraft.world.entity.EquipmentSlot.MAINHAND);  // brittle
                }
            }
            case "kairoseki" -> {
                if (isSpellcaster(victim)) {
                    event.setAmount(event.getAmount() * 1.4f);
                }
                victim.getActiveEffects().stream().filter(e -> e.getEffect().value().isBeneficial()).findFirst()
                        .ifPresent(e -> victim.removeEffect(e.getEffect()));
            }
            case "thunderstone" -> {
                if (rnd.nextFloat() < 0.1f) {
                    lightning(victim, attacker);
                }
            }
            case "dust" -> {
                if (rnd.nextBoolean()) {
                    victim.igniteForSeconds(3);
                } else {
                    victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), attacker);
                }
            }
            case "prismatic_shard" -> {
                victim.igniteForSeconds(3);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0), attacker);
                if (rnd.nextFloat() < 0.05f) {
                    lightning(victim, attacker);
                }
            }
            case "materia" -> {
                if (event.getSource().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, Pipican.id("is_magic")))) {
                    event.setAmount(event.getAmount() * 1.15f);
                }
            }
            default -> { }
        }
    }

    /** Bloodstone life steal: after the damage is final. */
    @SubscribeEvent
    static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Player attacker && event.getSource().getDirectEntity() == attacker
                && coating(attacker.getMainHandItem()).filter("bloodstone"::equals).isPresent() && event.getInflictedDamage() > 0) {
            attacker.heal(event.getInflictedDamage() * 0.15f);
        }
    }

    private static void lightning(LivingEntity victim, Player attacker) {
        if (victim.level() instanceof ServerLevel level) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(victim.position());
                if (attacker instanceof ServerPlayer sp) {
                    bolt.setCause(sp);
                }
                level.addFreshEntity(bolt);
            }
        }
    }

    private static boolean isSpirit(LivingEntity e) {
        return e.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Pipican.id("mobs/spirits")));
    }

    private static boolean isIceCreature(LivingEntity e) {
        return e.getType() == EntityType.STRAY || e.getType() == EntityType.POLAR_BEAR || e.getType() == EntityType.SNOW_GOLEM;
    }

    private static boolean isSpellcaster(LivingEntity e) {
        return e instanceof Player || e.getType() == EntityType.WITCH || e.getType() == EntityType.EVOKER || e.getType() == EntityType.ILLUSIONER;
    }

    // ------------------------------------------------------------------ carried items, coated armor

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        // Kyanite-coated helmet: water breathing.
        if (coating(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)).filter("kyanite"::equals).isPresent()) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
        }
        // Kryptonite anywhere in the inventory: bosses and large creatures nearby are weakened, and so is the carrier.
        if (player.getInventory().contains(new ItemStack(PipicanOres.item("kryptonite").get()))) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, true, true));
            var strong = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Pipican.id("mobs/large"));
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8),
                    e -> e != player && e.is(strong))) {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1), player);
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0), player);
            }
        }
    }

    // ------------------------------------------------------------------ right-click uses

    @SubscribeEvent
    static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItemStack().is(PipicanOres.material("octiron").get())) {
            return;
        }
        // Octiron compass: the nearest T4+ ore within 48 blocks.
        BlockPos at = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-48, -24, -48), at.offset(48, 24, 48))) {
            var path = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock());
            if (path.getNamespace().equals(Pipican.MOD_ID) && path.getPath().endsWith("_ore")
                    && PipicanOres.of(path.getPath()).map(o -> o.tier() >= 4).orElse(false)) {
                double d = pos.distSqr(at);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = pos.immutable();
                }
            }
        }
        player.getCooldowns().addCooldown(event.getItemStack(), 100);
        if (best == null) {
            player.sendOverlayMessage(Component.translatable("pipican.ore.octiron.none"));
        } else {
            var block = player.level().getBlockState(best).getBlock().getName();
            player.sendOverlayMessage(Component.translatable("pipican.ore.octiron.found", block, (int) Math.sqrt(bestDistance),
                    direction(at, best)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        event.setCanceled(true);
    }

    private static Component direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        int dy = to.getY() - from.getY();
        String horizontal = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "east" : "west") : (dz > 0 ? "south" : "north");
        return Component.translatable("pipican.ore.direction." + horizontal).append(dy < -3 ? Component.translatable("pipican.ore.direction.below")
                : dy > 3 ? Component.translatable("pipican.ore.direction.above") : Component.empty());
    }

    @SubscribeEvent
    static void onUseOnEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof IronGolem golem) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(PipicanOres.item("energon").get()) && !stack.is(PipicanOres.item("sakuradite").get())) {
            return;
        }
        // Golem fuel: a full heal and a minute of strength and speed.
        golem.heal(golem.getMaxHealth());
        golem.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 1200, 1), player);
        golem.addEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0), player);
        stack.consume(1, player);
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getItemStack().is(PipicanOres.item("verdantite").get())) {
            return;
        }
        // Verdantite: bone meal, three times over.
        boolean grew = false;
        for (int i = 0; i < 3; i++) {
            grew |= net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, event.getPos());
        }
        if (grew) {
            level.levelEvent(1505, event.getPos(), 15);
            event.getItemStack().consume(1, event.getEntity());
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    // ------------------------------------------------------------------ special blocks

    private static boolean near(net.minecraft.world.level.Level level, BlockPos center, String ore, int radius) {
        Block block = PipicanOres.block(ore + "_block").get();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(block)) {
                return true;
            }
        }
        return false;
    }

    /** Sunstone: undead don't target players within 8 blocks of a Sunstone block. */
    @SubscribeEvent
    static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player target && event.getEntity() instanceof Mob mob && mob.is(EntityTypeTags.UNDEAD)
                && near(mob.level(), target.blockPosition(), "sunstone", 8)) {
            event.setCanceled(true);
        }
    }

    /** Sekkiseki: nothing teleports from or to within 8 blocks of a Sekkiseki block (ender pearls, chorus fruit, endermen...). */
    @SubscribeEvent
    static void onTeleport(EntityTeleportEvent event) {
        if (event instanceof EntityTeleportEvent.TeleportCommand || event instanceof EntityTeleportEvent.SpreadPlayersCommand) {
            return;
        }
        var level = event.getEntity().level();
        if (near(level, event.getEntity().blockPosition(), "sekkiseki", 8) || near(level, BlockPos.containing(event.getTarget()), "sekkiseki", 8)) {
            event.setCanceled(true);
        }
    }

    /** Kairoseki: no class abilities within 6 blocks of a Kairoseki block. */
    public static boolean suppressesAbilities(ServerPlayer player) {
        return near(player.level(), player.blockPosition(), "kairoseki", 6);
    }

    /** Coated pieces of armor a player wears (for Starmetal's special-stat bonus). */
    public static int coatedPieces(Player player, String ore) {
        int n = 0;
        for (var slot : List.of(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET)) {
            if (coating(player.getItemBySlot(slot)).filter(ore::equals).isPresent()) {
                n++;
            }
        }
        return n;
    }
}
