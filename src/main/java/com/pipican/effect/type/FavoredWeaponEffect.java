package com.pipican.effect.type;

import com.mojang.serialization.MapCodec;
import com.pipican.effect.ClassEffect;
import com.pipican.effect.ClassEffectTypes;
import com.pipican.effect.EffectContext;

/**
 * {@code pipican:favored_weapon} - when you wake up, the kind of weapon in your main hand (sword, axe, spear, mace,
 * trident, bow or crossbow) becomes your favored weapon. Use the {@code pipican:holding_favored} condition for its bonuses.
 */
public record FavoredWeaponEffect() implements ClassEffect {

    public static final MapCodec<FavoredWeaponEffect> CODEC = MapCodec.unit(FavoredWeaponEffect::new);

    @Override
    public ClassEffectTypes.Type type() {
        return ClassEffectTypes.FAVORED_WEAPON;
    }

    @Override
    public void apply(EffectContext ctx) {
        ctx.enableFavoredWeapon();
    }
}
