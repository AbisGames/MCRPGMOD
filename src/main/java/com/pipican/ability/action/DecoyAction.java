package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.MobControl;
import com.pipican.ability.Summons;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * {@code pipican:decoy} - leaves a fake of the user (an unbreakable armor stand with their name) that
 * every hostile mob within {@code radius} attacks instead of them for {@code seconds}.
 *
 * <pre>{@code { "type": "pipican:decoy", "seconds": 6, "radius": 16 } }</pre>
 */
public record DecoyAction(double seconds, double radius) implements AbilityAction {

    public static final MapCodec<DecoyAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 120).optionalFieldOf("seconds", 6.0).forGetter(DecoyAction::seconds),
            Codec.doubleRange(1, 64).optionalFieldOf("radius", 16.0).forGetter(DecoyAction::radius)
    ).apply(i, DecoyAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.DECOY;
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        ArmorStand decoy = EntityType.ARMOR_STAND.create(ctx.level(), EntitySpawnReason.TRIGGERED);
        if (decoy == null) {
            return;
        }
        decoy.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        decoy.setInvulnerable(true);
        decoy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
        decoy.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        decoy.setCustomName(player.getName());
        decoy.setCustomNameVisible(true);
        ctx.level().addFreshEntity(decoy);
        long ticks = (long) (seconds * 20);
        Summons.track(decoy, player, ticks);
        for (Mob mob : ctx.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius), m -> m instanceof Enemy && m.isAlive())) {
            MobControl.taunt(mob, player, decoy, ticks);
        }
    }
}
