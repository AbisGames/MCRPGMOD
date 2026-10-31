package com.pipican.ability;

import com.pipican.Pipican;
import com.pipican.player.ClassEffectApplier;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Applies {@code pipican:mount} upgrades to the player's current mount, and takes them off the last one. */
public final class Mounts {

    private static final List<Holder<Attribute>> ATTRIBUTES = List.of(Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
            Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.ATTACK_DAMAGE);
    private static final Map<AttributeModifier.Operation, Identifier> IDS = new EnumMap<>(AttributeModifier.Operation.class);
    private static final Map<UUID, WeakReference<LivingEntity>> LAST = new ConcurrentHashMap<>();

    static {
        for (AttributeModifier.Operation op : AttributeModifier.Operation.values()) {
            IDS.put(op, Pipican.id("mount/" + op.getSerializedName()));
        }
    }

    private Mounts() {}

    /** Every 20 ticks. */
    public static void tick(ServerPlayer player) {
        var upgrades = ClassEffectApplier.state(player).mountUpgrades();
        LivingEntity mount = player.getVehicle() instanceof LivingEntity l && !(l instanceof Player) && !upgrades.isEmpty() ? l : null;
        WeakReference<LivingEntity> lastRef = LAST.get(player.getUUID());
        LivingEntity last = lastRef == null ? null : lastRef.get();
        if (last != null && last != mount) {
            strip(last);
        }
        if (mount == null) {
            LAST.remove(player.getUUID());
            return;
        }
        LAST.put(player.getUUID(), new WeakReference<>(mount));
        for (Holder<Attribute> attribute : ATTRIBUTES) {
            AttributeInstance instance = mount.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            IDS.values().forEach(instance::removeModifier);
            upgrades.getOrDefault(attribute, Map.of()).forEach((op, amount) -> {
                if (amount != 0) {
                    instance.addTransientModifier(new AttributeModifier(IDS.get(op), amount, op));
                }
            });
        }
        if (mount.getHealth() > mount.getMaxHealth()) {
            mount.setHealth(mount.getMaxHealth());
        }
    }

    private static void strip(LivingEntity mount) {
        for (Holder<Attribute> attribute : ATTRIBUTES) {
            AttributeInstance instance = mount.getAttribute(attribute);
            if (instance != null) {
                IDS.values().forEach(instance::removeModifier);
            }
        }
        if (mount.getHealth() > mount.getMaxHealth()) {
            mount.setHealth(mount.getMaxHealth());
        }
    }
}
