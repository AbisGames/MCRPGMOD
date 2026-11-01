package com.pipican.soul;

import com.mojang.serialization.Codec;
import com.pipican.Pipican;
import com.pipican.ore.PipicanOres;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * The source of the River of Souls: the world's one Tamashigane vein. It is placed once, in the first Soul Sand Valley
 * chunk generated at least 128 blocks from the Nether's centre - a single ore on a soul-soil shrine ringed by soul
 * campfires. The vein re-forms every time it is mined (each soul takes from it only once; {@link SoulEvents}).
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class SoulSpring extends SavedData {

    public static final int MIN_DISTANCE = 128;

    public static final Codec<SoulSpring> CODEC = BlockPos.CODEC.optionalFieldOf("pos").codec().xmap(SoulSpring::new, s -> s.pos);
    public static final SavedDataType<SoulSpring> TYPE = new SavedDataType<>(Pipican.id("soul_spring"), SoulSpring::new, CODEC, null);

    private Optional<BlockPos> pos = Optional.empty();

    public SoulSpring() {}

    private SoulSpring(Optional<BlockPos> pos) {
        this.pos = pos;
    }

    public static SoulSpring get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<BlockPos> pos() {
        return pos;
    }

    @SubscribeEvent
    static void onChunk(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.NETHER) {
            return;
        }
        SoulSpring spring = get(level);
        if (spring.pos.isPresent()) {
            return;
        }
        ChunkAccess chunk = event.getChunk();
        var cp = chunk.getPos();
        int x = cp.getMinBlockX() + 8;
        int z = cp.getMinBlockZ() + 8;
        if ((long) x * x + (long) z * z < (long) MIN_DISTANCE * MIN_DISTANCE) {
            return;
        }
        if (!chunk.getNoiseBiome(x >> 2, 64 >> 2, z >> 2).is(Biomes.SOUL_SAND_VALLEY)) {
            return;
        }
        // A floor with air above it, low in the valley.
        for (int y = 32; y < 100; y++) {
            BlockPos at = new BlockPos(x, y, z);
            if (!chunk.getBlockState(at).isAir() && chunk.getBlockState(at.above()).isAir() && chunk.getBlockState(at.above(2)).isAir()
                    && chunk.getBlockState(at.above(3)).isAir()) {
                spring.pos = Optional.of(build(chunk, at.above()));
                spring.setDirty();
                return;
            }
        }
    }

    /** Builds the shrine with its ore one block above {@code floor}'s top; returns the ore's position (public for tests). */
    public static BlockPos build(LevelAccessor level, BlockPos at) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(at.offset(dx, -1, dz), Blocks.SOUL_SOIL.defaultBlockState(), 2);
                for (int dy = 0; dy <= 2; dy++) {
                    level.setBlock(at.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            level.setBlock(at.offset(c[0], 0, c[1]), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 2);
        }
        level.setBlock(at, PipicanOres.block("deepslate_tamashigane_ore").get().defaultBlockState(), 2);
        return at.immutable();
    }

    /** Builds the shrine straight into a chunk (during generation). */
    private static BlockPos build(ChunkAccess chunk, BlockPos at) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                chunk.setBlockState(at.offset(dx, -1, dz), Blocks.SOUL_SOIL.defaultBlockState(), 0);
                for (int dy = 0; dy <= 2; dy++) {
                    chunk.setBlockState(at.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 0);
                }
            }
        }
        for (int[] c : new int[][] {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            chunk.setBlockState(at.offset(c[0], 0, c[1]), Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 0);
        }
        chunk.setBlockState(at, PipicanOres.block("deepslate_tamashigane_ore").get().defaultBlockState(), 0);
        return at.immutable();
    }

    /** A player who has no blade yet hears the river within 48 blocks of the spring. */
    static void hint(ServerPlayer player) {
        if (player.tickCount % 200 != 0 || player.level().dimension() != Level.NETHER || Soul.get(player).mined()) {
            return;
        }
        get((ServerLevel) player.level()).pos.ifPresent(p -> {
            if (p.distSqr(player.blockPosition()) < 48 * 48) {
                player.sendOverlayMessage(Component.translatable("pipican.soul.spring").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            }
        });
    }
}
