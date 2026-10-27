package com.pipican.rpgclass;

import com.mojang.serialization.Codec;
import com.pipican.core.PipicanConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

/**
 * How good a class is with a piece of equipment. Every item a player can use resolves to one tier
 * through the class's {@link AffinityRules}. Nothing is ever hard-blocked: a forbidden item can
 * still be used, it is just heavily penalised.
 *
 * <p>The actual numbers for each tier (damage, attack speed, armor, movement, trinket strength)
 * live in the server config {@code pipican-server.toml} so they can be balanced without code.
 */
public enum Affinity implements StringRepresentable {
    MASTERY("mastery", ChatFormatting.GOLD),
    PROFICIENT("proficient", ChatFormatting.GREEN),
    NEUTRAL("neutral", ChatFormatting.GRAY),
    CLUMSY("clumsy", ChatFormatting.YELLOW),
    FORBIDDEN("forbidden", ChatFormatting.RED);

    public static final Codec<Affinity> CODEC = StringRepresentable.fromEnum(Affinity::values);

    private final String name;
    private final ChatFormatting color;

    Affinity(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public ChatFormatting color() {
        return color;
    }

    public String translationKey() {
        return "pipican.affinity." + name;
    }

    /** This tier, but never worse than neutral when {@code floor} ("no gear is worse than neutral for you"). */
    public Affinity atLeastNeutral(boolean floor) {
        return floor && ordinal() > NEUTRAL.ordinal() ? NEUTRAL : this;
    }

    /** The configured numbers for this tier. */
    public PipicanConfig.TierValues tuning() {
        return PipicanConfig.tier(this);
    }
}
