package com.pipican.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.Pipican;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * One dimension's persistent renewal timetable for placed regional ore nodes.
 * All timestamps use the saved world's game tick, which survives reloading a
 * singleplayer save and cannot be manipulated by a client-side system clock.
 */
public final class RenewableOreSavedData extends SavedData {
    public record Node(long position, String oreId, long restoreAt) {}

    private static final Codec<Node> NODE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.LONG.fieldOf("position").forGetter(Node::position),
        Codec.STRING.fieldOf("ore").forGetter(Node::oreId),
        Codec.LONG.fieldOf("restore_at").forGetter(Node::restoreAt)
    ).apply(instance, Node::new));

    public static final Codec<RenewableOreSavedData> CODEC = NODE_CODEC.listOf().xmap(
        RenewableOreSavedData::new, data -> List.copyOf(data.nodes.values()));

    public static final SavedDataType<RenewableOreSavedData> TYPE = new SavedDataType<>(
        Pipican.id("renewable_mine_nodes"), RenewableOreSavedData::new, CODEC, null);

    private final Map<Long, Node> nodes = new HashMap<>();

    public RenewableOreSavedData() {}

    private RenewableOreSavedData(List<Node> saved) {
        for (Node node : saved) {
            nodes.put(node.position(), node);
        }
    }

    public static RenewableOreSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Stores only positions where the renewable mine resource has been consumed. */
    public void record(BlockPos position, String oreId, long readyAt) {
        nodes.put(position.asLong(), new Node(position.asLong(), oreId, readyAt));
        setDirty();
    }

    public List<Node> due(long gameTime, int limit) {
        if (limit <= 0) return List.of();
        List<Node> result = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (node.restoreAt() <= gameTime) {
                result.add(node);
                if (result.size() == limit) break;
            }
        }
        return result;
    }

    /** Removes a completed renewal entry only after confirming safe restoration. */
    public void remove(long position) {
        if (nodes.remove(position) != null) setDirty();
    }
}