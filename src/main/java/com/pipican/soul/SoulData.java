package com.pipican.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's Tamashigane: whether they took their one piece, the path their blade was forged on ("" until forged),
 * the souls it absorbed and the stage it reached (stages 5 and 7 wait for a won trial), and the Bankai / ability /
 * Absolute Form timers (game time; the Absolute Form by day number). Kept for life, through death.
 */
public record SoulData(boolean mined, String path, long souls, int stage, long abilityReady, long bankaiEnd, long bankaiReady,
                       long ultimateDay) {

    public static final SoulData NONE = new SoulData(false, "", 0, 0, 0, 0, 0, -1);

    public static final MapCodec<SoulData> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("mined", false).forGetter(SoulData::mined),
            Codec.STRING.optionalFieldOf("path", "").forGetter(SoulData::path),
            Codec.LONG.optionalFieldOf("souls", 0L).forGetter(SoulData::souls),
            Codec.INT.optionalFieldOf("stage", 0).forGetter(SoulData::stage),
            Codec.LONG.optionalFieldOf("ability_ready", 0L).forGetter(SoulData::abilityReady),
            Codec.LONG.optionalFieldOf("bankai_end", 0L).forGetter(SoulData::bankaiEnd),
            Codec.LONG.optionalFieldOf("bankai_ready", 0L).forGetter(SoulData::bankaiReady),
            Codec.LONG.optionalFieldOf("ultimate_day", -1L).forGetter(SoulData::ultimateDay)
    ).apply(i, SoulData::new));

    public boolean forged() {
        return !path.isEmpty();
    }

    public SoulData withMined() {
        return new SoulData(true, path, souls, stage, abilityReady, bankaiEnd, bankaiReady, ultimateDay);
    }

    public SoulData withPath(String p) {
        return new SoulData(mined, p, souls, Math.max(stage, 1), abilityReady, bankaiEnd, bankaiReady, ultimateDay);
    }

    public SoulData withSouls(long s) {
        return new SoulData(mined, path, s, stage, abilityReady, bankaiEnd, bankaiReady, ultimateDay);
    }

    public SoulData withStage(int s) {
        return new SoulData(mined, path, souls, s, abilityReady, bankaiEnd, bankaiReady, ultimateDay);
    }

    public SoulData withAbilityReady(long t) {
        return new SoulData(mined, path, souls, stage, t, bankaiEnd, bankaiReady, ultimateDay);
    }

    public SoulData withBankai(long end, long ready) {
        return new SoulData(mined, path, souls, stage, abilityReady, end, ready, ultimateDay);
    }

    public SoulData withUltimateDay(long day) {
        return new SoulData(mined, path, souls, stage, abilityReady, bankaiEnd, bankaiReady, day);
    }
}
