package com.pipican.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * The Witch's steerable green fireball (Witch Fire): it burns the mobs it touches and fizzles on blocks instead of
 * starting fires. {@link com.pipican.ability.action.SteerAction} spawns it and steers it every tick.
 */
public class WitchFireball extends SmallFireball {

    private float damage = 3;

    public WitchFireball(EntityType<? extends WitchFireball> type, Level level) {
        super(type, level);
        setItem(new ItemStack(Items.SLIME_BALL));
        this.accelerationPower = 0;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();
        // Only lives while it is being steered (SteerAction keeps it young); strays fizzle out.
        if (!level().isClientSide() && (getOwner() == null || tickCount > 40)) {
            discard();
        }
    }

    /** Called every tick while its witch steers it. */
    public void keepAlive() {
        tickCount = 0;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level() instanceof ServerLevel level && hit.getEntity() != getOwner()) {
            Entity target = hit.getEntity();
            target.igniteForSeconds(5.0F);
            target.hurtServer(level, damageSources().fireball(this, getOwner()), damage);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        // Witch fire burns flesh, not wood: no fire is started.
    }
}
