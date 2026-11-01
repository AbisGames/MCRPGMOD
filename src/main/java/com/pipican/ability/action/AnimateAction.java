package com.pipican.ability.action;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.MobControl;
import com.pipican.ability.Summons;
import com.pipican.player.ClassEffectApplier;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import org.joml.Vector3f;

/**
 * {@code pipican:animate} - turns the placed block the user looks at (an anvil, a cauldron...) into a friendly construct
 * for {@code seconds} (Animate Object). The construct looks like the block, fights the user's enemies and is as tough and
 * as hard-hitting as the block is hard; when it expires or is destroyed the block is set back down where it stands (or
 * dropped if that spot is taken). Blocks with contents (chests...), liquids and unbreakable blocks can't be animated.
 * At most {@code max} constructs (plus {@code summon_mod} count bonuses) at once: a new one makes the oldest fall back
 * into a block.
 *
 * <pre>{@code { "type": "pipican:animate", "seconds": 60, "range": 6 } }</pre>
 */
public record AnimateAction(double seconds, double range, int max) implements AbilityAction {

    public static final MapCodec<AnimateAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 60.0).forGetter(AnimateAction::seconds),
            Codec.doubleRange(1, 32).optionalFieldOf("range", 6.0).forGetter(AnimateAction::range),
            Codec.intRange(1, 16).optionalFieldOf("max", 1).forGetter(AnimateAction::max)
    ).apply(i, AnimateAction::new));

    public static final TagKey<Block> NOT_ANIMATABLE = TagKey.create(Registries.BLOCK, Pipican.id("not_animatable"));

    /** Construct -> the block it is made of. */
    private static final Map<UUID, BlockState> CONSTRUCTS = new ConcurrentHashMap<>();
    /** Player -> their constructs, oldest first. */
    private static final Map<UUID, ArrayDeque<IronGolem>> OWNED = new ConcurrentHashMap<>();

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.ANIMATE;
    }

    @Override
    public void run(AbilityContext ctx) {
        ServerPlayer player = ctx.player();
        ServerLevel level = ctx.level();
        HitResult hit = player.pick(range, 1.0f, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() == HitResult.Type.MISS) {
            ctx.cancel(Component.translatable("pipican.ability.needs_block"));
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || hardness < 0 || state.hasBlockEntity() || !state.getFluidState().isEmpty() || state.is(NOT_ANIMATABLE)
                || !level.mayInteract(player, pos)) {
            ctx.cancel(Component.translatable("pipican.ability.cant_animate"));
            return;
        }
        IronGolem golem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            return;
        }
        level.removeBlock(pos, false);
        golem.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0);
        golem.setPlayerCreated(true);
        golem.setInvisible(true);
        golem.setPersistenceRequired();
        golem.setCustomName(state.getBlock().getName());
        double health = Math.clamp(10 + hardness * 12, 10, 100);
        double attack = Math.clamp(3 + hardness * 2, 3, 15);
        golem.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(Pipican.id("animated_health"), health - 100, AttributeModifier.Operation.ADD_VALUE));
        golem.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new AttributeModifier(Pipican.id("animated_attack"), attack - 15, AttributeModifier.Operation.ADD_VALUE));
        golem.getAttribute(Attributes.SCALE).addPermanentModifier(new AttributeModifier(Pipican.id("animated_scale"), -0.55, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        golem.setHealth(golem.getMaxHealth());
        level.addFreshEntity(golem);
        // The block itself, riding the construct so it moves with it.
        Entity display = blockDisplay(level, state);
        if (display != null) {
            display.snapTo(golem.getX(), golem.getY(), golem.getZ(), 0, 0);
            level.addFreshEntity(display);
            display.startRiding(golem, true, false);
        }
        long ticks = Math.round((seconds + ctx.mod().seconds()) * 20);
        Summons.track(golem, player, ticks);
        MobControl.control(golem, player, MobControl.Mode.ALLY, ticks);
        CONSTRUCTS.put(golem.getUUID(), state);
        var mine = OWNED.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
        mine.removeIf(g -> !g.isAlive());
        int cap = max + ClassEffectApplier.state(player).summonMods().count();
        while (mine.size() >= cap) {
            mine.pollFirst().discard();
        }
        mine.addLast(golem);
    }

    private static Entity blockDisplay(ServerLevel level, BlockState state) {
        CompoundTag tag = new CompoundTag();
        BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).result().ifPresent(t -> tag.put("block_state", t));
        Transformation shape = new Transformation(new Vector3f(-0.45f, -1.1f, -0.45f), null, new Vector3f(0.9f, 0.9f, 0.9f), null);
        Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, shape).result().ifPresent(t -> tag.put("transformation", t));
        return EntityType.loadEntityRecursive(EntityType.BLOCK_DISPLAY, tag, level, EntitySpawnReason.TRIGGERED, e -> e);
    }

    @EventBusSubscriber(modid = Pipican.MOD_ID)
    public static final class Restore {
        private Restore() {}

        /** A construct that expires or is destroyed falls back into its block. */
        @SubscribeEvent
        static void onLeave(EntityLeaveLevelEvent event) {
            Entity entity = event.getEntity();
            if (!(event.getLevel() instanceof ServerLevel level) || entity.getRemovalReason() == null || !entity.getRemovalReason().shouldDestroy()) {
                return;
            }
            BlockState state = CONSTRUCTS.remove(entity.getUUID());
            if (state == null) {
                return;
            }
            entity.getPassengers().forEach(Entity::discard);
            BlockPos at = entity.blockPosition();
            if (level.getBlockState(at).canBeReplaced()) {
                level.setBlockAndUpdate(at, state);
            } else {
                Block.popResource(level, at, new ItemStack(state.getBlock()));
            }
        }
    }
}
