package com.pipican.ability;

import com.pipican.Pipican;
import com.pipican.player.PipicanAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Earth Glide ({@code phase_stone}): a phasing player passes through {@code #pipican:earth_glide} blocks at and above their
 * feet - sneaking lets them sink through the floor too - and doesn't suffocate inside them. Both sides read the synced
 * movement flag, so the client predicts the same collisions as the server.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class Phasing {

    public static final TagKey<Block> PASSABLE = TagKey.create(Registries.BLOCK, Pipican.id("earth_glide"));

    private Phasing() {}

    public static boolean phasing(Entity entity) {
        return entity instanceof Player player && player.getData(PipicanAttachments.MOVE_FLAGS).has("phase_stone");
    }

    public static boolean passes(Entity entity, BlockBehaviour.BlockStateBase state, BlockPos pos) {
        if (!(entity instanceof Player player) || !state.is(PASSABLE) || !phasing(player)) {
            return false;
        }
        return player.isShiftKeyDown() || pos.getY() >= Mth.floor(player.getY() + 0.01);
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.IN_WALL) && phasing(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
