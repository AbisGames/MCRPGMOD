package com.pipican.ability.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pipican.ability.AbilityAction;
import com.pipican.ability.AbilityActionTypes;
import com.pipican.ability.AbilityContext;
import com.pipican.ability.SpecialItems;
import com.pipican.ability.Targets;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * {@code pipican:weapon_strike} - hits the targets with the user's weapons, as real melee attacks: the main hand deals
 * the player's attack damage, the off hand its weapon's damage ("spin, hitting all mobs around you with both weapons").
 *
 * <pre>{@code { "type": "pipican:weapon_strike", "target": "hostiles", "radius": 3, "hands": "both", "multiplier": 1.0 } }</pre>
 * {@code hands}: {@code main}, {@code off} or {@code both} (the off hand only strikes with a weapon in it).
 */
public record WeaponStrikeAction(Targets targets, Hands hands, double multiplier) implements AbilityAction {

    public enum Hands implements StringRepresentable {
        MAIN("main"), OFF("off"), BOTH("both");

        public static final Codec<Hands> CODEC = StringRepresentable.fromEnum(Hands::values);
        private final String name;

        Hands(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final MapCodec<WeaponStrikeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Targets.mapCodec(Targets.Kind.HOSTILES).forGetter(WeaponStrikeAction::targets),
            Hands.CODEC.optionalFieldOf("hands", Hands.MAIN).forGetter(WeaponStrikeAction::hands),
            Codec.doubleRange(0, 100).optionalFieldOf("multiplier", 1.0).forGetter(WeaponStrikeAction::multiplier)
    ).apply(i, WeaponStrikeAction::new));

    @Override
    public AbilityActionTypes.Type type() {
        return AbilityActionTypes.WEAPON_STRIKE;
    }

    /** The attack damage an off-hand weapon would deal in the main hand (1 + its modifiers). */
    static double offhandDamage(ItemStack off) {
        double[] attack = {1};
        off.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                attack[0] += modifier.amount();
            }
        });
        return attack[0];
    }

    @Override
    public void run(AbilityContext ctx) {
        var player = ctx.player();
        double main = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ItemStack off = player.getOffhandItem();
        boolean offStrikes = hands != Hands.MAIN && SpecialItems.isWeapon(off);
        double k = multiplier * (1 + ctx.mod().strength());
        for (LivingEntity target : targets.select(ctx)) {
            if (target == player) {
                continue;
            }
            if (hands != Hands.OFF) {
                target.invulnerableTime = 0;
                target.hurtServer(ctx.level(), player.damageSources().playerAttack(player), (float) (main * k + ctx.mod().damage()));
            }
            if (offStrikes && target.isAlive()) {
                target.invulnerableTime = 0;
                target.hurtServer(ctx.level(), player.damageSources().playerAttack(player), (float) (offhandDamage(off) * k + ctx.mod().damage()));
            }
        }
        if (offStrikes) {
            player.swing(net.minecraft.world.InteractionHand.OFF_HAND, true);
        }
        if (hands != Hands.OFF) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        }
    }
}
