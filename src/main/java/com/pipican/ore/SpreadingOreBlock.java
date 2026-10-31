package com.pipican.ore;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An ore that grows (Chlorophyte, Tiberium): on a random tick, with {@code chance}, it turns one neighbouring block of
 * the {@code into} kinds into itself. A {@code toxic} one (Tiberium) also poisons whatever stands on it or near it.
 */
public class SpreadingOreBlock extends DropExperienceBlock {

    private final float chance;
    private final Set<Block> into = new HashSet<>();
    private final boolean toxic;
    private final JsonObject spec;

    public SpreadingOreBlock(IntProvider xp, JsonObject spec, Properties properties) {
        super(xp, properties);
        this.spec = spec;
        this.chance = spec.get("chance").getAsFloat();
        this.toxic = spec.has("toxic") && spec.get("toxic").getAsBoolean();
    }

    private Set<Block> into() {
        if (into.isEmpty()) {
            spec.getAsJsonArray("into").forEach(e -> BuiltInRegistries.BLOCK.get(Identifier.parse(e.getAsString())).ifPresent(h -> into.add(h.value())));
        }
        return into;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        spread(state, level, pos, random);
        if (toxic) {
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))) {
                near.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
            }
        }
    }

    /** One growth attempt; true if it spread (public for tests). */
    public boolean spread(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= chance) {
            return false;
        }
        BlockPos target = pos.relative(Direction.getRandom(random));
        if (into().contains(level.getBlockState(target).getBlock())) {
            level.setBlock(target, state, Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (toxic && !level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        }
        super.stepOn(level, pos, state, entity);
    }
}
