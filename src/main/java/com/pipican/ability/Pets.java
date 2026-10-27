package com.pipican.ability;

import com.pipican.Pipican;
import com.pipican.player.ClassEffectApplier;
import com.pipican.player.ClassState;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jspecify.annotations.Nullable;

/** Applies {@code pipican:pet_mod} upgrades to the player's tamed animals. */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class Pets {

    private static final Identifier HEALTH = Pipican.id("pet/health");
    private static final Identifier HEARTS = Pipican.id("pet/hearts");
    private static final Identifier ARMOR = Pipican.id("pet/armor");
    private static final double RADIUS = 32;

    private Pets() {}

    /** The player who owns {@code entity}, if it's a tamed animal of an online player. */
    public static @Nullable ServerPlayer owner(@Nullable Object entity) {
        if (!(entity instanceof OwnableEntity pet) || pet.getOwnerReference() == null) {
            return null;
        }
        if (pet.getOwner() instanceof ServerPlayer p) {
            return p;
        }
        var server = pet.level().getServer();
        return server == null ? null : server.getPlayerList().getPlayer(pet.getOwnerReference().getUUID());
    }

    /** Every 20 ticks: refreshes the modifiers on tamed animals near the player. */
    public static void tick(ServerPlayer player) {
        ClassState.PetMods mods = ClassEffectApplier.state(player).petMods();
        for (LivingEntity pet : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                e -> e instanceof OwnableEntity o && o.getOwnerReference() != null && o.getOwnerReference().getUUID().equals(player.getUUID()))) {
            set(pet, Attributes.MAX_HEALTH, HEALTH, mods.health(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            set(pet, Attributes.MAX_HEALTH, HEARTS, mods.hearts() * 2, AttributeModifier.Operation.ADD_VALUE);
            set(pet, Attributes.ARMOR, ARMOR, mods.armor(), AttributeModifier.Operation.ADD_VALUE);
            if (pet.getHealth() > pet.getMaxHealth()) {
                pet.setHealth(pet.getMaxHealth());
            }
        }
    }

    private static void set(LivingEntity pet, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = pet.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (current != null && current.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        ServerPlayer attackerOwner = owner(event.getSource().getEntity());
        if (attackerOwner != null) {
            event.setAmount((float) (event.getAmount() * Math.max(0, 1 + ClassEffectApplier.state(attackerOwner).petMods().damage())));
        }
        ServerPlayer victimOwner = owner(event.getEntity());
        if (victimOwner != null) {
            event.setAmount((float) (event.getAmount() * Math.max(0, 1 + ClassEffectApplier.state(victimOwner).petMods().damageTaken())));
        }
    }
}
