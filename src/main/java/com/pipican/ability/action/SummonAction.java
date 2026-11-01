package com.pipican.ability.action;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.pipican.player.ClassEffectApplier;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.MobControl;
import com.pipican.ability.Summons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;

/**
 * {@code pipican:summon} - spawns temporary helpers next to the user. Tameable animals (wolves, cats)
 * are tamed to the user and iron golems count as player-made, so both fight for them. Summons vanish
 * after {@code seconds} and drop nothing. With {@code ally: true} any mob (even a ravager) fights for the
 * user instead: it attacks hostile mobs near them and never targets players.
 *
 * <pre>{@code { "type": "pipican:summon", "entity": "minecraft:wolf", "count": 2, "seconds": 30 } }</pre>
 * In a reaction they appear where the mob was ({@code victims} / {@code unless_victims}: only when it was / wasn't one of
 * these kinds - "a mob you kill rises as a zombie, or a skeleton if it was one").
 */
public record SummonAction(EntityType<?> entity, int count, double seconds, boolean ally,
                           java.util.Optional<net.minecraft.core.HolderSet<EntityType<?>>> victims, java.util.Optional<net.minecraft.core.HolderSet<EntityType<?>>> unlessVictims) implements AbilityAction {

    public static final MapCodec<SummonAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(SummonAction::entity),
            Codec.intRange(1, 16).optionalFieldOf("count", 1).forGetter(SummonAction::count),
            Codec.doubleRange(1, 3600).optionalFieldOf("seconds", 30.0).forGetter(SummonAction::seconds),
            Codec.BOOL.optionalFieldOf("ally", false).forGetter(SummonAction::ally),
            net.minecraft.core.RegistryCodecs.homogeneousList(net.minecraft.core.registries.Registries.ENTITY_TYPE).optionalFieldOf("victims").forGetter(SummonAction::victims),
            net.minecraft.core.RegistryCodecs.homogeneousList(net.minecraft.core.registries.Registries.ENTITY_TYPE).optionalFieldOf("unless_victims").forGetter(SummonAction::unlessVictims)
    ).apply(i, SummonAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.SUMMON;
    }

    private static void boost(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount) {
        var instance = mob.getAttribute(attribute);
        if (instance != null && amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(com.pipican.Pipican.id("summon_mod"), amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        var kind = ctx.subject().map(s -> s.getType().builtInRegistryHolder());
        if (victims.isPresent() && kind.map(k -> !victims.get().contains(k)).orElse(true)
                || unlessVictims.isPresent() && kind.map(k -> unlessVictims.get().contains(k)).orElse(false)) {
            return;
        }
        var mods = ClassEffectApplier.state(player).summonMods();
        int count = this.count + mods.count();
        double seconds = this.seconds + mods.seconds();
        for (int n = 0; n < count; n++) {
            var spawned = entity.create(ctx.level(), EntitySpawnReason.MOB_SUMMONED);
            if (!(spawned instanceof Mob mob)) {
                continue;
            }
            double angle = Math.PI * 2 * n / count;
            var at = ctx.origin();
            mob.snapTo(at.x + Math.cos(angle) * 1.5, at.y, at.z + Math.sin(angle) * 1.5, player.getYRot(), 0);
            if (mob instanceof TamableAnimal pet) {
                pet.tame(player);
            }
            if (mob instanceof IronGolem golem) {
                golem.setPlayerCreated(true);
            }
            mob.setPersistenceRequired();
            boost(mob, Attributes.ATTACK_DAMAGE, mods.damage());
            boost(mob, Attributes.MAX_HEALTH, mods.health());
            var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null && mods.hearts() != 0) {
                maxHealth.addPermanentModifier(new AttributeModifier(com.pipican.Pipican.id("summon_hearts"), mods.hearts() * 2, AttributeModifier.Operation.ADD_VALUE));
            }
            mob.setHealth(mob.getMaxHealth());
            ctx.level().addFreshEntity(mob);
            Summons.track(mob, player, (long) (seconds * 20));
            if (ally) {
                MobControl.control(mob, player, MobControl.Mode.ALLY, (long) (seconds * 20));
            }
        }
    }
}
