package com.pipican.event;

import com.pipican.Pipican;
import com.pipican.ability.Marks;
import com.pipican.effect.type.BonusEffect.Kind;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Crafting, brewing, farming, loot and shield bonus kinds that need their own game hooks. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class CraftAndFarmEvents {

    /** Shields already on cooldown last tick, so a new disable is noticed once. */
    private static final Map<UUID, Boolean> SHIELD_WAS_DOWN = new ConcurrentHashMap<>();

    private CraftAndFarmEvents() {}

    @SubscribeEvent
    static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getCrafting().isEmpty()) {
            return;
        }
        for (ClassState.CraftBonus bonus : ClassEffectApplier.state(player).craftBonuses()) {
            if (event.getCrafting().is(bonus.items()) && player.getRandom().nextDouble() < bonus.chance()) {
                ItemStack extra = event.getCrafting().copy();
                if (!player.getInventory().add(extra)) {
                    player.drop(extra, false);
                }
            }
        }
    }

    @SubscribeEvent
    static void onCraftedDurability(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().isDamageableItem()) {
            double more = ClassEffectApplier.state(player).chance(Kind.CRAFTED_DURABILITY);
            if (more > 0) {
                ItemStack stack = event.getCrafting();
                stack.set(DataComponents.MAX_DAMAGE, (int) Math.round(stack.getMaxDamage() * (1 + more)));
            }
        }
    }

    /** Your summons' and tamed animals' hits: pipican:on "pet_hit" reactions of the owner. */
    @SubscribeEvent
    static void onPetHit(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        var attacker = event.getSource().getEntity();
        if (attacker == null || event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        ServerPlayer owner = com.pipican.ability.Pets.owner(attacker);
        if (owner == null) {
            owner = com.pipican.ability.Summons.ownerOf(attacker).orElse(null);
        }
        if (owner != null && owner != attacker) {
            com.pipican.ability.Reactions.fire(owner, event.getEntity(), ItemStack.EMPTY, com.pipican.effect.type.ReactionEffect.Event.PET_HIT);
        }
    }

    @SubscribeEvent
    static void onPhantoms(net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && ClassEffectApplier.state(player).chance(Kind.NO_PHANTOMS) > 0) {
            event.setPhantomsToSpawn(0);
        }
    }

    @SubscribeEvent
    static void onBrewed(PlayerBrewedPotionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        double mult = ClassEffectApplier.state(player).bonus(Kind.BREWED_DURATION);
        ItemStack stack = event.getStack();
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (mult == 1 || contents == null || contents.potion().isEmpty()) {
            return;
        }
        // The potion's own effects become custom ones with longer times; its name and colour stay.
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance e : contents.getAllEffects()) {
            int duration = e.getEffect().value().isInstantenous() ? e.getDuration() : (int) Math.round(e.getDuration() * mult);
            effects.add(new MobEffectInstance(e.getEffect(), duration, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
        }
        int color = contents.getColor();
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(java.util.Optional.empty(), java.util.Optional.of(color), effects,
                java.util.Optional.of(contents.potion().get().value().name())));
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getDrops().isEmpty()
                || !Marks.isMarkedBy(event.getEntity(), player)) {
            return;
        }
        int extra = (int) Math.floor(ClassEffectApplier.state(player).chance(Kind.MARKED_LOOT) + 1e-9);
        List<ItemEntity> drops = new ArrayList<>(event.getDrops());
        for (int n = 0; n < extra; n++) {
            ItemEntity pick = drops.get(player.getRandom().nextInt(drops.size()));
            ItemStack copy = pick.getItem().copyWithCount(1);
            event.getDrops().add(new ItemEntity(pick.level(), pick.getX(), pick.getY(), pick.getZ(), copy));
        }
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ClassState state = ClassEffectApplier.state(player);
        // Sun burn: like a zombie, in daylight under the open sky without a helmet.
        if (player.tickCount % 20 == 0 && state.chance(Kind.SUN_BURN) > 0 && player.level().isBrightOutside() && !player.isInWaterOrRain()
                && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()
                && player.level().canSeeSky(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))) {
            player.igniteForSeconds(8);
        }
        // Shields: replace the disable cooldown the moment it appears.
        double disable = state.bonus(Kind.SHIELD_DISABLE);
        if (disable != 1) {
            ItemStack shield = new ItemStack(Items.SHIELD);
            boolean down = player.getCooldowns().isOnCooldown(shield);
            if (down && !SHIELD_WAS_DOWN.getOrDefault(player.getUUID(), false)) {
                var group = player.getCooldowns().getCooldownGroup(shield);
                player.getCooldowns().removeCooldown(group);
                int ticks = (int) Math.round(100 * disable);
                if (ticks > 0) {
                    player.getCooldowns().addCooldown(group, ticks);
                }
                down = ticks > 0;
            }
            SHIELD_WAS_DOWN.put(player.getUUID(), down);
        }
        // Crops near you grow faster: extra random ticks at the vanilla rate times the bonus (once a second).
        double growth = state.chance(Kind.CROP_GROWTH);
        if (growth > 0 && player.tickCount % 20 == 0 && player.level() instanceof ServerLevel level) {
            double perSecond = 20 * 3 / 4096.0 * growth;
            BlockPos center = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-6, -2, -6), center.offset(6, 2, 6))) {
                BlockState block = level.getBlockState(pos);
                if (block.getBlock() instanceof CropBlock && block.isRandomlyTicking() && level.getRandom().nextDouble() < perSecond) {
                    block.randomTick(level, pos.immutable(), level.getRandom());
                }
            }
        }
    }
}
