package com.pipican.boss;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class BossBalance {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CLEAVE_RANGE,CLEAVE_HALF_ANGLE;
    static {
        var builder=new ModConfigSpec.Builder();
        builder.push("morvath");
        CLEAVE_RANGE=builder.defineInRange("cleave_range",6.0,2,12);
        CLEAVE_HALF_ANGLE=builder.comment("Half of the frontal cone, degrees.").defineInRange("cleave_half_angle",60.0,15,85);
        builder.pop(); SPEC=builder.build();
    }
    private BossBalance(){}
}
