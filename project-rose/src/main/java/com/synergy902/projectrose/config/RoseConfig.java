package com.synergy902.projectrose.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RoseConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue SCORE_LIMIT;
    public static final ForgeConfigSpec.IntValue POINTS_PER_KILL;
    public static final ForgeConfigSpec.IntValue TIME_LIMIT_SECONDS;
    public static final ForgeConfigSpec.IntValue START_COUNTDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue POST_MATCH_SECONDS;
    public static final ForgeConfigSpec.IntValue MAP_VOTE_SECONDS;
    public static final ForgeConfigSpec.IntValue MAP_VOTE_OPTIONS;
    public static final ForgeConfigSpec.IntValue NEXT_MAP_TEAM_SELECTION_SECONDS;
    public static final ForgeConfigSpec.BooleanValue AUTO_START;
    public static final ForgeConfigSpec.BooleanValue FRIENDLY_FIRE;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ITEM_DROPS;
    public static final ForgeConfigSpec.BooleanValue BLOCK_ITEM_PICKUPS;
    public static final ForgeConfigSpec.BooleanValue PROTECT_ARENA;
    public static final ForgeConfigSpec.IntValue SPAWN_PROTECTION_SECONDS;

    public static final ForgeConfigSpec.BooleanValue BALANCING_ENABLED;
    public static final ForgeConfigSpec.IntValue BALANCE_MIN_LARGER_TEAM_SIZE;
    public static final ForgeConfigSpec.IntValue BALANCE_MIN_SIZE_GAP;
    public static final ForgeConfigSpec.IntValue BALANCE_WARNING_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("team_deathmatch");
        SCORE_LIMIT = builder
                .comment("Team points required to win. Black Ops 1 Team Deathmatch uses 7500.")
                .defineInRange("scoreLimit", 7500, 100, 1000000);
        POINTS_PER_KILL = builder
                .comment("Team points awarded for an enemy elimination. Black Ops 1 uses 100.")
                .defineInRange("pointsPerKill", 100, 1, 10000);
        TIME_LIMIT_SECONDS = builder
                .comment("Match length in seconds. Ten minutes is 600 seconds.")
                .defineInRange("timeLimitSeconds", 600, 30, 86400);
        START_COUNTDOWN_SECONDS = builder
                .defineInRange("startCountdownSeconds", 5, 0, 60);
        POST_MATCH_SECONDS = builder
                .comment("Seconds the winner overlay remains before the map vote opens.")
                .defineInRange("postMatchSeconds", 5, 1, 120);
        MAP_VOTE_SECONDS = builder
                .comment("Seconds players have to vote for the next active map.")
                .defineInRange("mapVoteSeconds", 15, 5, 120);
        MAP_VOTE_OPTIONS = builder
                .comment("Maximum configured maps shown in one vote.")
                .defineInRange("mapVoteOptions", 5, 1, 10);
        NEXT_MAP_TEAM_SELECTION_SECONDS = builder
                .comment("Minimum team-selection window after a map vote before the next match can start automatically.")
                .defineInRange("nextMapTeamSelectionSeconds", 15, 0, 120);
        AUTO_START = builder
                .comment("When false, an operator must use /rose match start.")
                .define("autoStart", false);
        FRIENDLY_FIRE = builder.define("friendlyFire", false);
        BLOCK_ITEM_DROPS = builder.define("blockItemDrops", true);
        BLOCK_ITEM_PICKUPS = builder.define("blockItemPickups", true);
        PROTECT_ARENA = builder
                .comment("Prevent participants from breaking or placing blocks and taking items from map containers.")
                .define("protectArena", true);
        SPAWN_PROTECTION_SECONDS = builder
                .comment("Invulnerability after a match spawn. Firing a TACZ gun ends it immediately.")
                .defineInRange("spawnProtectionSeconds", 3, 0, 30);
        builder.pop();

        builder.push("pre_match_balancing");
        BALANCING_ENABLED = builder.define("enabled", true);
        BALANCE_MIN_LARGER_TEAM_SIZE = builder
                .comment("Balancing can begin only when the larger team has at least this many players.")
                .defineInRange("minimumLargerTeamSize", 5, 2, 1000);
        BALANCE_MIN_SIZE_GAP = builder
                .comment("Minimum difference between team sizes that begins the warning.")
                .defineInRange("minimumTeamSizeGap", 2, 1, 1000);
        BALANCE_WARNING_SECONDS = builder
                .defineInRange("warningSeconds", 8, 1, 60);
        builder.pop();

        SPEC = builder.build();
    }

    private RoseConfig() {
    }
}
