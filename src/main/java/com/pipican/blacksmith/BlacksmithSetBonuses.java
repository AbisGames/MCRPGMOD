package com.pipican.blacksmith;

import com.pipican.Pipican;
import com.pipican.blacksmith.BlacksmithUpgrades.State;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Small, composable regional armor bonuses. Uses only private transient modifiers, preserving class stats. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class BlacksmithSetBonuses {
    private static final Identifier TWO_PIECE = Pipican.id("blacksmith/set/two_piece");
    private static final Identifier FOUR_PIECE = Pipican.id("blacksmith/set/four_piece");
    private static final Map<ServerPlayer, String> LAST = Collections.synchronizedMap(new WeakHashMap<>());

    private BlacksmithSetBonuses() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    /** Re-evaluated after equipment changes; public for deterministic server tests. */
    public static void refresh(ServerPlayer player) {
        var groups = new TreeMap<String, List<BlacksmithUpgrades.Tier>>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !BlacksmithUpgrades.supported(stack)) continue;
            try {
                State state = State.read(stack);
                if (!state.region().isEmpty()) groups.computeIfAbsent(state.region(), key -> new ArrayList<>()).add(state.tier());
            } catch (IllegalArgumentException ignored) {
                // Corrupt external item metadata cannot crash player ticking.
            }
        }
        var active = groups.entrySet().stream().filter(entry -> entry.getValue().size() >= 2)
                .max(Comparator.<Map.Entry<String, List<BlacksmithUpgrades.Tier>>>comparingInt(entry -> entry.getValue().size())
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder())).orElse(null);
        String key = active == null ? "" : active.getKey() + "/" + active.getValue().size() + "/"
                + active.getValue().stream().mapToInt(Enum::ordinal).sum();
        if (key.equals(LAST.get(player))) return;
        LAST.put(player, key);
        player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(TWO_PIECE);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(FOUR_PIECE);
        if (active == null) return;

        double averageTier = active.getValue().stream().mapToInt(Enum::ordinal).average().orElse(0);
        if (active.getValue().size() >= 2) {
            double resistance = 0.02 + averageTier * 0.0025;
            player.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addTransientModifier(new AttributeModifier(
                    TWO_PIECE, resistance, AttributeModifier.Operation.ADD_VALUE));
        }
        if (active.getValue().size() >= 4) {
            double speed = 0.015 + averageTier * 0.005;
            player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(
                    FOUR_PIECE, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
