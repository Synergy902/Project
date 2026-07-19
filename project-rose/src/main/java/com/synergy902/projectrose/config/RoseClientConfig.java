package com.synergy902.projectrose.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RoseClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue HUD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SHOW_PERSONAL_STATS;
    public static final ForgeConfigSpec.BooleanValue SHOW_BALANCE_WARNING;
    public static final ForgeConfigSpec.DoubleValue HUD_OPACITY;
    public static final ForgeConfigSpec.IntValue SCOREBOARD_Y_OFFSET;
    public static final ForgeConfigSpec.IntValue PERSONAL_STATS_X_OFFSET;
    public static final ForgeConfigSpec.IntValue PERSONAL_STATS_Y_OFFSET;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("hud");
        HUD_ENABLED = builder.define("enabled", true);
        SHOW_PERSONAL_STATS = builder.define("showPersonalStats", true);
        SHOW_BALANCE_WARNING = builder.define("showBalanceWarning", true);
        HUD_OPACITY = builder.defineInRange("panelOpacity", 0.85D, 0.20D, 1.0D);
        SCOREBOARD_Y_OFFSET = builder.defineInRange("scoreboardYOffset", 0, -500, 500);
        PERSONAL_STATS_X_OFFSET = builder.defineInRange("personalStatsXOffset", 0, -2000, 2000);
        PERSONAL_STATS_Y_OFFSET = builder.defineInRange("personalStatsYOffset", 0, -1000, 1000);
        builder.pop();
        SPEC = builder.build();
    }

    private RoseClientConfig() {
    }
}

