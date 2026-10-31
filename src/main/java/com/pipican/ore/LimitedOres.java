package com.pipican.ore;

import com.mojang.serialization.Codec;
import com.pipican.Pipican;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Ores that exist a fixed number of times per world (T8/T9). When a new overworld chunk generates, each still has a
 * small chance to be placed in it, until its count is used up; the counts are saved with the world.
 * <ul>
 *   <li>Unobtanium: one vein of exactly 24 ore (one full armor set) under the world's centre (chunk 0,0)</li>
 *   <li>Pale Ore: 6 single blocks; Chaos Emerald: 7; Trellium: 12; deep in the world</li>
 *   <li>Lerasium and the Philosopher's Stone: one each, at least 2000 / 3000 blocks from the centre</li>
 * </ul>
 * Uru, Adamantium, Draconium, Luminite and Phazon come from bosses and meteors instead ({@link OreWorldEvents}).
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class LimitedOres extends SavedData {

    /** id -> how many may exist, chance per new chunk, lowest/highest Y, minimum distance from the centre. */
    public record Rule(String ore, int count, float chance, int minY, int maxY, int minDistance) {}

    public static final List<Rule> RULES = List.of(
            new Rule("pale", 6, 1 / 400f, -60, -40, 0),
            new Rule("chaos_emerald", 7, 1 / 300f, -55, 20, 0),
            new Rule("trellium", 12, 1 / 200f, -62, -45, 0),
            new Rule("lerasium", 1, 1 / 1500f, -62, -50, 2000),
            new Rule("philosophers_stone", 1, 1 / 2000f, -62, -50, 3000));
    /** Unobtanium: one full set's worth, once. */
    public static final int UNOBTANIUM_VEIN = 24;

    public static final Codec<LimitedOres> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(LimitedOres::new, d -> Map.copyOf(d.placed));
    public static final SavedDataType<LimitedOres> TYPE = new SavedDataType<>(Pipican.id("limited_ores"), LimitedOres::new, CODEC, null);

    private final Map<String, Integer> placed = new HashMap<>();

    public LimitedOres() {}

    private LimitedOres(Map<String, Integer> saved) {
        placed.putAll(saved);
    }

    public static LimitedOres get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int placed(String ore) {
        return placed.getOrDefault(ore, 0);
    }

    private void add(String ore, int n) {
        placed.merge(ore, n, Integer::sum);
        setDirty();
    }

    @SubscribeEvent
    static void onChunk(ChunkEvent.Load event) {
        if (!event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        place(level, event.getChunk(), level.getRandom());
    }

    /** Places this chunk's share of the limited ores (public for tests). */
    public static void place(ServerLevel level, ChunkAccess chunk, RandomSource random) {
        LimitedOres data = get(level);
        var pos = chunk.getPos();
        if (pos.x() == 0 && pos.z() == 0 && data.placed("unobtanium") == 0) {
            data.add("unobtanium", vein(chunk, ore("unobtanium", true), new BlockPos(8, -58, 8), UNOBTANIUM_VEIN));
        }
        long distance = (long) Math.sqrt((double) pos.getMinBlockX() * pos.getMinBlockX() + (double) pos.getMinBlockZ() * pos.getMinBlockZ());
        for (Rule rule : RULES) {
            if (data.placed(rule.ore()) >= rule.count() || distance < rule.minDistance() || random.nextFloat() >= rule.chance()) {
                continue;
            }
            int y = rule.minY() + random.nextInt(rule.maxY() - rule.minY() + 1);
            BlockPos at = new BlockPos(pos.getMinBlockX() + random.nextInt(16), y, pos.getMinBlockZ() + random.nextInt(16));
            data.add(rule.ore(), vein(chunk, ore(rule.ore(), y < 0), at, 1));
        }
    }

    private static Block ore(String id, boolean deep) {
        return PipicanOres.block((deep ? "deepslate_" : "") + id + "_ore").get();
    }

    /** Puts up to {@code size} ore blocks into solid ground around {@code at} (chunk-local x/z); returns how many. */
    private static int vein(ChunkAccess chunk, Block block, BlockPos at, int size) {
        int done = 0;
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        int x0 = at.getX() >= baseX ? at.getX() - baseX : at.getX();
        int z0 = at.getZ() >= baseZ ? at.getZ() - baseZ : at.getZ();
        for (int r = 0; r < 4 && done < size; r++) {
            for (int dx = -r; dx <= r && done < size; dx++) {
                for (int dy = -r; dy <= r && done < size; dy++) {
                    for (int dz = -r; dz <= r && done < size; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r) {
                            continue;
                        }
                        int x = x0 + dx;
                        int z = z0 + dz;
                        if (x < 0 || x > 15 || z < 0 || z > 15) {
                            continue;
                        }
                        BlockPos p = new BlockPos(baseX + x, at.getY() + dy, baseZ + z);
                        if (p.getY() <= chunk.getMinY() || p.getY() >= chunk.getMaxY()) {
                            continue;
                        }
                        var state = chunk.getBlockState(p);
                        if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(chunk, p) >= 0) {
                            chunk.setBlockState(p, block.defaultBlockState(), 0);
                            done++;
                        }
                    }
                }
            }
        }
        return done;
    }
}
