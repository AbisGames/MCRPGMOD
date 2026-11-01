package com.pipican.firearm;

import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A firearm: loads one bullet like a crossbow loads a bolt (hold use), then fires it with use. {@link Stats} sets the
 * bullet's damage and speed, how long loading takes compared to a crossbow, and the chance a shot misfires (the round is
 * spent with a click and nothing comes out).
 */
public class FirearmItem extends CrossbowItem {

    /** @param damage bullet damage; @param speed muzzle velocity (a crossbow bolt is 3.15); @param reload loading time x crossbow; @param misfire chance */
    public record Stats(double damage, float speed, double reload, double misfire) {}

    private final Stats stats;

    public FirearmItem(Properties properties, Stats stats) {
        super(properties);
        this.stats = stats;
    }

    public Stats stats() {
        return stats;
    }

    private static final Predicate<ItemStack> BULLETS = stack -> stack.is(Firearms.BULLETS);

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return BULLETS;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return BULLETS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ChargedProjectiles loaded = gun.get(DataComponents.CHARGED_PROJECTILES);
        if (loaded != null && !loaded.isEmpty()) {
            if (level instanceof ServerLevel && stats.misfire() > 0 && player.getRandom().nextDouble() < stats.misfire()) {
                // Misfire: the round is spent, nothing leaves the barrel.
                gun.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.6F);
                return InteractionResult.CONSUME;
            }
            this.performShooting(level, player, hand, gun, stats.speed(), 1.0F, null);
            return InteractionResult.CONSUME;
        }
        return super.use(level, player, hand);
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle,
                                   @Nullable LivingEntity targetOverride) {
        Vec3 aim = targetOverride != null
                ? targetOverride.getEyePosition().subtract(projectile.position())
                : shooter.getViewVector(1.0F);
        projectile.shoot(aim.x, aim.y, aim.z, power, uncertainty);
        shooter.level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(), SoundEvents.GENERIC_EXPLODE, shooter.getSoundSource(),
                0.6F, 1.8F + shooter.getRandom().nextFloat() * 0.2F);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack heldItem, ItemStack projectile, boolean isCrit) {
        Projectile shot = super.createProjectile(level, shooter, heldItem, projectile, isCrit);
        if (shot instanceof AbstractArrow bullet) {
            bullet.setBaseDamage(stats.damage());
        }
        return shot;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 24;
    }
}
