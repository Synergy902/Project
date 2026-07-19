package com.synergy902.projectrose.client;

import com.synergy902.projectrose.game.MatchPhase;
import com.synergy902.projectrose.game.RoseTeam;
import com.synergy902.projectrose.network.MatchSnapshot;

import java.util.List;

public final class ClientMatchState {
    private static MatchSnapshot snapshot = new MatchSnapshot(
            MatchPhase.WAITING,
            RoseTeam.NONE,
            -1,
            0,
            0,
            7500,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            false,
            RoseTeam.NONE,
            "default",
            -1,
            List.of(),
            List.of(),
            List.of("", "", "", "", "")
    );

    private static long victoryFadeStartedNanos;

    private ClientMatchState() {
    }

    public static MatchSnapshot snapshot() {
        return snapshot;
    }

    public static void update(MatchSnapshot updated) {
        if (updated.phase() == MatchPhase.POST_MATCH && snapshot.phase() != MatchPhase.POST_MATCH) {
            victoryFadeStartedNanos = System.nanoTime();
        }
        snapshot = updated;
    }

    public static float victoryFadeProgress() {
        if (snapshot.phase() != MatchPhase.POST_MATCH || victoryFadeStartedNanos == 0L) {
            return 0.0F;
        }
        double seconds = (System.nanoTime() - victoryFadeStartedNanos) / 1_000_000_000.0D;
        return (float) Math.max(0.0D, Math.min(1.0D, seconds / 1.5D));
    }
}
