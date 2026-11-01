package com.pipican.firearm;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A fired bullet: flies flat and fast, can't be picked up, and is gone when it hits a block or after 3 seconds. */
public class Bullet extends AbstractArrow {

    public Bullet(EntityType<? extends Bullet> type, Level level) {
        super(type, level);
        init();
    }

    public Bullet(Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon) {
        super(Firearms.BULLET_ENTITY.get(), owner, level, pickup, weapon);
        init();
    }

    private void init() {
        this.pickup = Pickup.DISALLOWED;
        this.setNoGravity(true);
        this.setBaseDamage(6.0);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Firearms.BULLET.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (tickCount > 60 || isInGround())) {
            discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!level().isClientSide()) {
            discard();
        }
    }
}
