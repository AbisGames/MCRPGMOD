package com.pipican.ore;

import com.pipican.Pipican;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Phase 4 world mechanics of the ores:
 * <ul>
 *   <li>meteors: on overworld nights, now and then a meteor strikes near a player and leaves a crater of Meteoric Iron,
 *       with a chance of the sky metals (Raritanium, Luna Titanium, Element Zero, Kryptonite, Elerium, Nth Metal,
 *       Vibranium; rarely Uru from a dead star or Phazon)</li>
 *   <li>Quantanium is unstable: picked up raw, it explodes after 5 minutes unless refined in a blast furnace</li>
 *   <li>Warpstone mutates hostile mobs near a player carrying it (tougher, bigger, stronger); sometimes the carrier too</li>
 *   <li>boss drops: regional bosses (Adamantium, Mega Stone), the Wither (Luminite), the Ender Dragon (Draconium),
 *       the Warden (Adamantium); Valyrian Steel turns up in ancient ruins</li>
 *   <li>Zonaite fused onto a tool: +25% mining speed</li>
 * </ul>
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class OreWorldEvents {

    private static final String UNSTABLE = "pipican_unstable_since";
    /** Ticks raw Quantanium lasts in an inventory. */
    public static final long QUANTANIUM_FUSE = 20 * 60 * 5;

    private OreWorldEvents() {}

    // ------------------------------------------------------------------ meteors

    /** Sky metals a meteor can carry: (ore, chance per meteor). Meteoric Iron is always the bulk. */
    private static final List<java.util.Map.Entry<String, Float>> SKY_METALS = List.of(
            java.util.Map.entry("raritanium", 0.35f), java.util.Map.entry("luna_titanium", 0.3f), java.util.Map.entry("element_zero", 0.25f),
            java.util.Map.entry("kryptonite", 0.2f), java.util.Map.entry("elerium", 0.12f), java.util.Map.entry("nth_metal", 0.1f),
            java.util.Map.entry("vibranium", 0.06f), java.util.Map.entry("phazon", 0.03f), java.util.Map.entry("uru", 0.02f));

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // About once every few nights per player.
        if (player.tickCount % 600 == 0 && level.dimension() == Level.OVERWORLD && level.isDarkOutside() && level.getRandom().nextFloat() < 0.02f) {
            RandomSource r = level.getRandom();
            double angle = r.nextDouble() * Math.PI * 2;
            int distance = 24 + r.nextInt(40);
            BlockPos column = player.blockPosition().offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
            meteor(level, level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, column));
            level.players().forEach(p -> p.sendSystemMessage(Component.translatable("pipican.ore.meteor").withStyle(ChatFormatting.GOLD)));
        }
        if (player.tickCount % 20 == 0) {
            quantanium(player, level);
            warpstone(player, level);
        }
    }

    /** A meteor strikes {@code ground}: a blast, then a ball of meteor rock and ore under the impact (public for tests). */
    public static void meteor(ServerLevel level, BlockPos ground) {
        level.explode(null, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 4.0f, true, Level.ExplosionInteraction.TNT);
        RandomSource r = level.getRandom();
        List<String> metals = new java.util.ArrayList<>();
        SKY_METALS.forEach(m -> {
            if (r.nextFloat() < m.getValue()) {
                metals.add(m.getKey());
            }
        });
        BlockPos core = ground.below(3);
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-2, -2, -2), core.offset(2, 2, 2))) {
            if (p.distSqr(core) > 5.5 || level.getBlockState(p).getDestroySpeed(level, p) < 0) {
                continue;
            }
            Block block;
            float roll = r.nextFloat();
            if (!metals.isEmpty() && roll < 0.15f) {
                block = oreBlock(metals.get(r.nextInt(metals.size())));
            } else if (roll < 0.55f) {
                block = oreBlock("meteoric_iron");
            } else {
                block = roll < 0.8f ? Blocks.BLACKSTONE : Blocks.MAGMA_BLOCK;
            }
            level.setBlock(p, block.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static Block oreBlock(String ore) {
        for (String id : new String[] {ore + "_ore", "end_" + ore + "_ore", "nether_" + ore + "_ore"}) {
            try {
                return PipicanOres.block(id).get();
            } catch (IllegalArgumentException missing) {
                // try the next variant
            }
        }
        return PipicanOres.block("meteoric_iron_ore").get();
    }

    // ------------------------------------------------------------------ quantanium, warpstone

    /** Raw Quantanium in an inventory gets a timestamp; 5 minutes later it explodes (public for tests). */
    public static boolean quantanium(ServerPlayer player, ServerLevel level) {
        var raw = PipicanOres.item("quantanium").get();
        var inventory = player.getInventory();
        long now = level.getGameTime();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(raw)) {
                continue;
            }
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            long since = data == null ? Long.MIN_VALUE : data.copyTag().getLongOr(UNSTABLE, Long.MIN_VALUE);
            if (since == Long.MIN_VALUE) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(UNSTABLE, now));
            } else if (now - since >= QUANTANIUM_FUSE) {
                inventory.setItem(slot, ItemStack.EMPTY);
                level.explode(null, player.getX(), player.getY(), player.getZ(), 2.0f + stack.getCount() * 0.1f, false, Level.ExplosionInteraction.TNT);
                return true;
            }
        }
        return false;
    }

    private static final Identifier MUTATED = Pipican.id("warpstone_mutation");

    /** Carrying Warpstone mutates hostile mobs nearby, now and then; and sometimes the carrier. */
    private static void warpstone(ServerPlayer player, ServerLevel level) {
        if (!player.getInventory().contains(new ItemStack(PipicanOres.item("warpstone").get()))) {
            return;
        }
        RandomSource r = level.getRandom();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8), m -> m instanceof Enemy && m.isAlive())) {
            if (r.nextFloat() < 0.02f) {
                mutate(mob);
            }
        }
        if (r.nextFloat() < 0.01f) {
            player.addEffect(new MobEffectInstance(r.nextBoolean() ? MobEffects.NAUSEA : MobEffects.STRENGTH, 200, 0));
        }
    }

    /** A warp-touched mob: half again its health, a quarter bigger, stronger (once). */
    public static void mutate(LivingEntity mob) {
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || health.hasModifier(MUTATED)) {
            return;
        }
        health.addPermanentModifier(new AttributeModifier(MUTATED, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        var scale = mob.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.addPermanentModifier(new AttributeModifier(MUTATED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        mob.setHealth(mob.getMaxHealth());
        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, MobEffectInstance.INFINITE_DURATION, 0));
    }

    // ------------------------------------------------------------------ drops and loot

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel) || !(event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }
        RandomSource r = dead.getRandom();
        if (dead instanceof com.pipican.boss.RegionalBossEntity) {
            if (r.nextFloat() < 0.15f) {
                drop(event, "raw_adamantium", 2 + r.nextInt(3));
            }
            if (r.nextFloat() < 0.25f) {
                drop(event, "mega_stone", 1);
            }
        } else if (dead.getType() == EntityType.WITHER) {
            drop(event, "raw_luminite", 3 + r.nextInt(4));
            if (r.nextFloat() < 0.2f) {
                drop(event, "mega_stone", 1);
            }
        } else if (dead.getType() == EntityType.ENDER_DRAGON) {
            drop(event, "raw_draconium", 6 + r.nextInt(5));
        } else if (dead.getType() == EntityType.WARDEN) {
            if (r.nextFloat() < 0.5f) {
                drop(event, "raw_adamantium", 1 + r.nextInt(2));
            }
        }
    }

    private static void drop(LivingDropsEvent event, String item, int count) {
        LivingEntity dead = event.getEntity();
        event.getDrops().add(new ItemEntity(dead.level(), dead.getX(), dead.getY(), dead.getZ(), new ItemStack(PipicanOres.item(item).get(), count)));
    }

    /** Valyrian Steel: never forged, only found - in the chests of ancient ruins. */
    private static final java.util.Set<String> RUINS = java.util.Set.of("chests/ancient_city", "chests/bastion_treasure", "chests/end_city_treasure",
            "archaeology/trail_ruins_rare", "chests/stronghold_library", "chests/woodland_mansion");

    @SubscribeEvent
    static void onLootLoad(LootTableLoadEvent event) {
        if (!event.getName().getNamespace().equals("minecraft") || !RUINS.contains(event.getName().getPath())) {
            return;
        }
        event.getTable().addPool(LootPool.lootPool().name("pipican_valyrian_steel").setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(0.12f))
                .add(LootItem.lootTableItem(PipicanOres.item("valyrian_steel_ingot").get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))))
                .build());
    }

    // ------------------------------------------------------------------ zonaite

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (SpecialOreEvents.coating(event.getEntity().getMainHandItem()).filter("zonaite"::equals).isPresent()) {
            event.setNewSpeed(event.getNewSpeed() * 1.25f);
        }
    }
}
